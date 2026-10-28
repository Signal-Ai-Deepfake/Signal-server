# signal-detect-lab

딥페이크 탐지 추론 서비스(FastAPI + onnxruntime). Signal 서버가 `deepfake-detection.mode=http`일 때 `POST /detect`를 호출한다.
모델은 서버 인스턴스에 올리지 않고 이 서비스로 분리했다(보호 노이즈의 `protection-lab`과 같은 구조).
모델 선택 근거와 주의점은 `docs/deepfake-detection.md`.

## 모델

기본은 CommunityForensics ViT-S(`buildborderless/CommunityForensics-DeepfakeDet-ViT`)의 FP32 ONNX다(MIT, 약 83MB, CPU 추론).
출력은 로짓 1개이고 sigmoid를 거친 값이 "가짜일 확률"(`score`)이다. 이 값은 **보정되지 않은 모델 출력**이다.

- 전처리: RGB → 짧은 변 440(bicubic) → 가운데 384x384 자르기 → CLIP 정규화. `transformers`의 `CLIPImageProcessor`와 같은 결과를 낸다.
  가운데를 자르므로 가장자리의 단서는 보지 못한다.
- 양자화 ONNX(int8, uint8, q4)는 FP32와 점수가 크게 달라질 수 있어 기본으로 쓰지 않는다.
- 2026-07 정정 이전의 옛 가중치는 쓰면 안 된다. `fetch_model.py`가 `config.json`으로 정정본인지 확인한다.

## 실행

```bash
pip install -r requirements.txt
python fetch_model.py                      # /models 에 받는다(DETECT_MODEL_DIR로 변경)
uvicorn app:app --host 0.0.0.0 --port 8000
curl -F file=@photo.jpg http://localhost:8000/detect
# {"score":0.0~1.0,"model":"CommunityForensics-DeepfakeDet-ViT","modelVersion":"onnx-fp32+<가중치 해시12자리>","width":..,"height":..}
```

Docker Compose에서는 `docker compose --profile detect up -d detect`.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DETECT_MODEL_PATH` | `/models/onnx/model.onnx` | 모델 파일 |
| `DETECT_MODEL_REPO` / `DETECT_MODEL_FILE` / `DETECT_MODEL_REVISION` | CommunityForensics / `onnx/model.onnx` / `main` | `fetch_model.py`가 받을 곳. 재현하려면 REVISION을 커밋 해시로 고정한다 |
| `DETECT_MODEL_NAME` / `DETECT_MODEL_VERSION` | `CommunityForensics-DeepfakeDet-ViT` / `onnx-fp32` | 응답의 `model`, `modelVersion`(뒤에 가중치 해시가 붙는다) |
| `DETECT_MAX_UPLOAD_MB` | `25` | 넘으면 413 |
| `DETECT_MAX_CONCURRENCY` / `DETECT_THREADS` | `2` / `2` | 동시 추론 수 / 추론 스레드 수 |

## API

- `GET /health` — 모델이 있으면 200, 없으면 503과 이유. 서비스는 모델이 없어도 뜨고 `/detect`가 503을 돌려준다.
- `POST /detect` (multipart `file`) — 200 `{score, model, modelVersion, width, height}`, 400 이미지 아님, 413 용량·픽셀 초과, 503 모델 없음.

## 테스트

```bash
pip install -r requirements-dev.txt
pytest tests
```

실제 가중치 없이 전처리와 API를 숫자로 검증하려고, 입력 평균에 10을 곱한 값을 로짓으로 내는 작은 ONNX 모델을 테스트에서 만들어 쓴다.
