"""모델 다운로드 없이 돌아가는 테스트. 실제 VAE 대신 작은 합성곱 인코더로 로직만 검증합니다.
(노이즈 크기 제한, 크기 보존, 최적화 동작, 수준별 차이, API 입출력)"""
import io
import sys
from pathlib import Path

import numpy as np
import pytest
import torch
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from fastapi.testclient import TestClient  # noqa: E402

from app import create_app  # noqa: E402
from protect_core import LEVELS, protect_image  # noqa: E402


def tiny_encoder():
    torch.manual_seed(0)
    net = torch.nn.Sequential(
        torch.nn.Conv2d(3, 8, 3, stride=2, padding=1),
        torch.nn.Tanh(),
        torch.nn.Conv2d(8, 4, 3, stride=2, padding=1),
    ).eval()
    for p in net.parameters():
        p.requires_grad_(False)
    return lambda x: net(x)


def sample_image(w=301, h=203, seed=1):
    rng = np.random.default_rng(seed)
    base = rng.integers(40, 215, size=(h, w, 3), dtype=np.uint8)
    return Image.fromarray(base)


@pytest.mark.parametrize("level", list(LEVELS))
def test_perturbation_never_exceeds_eps(level):
    img = sample_image()
    r = protect_image(img, level, tiny_encoder(), steps=15)
    assert r.linf_255 <= LEVELS[level]["eps"] * 255 + 1e-6


def test_output_keeps_original_size_even_when_downscaled_for_work():
    img = sample_image(1200, 800)
    r = protect_image(img, "NORMAL", tiny_encoder(), steps=5, max_side=256)
    assert r.image.size == (1200, 800)
    assert max(r.work_size) <= 256 + 8


def test_optimization_reduces_loss():
    r = protect_image(sample_image(), "STRONG", tiny_encoder(), steps=40)
    assert r.loss_end < r.loss_start


def test_stronger_level_changes_more():
    enc = tiny_encoder()
    img = sample_image()
    light = protect_image(img, "LIGHT", enc, steps=20)
    strong = protect_image(img, "STRONG", enc, steps=20)
    assert strong.psnr_db < light.psnr_db


def test_invalid_level_raises():
    with pytest.raises(ValueError):
        protect_image(sample_image(), "EXTREME", tiny_encoder())


def test_api_returns_png_with_metrics():
    with TestClient(create_app(encoder_factory=tiny_encoder, device="cpu")) as client:
        buf = io.BytesIO()
        sample_image().save(buf, format="JPEG")
        r = client.post("/protect", files={"file": ("a.jpg", buf.getvalue(), "image/jpeg")},
                        data={"level": "LIGHT", "steps": "5"})
        assert r.status_code == 200
        assert r.headers["content-type"] == "image/png"
        assert float(r.headers["x-psnr-db"]) > 20
        out = Image.open(io.BytesIO(r.content))
        assert out.size == (301, 203)


def test_api_rejects_bad_input():
    with TestClient(create_app(encoder_factory=tiny_encoder, device="cpu")) as client:
        assert client.post("/protect", files={"file": ("a.txt", b"not an image", "text/plain")}).status_code == 400
        buf = io.BytesIO(); sample_image().save(buf, format="PNG")
        assert client.post("/protect", files={"file": ("a.png", buf.getvalue(), "image/png")},
                           data={"level": "NOPE"}).status_code == 422
        assert client.get("/health").json()["status"] == "ok"
