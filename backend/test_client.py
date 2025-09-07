#!/usr/bin/env python3            print(response.json())        except requests.exceptions.JSONDecodeError:            print(response.text)    except Exception as e:        print(f"클라이언트 오류 발생: {e}")
# -*- coding: utf-8 -*-
"""Flask 서버 API 테스트 클라이언트

Postman을 대신하여 서버에 직접 요청을 보냅니다.
"""

import requests
import os

# --- 설정 ---
# 서버 주소와 포트를 확인하세요.
SERVER_URL = "https://BoringStarG.pythonanywhere.com/api/receipt/process-receipt"

# 테스트에 사용할 이미지 파일의 경로를 지정하세요.
# 이 스크립트(test_client.py)는 backend 폴더에 있으므로,
# test_data 폴더까지의 상대 경로를 사용합니다.
IMAGE_PATH = os.path.join(os.path.dirname(__file__), 'test_data', '영수증 이미지 1.jpg')

print(f"서버에 요청을 보냅니다: {SERVER_URL}")
print(f"사용할 이미지 파일: {IMAGE_PATH}")

if not os.path.exists(IMAGE_PATH):
    print(f"오류: 테스트 이미지 파일을 찾을 수 없습니다. '{IMAGE_PATH}'")
else:
    try:
        with open(IMAGE_PATH, 'rb') as f:
            files = {'image': (os.path.basename(IMAGE_PATH), f, 'image/jpeg')}
            response = requests.post(SERVER_URL, files=files, timeout=600)

        print("--- 서버 응답 ---")
        print(f"상태 코드: {response.status_code}")
        print("응답 내용 (JSON):")
        try:
            print(response.json())
        except requests.exceptions.JSONDecodeError:
            print(response.text)

    except Exception as e:
        print(f"클라이언트 오류 발생: {e}")