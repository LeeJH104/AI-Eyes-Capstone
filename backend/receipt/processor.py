#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
영수증 이미지 분석 및 OCR 처리 모듈
Google Vision API와 Gemma LLM을 활용한 영수증 데이터 추출
"""

import torch
# Windows 환경에서 Triton 호환성 문제 해결을 위한 설정
import os
os.environ["TORCH_LOGS"] = ""  # 로그 비활성화
os.environ["TORCHDYNAMO_DISABLE"] = "1"  # 동적 컴파일 완전 비활성화

import io
import json
import base64
import re
from PIL import Image
from google.cloud import vision
from transformers import AutoTokenizer, AutoModelForCausalLM, Gemma3ForConditionalGeneration, BitsAndBytesConfig
from typing import Dict, Any, Optional


# --- Model and Tokenizer Loading ---
# This section should be executed only once when the application starts.
gemma_model_id = os.getenv("GEMMA_MODEL_ID", "google/gemma-3-4b-it")
gemma_tokenizer = None
gemma_model = None

def load_models():
    """Loads the Gemma tokenizer and model."""
    global gemma_tokenizer, gemma_model
    if gemma_tokenizer is None or gemma_model is None:
        print(f"Loading Gemma model: {gemma_model_id}")
        try:
            gemma_tokenizer = AutoTokenizer.from_pretrained(gemma_model_id)
            
            # Determine device and dtype
            device = "cuda" if torch.cuda.is_available() else "cpu"
            # Correctly check for bfloat16 support
            dtype = torch.bfloat16 if torch.cuda.is_available() and torch.cuda.is_bf16_supported() else torch.float16 if torch.cuda.is_available() else torch.float32

            print(f"Using device: {device}, dtype: {dtype}")

            # 4비트 양자화를 사용하지 않고 속도 우선으로 모델 로드
            gemma_model = AutoModelForCausalLM.from_pretrained(
                gemma_model_id,
                torch_dtype=dtype, # 4비트 양자화 대신 원본 dtype 사용
                device_map="auto",
            )
            print(f"Gemma model loaded successfully on device: {gemma_model.device}")
        except Exception as e:
            print(f"Error loading Gemma model: {e}")
            # Handle model loading failure
            raise


def get_ocr_text(image_content: bytes) -> str:
    """
    Performs OCR on the given image content using Google Cloud Vision API.
    """
    try:
        # The client will automatically use the credentials from the environment variable
        vision_client = vision.ImageAnnotatorClient()
        image = vision.Image(content=image_content)
        response = vision_client.text_detection(image=image)
        
        if response.error.message:
            raise Exception(f"Vision API Error: {response.error.message}")
        
        if response.text_annotations:
            return response.text_annotations[0].description
        return ""
    except Exception as e:
        print(f"Error in Google Cloud Vision API call: {e}")
        raise


def extract_json_from_text(text: str) -> dict:
    """
    Cleans the raw output from the LLM to extract a valid JSON object.
    """
    # Find text between ```json and ```
    match = re.search(r"```json\s*([\s\S]+?)\s*```", text, re.DOTALL)
    if match:
        text = match.group(1)
    else:
        # If no json block, find the outermost curly braces
        first_brace = text.find('{')
        last_brace = text.rfind('}')
        if first_brace != -1 and last_brace != -1:
            text = text[first_brace:last_brace+1]
        else:
            raise ValueError("No JSON object found in the model output.")

    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON: {e}")
        print(f"Problematic JSON string: {text}")
        raise ValueError("Failed to parse JSON from model output.") from e


def structure_text_with_gemma(ocr_text: str) -> dict:
    """
    Uses the Gemma model to structure the OCR text into a JSON format.
    """
    if gemma_model is None or gemma_tokenizer is None:
        raise RuntimeError("Gemma model is not loaded. Call load_models() first.")

    prompt = f'''당신은 제공된 "OCR 결과" 텍스트에서 정보를 추출하여 지정된 JSON 형식으로 정리하는 AI 어시스턴트입니다.
가장 중요한 목표는 영수증의 **최종 결제 총액 (total_price)**을 정확하게 추출하는 것입니다.
다른 모든 정보도 중요하지만, **total_price**는 반드시 포함되어야 합니다.

**정보 추출 지침:**
*   **반드시 "OCR 결과" 텍스트에 있는 실제 데이터를 사용해야 합니다.** 없는 정보는 추측하거나 임의로 만들지 마세요.
*   존재하지 않는 정보는 빈 문자열("") 또는 JSON 형식에 따라 `null` 값으로 표시합니다.
*   숫자 값(가격, 수량 등)은 콤마(,)를 제거하고 숫자 형태의 문자열로 표현해주세요 (예: "48360").
*   날짜 및 시간은 "YYYY-MM-DD HH:MM:SS" 형식을 최우선으로 시도하고, 어려우면 OCR 결과에 인식된 그대로 제공해주세요.
*   상품 목록(items)에서 단가(unit_price), 수량(quantity), 합계(total)는 숫자 형태의 문자열로, 상품명(name)은 문자열로 추출해주세요.
*   수량이 명시되지 않은 경우 기본값으로 "1"을 사용합니다.

**요구되는 JSON 형식 예시:**
```json
{{
  "store_name": "가게 이름 (문자열)",
  "address": "가게 주소 (문자열)",
  "business_number": "사업자 번호 (문자열, 예: 123-45-67890)",
  "phone": "전화번호 (문자열, 예: 010-1234-5678)",
  "date": "거래 날짜 및 시간 (문자열, 예: 2024-08-17 11:31:45)",
  "total_price": "영수증의 최종 결제 총액 (숫자 문자열, 예: 48360)",
  "items": [
    {{
      "name": "상품명1 (문자열)",
      "unit_price": "단가1 (숫자 문자열)",
      "quantity": "수량1 (숫자 문자열, 없으면 1)",
      "total": "상품1 총액 (숫자 문자열)"
    }},
    {{
      "name": "상품명2 (문자열)",
      "unit_price": "단가2 (숫자 문자열)",
      "quantity": "수량2 (숫자 문자열)",
      "total": "상품2 총액 (숫자 문자열)"
    }}
  ]
}}
```

--- OCR 결과 시작 ---
{ocr_text}
--- OCR 결과 끝 ---

위 "OCR 결과"를 바탕으로 JSON 객체를 생성해주세요.
응답은 다른 설명 없이 순수한 JSON 객체만 포함해야 합니다.
**특히, `total_price` 필드에는 영수증에 명시된 사용자가 실제로 지불해야 하는 최종 금액을 정확히 기입해야 합니다.**

정리된 JSON:
'''
    
    # The prompt is part of the input, so the model output will contain it.
    # We need to decode the full output and then separate the generated part.
    inputs = gemma_tokenizer(prompt, return_tensors="pt").to(gemma_model.device)
    
    # Windows 호환성을 위해 torch.no_grad()로 최적화 기능 비활성화
    with torch.no_grad():
        outputs = gemma_model.generate(**inputs, max_new_tokens=1024, temperature=0.0, do_sample=False)
    
    # Decode the output
    generated_text = gemma_tokenizer.decode(outputs[0], skip_special_tokens=True)
    
    # The generated text includes the prompt, so we need to remove it.
    # We find the start of the actual response, which is after the prompt.
    response_start_marker = "정리된 JSON:"
    response_start_index = generated_text.rfind(response_start_marker)
    if response_start_index != -1:
        response_text = generated_text[response_start_index + len(response_start_marker):].strip()
    else:
        # Fallback if the marker is not found in the output (less reliable)
        response_text = generated_text[len(prompt):]

    return extract_json_from_text(response_text)


def process_receipt_image(image_content: bytes) -> dict:
    """
    Main function to process a receipt image.
    It performs OCR and then uses Gemma to structure the data.
    """
    print("Step 1: Performing OCR on the image.")
    ocr_text = get_ocr_text(image_content)
    if not ocr_text.strip():
        raise ValueError("OCR failed or no text was detected in the image.")
    
    print("Step 2: Structuring OCR text with Gemma model.")
    structured_data = structure_text_with_gemma(ocr_text)
    
    print("Step 3: Processing complete.")
    return structured_data


class ReceiptProcessor:
    """영수증 분석 처리 클래스"""
    
    def __init__(self):
        self.vision_client = None
        self.gemma_model = None
        self.gemma_tokenizer = None
        self.is_initialized = False
        
    def initialize_models(self):
        """모델 초기화"""
        if self.is_initialized:
            return True
            
        try:
            # Google Vision API 클라이언트 초기화
            self.vision_client = vision.ImageAnnotatorClient()
            print("✅ Google Vision API 초기화 완료")
            
            # Gemma 모델 초기화
            model_id = os.getenv("GEMMA_MODEL_ID", "google/gemma-3-4b-it")
            self.gemma_tokenizer = AutoTokenizer.from_pretrained(model_id)
            
            device = "cuda" if torch.cuda.is_available() else "cpu"
            dtype = torch.bfloat16 if torch.cuda.is_available() else torch.float32
            
            self.gemma_model = Gemma3ForConditionalGeneration.from_pretrained(
                model_id,
                torch_dtype=dtype,
                device_map="auto",
                load_in_4bit=True,
            )
            print(f"✅ Gemma 모델 초기화 완료 (Device: {device})")
            
            self.is_initialized = True
            return True
            
        except Exception as e:
            print(f"❌ 모델 초기화 실패: {e}")
            return False
    
    def extract_text_from_image(self, image_data: bytes) -> str:
        """Google Vision API를 사용한 OCR 텍스트 추출"""
        try:
            if not self.vision_client:
                raise Exception("Vision API 클라이언트가 초기화되지 않았습니다.")
                
            image = vision.Image(content=image_data)
            response = self.vision_client.text_detection(image=image)
            
            if response.error.message:
                raise Exception(f"Vision API 오류: {response.error.message}")
                
            texts = response.text_annotations
            if texts:
                return texts[0].description
            else:
                return ""
                
        except Exception as e:
            print(f"OCR 추출 오류: {e}")
            return ""
    
    def analyze_with_gemma(self, ocr_text: str) -> Dict[str, Any]:
        """Gemma LLM을 사용한 영수증 데이터 구조화"""
        try:
            if not self.gemma_model or not self.gemma_tokenizer:
                raise Exception("Gemma 모델이 초기화되지 않았습니다.")
                
            prompt = f"""
다음은 영수증에서 추출한 텍스트입니다. 이 정보를 JSON 형태로 구조화해주세요.

OCR 텍스트:
{ocr_text}

다음 형식으로 결과를 제공해주세요:
{{
    "store_name": "상점명",
    "address": "주소",
    "business_number": "사업자번호",
    "phone": "전화번호",
    "date": "날짜 및 시간",
    "total_price": "총 금액",
    "items": [
        {{
            "name": "상품명",
            "unit_price": "단가",
            "quantity": "수량",
            "total": "소계"
        }}
    ]
}}

정확한 JSON 형식으로만 응답해주세요.
"""

            # 토큰화 및 모델 실행
            inputs = self.gemma_tokenizer(prompt, return_tensors="pt")
            
            with torch.no_grad():
                outputs = self.gemma_model.generate(
                    **inputs,
                    max_new_tokens=512,
                    temperature=0.1,
                    do_sample=True,
                    pad_token_id=self.gemma_tokenizer.eos_token_id
                )
            
            # 응답 디코딩
            response = self.gemma_tokenizer.decode(
                outputs[0][inputs['input_ids'].shape[1]:], 
                skip_special_tokens=True
            )
            
            # JSON 파싱 시도
            try:
                # 응답에서 JSON 부분 추출
                start_idx = response.find('{')
                end_idx = response.rfind('}') + 1
                
                if start_idx != -1 and end_idx != 0:
                    json_str = response[start_idx:end_idx]
                    return json.loads(json_str)
                else:
                    raise ValueError("JSON 형식을 찾을 수 없습니다.")
                    
            except json.JSONDecodeError:
                print(f"JSON 파싱 실패. 원본 응답: {response}")
                return self._get_fallback_data(ocr_text)
                
        except Exception as e:
            print(f"Gemma 분석 오류: {e}")
            return self._get_fallback_data(ocr_text)
    
    def _get_fallback_data(self, ocr_text: str) -> Dict[str, Any]:
        """분석 실패 시 기본 데이터 반환"""
        return {
            "store_name": "인식 실패",
            "address": "주소 정보 없음",
            "business_number": "",
            "phone": "",
            "date": "",
            "total_price": "0",
            "items": [
                {
                    "name": "OCR 텍스트 전체",
                    "unit_price": "",
                    "quantity": "1",
                    "total": "",
                    "raw_text": ocr_text[:500]  # 처음 500자만
                }
            ],
            "error": "자동 분석에 실패했습니다. 수동으로 확인해주세요."
        }
    
    def process_receipt_image(self, image_data: bytes) -> Dict[str, Any]:
        """영수증 이미지 전체 처리 파이프라인"""
        try:
            # 모델 초기화 확인
            if not self.initialize_models():
                return {"error": "모델 초기화에 실패했습니다."}
            
            # 1단계: OCR 텍스트 추출
            print("1단계: OCR 텍스트 추출 중...")
            ocr_text = self.extract_text_from_image(image_data)
            
            if not ocr_text:
                return {"error": "이미지에서 텍스트를 추출할 수 없습니다."}
            
            # 2단계: 텍스트 구조화
            print("2단계: AI 분석 중...")
            structured_data = self.analyze_with_gemma(ocr_text)
            
            # 3단계: 결과 반환
            return {
                "success": True,
                "ocr_text": ocr_text,
                "data": structured_data,
                "processing_steps": [
                    "Google Vision API로 OCR 수행",
                    "Gemma LLM으로 데이터 구조화",
                    "결과 검증 및 정리"
                ]
            }
            
        except Exception as e:
            return {"error": f"영수증 처리 중 오류: {str(e)}"}


# 전역 인스턴스
receipt_processor = ReceiptProcessor()


def process_receipt_from_file(image_file) -> Dict[str, Any]:
    """파일 객체로부터 영수증 처리"""
    try:
        image_data = image_file.read()
        return receipt_processor.process_receipt_image(image_data)
    except Exception as e:
        return {"error": f"파일 처리 오류: {str(e)}"}


def process_receipt_from_base64(base64_data: str) -> Dict[str, Any]:
    """Base64 이미지로부터 영수증 처리"""
    try:
        # Base64 디코딩
        if ',' in base64_data:
            base64_data = base64_data.split(',')[1]
        
        image_data = base64.b64decode(base64_data)
        return receipt_processor.process_receipt_image(image_data)
        
    except Exception as e:
        return {"error": f"Base64 처리 오류: {str(e)}"}
