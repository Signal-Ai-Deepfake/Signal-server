"""명령줄에서 사진 1장 처리: python cli.py photo.jpg protected.png --level NORMAL"""
import argparse

from PIL import Image

from protect_core import LEVELS, load_sd_vae_encoder, protect_image


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input")
    ap.add_argument("output")
    ap.add_argument("--level", default="NORMAL", choices=list(LEVELS))
    ap.add_argument("--steps", type=int, default=None, help="미지정 시 수준별 기본값(LIGHT 50, NORMAL 100, STRONG 150)")
    ap.add_argument("--device", default=None, help="cpu 또는 cuda (미지정 시 자동)")
    a = ap.parse_args()

    device = a.device
    if device is None:
        import torch

        device = "cuda" if torch.cuda.is_available() else "cpu"
    enc = load_sd_vae_encoder(device)
    res = protect_image(Image.open(a.input), a.level, enc, steps=a.steps, device=device)
    res.image.save(a.output, format="PNG")
    print(f"level={res.level} eps={res.eps_255:.0f}/255 steps={res.steps} work_size={res.work_size}")
    print(f"loss {res.loss_start:.5f} -> {res.loss_end:.5f}  (낮을수록 인코더가 회색으로 읽음)")
    print(f"PSNR {res.psnr_db:.2f} dB, 최대 픽셀 변화 {res.linf_255}/255, {res.seconds:.1f}s")


if __name__ == "__main__":
    main()
