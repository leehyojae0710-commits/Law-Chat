# CUDA 12.4 + Ubuntu 22.04 (드라이버 580.x / CUDA 13.0 런타임과 호환)
FROM nvidia/cuda:12.4.1-runtime-ubuntu22.04

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3.11 python3-pip git \
    && rm -rf /var/lib/apt/lists/*
RUN ln -sf /usr/bin/python3.11 /usr/bin/python

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# weights/ indexes/ kobart/ 는 이미지에 안 넣고 docker-compose에서 볼륨으로 마운트한다
# (용량 크고, 모델 파일 바뀔 때마다 이미지 다시 빌드하지 않기 위해)

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
