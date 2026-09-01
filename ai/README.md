# law-chat-ai

챗봇(QA + RAG) + 판례요약(KoBART) 통합 서버. 하나의 FastAPI 프로세스, 하나의 GPU(A10G)에서
두 기능을 모두 서빙한다.

## 디렉토리 구조 (EC2에서 최종적으로 이렇게 있어야 함)

이 저장소(git clone)에는 **코드만** 들어있다. 아래 3개 디렉토리는 `.gitignore`에 들어있어서
git에는 없고, **로컬 PC에서 scp로 직접 전송**해야 한다 (용량이 크고 모델 파일이라 GitHub에 안 맞음).

```
law-chat-ai/                       <- git clone 받은 위치
├── main.py
├── summarizer.py
├── router.py / db_loader.py / rag_chain.py / server.py
├── precedent_summarizer/
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
├── .env                          <- .env.example 복사해서 만들기
│
├── weights/                      <- ★ scp로 전송 (LoRA 어댑터, 챗봇 QA용)
│   ├── criminal_qa/
│   ├── administrative_qa/
│   └── civil_qa/
│
├── indexes/                      <- ★ scp로 전송 (RAG FAISS+BM25 인덱스, db_loader.py 산출물)
│   ├── civil/faiss/  civil/bm25_docs.json
│   ├── criminal/...
│   └── administrative/...
│
└── kobart/                       <- ★ scp로 전송 (판례요약 KoBART 체크포인트)
    └── checkpoint-26606/         <- config.json, model.safetensors 등
        (tokenizer 파일이 여기 없고 kobart/ 루트에 있다면 .env의
         KOBART_TOKENIZER=kobart 로 지정)
```

## 로컬 PC -> EC2로 모델 파일 옮기기 (Windows PowerShell)

```powershell
# weights (LoRA 어댑터)
scp -i law_chat.pem -r "C:\경로\weights" ubuntu@<EC2 퍼블릭 IP>:~/law-chat-ai/

# indexes (RAG 인덱스, 있다면)
scp -i law_chat.pem -r "C:\경로\indexes" ubuntu@<EC2 퍼블릭 IP>:~/law-chat-ai/

# kobart 체크포인트
scp -i law_chat.pem -r "C:\경로\checkpoint-26606" ubuntu@<EC2 퍼블릭 IP>:~/law-chat-ai/kobart/
```

폴더 용량이 크면 시간이 꽤 걸릴 수 있다 (LLM LoRA 어댑터는 보통 수십~수백MB, KoBART 체크포인트도
비슷한 수준). 진행 중 끊기면 `scp` 대신 `rsync -avz -e "ssh -i law_chat.pem"`를 쓰면 이어받기가 된다.

## 실행 (EC2, Docker)

```bash
git clone <GitHub repo> law-chat-ai
cd law-chat-ai
cp .env.example .env
# (위 scp로 weights/ indexes/ kobart/ 채워넣은 다음)
docker compose up -d --build
```

## 헬스체크

```bash
curl http://localhost:8000/health
```

```json
{
  "status": "ok",
  "groups": { "ko_llama3": {...}, "llama31": {...} },
  "rag_legal_types": ["civil", "criminal", "administrative"],
  "kobart_ready": true
}
```

`kobart_ready: false`면 `kobart/` 경로에 체크포인트가 없거나 `.env`의 `KOBART_CHECKPOINT` 경로가
잘못된 것이다. `/summarize/precedent`는 이때 503을 반환한다 (서버 전체는 정상 작동, QA 챗봇은
그대로 쓸 수 있음).

## 엔드포인트 요약

| 경로 | 용도 | 사용처 |
|---|---|---|
| `POST /chat/auto` | 챗봇 QA (도메인 자동분류 + RAG) | 백엔드 `LegalChatbotClient` |
| `POST /summarize/precedent` | 판례요약 (KoBART) | 백엔드 `PrecedentSummaryClient` |
| `GET /health` | 헬스체크 | 배포 직후 확인 |
| `POST /chat`, `/chat/simple*` | 테스트/저수준 엔드포인트 | 직접 호출 금지 (주석 참고) |
