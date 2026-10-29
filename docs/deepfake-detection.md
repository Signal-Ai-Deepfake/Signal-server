# 딥페이크 탐지 개편 메모

## 왜 바꾸나

지금 탐지(`LlmDeepfakeDetector`)는 범용 비전 LLM이 verdict와 확신도를 함께 내놓는다. 이 모델은 진짜/가짜 이진 분류로 학습된 모델이 아니라서,
그럴듯한 설명을 붙인 채 틀릴 수 있다. 목표는 "정확도 높은 탐지"보다 "틀릴 때 틀렸다고 말하는 탐지"다.

- 판정(점수)은 전용 탐지 모델이 정하고, LLM은 결과를 풀어쓰는 설명만 맡는다. (#37)
- 모델은 서버에 올리지 않고 별도 추론 서비스(`detect-lab`)로 분리한다. (#36)
- 모델을 바꾸는 근거는 같은 평가 세트로 잰 결과다. 기준선(현재 LLM 탐지)을 넘지 못하면 바꾸지 않는다. (#33, #35)

## 모델

| 모델 | 라이선스 | 비고 | 상태 |
|---|---|---|---|
| CommunityForensics ViT-S (`buildborderless/CommunityForensics-DeepfakeDet-ViT`) | MIT | **1순위.** ViT-Small(약 2,180만 파라미터), 가중치 약 87MB, ONNX FP32 83MB. 논문 기준 학습 데이터 270만 장·생성기 4,803종. 출력은 로짓 1개(sigmoid → 가짜 확률). | 라이선스·구조 확인됨 |
| GenD CLIP-L/14 (`yermandy/GenD_CLIP_L_14`) | MIT | 2순위(비교용). 얼굴 조작 영상 중심의 FF++ 계열 학습. 카드에 학습 데이터와 성능 수치가 없다. | 라이선스 확인됨, 성능 근거는 논문 |
| DEAR (K-AISI) | CC BY-NC 4.0 + OpenRAIL-M 제한 | 비상업 라이선스라 서비스에 쓰지 않는다. | 카드 직접 확인 필요 |

두 모델 모두 한국 SNS 사진 같은 이 서비스의 입력으로는 검증된 적이 없다. 그래서 #35의 평가에서 현재 LLM 탐지와 같은 세트로 비교하고, 결과로 최종 선택한다.

### CommunityForensics 사용 시 알아둘 점 (모델 카드 기준)

- 가중치는 2026-07에 정정됐다. `model.safetensors`와 `model_fixed.safetensors`(별칭)는 정정본, `model_legacy.safetensors`와 `onnx_legacy/`는 쓰지 않는다.
  받은 `config.json`이 `num_labels=1`, `num_attention_heads=6`, `intermediate_size=1536`이 아니면 옛 파일이다.
- 전처리: RGB → 짧은 변을 440으로 → 가운데 384x384 자르기 → CLIP 정규화(mean 0.4815/0.4578/0.4082, std 0.2686/0.2613/0.2758).
  **가운데를 자르기 때문에 가장자리의 단서는 보지 못한다.** 캡처처럼 여백이 큰 이미지는 변형별 평가(`screenshot`)로 따로 본다.
- 양자화 ONNX(int8, uint8, q4)는 FP32와 점수가 크게 달라질 수 있다(카드: 최대 10~70%p). 평가·서비스에는 FP32 `onnx/model.onnx`를 기본으로 쓴다.
- 카드의 판정 기준은 확률 0.5 초과가 fake지만 다른 임계값 지침은 없다. 임계값은 #35 평가 결과로 정한다.
- 카드는 원본이 해커톤 PoC였다고 밝히고 있고 성능 수치는 싣지 않았다.

## 평가

`scripts/detect-eval/README.md` 참고. 평가 세트에는 실제 피해자 이미지, 실존 인물 얼굴로 만든 합성 이미지, 성적 이미지를 넣지 않는다.

## 구조 (#36)

```
Signal 서버 ── mode=llm  ──▶ LlmDeepfakeDetector ──▶ 비전 LLM (기본, 기존 동작)
             └ mode=http ──▶ HttpDeepfakeDetector ──▶ detect-lab: POST /detect ──▶ ONNX 모델
```

- 모드는 `deepfake-detection.mode`(환경변수 `DEEPFAKE_DETECTION_MODE`, 기본 `llm`)로 바꾼다. 주소와 시간 제한은 `deepfake-detection.http.base-url`, `timeout-seconds`.
- 점수(`score`, 0~1, 가짜일 확률)는 모델 출력 그대로(보정 전)다. 서버는 이를 `riskScore`(0~100)로 옮기고 기존 구간(30 미만 REAL, 70 이하 SUSPICIOUS, 초과 FAKE)으로 `verdict`를 정한다.
  `confidence`는 모델이 고른 쪽의 확률 `max(score, 1 - score)`이다. 등급과 판단 불가는 아래 "합성 가능성 등급과 판단 불가"를 참고.
- 이 모델은 얼굴 검출을 하지 않아 `NO_FACE_DETECTED`를 내지 않는다. 얼굴이 없는 이미지도 점수가 나온다. 근거(`evidences`)는 모델이 내지 않으며, 설명기를 켰을 때만 이미 정해진 등급의 설명이 들어간다(기본은 빈 목록).
- 영상은 지원하지 않는다(`VIDEO_NOT_SUPPORTED`). 호출 실패, 시간 초과, 범위 밖 점수는 룰 기반 값으로 대체하지 않고 `FAILED`로 처리한다.
- 서비스는 모델이 없어도 뜨며, `GET /health`가 503과 이유를 돌려준다. 자세한 실행 방법은 `detect-lab/README.md`.
- 가중치는 커밋 해시(`DETECT_MODEL_REVISION`)로 고정해서 받고, 필요하면 `DETECT_MODEL_SHA256`으로 파일을 확인한다. 평가(#35)에서 고른 해시를 그대로 운영에 쓴다.
- 추론 서비스는 서버가 받는 이미지 한도(100MB)와 같은 크기까지 받고, 그보다 크거나 64MP를 넘는 이미지는 413을 돌려준다. 이 경우 서버는 `AI_SERVER_ERROR`로 처리한다.
- `/detect`에는 인증이 없다. compose는 포트를 게시하지 않으며, 외부에 열지 않는다.

## 합성 가능성 등급과 판단 불가 (#37)

`mode=http`(전용 탐지 모델)의 결과에는 퍼센트 대신 **등급(`band`)** 을 쓴다. 화면에는 "합성 가능성 낮음 / 불확실 / 높음 / 판단 불가"로 보여 주고, "가짜 확정" 같은 표현은 쓰지 않는다.

| band | 뜻 | 정하는 기준 |
|---|---|---|
| `LOW` | 합성 가능성이 낮음(진짜라는 보증은 아님) | 점수 < `low-below` |
| `UNCERTAIN` | 점수가 중간이라 어느 쪽이라고 말하기 어려움 | `low-below` ≤ 점수 ≤ `high-above`, 또는 점수는 낮지만 AI 편집 표시(`AI_EDIT_DECLARED`)가 있음 |
| `HIGH` | 합성 가능성이 높음 | 점수 > `high-above`, 또는 강한 출처 신호 |
| `UNDETERMINED` | 판단 불가 | 짧은 변이 `min-short-edge-px`보다 작음(`undeterminedReason=LOW_RESOLUTION`) |

- 등급은 `DetectionBandPolicy`가 정한다. 순서는 강한 출처 신호 → 해상도 → 점수 → (점수가 낮을 때) AI 편집 표시이며, LLM은 관여하지 않는다.
- `deepfake-detection.bands.low-below`(30), `high-above`(70), `min-short-edge-px`(256)은 **잠정값**이다. 앞의 둘은 기존 verdict 구간을 그대로 옮긴 것이고, 평가 세트(#35)로 측정해서 정한다. 그때까지는 이 값으로 정확도를 말하지 않는다.
- 압축이 심한 사진을 판단 불가로 보내는 규칙은 아직 없다. 어떤 신호(JPEG 품질, 메신저 전송 흔적 등)가 믿을 만한지 평가에서 확인한 뒤에 넣는다.
- 응답 필드: `band`, `assessmentBasis`(`MODEL` | `PROVENANCE`), `undeterminedReason`, `modelName`, `modelVersion`(가중치 해시 포함), `provenanceSignals`. 비전 LLM(`mode=llm`)과 이전 기록은 새 필드가 비어 있다.
- **점수 숫자는 등급이 점수에서 나왔을 때만** 준다. `UNDETERMINED`이거나 `assessmentBasis=PROVENANCE`이면 `verdict`, `confidence`, `riskScore`가 `null`이다(등급과 모순되는 숫자가 보이지 않게). 하위 호환용 `verdict`는 LOW→REAL, UNCERTAIN→SUSPICIOUS, HIGH→FAKE이고 판단 불가에는 없다. 화면은 `band`를 기준으로 그리고 `riskScore`를 퍼센트로 보여 주지 않는 것을 권장한다.

### 출처 신호 (결정론적)

`ProvenanceSignalExtractor`가 파일에서 직접 읽는다. 모델을 쓰지 않는다.

| 신호 | 찾는 것 | 등급에 쓰는가 |
|---|---|---|
| `GENERATOR_METADATA` | PNG 텍스트 청크(Stable Diffusion WebUI의 `parameters`, ComfyUI의 `prompt`·`workflow`, InvokeAI), XMP `CreatorTool`·C2PA `softwareAgent` 필드에 적힌 생성 전용 도구 이름(Midjourney, DALL-E, Stable Diffusion, ComfyUI, NovelAI, Leonardo.Ai) | 예(강한 근거 → `HIGH`) |
| `AI_SOURCE_DECLARED` | IPTC `digitalSourceType`이 `trainedAlgorithmicMedia`(AI가 생성)임을 표시 | 예(강한 근거 → `HIGH`) |
| `AI_EDIT_DECLARED` | IPTC `digitalSourceType`이 `compositeWithTrainedAlgorithmicMedia`·`compositeSynthetic`(일부를 AI로 합성·편집), 또는 제작 도구 필드에 Adobe Firefly | 아니오(점수가 `LOW`일 때만 `UNCERTAIN`으로 올림, `assessmentBasis=PROVENANCE`) |
| `C2PA_MANIFEST` | C2PA(JUMBF) 콘텐츠 자격 증명이 포함됨 | 아니오(있다는 사실만 알림) |

- 합성(composite) 값과 Adobe Firefly는 실제로 찍은 사진에 생성형 채우기를 쓴 경우에도 남는다. 이미지 전체가 합성이라는 근거가 아니므로 `HIGH`로 올리지 않는다. 다만 일부가 AI로 만들어졌으니 모델이 낮은 점수를 줘도 "합성 가능성 낮음"이라고 하지 않고 `UNCERTAIN`으로 둔다. `algorithmicMedia`(학습 모델 없이 알고리즘으로 만든 이미지)는 AI 생성 표시로 보지 않는다.
- C2PA는 **서명을 검증하지 않는다.** 매니페스트가 있다는 것과 거기에 AI 생성 표시(`trainedAlgorithmicMedia`)가 있다는 것만 읽는다. 서명 검증은 별도 작업이다.
- 메타데이터는 스크린샷, 재저장, 메신저 전송에서 사라진다. 그래서 **신호가 없다고 진짜로 보지 않는다.** 신호가 있을 때만 근거로 쓰고, 강한 신호가 있으면 점수가 낮아도 `HIGH`(`assessmentBasis=PROVENANCE`)가 된다.
- 생성 도구 이름은 파일 전체에서 찾지 않고 `CreatorTool`, `softwareAgent` 필드 뒤 200자 안에서만 찾는다. 캡션이나 설명문에 도구 이름이 적혀 있다고 신호로 보지 않기 위해서다. 한계: XMP를 파서로 읽지 않는 문자열 검색이라 필드 이름과 값이 멀리 떨어진 형식은 놓치고(놓치면 신호 없음), EXIF `Software` 태그(바이너리)는 읽지 않는다. 목록을 늘릴 때는 평가 세트의 진짜 이미지에서 걸리는지 먼저 확인한다.

### LLM은 설명만 한다

`deepfake-detection.explain.enabled=true`(기본 `false`)이면 `LlmDeepfakeExplainer`가 이미 정해진 등급을 설명하는 문장(`evidences`)을 만든다.

- 설명기는 등급을 입력으로만 받고 결과로는 근거 목록만 돌려준다. 등급을 바꿀 방법이 없고, 실패해도 탐지 결과는 그대로 나간다(근거는 빈 목록).
- `LOW`와 `UNDETERMINED`에서는 호출하지 않는다(설명할 근거가 없는데 근거를 만들어 내지 않게).
- 프롬프트로 이미지에서 실제로 보이는 특징만 가능성 표현으로 서술하게 하고, "확정", "100%" 같은 단정 표현이 든 문장은 버린다.

## 전처리 확인

`detect-lab/preprocess.py`는 `transformers`의 `CLIPImageProcessor`(PIL 백엔드, 모델 카드의 `preprocessor_config.json` 값)와 크기가 다른 9종의 무작위 이미지에서
출력이 완전히 같음(최대 차이 0)을 확인했다(transformers 5.21, Pillow 12.3). 방향 보정(EXIF 회전)은 모델 카드의 전처리에 없어 적용하지 않는다.
