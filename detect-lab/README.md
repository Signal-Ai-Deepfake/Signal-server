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
DETECT_MODEL_REVISION=<커밋 해시40자리> python fetch_model.py   # /models 에 받는다(DETECT_MODEL_DIR로 변경)
uvicorn app:app --host 0.0.0.0 --port 8000
curl -F file=@photo.jpg http://localhost:8000/detect
# {"score":0.0~1.0,"model":"CommunityForensics-DeepfakeDet-ViT","modelVersion":"onnx-fp32+<가중치 해시12자리>","width":..,"height":..}
```

Docker Compose에서는 `DETECT_MODEL_REVISION=<커밋 해시> docker compose --profile detect up -d detect` (`.env`에 적어도 된다).

**포트를 외부에 열지 마세요.** `/detect`에는 인증이 없습니다. compose는 포트를 게시하지 않아 같은 네트워크의 서버만 호출하며, 컨테이너는 root가 아닌 사용자로 실행됩니다. 서비스를 단독으로 띄워 포트를 열어야 한다면 앞단에서 접근을 제한하세요.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `DETECT_MODEL_PATH` | `/models/onnx/model.onnx` | 모델 파일 |
| `DETECT_MODEL_REPO` / `DETECT_MODEL_FILE` | CommunityForensics / `onnx/model.onnx` | `fetch_model.py`가 받을 곳 |
| `DETECT_MODEL_REVISION` | (필수) | 새로 받을 때 모델 저장소의 **커밋 해시 40자리**. 비어 있거나 `main` 같은 이름이면 받지 않고 멈춘다. 평가(#35)로 고른 가중치와 운영 가중치가 달라지지 않게 하려는 것이다 |
| `DETECT_MODEL_SHA256` | (선택) | 주면 `onnx/model.onnx`의 sha256을 확인한다(이미 받아 둔 파일도) |
| `DETECT_MODEL_NAME` / `DETECT_MODEL_VERSION` | `CommunityForensics-DeepfakeDet-ViT` / `onnx-fp32` | 응답의 `model`, `modelVersion`(뒤에 가중치 해시가 붙는다) |
| `DETECT_MAX_UPLOAD_MB` | `100` | 넘으면 413. 서버가 받는 이미지 한도(100MB)와 같다 |
| `DETECT_MAX_PIXELS` | `64000000` | 넘으면 413(압축 폭탄 방지). 휴대폰 48~50MP는 받는다 |
| `DETECT_MAX_CONCURRENCY` / `DETECT_THREADS` | `2` / `2` | 동시 처리 수 / 추론 스레드 수. 동시 처리 수는 파일 읽기와 디코딩까지 묶는다(이미지 한 장당 최대 RGB 3바이트×픽셀 수 정도를 쓴다) |

## API

- `GET /health` — 모델이 있으면 200, 없으면 503과 이유. 서비스는 모델이 없어도 뜨고 `/detect`가 503을 돌려준다.
- `POST /detect` (multipart `file`) — 200 `{score, model, modelVersion, width, height}`, 400 이미지 아님, 413 용량·픽셀 초과, 503 모델 없음.

## 테스트

```bash
pip install -r requirements-dev.txt
pytest tests
```

`requirements.txt`는 검증한 버전으로 고정돼 있다. 특히 pillow(resize 구현)와 onnxruntime은 올리면 같은 입력의 점수가 달라질 수 있으므로, 올릴 때는 전처리 확인과 평가를 다시 한다.

실제 가중치 없이 전처리와 API를 숫자로 검증하려고, 입력 평균에 10을 곱한 값을 로짓으로 내는 작은 ONNX 모델을 테스트에서 만들어 쓴다.
