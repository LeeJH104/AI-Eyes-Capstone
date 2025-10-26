# -*- coding: utf-8 -*-
"""Flask 서버 API 테스트 클라이언트

Postman을 대신하여 서버에 직접 요청을 보냅니다.
"""

import requests
import os

# --- 서버 주소 설정 ---
# SERVER_URL = "http://192.168.0.215:5000/api/receipt/process-receipt"
SERVER_URL = "https://62b2d7a86c54.ngrok-free.app/api/receipt/process-receipt"
# 테스트용 이미지 경로
IMAGE_PATH = os.path.join(os.path.dirname(__file__), 'test_data', '영수증 이미지 1.jpg')

print(f"서버에 요청을 보냅니다: {SERVER_URL}")
print(f"사용할 이미지 파일: {IMAGE_PATH}")

if not os.path.exists(IMAGE_PATH):
    print(f"❌ 오류: 테스트 이미지 파일을 찾을 수 없습니다. '{IMAGE_PATH}'")
else:
    try:
        with open(IMAGE_PATH, 'rb') as f:
            files = {'image': (os.path.basename(IMAGE_PATH), f, 'image/jpeg')}
            response = requests.post(SERVER_URL, files=files, timeout=600)

        print("\n--- 서버 응답 ---")
        print(f"상태 코드: {response.status_code}")
        try:
            print("응답 내용 (JSON):", response.json())
        except requests.exceptions.JSONDecodeError:
            print("응답 내용 (RAW):", response.text)

    except Exception as e:
        print(f"❌ 클라이언트 오류 발생: {e}")
