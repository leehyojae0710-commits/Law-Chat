#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# 4개 법률 도메인(civil/criminal/administrative/ip) 인덱스 전체 재생성
#
# db_loader.py의 source_id 버그(법령ID → MST) 수정을 인덱스에 반영하려면
# 기존 인덱스 폴더를 지우고 다시 만들어야 함. 국가법령정보센터 API를 키워드마다
# 순차 호출하기 때문에 4개 다 돌면 꽤 오래 걸릴 수 있음 -> 백그라운드 실행 권장.
#
# 사용법:
#   ./reindex_all.sh              # 4개 전부
#   ./reindex_all.sh civil ip     # 특정 도메인만
# ──────────────────────────────────────────────────────────────
set -uo pipefail  # 도메인 하나 실패해도 나머지는 계속 진행 (set -e 안 씀)

cd "$(dirname "$0")"

LEGAL_TYPES=("$@")
if [ ${#LEGAL_TYPES[@]} -eq 0 ]; then
    LEGAL_TYPES=(civil criminal administrative ip)
fi

LOG_DIR="./reindex_logs/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$LOG_DIR"

echo "재생성 대상: ${LEGAL_TYPES[*]}"
echo "로그 위치  : $LOG_DIR"
echo "──────────────────────────────────────"

FAILED=()

for lt in "${LEGAL_TYPES[@]}"; do
    echo "[$(date '+%H:%M:%S')] [$lt] 시작..."

    # 기존 인덱스 백업 (덮어쓰기 실패 시 롤백할 수 있게)
    if [ -d "./indexes/$lt" ]; then
        mv "./indexes/$lt" "./indexes/${lt}.bak.$(date +%s)"
    fi

    if python3 db_loader.py --legal-type "$lt" > "$LOG_DIR/${lt}.log" 2>&1; then
        echo "[$(date '+%H:%M:%S')] [$lt] 완료 ✅ (로그: $LOG_DIR/${lt}.log)"
    else
        echo "[$(date '+%H:%M:%S')] [$lt] 실패 ❌ (로그 확인: $LOG_DIR/${lt}.log)"
        FAILED+=("$lt")
    fi
    echo "──────────────────────────────────────"
done

echo ""
if [ ${#FAILED[@]} -eq 0 ]; then
    echo "전체 완료. 실패 없음."
else
    echo "실패한 도메인: ${FAILED[*]}"
    echo "-> 위 로그 파일 확인 후 해당 도메인만 다시: ./reindex_all.sh ${FAILED[*]}"
fi

echo ""
echo "다 끝났으면 AI 서버(uvicorn) 재시작해서 새 인덱스를 로드하세요."
