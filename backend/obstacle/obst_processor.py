import google.generativeai as genai
from PIL import Image
import io
import base64

# Gemini API 키 설정
# 실제 키는 .env 파일 같은 곳에 안전하게 보관하는 것이 좋습니다.
genai.configure(api_key="YOUR_GEMINI_API_KEY") 

model = genai.GenerativeModel(model_name="gemini-1.5-flash")

PROMPTS = {
    1: """
        당신은 시각장애인을 위한 보행 보조 AI입니다. 사용자는 빠르게 움직이고 있습니다.
        가장 즉각적이고 치명적인 위험 요소 하나와 목표 지점 하나만 식별하세요.
        '거리', '방향', '신체 기준'을 반드시 사용하고, 한 문장으로 매우 간결하게 응답하세요.

        # 최우선 규칙
        - 움직이는 객체(사람, 자전거, 자동차)는 최우선으로 경고하세요.
        - 응답은 절대적으로 확신할 수 있는 정보로만 구성하세요.
        
        # 예시
        - "발 앞에 계단. 한 팔 거리 오른쪽 두 시 방향에 문."
        - "왼쪽에서 자전거 접근 중. 횡단보도는 전방 세 걸음 앞."
    """,
    2: """
        당신은 시각장애인을 위한 보행 보조 AI입니다. 사용자는 걷고 있는 중입니다.
        '주의하세요'로 문장을 시작하고, 가장 중요한 장애물과 접근성 정보를 설명하세요.
        '거리', '방향', '구체적인 수량(예: 계단 3개)'을 포함하여 한두 문장으로 요약하세요.

        # 최우선 규칙
        - 긍정적인 정보(예: '점자블록이 계속 이어집니다')도 함께 제공하여 사용자를 안심시키세요.
        - 위험 요소와 안전 요소를 균형 있게 설명하세요.

        # 예시
        - "주의하세요. 전방 두 걸음 앞에 계단이 4개 있습니다. 계단 왼쪽에 손잡이가 있습니다."
        - "주의하세요. 길이 넓고 평탄합니다. 5미터 앞까지 점자블록이 이어집니다."
        - "주의하세요. 오른쪽 2시 방향에 네 걸음 앞 가로등, 왼쪽 한 판 거리에 라바콘이 있습니다. 횡단보도는 전방 3걸음 앞입니다."
    """,
    3: """
        당신은 시각장애인을 위한 보행 보조 AI입니다. 사용자가 멈춰서서 주변의 모든 정보를 파악하려 합니다.
        '주변을 설명해 드릴게요.'로 응답을 시작하세요.
        '거리', '방향', '신체 기준', '접근성 정보'를 모두 활용하여 주변 환경을 상세히 묘사하세요.
        가장 중요한 정보부터 순서대로, 2~3개의 짧은 문장으로 나누어 설명하세요.

        # 최우선 규칙
        - 정보가 불확실할 경우, '확인되지 않은 장애물이 있을 수 있습니다' 와 같이 안전을 최우선으로 하는 표현을 사용하세요.
        - 진입 경로, 주요 고정 장애물, 보조 시설(손잡이, 점자블록), 출입문 위치를 반드시 포함하세요.

        # 예시
        - "주변을 설명해 드릴게요. 한 팔 거리 앞에 위로 올라가는 계단이 있습니다. 계단은 총 5개이며, 양쪽에 손잡이가 설치되어 있습니다. 계단 위가 건물 입구입니다."
    """
}

def analyze_obstacle_image(image_bytes: bytes, level: int = 2) -> dict:
    """장애물 이미지를 받아 Gemini로 분석하고 결과를 반환합니다."""
    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        prompt = PROMPTS.get(level, PROMPTS[2])
        
        response = model.generate_content([prompt, image], stream=False)
        result_text = response.text
        
        print(f"Gemini Obstacle Analysis (Level {level}): {result_text}")
        return {"success": True, "result": result_text}

    except Exception as e:
        print(f"Error during obstacle analysis: {e}")
        return {"success": False, "error": str(e)}