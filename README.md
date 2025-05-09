# AI Eyes - 캡스톤디자인 프로젝트

## 📱 개요
AI 기반 시각장애인 보조 앱 개발 프로젝트입니다.

## 🛠 개발 환경
- Android Studio
- Java
- GitHub
- TMAP API, OCR API, Gemini API

## 🧱 브랜치 전략
- main: 최종 안정 버전
- dev: 통합 테스트용
- feature/nav, feature/receipt, feature/obstacle 등

## 📁 폴더 구조
- /ui : 화면(Activity, Fragment)
- /service : 기능 처리
- /utils : 공통 유틸 (TTSManager 등)


## Android Studio(Java)와 GitHub를 사용해 협업 개발을 과정

✅ 1단계. GitHub 저장소 생성 및 설정
🔸 저장소 생성 (팀장 또는 리드 담당자)
GitHub 로그인 → [New Repository] 클릭

설정 예시:

이름: ai-eyes-capstone

설명: AI 기반 시각장애인 보조 앱 프로젝트

공개 여부: Private 또는 Public (학교/팀 방침에 따름)

체크: Initialize this repository with README ✅

생성 후 저장소 주소 복사: https://github.com/LeeJH104/AI-Eyes-Capstone.git


✅ 2단계. Android Studio에서 Git 연동 설정
🔸 Android Studio에서 새 프로젝트 생성
언어: Java

최소 SDK: API 30 (Android 11) 이상

빈 액티비티로 시작

🔸 Git 연동
Android Studio 메뉴 → VCS → Enable Version Control Integration → Git

터미널 열기 (View → Tool Windows → Terminal)

명령어 입력:


-bash-

git init

git remote add origin https://github.com/LeeJH104/AI-Eyes-Capstone.git

git add .

git commit -m "Initial commit(초기 커밋)"

git branch -M main

git push -u origin main


✅ 이로써 로컬 Android 프로젝트가 GitHub와 연결됩니다.


✅ 3단계. 팀원 GitHub 협업 세팅
🔸 팀원 초대
GitHub 저장소 → Settings → Collaborators

팀원 GitHub ID 입력 → 초대

팀원이 초대 수락 후 협업 가능

🔸 팀원 로컬 프로젝트 복제
각 팀원은 아래 명령어로 프로젝트 복제:


-bash- 

git clone https://github.com/LeeJH104/AI-Eyes-Capstone.git

cd ai-eyes-capstone


Android Studio에서 Open Project로 열면 바로 사용 가능.


✅ 4단계. 협업을 위한 브랜치 전략
📁 추천 브랜치 구조
브랜치명	용도
main	최종 완성 코드 (팀장만 병합)
dev	테스트용 통합 브랜치
feature/nav	네비게이션 기능 (팀원 A)
feature/receipt	영수증 기능 (팀원 B)
feature/obstacle	장애물 탐지 기능 (팀원 C)


🔄 개발 절차 예시 (팀원)


-bash-

git checkout -b feature/nav       # 기능 브랜치 생성

... 작업 ...

git add .

git commit -m "Add: 목적지 음성입력 기능"

git push origin feature/nav       # GitHub에 올리기


✅ 추가 팁: 안드로이드에서 자주 발생하는 실수 방지

항목	조치 방법

프로젝트 열 때 에러 발생	File → Invalidate Caches / Restart 시도

TTS/STT 테스트 오류	AndroidManifest.xml에 마이크, 인터넷, TTS 권한 추가

충돌 방지	항상 git pull origin dev 후 작업 시작
