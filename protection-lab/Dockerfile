# 실험용 보호 서비스 이미지. CPU 전용 torch를 사용하며 (이미지 약 2GB+), 첫 요청 때 VAE 모델(~330MB)을 내려받는다.
# 주의: 이 Dockerfile은 작성만 되었고 빌드/기동 검증은 하지 않았다.
FROM python:3.11-slim
WORKDIR /srv
ENV HF_HOME=/models PROTECT_DEVICE=cpu PYTHONUNBUFFERED=1
COPY requirements.txt .
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir -r requirements.txt
COPY protect_core.py app.py cli.py ./
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
