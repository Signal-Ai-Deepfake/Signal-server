"""적대적 노이즈 보호 처리 (실험 구현).

원리: PhotoGuard의 "encoder attack"을 단순화한 것입니다.
  - 이미지 생성/편집 모델(Stable Diffusion 계열)은 사진을 먼저 인코더(VAE)로 압축해서 읽습니다.
  - 사진 x에 눈에 거의 안 보이는 노이즈 δ (|δ|∞ ≤ ε)를 더해서,
    인코더가 x+δ를 "회색 이미지"처럼 읽도록 PGD로 δ를 최적화합니다.
  - 그러면 그 사진을 편집(img2img, inpainting)하려는 모델 안에서 표현이 깨집니다.

한계 (README 참고): 특정 인코더(SD 1.x/2.x의 KL-f8 VAE)를 겨냥한 것이며, 얼굴 교체형 도구,
JPEG 재압축/리사이즈/스크린샷, 노이즈 제거(purification)에는 약합니다. "변조 방지"가 아닙니다.
"""
from __future__ import annotations

import math
import time
from dataclasses import dataclass
from typing import Callable, Optional

import numpy as np
from PIL import Image, ImageOps

# 수준별 설정. eps는 0~1 스케일에서 픽셀당 최대 변화량(= n/255).
LEVELS = {
    "LIGHT": {"eps": 4 / 255, "steps": 50},
    "NORMAL": {"eps": 8 / 255, "steps": 100},
    "STRONG": {"eps": 16 / 255, "steps": 150},
}
MAX_SIDE = 512  # 노이즈 최적화는 이 크기로 줄여서 수행하고, 노이즈만 원본 크기로 되돌려 더합니다.

# 인코더: [1,3,H,W] (값 범위 [-1,1]) -> latent 텐서
Encoder = Callable[["torch.Tensor"], "torch.Tensor"]


@dataclass
class ProtectResult:
    image: Image.Image
    level: str
    eps_255: float
    steps: int
    work_size: tuple
    loss_start: float
    loss_end: float
    psnr_db: float
    linf_255: int
    seconds: float

    def as_headers(self) -> dict:
        return {
            "X-Protection-Level": self.level,
            "X-Eps-255": f"{self.eps_255:.0f}",
            "X-Steps": str(self.steps),
            "X-PSNR-dB": f"{self.psnr_db:.2f}",
            "X-Linf-255": str(self.linf_255),
            "X-Loss-Start": f"{self.loss_start:.5f}",
            "X-Loss-End": f"{self.loss_end:.5f}",
            "X-Seconds": f"{self.seconds:.2f}",
        }


def load_sd_vae_encoder(device: str = "cpu", model_id: str = "stabilityai/sd-vae-ft-mse") -> Encoder:
    """Stable Diffusion 1.x/2.x가 쓰는 VAE 인코더를 불러옵니다. (MIT 라이선스 모델 카드 기준)"""
    import torch
    from diffusers import AutoencoderKL

    vae = AutoencoderKL.from_pretrained(model_id).to(device).eval()
    for p in vae.parameters():
        p.requires_grad_(False)

    def encode(x: "torch.Tensor") -> "torch.Tensor":
        return vae.encode(x).latent_dist.mean

    return encode


def _work_size(width: int, height: int, max_side: int) -> tuple:
    scale = min(1.0, max_side / max(width, height))
    w = max(64, int(round(width * scale / 8)) * 8)
    h = max(64, int(round(height * scale / 8)) * 8)
    return w, h


def protect_image(
    img: Image.Image,
    level: str,
    encoder: Encoder,
    steps: Optional[int] = None,
    device: str = "cpu",
    seed: int = 0,
    max_side: int = MAX_SIDE,
) -> ProtectResult:
    import torch
    import torch.nn.functional as F

    level = level.upper()
    if level not in LEVELS:
        raise ValueError(f"level must be one of {list(LEVELS)}")
    cfg = LEVELS[level]
    eps = cfg["eps"]
    steps = int(steps if steps is not None else cfg["steps"])
    if steps < 1:
        raise ValueError("steps must be >= 1")

    t0 = time.time()
    img = ImageOps.exif_transpose(img).convert("RGB")
    width, height = img.size
    x_full = np.asarray(img, dtype=np.float32) / 255.0  # H,W,3 in [0,1]

    ww, wh = _work_size(width, height, max_side)
    small = img.resize((ww, wh), Image.LANCZOS)
    x = torch.from_numpy(np.asarray(small, dtype=np.float32) / 255.0).permute(2, 0, 1).unsqueeze(0)
    x = (x * 2 - 1).to(device)  # [-1,1]

    eps_t = eps * 2  # [-1,1] 스케일
    alpha = eps_t / 10
    mse = F.mse_loss

    with torch.no_grad():
        target = encoder(torch.zeros_like(x))  # 회색 이미지의 latent
        loss_start = float(mse(encoder(x), target))

    g = torch.Generator(device="cpu").manual_seed(seed)
    delta = ((torch.rand(x.shape, generator=g) * 2 - 1) * eps_t * 0.1).to(device)
    delta = ((x + delta).clamp(-1, 1) - x).detach()

    for _ in range(steps):
        delta.requires_grad_(True)
        with torch.enable_grad():
            loss = mse(encoder((x + delta).clamp(-1, 1)), target)
            (grad,) = torch.autograd.grad(loss, delta)
        with torch.no_grad():
            delta = delta - alpha * grad.sign()
            delta = delta.clamp(-eps_t, eps_t)
            delta = (x + delta).clamp(-1, 1) - x
        delta = delta.detach()

    with torch.no_grad():
        loss_end = float(mse(encoder((x + delta).clamp(-1, 1)), target))

        # 노이즈만 원본 크기로 되돌려 더한다. (0~1 스케일로 환산)
        d01 = delta / 2
        if (ww, wh) != (width, height):
            d01 = F.interpolate(d01, size=(height, width), mode="bicubic", align_corners=False)
        d01 = d01.clamp(-eps, eps).squeeze(0).permute(1, 2, 0).cpu().numpy()

    out = np.clip(x_full + d01, 0.0, 1.0)
    out_u8 = np.rint(out * 255).astype(np.uint8)
    orig_u8 = np.asarray(img, dtype=np.uint8)
    diff = out_u8.astype(np.int16) - orig_u8.astype(np.int16)
    linf = int(np.abs(diff).max())
    mse_px = float((diff.astype(np.float64) ** 2).mean())
    psnr = 99.0 if mse_px == 0 else 10 * math.log10(255.0 ** 2 / mse_px)

    return ProtectResult(
        image=Image.fromarray(out_u8),
        level=level,
        eps_255=eps * 255,
        steps=steps,
        work_size=(ww, wh),
        loss_start=loss_start,
        loss_end=loss_end,
        psnr_db=psnr,
        linf_255=linf,
        seconds=time.time() - t0,
    )
