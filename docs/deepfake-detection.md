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
