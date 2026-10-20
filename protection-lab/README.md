# signal-protection-lab

Signal의 "이미지 보호 처리(노이즈 삽입)" 기능을 실제로 구현해 보는 **실험용** 코드입니다.

> **상태: 실험 구현, Signal 서비스에는 연결되어 있지 않습니다.**
> 서비스(`Signal-server`)는 기본적으로 `NoiseImageProtector`(서버 내 일반 픽셀 노이즈 삽입, 모델을 겨냥한 적대적 노이즈는 아님)를 사용합니다.
> 이 폴더의 코드는 `PROTECTION_MODE=http`로 선택할 수 있는 모델 겨냥 적대적 노이즈 엔진이며, 운영 검증은 하지 않았습니다.

## 무엇을 하나
사진에 **눈에 거의 안 보이는 적대적 노이즈**를 더합니다 (PhotoGuard의 encoder attack을 단순화).

1. Stable Diffusion 1.x/2.x가 쓰는 VAE 인코더(`stabilityai/sd-vae-ft-mse`, 모델 카드 기준 MIT)를 불러옵니다.
2. 사진 x에 더할 노이즈 δ를 PGD로 100번(수준별 50/100/150번) 다듬습니다. 목표는 "인코더가 x+δ를 회색 이미지처럼 읽게 하기"입니다.
3. δ의 크기는 픽셀당 최대 ε로 제한합니다: LIGHT 4/255, NORMAL 8/255, STRONG 16/255.
4. 최적화는 최대 512px로 줄여서 하고, 만든 노이즈만 원본 크기로 되돌려 더해 PNG로 저장합니다.

## 한계 (반드시 함께 안내)
- 특정 인코더(SD 1.x/2.x 계열)를 겨냥한 것이라 **다른 모델(SDXL, SD3, Flux 등)과 얼굴 교체형 도구에는 효과가 보장되지 않습니다.**
- JPEG 재압축, 리사이즈, 스크린샷, 메신저 전송 후에는 효과가 약해질 수 있고, 노이즈 제거(purification) 기법으로 지워질 수 있습니다.
- 그래서 서비스 문구는 "딥페이크 방지"가 아니라 "특정 이미지 편집 모델에 대한 방해 노이즈 삽입(연구 방식 적용)"으로 써야 합니다.
- 효과는 `notebooks/verify_on_colab.ipynb`로 직접 확인해야 합니다. 이 저장소를 만든 환경에서는 실제 VAE를 내려받을 수 없어 **실제 모델로 돌려 본 결과가 없습니다.**

## 실행
```bash
pip install -r requirements.txt

# 사진 1장
python cli.py photo.jpg protected.png --level NORMAL     # GPU가 없으면 CPU로 수 분 걸림 (--steps 30 으로 줄일 수 있음)

# 서버
uvicorn app:app --host 0.0.0.0 --port 8000
curl -F file=@photo.jpg -F level=NORMAL http://localhost:8000/protect -o protected.png -D -
```
응답 헤더에 `X-PSNR-dB`, `X-Linf-255`(최대 픽셀 변화), `X-Loss-Start/End`가 들어 있습니다.
`X-PSNR-dB`는 서비스의 `visualDifference` 값으로 쓸 수 있습니다 (지금은 가짜 값).

## 테스트
```bash
pytest -q
```
실제 모델 없이 작은 합성곱 인코더로 **로직만** 검증합니다: 노이즈가 ε를 넘지 않음, 크기 보존, 최적화로 loss 감소, 수준이 높을수록 변화가 큼, API 입출력.

## 효과 확인 (GPU)
`notebooks/verify_on_colab.ipynb` 를 Colab(T4)에서 실행하면 원본과 보호본을 Stable Diffusion img2img에 넣은 결과를 나란히 볼 수 있습니다.
**테스트에는 본인 사진이나 공개 샘플만 사용하고, 타인의 사진은 쓰지 마세요.**

## 서비스에 연결 (Signal-server)
`HttpImageProtector`(서버 코드)가 이 서비스의 `POST /protect`를 호출합니다. **기본값은 `noise`(NoiseImageProtector)**이고, 아래 설정을 켜야 동작합니다.

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `PROTECTION_MODE` | `noise` | `http`로 바꾸면 이 서비스를 호출 |
| `PROTECTION_API_URL` | `http://localhost:8000` (compose에서는 `http://protection:8000`) | 보호 서비스 주소 |
| `PROTECTION_API_TIMEOUT_SECONDS` | `300` | CPU에서는 수 분이 걸릴 수 있음 |

```bash
# 로컬: 보호 서비스 실행 후 서버를 http 모드로 기동
uvicorn app:app --port 8000
PROTECTION_MODE=http ./gradlew bootRun

# 배포(docker compose)
PROTECTION_MODE=http docker compose --profile protection up -d --build
```
- 실패(연결 불가, 타임아웃, 비정상 응답)하면 NoiseImageProtector로 조용히 대체하지 않고 해당 보호 요청이 `FAILED`로 저장됩니다.
- `visualDifference`는 응답 헤더 `X-PSNR-dB`를 `10^(-PSNR/20)`(0~1)로 환산한 값입니다. 예: 40dB → 0.010.
- PyTorch가 필요해 이미지가 큽니다(약 2GB+). 서버 인스턴스 메모리가 부족하면 별도 머신에 두고 `PROTECTION_API_URL`만 바꾸세요.
- 이 Dockerfile과 compose 설정은 작성만 했고 빌드·기동 검증은 하지 않았습니다.

## 라이선스 확인
- VAE `stabilityai/sd-vae-ft-mse`: 모델 카드 라이선스 MIT.
- 효과 확인용 `stable-diffusion-v1-5`: CreativeML OpenRAIL-M (사용 조건 확인 필요).
- 상업화 전에 각 모델과 의존 라이브러리의 라이선스를 다시 확인하세요.
