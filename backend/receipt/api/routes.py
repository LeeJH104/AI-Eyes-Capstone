#!/usr/bin/env python3
# -*- coding: utf-8 -*- 테스트 1

"""영수증 분석 API 라우트"""

from flask import Blueprint, request, jsonify
from . import processor_gemma_ver as processor

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

        # OCR + LLM 구조화까지 한 번에 처리
        result = processor.receipt_processor.process_receipt_image(image_bytes)
        if not result.get("success"):
            return jsonify({"error": result.get("error", "Receipt processing failed")}), 500

        structured_data = result.get("data")
        if not structured_data:
            return jsonify({"error": "Failed to structure data with LLM"}), 500

        print("API: Process completed successfully.")
        return jsonify({"success": True, "data": structured_data})

    except Exception as e:
        print(f"API: 예측하지 못한 오류 발생 - {e}")
        return jsonify({"error": "서버 내부에서 오류가 발생했습니다."}), 500