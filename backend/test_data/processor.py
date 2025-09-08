# import os
# import google.generativeai as genai
# from google.cloud import vision
# from PIL import Image
# import io
# import json
# import re
# from dotenv import load_dotenv

# #1. .env 파일 로드 (현재 파일 기준 부모 폴더에서 garakey.env를 찾음)
# backend_dir = os.path.dirname(os.path.dirname(__file__))
# dotenv_path = os.path.join(backend_dir, 'garakey.env')

# if os.path.exists(dotenv_path):
#     load_dotenv(dotenv_path)
#     print(f"✅ Environment variables loaded from: {dotenv_path}")
# else:
#     print(f"⚠️ Warning: '{dotenv_path}' not found.")

# # 2. Google Cloud 인증 설정 (상대 경로 사용)
# # .env 파일에서 상대 경로를 읽어와, 현재 환경에 맞는 절대 경로를 생성
# credential_path_from_env = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
# if credential_path_from_env:
#     full_credential_path = os.path.join(backend_dir, credential_path_from_env)
#     if os.path.exists(full_credential_path):
#         os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = full_credential_path
#         print("✅ Google Cloud credentials set dynamically.")
#     else:
#         print(f"❌ Error: Credential file not found at '{full_credential_path}'")
# else:
#     print("❌ Error: GOOGLE_APPLICATION_CREDENTIALS not set in .env file.")

# # 3. Gemini API 키 설정
# gemini_key = os.getenv("GEMINI_API_KEY")
# if gemini_key:
#     genai.configure(api_key=gemini_key)
#     print("✅ Gemini API key configured.")
#     print("GEMINI_API_KEY:", os.getenv("GEMINI_API_KEY"))
# else:
#     print("❌ Error: GEMINI_API_KEY not found in .env.")
#     print("GEMINI_API_KEY:", os.getenv("GEMINI_API_KEY"))

# class ReceiptProcessor:
#     def __init__(self):
#         self.vision_client = None
#         self.gemini_model = None
#         self.is_initialized = False

#     def initialize_clients(self):
#         if self.is_initialized:
#             return
#         print("Initializing Google Cloud clients...")
#         try:
#             self.vision_client = vision.ImageAnnotatorClient()
#             print("✅ Google Vision API client initialized.")
#             self.gemini_model = genai.GenerativeModel(model_name="gemini-1.5-flash")
#             print("✅ Google Gemini model initialized.")
#             self.is_initialized = True
#         except Exception as e:
#             print(f"❌ Client initialization failed: {e}")
#             self.is_initialized = False
#             raise

#     def _extract_text_from_image(self, image_data: bytes) -> str:
#         image = vision.Image(content=image_data)
#         response = self.vision_client.text_detection(image=image)
#         if response.error.message:
#             raise Exception(f"Vision API Error: {response.error.message}")
#         return response.text_annotations[0].description if response.text_annotations else ""

#     def _extract_json_from_text(self, text: str) -> dict:
#         match = re.search(r'\{.*\}', text, re.DOTALL)
#         if match:
#             try:
#                 return json.loads(match.group(0))
#             except json.JSONDecodeError as e:
#                 raise ValueError(f"Failed to parse JSON from model output: {e}")
#         raise ValueError("No JSON object found in the model output.")

#     def _structure_text_with_gemini(self, ocr_text: str) -> dict:
#         # Gemini API에 보낼 프롬프트입니다. JSON 형식으로 total_price만 요청합니다.
#         prompt = f'''당신은 영수증의 "OCR 결과"에서 **최종 결제 금액(total_price)**을 가장 정확하게 찾아내는 AI 전문가입니다.

# ### 가장 중요한 목표 ###
# - 수많은 금액 중에서 고객이 실제로 지불한 **단 하나의 최종 결제 총액**을 찾아 `total_price`에 할당해야 합니다.

# ### "total_price" 추출을 위한 구체적인 규칙 및 우선순위 ###
# 1.  아래 키워드가 보이면 **가장 높은 우선순위**로 그 값을 선택하세요.
#     - **1순위:** `받을금액`, `TOTAL`, `합계`, `총액`, `실결제금액`, `결제금액`, `카드승인`
# 2.  만약 위 키워드가 없다면, 영수증의 **가장 마지막에 위치한 가장 큰 숫자**를 `total_price`로 간주하세요.
# 3.  **절대 `total_price`로 선택해서는 안 되는 값들:**
#     - `과세물품`, `면세물품`, `공급가액`, `부가세(V.A.T)`, `할인금액`은 중간 계산 값이므로 무시하세요.

# ### 일반 정보 추출 지침 ###
# - 모든 정보는 반드시 "OCR 결과" 텍스트에 있는 실제 데이터를 사용해야 합니다.
# - 없는 정보는 빈 문자열("") 또는 `null` 값으로 표시합니다.
# - 모든 숫자 값(가격, 수량 등)은 콤마(,)와 원(₩) 표시를 제거하고 오직 숫자만으로 구성된 문자열로 만드세요. (예: "48360")
# - 날짜 및 시간은 "YYYY-MM-DD HH:MM:SS" 형식을 우선으로 시도하고, 어려우면 인식된 그대로 제공하세요.

# ### 요구되는 JSON 형식 ###
# {{
# "store_name": "가게 이름 (문자열)",
# "address": "가게 주소 (문자열)",
# "business_number": "사업자 번호 (문자열, 예: 123-45-67890)",
# "phone": "전화번호 (문자열, 예: 010-1234-5678)",
# "date": "거래 날짜 및 시간 (문자열, 예: 2024-08-17 11:31:45)",
# "total_price": "최종 결제 총액 (문자열, 예: 44444)",
# "items": [
# {{
# "name": "상품명1 (문자열)",
# "unit_price": "단가1 (문자열)",
# "quantity": "수량1 (문자열, 없으면 1)",
# "total": "상품1 총액 (문자열)"
# }}
# ]
# }}


# --- OCR 결과 시작 ---
# {ocr_text}
# --- OCR 결과 끝 ---

# 위 지침을 엄격하게 따라서, JSON 객체만 생성해주세요. 다른 설명은 절대 포함하지 마세요.

# 정리된 JSON:
#          '''
#         response = self.gemini_model.generate_content(prompt)
#         return self._extract_json_from_text(response.text)

#     def process_receipt_image(self, image_data: bytes) -> dict:
#         try:
#             self.initialize_clients()

#             print("Step 1: Performing OCR with Vision API.")
#             ocr_text = self._extract_text_from_image(image_data)
#             if not ocr_text.strip():
#                 raise ValueError("OCR failed or no text was detected in the image.")

#             print("Step 2: Structuring text with Gemini API.")
#             structured_data = self._structure_text_with_gemini(ocr_text)

#             return {"success": True, "data": structured_data}
#         except Exception as e:
#             print(f"Error during receipt processing: {e}")
#             return {"success": False, "error": str(e)}

# # 애플리케이션 전체에서 재사용할 단일 인스턴스 생성
# receipt_processor = ReceiptProcessor()