# 딥페이크 탐지 개편 메모

## 왜 바꾸나

지금 탐지(`LlmDeepfakeDetector`)는 범용 비전 LLM이 verdict와 확신도를 함께 내놓는다. 이 모델은 진짜/가짜 이진 분류로 학습된 모델이 아니라서,
그럴듯한 설명을 붙인 채 틀릴 수 있다. 목표는 "정확도 높은 탐지"보다 "틀릴 때 틀렸다고 말하는 탐지"다.

- 판정(점수)은 전용 탐지 모델이 정하고, LLM은 결과를 풀어쓰는 설명만 맡는다. (#37)
- 모델은 서버에 올리지 않고 별도 추론 서비스(`detect-lab`)로 분리한다. (#36)
- 모델을 바꾸는 근거는 같은 평가 세트로 잰 결과다. 기준선(현재 LLM 탐지)을 넘지 못하면 바꾸지 않는다. (#33, #35)

## 모델

확인일: 2026-10-29. 모델 카드, 저장소의 LICENSE, Hugging Face 메타데이터를 직접 읽어 확인했다. 아래에서 "측정 전"은 이 서비스 입력으로 돌려 보지 않았다는 뜻이다.

| 모델 | 코드·가중치 라이선스 | 크기 | CPU 추론 | 학습 데이터 | 결정 |
|---|---|---|---|---|---|
| CommunityForensics ViT-S (`buildborderless/CommunityForensics-DeepfakeDet-ViT`) | MIT | 2,181만 파라미터. `model.safetensors` 87MB, ONNX FP32 87MB(int8 23MB, q4 16MB) | onnxruntime CPU로 구성(`detect-lab`). 지연은 측정 전 | 270만 장, 생성기 4,803종. 진짜 이미지는 LAION 중심(40%) 등 11개 데이터셋 | **1순위** |
| GenD CLIP-L/14 (`yermandy/GenD_CLIP_L_14`) | MIT(Hugging Face 카드, GitHub 저장소 LICENSE 모두) | 3.04억 파라미터. `model.safetensors` 1.2GB | 돌아가지만 ViT-S보다 훨씬 크다. 지연은 측정 전. ONNX는 제공되지 않음 | FaceForensics++ 같은 얼굴 조작 영상 벤치마크 계열 | **2순위(비교용)** |
| DEAR (`k-aisi-anti-deepfake/dear-checkpoints`) | **CC BY-NC 4.0** + CreativeML OpenRAIL-M 사용 제한 | 체크포인트 4개, 각 약 94MB(ResNet-50 기반) | 구조상 가능. 쓰지 않으므로 확인 안 함 | LSUN·MS COCO 진짜 이미지, LDM 가짜 이미지, SD 1.5 인페인팅 | **제외**(비상업) |

DEAR는 카드에 "연구 및 비상업 용도"라고 명시돼 있고 SD 1.5로 만든 데이터로 보정해서 OpenRAIL-M의 사용 제한도 진다. 서비스에 쓸 수 없다.

두 후보 모두 한국 SNS 사진 같은 이 서비스의 입력으로는 검증된 적이 없다. 그래서 #35의 평가에서 현재 LLM 탐지와 같은 세트로 비교하고, 결과로 최종 선택한다.

### 라이선스에서 따로 확인해야 하는 것

- **CommunityForensics 학습 데이터는 연구용이다.** 가중치와 코드는 MIT지만, 학습에 쓴 데이터셋(`OwensLab/CommunityForensics`)은 "CC BY 4.0, 연구 목적으로만 공개"이고, 데이터의 가짜 이미지는 각 생성 모델의 라이선스(대부분 CreativeML OpenRAIL-M)를 따른다. 진짜 이미지는 LAION 같은 웹 수집 이미지다. 가중치 라이선스(MIT)가 이를 덮는지는 이 문서가 판단하지 않는다. 상업 서비스에 쓰기 전에 팀과 법률 검토로 정해야 한다.
- **재업로드 저장소를 쓴다.** 우리가 쓰는 저장소는 원저자(Jeongsoo Park, Andrew Owens)의 공식 저장소(`OwensLab/commfor-model-384`, MIT)가 아니라 Borderless / Ethix R&D가 올린 변환본이다. 이 저장소의 가중치는 공식 체크포인트(`model_v11_ViT_384_base_ckpt.pt`)에서 다시 만들었다고 카드에 적혀 있다. 공식 저장소와 점수가 같은지는 직접 대조하지 않았다. 평가 전에 공식 가중치와 몇 장을 대조하거나, 공식 체크포인트를 직접 변환하는 방법을 검토한다.
- **GenD는 코드를 같이 가져와야 한다.** 카드의 사용 예가 저장소의 `src.hf.modeling_gend`를 불러오는 방식이고, 백본으로 `openai/clip-vit-large-patch14`를 따로 받는다. 학습 데이터(FaceForensics++ 등)의 이용 약관은 확인하지 않았다. 비교용으로만 쓰고 서비스 후보로 올리기 전에 확인한다.
- **원저자의 경고.** 데이터셋 카드는 "우리가 학습하거나 평가한 분류기는 실제 환경에서 그대로 쓰기에는 오류율이 너무 높고, 저자를 가짜 이미지 제작자로 잘못 지목하거나 생성물이 진짜로 인증되는 결과로 이어질 수 있다"고 적고 있다. 이 서비스가 점수 대신 등급과 "판단 불가"를 쓰고 이미지를 단정하지 않는 이유와 같다.
- **편향.** 데이터셋은 LAION 캡션 중심이고 생성기의 대부분이 Stable Diffusion 계열이라, 다른 계열 생성기에는 약할 수 있다고 카드가 밝히고 있다.

### CommunityForensics 사용 시 알아둘 점 (모델 카드 기준)

- 가중치는 2026-07에 정정됐다. `model.safetensors`와 `model_fixed.safetensors`(별칭)는 정정본, `model_legacy.safetensors`와 `onnx_legacy/`는 쓰지 않는다.
  받은 `config.json`이 `num_labels=1`, `num_attention_heads=6`, `intermediate_size=1536`이 아니면 옛 파일이다.
- 전처리: RGB → 짧은 변을 440으로 → 가운데 384x384 자르기 → CLIP 정규화(mean 0.4815/0.4578/0.4082, std 0.2686/0.2613/0.2758).
  **가운데를 자르기 때문에 가장자리의 단서는 보지 못한다.** 캡처처럼 여백이 큰 이미지는 변형별 평가(`screenshot`)로 따로 본다.
- 양자화 ONNX(int8, uint8, q4)는 FP32와 점수가 크게 달라질 수 있다(카드: 최대 10~70%p). 평가·서비스에는 FP32 `onnx/model.onnx`를 기본으로 쓴다.
- 카드의 판정 기준은 확률 0.5 초과가 fake지만 다른 임계값 지침은 없다. 임계값은 #35 평가 결과로 정한다.
- 카드는 원본이 해커톤 PoC였다고 밝히고 있고 성능 수치는 싣지 않았다.
- 확인일 기준 `detect-lab`이 고정한 커밋(`ac6ee457…`)이 이 저장소의 최신이고, FP32 `onnx/model.onnx`의 SHA-256도 고정해 둔 값과 일치한다.

## 평가

`scripts/detect-eval/README.md` 참고. 평가 세트에는 실제 피해자 이미지, 실존 인물 얼굴로 만든 합성 이미지, 성적 이미지를 넣지 않는다.

## 구조 (#36)

```
Signal 서버 ── mode=llm  ──▶ LlmDeepfakeDetector ──▶ 비전 LLM (기본, 기존 동작)
             └ mode=http ──▶ HttpDeepfakeDetector ──▶ detect-lab: POST /detect ──▶ ONNX 모델
```

- 모드는 `deepfake-detection.mode`(환경변수 `DEEPFAKE_DETECTION_MODE`, 기본 `llm`)로 바꾼다. 주소와 시간 제한은 `deepfake-detection.http.base-url`, `timeout-seconds`.
- 점수(`score`, 0~1, 가짜일 확률)는 모델 출력 그대로(보정 전)다. 서버는 이를 `riskScore`(0~100)로 옮기고 기존 구간(30 미만 REAL, 70 이하 SUSPICIOUS, 초과 FAKE)으로 `verdict`를 정한다.
  `confidence`는 모델이 고른 쪽의 확률 `max(score, 1 - score)`이다. 구간 임계값을 평가 결과로 정하고 "판단 불가"를 두는 일은 #37에서 한다.
- 이 모델은 얼굴 검출을 하지 않아 `NO_FACE_DETECTED`를 내지 않고, 근거 영역(`evidences`)도 내지 않는다(빈 목록). 얼굴이 없는 이미지도 점수가 나온다.
- 영상은 지원하지 않는다(`VIDEO_NOT_SUPPORTED`). 호출 실패, 시간 초과, 범위 밖 점수는 룰 기반 값으로 대체하지 않고 `FAILED`로 처리한다.
- 서비스는 모델이 없어도 뜨며, `GET /health`가 503과 이유를 돌려준다. 자세한 실행 방법은 `detect-lab/README.md`.
- 가중치는 커밋 해시(`DETECT_MODEL_REVISION`)로 고정해서 받고, 필요하면 `DETECT_MODEL_SHA256`으로 파일을 확인한다. 평가(#35)에서 고른 해시를 그대로 운영에 쓴다.
- 추론 서비스는 서버가 받는 이미지 한도(100MB)와 같은 크기까지 받고, 그보다 크거나 64MP를 넘는 이미지는 413을 돌려준다. 이 경우 서버는 `AI_SERVER_ERROR`로 처리한다.
- `/detect`에는 인증이 없다. compose는 포트를 게시하지 않으며, 외부에 열지 않는다.

## 전처리 확인

`detect-lab/preprocess.py`는 `transformers`의 `CLIPImageProcessor`(PIL 백엔드, 모델 카드의 `preprocessor_config.json` 값)와 크기가 다른 9종의 무작위 이미지에서
출력이 완전히 같음(최대 차이 0)을 확인했다(transformers 5.21, Pillow 12.3). 방향 보정(EXIF 회전)은 모델 카드의 전처리에 없어 적용하지 않는다.
