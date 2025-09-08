#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
영수증 이미지 분석 및 OCR 처리 모듈 (구조 단일화 최종 버전)
Google Vision API와 Gemma LLM을 활용한 영수증 데이터 추출
"""

# 1. torch를 가장 먼저 임포트하여 초기화 충돌 방지
import torch

# 2. Windows 환경에서 발생 가능한 호환성 문제 해결을 위한 설정
import os
os.environ.pop("TORCH_LOGS", None)
os.environ["TORCHDYNAMO_DISABLE"] = "1"
os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = os.path.join(os.path.dirname(__file__), "chinamoney-fb6772e4457f.json")

# 3. 나머지 필수 라이브러리 임포트
import json
import base64
import re
from PIL import Image
from google.cloud import vision
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
from typing import Dict, Any

class ReceiptProcessor:
    """
    영수증 분석을 위한 단일 처리 클래스.
    모든 관련 로직을 캡슐화하여 관리합니다.
    """

    def __init__(self):
        """초기화: 모든 모델과 클라이언트를 None으로 설정하고 초기화 상태를 False로 지정"""
        self.vision_client = None
        self.gemma_model = None
        self.gemma_tokenizer = None
        self.is_initialized = False

    def initialize_models(self):
        """
        필요한 모든 모델과 API 클라이언트를 초기화합니다.
        서버 시작 후 첫 요청 시에만 실행되며, 이후에는 중복 실행되지 않습니다.
        """
        if self.is_initialized:
            return

        print("Initializing models for the first time...")
        try:
            # Google Vision API 클라이언트 초기화
            self.vision_client = vision.ImageAnnotatorClient()
            print("✅ Google Vision API client initialized.")

            # Gemma 모델 및 토크나이저 초기화
            model_id = os.getenv("GEMMA_MODEL_ID", "google/gemma-3-4b-it")
            print(f"✅ Loading Gemma model: {model_id}")

            self.gemma_tokenizer = AutoTokenizer.from_pretrained(model_id)

            # 디바이스 및 데이터 타입 결정 (GPU 우선)
            device = "cuda" if torch.cuda.is_available() else "cpu"
            dtype = torch.bfloat16 if torch.cuda.is_available() and torch.cuda.is_bf16_supported() else torch.float32

            print(f"✅ Using device: {device}, dtype: {dtype}")

            # 4비트 양자화 설정 (GPU 메모리 사용량 절약)
            quantization_config = BitsAndBytesConfig(load_in_4bit=True)

            # AutoModelForCausalLM을 사용하여 모델 로드 (Gemma3ForConditionalGeneration은 잘못된 클래스명)
            self.gemma_model = AutoModelForCausalLM.from_pretrained(
                model_id,
                torch_dtype=dtype,
                quantization_config=quantization_config, # 4비트 양자화 적용
                device_map="auto", # 자동으로 GPU/CPU 할당
            )
            print(f"✅ Gemma model loaded successfully on device: {self.gemma_model.device}")

            self.is_initialized = True
            print("✅ All models initialized successfully.")

        except Exception as e:
            print(f"❌ Model initialization failed: {e}")
            # 초기화 실패 시 is_initialized를 False로 유지하여 다음 요청에서 재시도할 수 있도록 함
            self.is_initialized = False
            raise  # 오류를 다시 발생시켜 호출한 쪽(Flask)에서 처리하도록 함

    def _extract_text_from_image(self, image_data: bytes) -> str:
        """[내부 함수] Google Vision API를 사용하여 이미지에서 OCR 텍스트를 추출"""
        if not self.vision_client:
            raise RuntimeError("Vision API client is not initialized.")
        
        image = vision.Image(content=image_data)
        response = self.vision_client.text_detection(image=image)

        if response.error.message:
            raise Exception(f"Vision API Error: {response.error.message}")
        
        return response.text_annotations[0].description if response.text_annotations else ""

    def _extract_json_from_text(self, text: str) -> dict:
        """[내부 함수] LLM 응답 텍스트에서 순수 JSON 객체를 안정적으로 추출"""
        # `````` 코드 블록 먼저 검색
        match = re.search(r"``````", text, re.DOTALL)
        if match:
            clean_text = match.group(1)
        else:
            # 코드 블록이 없으면 가장 바깥쪽 중괄호로 감싸인 부분을 찾음
            first_brace = text.find('{')
            last_brace = text.rfind('}')
            if first_brace != -1 and last_brace != -1:
                clean_text = text[first_brace:last_brace + 1]
            else:
                raise ValueError("No JSON object found in the model output.")

        try:
            return json.loads(clean_text)
        except json.JSONDecodeError as e:
            print(f"Failed to parse JSON: {e}")
            print(f"Problematic JSON string: {clean_text}")
            raise ValueError("Failed to parse JSON from model output.") from e

    def _structure_text_with_gemma(self, ocr_text: str) -> dict:
        """[내부 함수] Gemma LLM을 사용하여 OCR 텍스트를 JSON으로 구조화"""
        if not self.gemma_model or not self.gemma_tokenizer:
            raise RuntimeError("Gemma model is not initialized.")

        # 가장 상세하고 정확했던 프롬프트로 통합
        prompt = f'''당신은 영수증의 "OCR 결과"에서 **최종 결제 금액(total_price)**을 가장 정확하게 찾아내는 AI 전문가입니다.

### 가장 중요한 목표 ###
- 수많은 금액 중에서 고객이 실제로 지불한 **단 하나의 최종 결제 총액**을 찾아 `total_price`에 할당해야 합니다.

### "total_price" 추출을 위한 구체적인 규칙 및 우선순위 ###
1.  아래 키워드가 보이면 **가장 높은 우선순위**로 그 값을 선택하세요.
    - **1순위:** `받을금액`, `TOTAL`, `합계`, `총액`, `실결제금액`, `결제금액`, `카드승인`
2.  만약 위 키워드가 없다면, 영수증의 **가장 마지막에 위치한 가장 큰 숫자**를 `total_price`로 간주하세요.
3.  **절대 `total_price`로 선택해서는 안 되는 값들:**
    - `과세물품`, `면세물품`, `공급가액`, `부가세(V.A.T)`, `할인금액`은 중간 계산 값이므로 무시하세요.

### 일반 정보 추출 지침 ###
- 모든 정보는 반드시 "OCR 결과" 텍스트에 있는 실제 데이터를 사용해야 합니다.
- 없는 정보는 빈 문자열("") 또는 `null` 값으로 표시합니다.
- 모든 숫자 값(가격, 수량 등)은 콤마(,)와 원(₩) 표시를 제거하고 오직 숫자만으로 구성된 문자열로 만드세요. (예: "48360")
- 날짜 및 시간은 "YYYY-MM-DD HH:MM:SS" 형식을 우선으로 시도하고, 어려우면 인식된 그대로 제공하세요.

### 요구되는 JSON 형식 ###
{{
"store_name": "가게 이름 (문자열)",
"address": "가게 주소 (문자열)",
"business_number": "사업자 번호 (문자열, 예: 123-45-67890)",
"phone": "전화번호 (문자열, 예: 010-1234-5678)",
"date": "거래 날짜 및 시간 (문자열, 예: 2024-08-17 11:31:45)",
"total_price": "최종 결제 총액 (문자열, 예: 44444)",
"items": [
{{
"name": "상품명1 (문자열)",
"unit_price": "단가1 (문자열)",
"quantity": "수량1 (문자열, 없으면 1)",
"total": "상품1 총액 (문자열)"
}}
]
}}


--- OCR 결과 시작 ---
{ocr_text}
--- OCR 결과 끝 ---

위 지침을 엄격하게 따라서, JSON 객체만 생성해주세요. 다른 설명은 절대 포함하지 마세요.

정리된 JSON:
'''
# {{
# "store_name": "가게 이름 (문자열)",
# "address": "가게 주소 (문자열)",
# "business_number": "사업자 번호 (문자열, 예: 123-45-67890)",
# "phone": "전화번호 (문자열, 예: 010-1234-5678)",
# "date": "거래 날짜 및 시간 (문자열, 예: 2024-08-17 11:31:45)",
# "total_price": "영수증의 최종 결제 총액(예: 합계, 총액, 결제금액, 총구매금액, 총결제금액, 실결제금액, 실납부금액, 실지불금액) (숫자 문자열, 예: 48360)",
# "items": [
# {{
# "name": "상품명1 (문자열)",
# "unit_price": "단가1 (숫자 문자열)",
# "quantity": "수량1 (숫자 문자열, 없으면 1)",
# "total": "상품1 총액 (숫자 문자열)"
# }}
        inputs = self.gemma_tokenizer(prompt, return_tensors="pt").to(self.gemma_model.device)
        
        # no_grad() 컨텍스트 안에서 실행하여 메모리 사용량 최적화
        with torch.no_grad():
            outputs = self.gemma_model.generate(**inputs, max_new_tokens=1024, temperature=0.0, do_sample=False)
        
        generated_text = self.gemma_tokenizer.decode(outputs[0], skip_special_tokens=True)
        
        # 프롬프트를 제외한 순수 응답 부분만 추출
        response_start_marker = "정리된 JSON:"
        response_start_index = generated_text.rfind(response_start_marker)
        if response_start_index != -1:
            response_text = generated_text[response_start_index + len(response_start_marker):].strip()
        else: # 마커를 찾지 못할 경우의 예외 처리
            response_text = generated_text[len(prompt):]

        return self._extract_json_from_text(response_text)

    def process_receipt_image(self, image_data: bytes) -> Dict[str, Any]:
        """[클래스의 메인 진입점] 영수증 이미지 전체 처리 파이프라인"""
        try:
            # 1단계: 모델 초기화 (최초 1회만 실행됨)
            self.initialize_models()

            # 2단계: OCR 텍스트 추출
            print("Step 1: Performing OCR on the image.")
            ocr_text = self._extract_text_from_image(image_data)
            if not ocr_text.strip():
                raise ValueError("OCR failed or no text was detected in the image.")
            
            # 3단계: OCR 텍스트 구조화
            print("Step 2: Structuring OCR text with Gemma model.")
            structured_data = self._structure_text_with_gemma(ocr_text)

            print("Step 3: Processing complete.")
            return {
                "success": True,
                "data": structured_data
            }

        except Exception as e:
            print(f"Error during receipt processing: {e}")
            # 어떤 단계에서든 오류 발생 시 일관된 형식의 오류 메시지 반환
            return {"success": False, "error": str(e)}

# --- 전역 인스턴스 및 외부 호출용 함수 ---

# ReceiptProcessor의 전역 인스턴스를 하나만 생성하여 애플리케이션 전체에서 재사용
# 이렇게 하면 모델을 여러 번 로드하지 않아 메모리와 시간을 절약할 수 있음
receipt_processor = ReceiptProcessor()

def process_receipt_from_base64(base64_data: str) -> Dict[str, Any]:
    """[외부 호출용] Base64로 인코딩된 이미지 데이터를 받아 처리하는 함수"""
    try:
        # 데이터 URL 형식(e.g., "data:image/png;base64,iVBOR...") 처리
        if ',' in base64_data:
            base64_data = base64_data.split(',')[1]
        
        image_data = base64.b64decode(base64_data)
        
        # 전역 인스턴스의 메인 처리 함수를 호출
        return receipt_processor.process_receipt_image(image_data)
        
    except Exception as e:
        return {"success": False, "error": f"Base64 decoding or processing error: {str(e)}"}