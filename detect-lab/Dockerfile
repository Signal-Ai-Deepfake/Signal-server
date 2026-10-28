# 딥페이크 탐지 추론 서비스 이미지. onnxruntime(CPU)만 쓰므로 PyTorch가 필요 없다.
# 첫 기동 때 모델(ONNX FP32, 약 83MB)을 /models에 내려받는다. 볼륨(model-cache)에 남으므로 다음부터는 받지 않는다.
FROM python:3.11-slim
WORKDIR /srv
ENV DETECT_MODEL_DIR=/models DETECT_MODEL_PATH=/models/onnx/model.onnx HF_HOME=/models/.hf PYTHONUNBUFFERED=1
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY preprocess.py model.py fetch_model.py app.py ./
EXPOSE 8000
CMD ["sh", "-c", "python fetch_model.py && exec uvicorn app:app --host 0.0.0.0 --port 8000"]
