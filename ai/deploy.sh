#!/bin/bash
# EC2에서 실행: GitHub main의 ai/ 폴더 최신 코드로 서버를 다시 띄운다.
#
# 전제:
#   - /data/Law-Chat-src 에 Law-Chat 모노레포가 이미 git clone 되어 있어야 한다 (최초 1회만).
#   - 이 스크립트는 GitHub에 커밋하지 않는다 (EC2 로컬 운영 스크립트).
#
# 하는 일:
#   1. /data/Law-Chat-src 를 origin/main 과 완전히 동일하게 맞춘다 (fetch + reset --hard).
#   2. 그 안의 ai/ 폴더만 골라 운영 폴더(/data/Law-Chat/ai)에 동기화한다.
#      - weights/ indexes/ kobart/ .env law_chat.pem venv/ 등은 절대 건드리지 않는다.
#      - GitHub에는 없는데 운영 폴더에만 있는 파일(예: llm_client.py 같은 실험 잔재)은
#        rsync --delete 로 자동 정리된다.
#   3. docker compose로 이미지를 다시 빌드하고 컨테이너를 재시작한다.

set -euo pipefail

SRC_REPO="/data/Law-Chat-src"
LIVE_DIR="/data/Law-Chat/ai"
BRANCH="main"

if [ ! -d "$SRC_REPO/.git" ]; then
    echo "[deploy] $SRC_REPO 가 git 저장소가 아닙니다. 먼저 최초 1회 clone 해주세요:" >&2
    echo "    git clone https://github.com/leehyojae0710-commits/Law-Chat.git $SRC_REPO" >&2
    exit 1
fi

echo "[deploy] GitHub($BRANCH) 최신 코드 확인 중..."
git -C "$SRC_REPO" fetch origin
git -C "$SRC_REPO" reset --hard "origin/$BRANCH"

echo "[deploy] ai/ 코드를 운영 폴더로 동기화 중 (모델/설정 파일은 보존)..."
rsync -av --delete \
    --exclude='deploy.sh' \
    --exclude='.env' \
    --exclude='law_chat.pem' \
    --exclude='weights/' \
    --exclude='indexes/' \
    --exclude='kobart/' \
    --exclude='venv/' \
    --exclude='__pycache__/' \
    --exclude='*.log' \
    "$SRC_REPO/ai/" "$LIVE_DIR/"

echo "[deploy] 컨테이너 재빌드 및 재시작 중..."
cd "$LIVE_DIR"
docker compose up -d --build

echo "[deploy] 완료. 로그 확인: docker logs law-chat-ai --tail 50 -f"
