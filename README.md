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


## ✅ 팀원이 기존 브랜치에서 작업 시작하는 방법
🔹 1. GitHub 저장소 클론
팀원이 로컬에 저장소가 없다면 먼저 클론합니다:

Android Studio에서:

File > New > Project from Version Control > Git

URL 붙여넣고 Clone


🔹 2. 원격 브랜치 목록 확인

-bash-

git fetch

git branch -r


출력 예시:

-bash-

origin/main

origin/voice-assist

origin/object-detection


🔹 3. 작업하려는 브랜치를 로컬로 가져오기
예: feature/nav 브랜치에서 작업하려면

-bash-

git checkout -b feature/nav origin/feature/nav

or

-bash-

git switch -c feature/nav origin/feature/nav

✔ 이 명령은 origin/feature/nav라는 원격 브랜치를 로컬로 복제하고 해당 브랜치로 이동하는 것입니다.

만약 fatal: a branch named 'feature/nav' already exists 라고 뜬다면 
이미 만들어져 있으니까, 새로 만들 필요 없이 그냥 브랜치 전환만 하면 됩니다:

-bash-

git checkout feature/nav

or

-bash-

git switch feature/nav


🔹 4. 코드 작성 → 커밋 → 푸시
코드를 작성한 후 아래 명령어로 변경사항을 저장하고 GitHub에 올립니다:

-bash-

git add .

git commit -m "작업한 기능 설명 예: Add voice command handler"

git push origin voice-assist


🔹 5. Pull Request 만들기 (GitHub에서)

GitHub 웹사이트에서 voice-assist 브랜치를 선택 후

→ "Compare & pull request" 클릭

→ 작업 내용 작성하고 PR 생성

→ 팀장이 확인 후 main에 병합


🔄 6. 다른 팀원 작업 반영 (선택)

최신 코드를 반영하고 싶다면:

-bash-

git checkout main

git pull origin main

git checkout voice-assist

git merge main    # main의 최신 내용을 현재 브랜치에 병합

📌 요약: 브랜치 별로 작업 시작하는 명령어 예시
원격 브랜치 이름	                작업 명령어
origin/dev	                    git checkout -b dev origin/dev 또는 git switch -c dev origin/dev
origin/feature/nav	            git checkout -b feature/nav origin/feature/nav
origin/feature/obstacle	        git checkout -b feature/obstacle origin/feature/obstacle
origin/feature/receipt	        git checkout -b feature/receipt origin/feature/receipt

✅ 브랜치 연결되었는지 확인하려면
체크아웃 후 다음 명령으로 확인할 수 있어요:

-bash-

git branch -vv

해당 브랜치 옆에 origin/feature/xxx처럼 뜨면, 원격과 연결(추적)된 상태입니다.

이제 브랜치를 로컬로 잘 가져왔으면, 각자 기능별 브랜치에서 코딩하고 커밋 + 푸시 + PR 하면 됩니다.
