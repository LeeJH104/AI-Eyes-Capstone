#!/usr/bin/env python3
# -*- coding: utf-8 -*- 테스트 1

"""영수증 분석 API 라우트"""

from flask import Blueprint, request, jsonify
from . import processor

# 블루프린트 생성
receipt_bp = Blueprint('receipt_bp', __name__)

@receipt_bp.route('/process-receipt', methods=['POST'])
def process_receipt_route():
    """    영수증 이미지를 처리하는 메인 API 엔드포인트입니다.    HTTP POST 요청으로 'image'라는 이름의 이미지 파일을 받습니다.    """
    print("API: 이미지 수신 완료. 처리 시작...")
    # 1. 요청에 'image' 파일이 있는지 확인합니다.
    if 'image' not in request.files:
        return jsonify({"error": "No image file provided"}), 400
    
    try:
        file = request.files['image']
        image_bytes = file.read()
        
        # 2. OCR을 수행하여 이미지에서 텍스트를 추출합니다.
        ocr_text = processor.perform_ocr(image_bytes)
        if not ocr_text:
            return jsonify({"error": "OCR failed to detect text"}), 500
            
        # 3. Gemma를 사용하여 추출된 텍스트의 구조를 정의합니다.
        structured_data = processor.structure_text_with_gemma(ocr_text)
        if not structured_data:
            return jsonify({"error": "Failed to structure data with LLM"}), 500
            
        print("API: Process completed successfully.")
        # 성공적으로 처리된 JSON 결과를 클라이언트(안드로이드 앱)에 반환합니다.
        return jsonify({"success": True, "data": structured_data})

    except Exception as e:
        # 그 외 예측하지 못한 서버 내부 오류
        print(f"API: 예측하지 못한 오류 발생 - {e}")
        return jsonify({"error": "서버 내부에서 오류가 발생했습니다."}), 500