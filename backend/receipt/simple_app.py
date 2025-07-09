#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import io
import json
import base64
from PIL import Image
import torch

# 환경 설정
from dotenv import load_dotenv
from google.cloud import vision
from transformers import AutoTokenizer, Gemma3ForConditionalGeneration

app = Flask(__name__)
CORS(app)

# 환경 변수 로드
load_dotenv("garakey.env")

# 전역 변수로 모델들 저장
vision_client = None
gemma_model = None
gemma_tokenizer = None

def init_models():
    """모델 초기화"""
    global vision_client, gemma_model, gemma_tokenizer
    
    try:
        # Google Vision API
        vision_client = vision.ImageAnnotatorClient()
        print("✅ Vision API 초기화 완료")
        
        # Gemma 모델
        model_id = "google/gemma-3-4b-it"
        gemma_tokenizer = AutoTokenizer.from_pretrained(model_id)
        
        device = "cuda" if torch.cuda.is_available() else "cpu"
        dtype = torch.bfloat16 if torch.cuda.is_available() else torch.float32
        
        gemma_model = Gemma3ForConditionalGeneration.from_pretrained(
            model_id,
            torch_dtype=dtype,
            device_map="auto",
            load_in_4bit=True,
        )
        print(f"✅ Gemma 모델 초기화 완료 ({device})")
        
    except Exception as e:
        print(f"❌ 모델 초기화 오류: {e}")

def ocr_image(image_bytes):
    """이미지 OCR 처리"""
    try:
        image = vision.Image(content=image_bytes)
        response = vision_client.text_detection(image=image)
        
        if response.text_annotations:
            return response.text_annotations[0].description
        return None
    except Exception as e:
        print(f"OCR 오류: {e}")
        return None

def extract_json(ocr_text):
    """OCR 텍스트를 JSON으로 변환"""
    try:
        prompt = f"""다음 영수증 텍스트를 JSON으로 변환해주세요:

{ocr_text}

JSON 형식:
{{
  "store_name": "상점명",
  "total_price": "총액",
  "date": "날짜",
  "items": [
    {{"name": "상품명", "price": "가격"}}
  ]
}}

JSON:"""

        inputs = gemma_tokenizer(prompt, return_tensors="pt", truncation=True)
        
        with torch.no_grad():
            outputs = gemma_model.generate(
                **inputs,
                max_new_tokens=512,
                temperature=0.1,
                do_sample=True
            )
        
        # 생성된 텍스트 추출
        generated = gemma_tokenizer.decode(outputs[0][inputs.input_ids.shape[1]:], skip_special_tokens=True)
        
        # JSON 부분만 추출
        start = generated.find('{')
        end = generated.rfind('}') + 1
        
        if start != -1 and end > start:
            json_str = generated[start:end]
            return json.loads(json_str)
        
        return None
    except Exception as e:
        print(f"JSON 변환 오류: {e}")
        return None

@app.route('/')
def home():
    return jsonify({"message": "영수증 분석 서버", "status": "running"})

@app.route('/analyze', methods=['POST'])
def analyze():
    try:
        # 파일 업로드 처리
        if 'image' in request.files:
            file = request.files['image']
            image_bytes = file.read()
        # Base64 처리
        elif request.json and 'image' in request.json:
            image_data = request.json['image']
            if ',' in image_data:
                image_data = image_data.split(',')[1]
            image_bytes = base64.b64decode(image_data)
        else:
            return jsonify({"error": "이미지가 없습니다"}), 400
        
        # OCR 처리
        ocr_text = ocr_image(image_bytes)
        if not ocr_text:
            return jsonify({"error": "텍스트를 인식할 수 없습니다"}), 400
        
        # JSON 변환
        result = extract_json(ocr_text)
        if not result:
            return jsonify({"error": "데이터 추출 실패", "ocr_text": ocr_text}), 400
        
        return jsonify({
            "success": True,
            "data": result,
            "ocr_text": ocr_text
        })
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("🚀 서버 시작 중...")
    init_models()
    print("🌐 서버 실행: http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)
