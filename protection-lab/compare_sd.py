"""보호 효과 확인용: 원본과 보호본을 같은 조건으로 Stable Diffusion img2img에 넣어 결과를 나란히 저장합니다.

  python compare_sd.py original.jpg protected.png --out grid.png --strength 0.6

GPU 권장 (Colab T4 무료 사용 가능). 눈으로 비교하는 용도이며 정량 평가가 아닙니다.
검증 모델: stable-diffusion-v1-5 (CreativeML OpenRAIL-M 라이선스, 사용 조건 확인 필요)
"""
import argparse

import torch
from diffusers import StableDiffusionImg2ImgPipeline
from PIL import Image


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("original")
    ap.add_argument("protected")
    ap.add_argument("--out", default="grid.png")
    ap.add_argument("--prompt", default="a photo of a person, natural light")
    ap.add_argument("--strength", type=float, default=0.6)
    ap.add_argument("--seed", type=int, default=0)
    ap.add_argument("--model", default="stable-diffusion-v1-5/stable-diffusion-v1-5")
    a = ap.parse_args()

    device = "cuda" if torch.cuda.is_available() else "cpu"
    dtype = torch.float16 if device == "cuda" else torch.float32
    pipe = StableDiffusionImg2ImgPipeline.from_pretrained(a.model, torch_dtype=dtype, safety_checker=None).to(device)

    def edit(path):
        img = Image.open(path).convert("RGB").resize((512, 512), Image.LANCZOS)
        g = torch.Generator(device=device).manual_seed(a.seed)
        out = pipe(prompt=a.prompt, image=img, strength=a.strength, guidance_scale=7.5, generator=g).images[0]
        return img, out

    o_in, o_out = edit(a.original)
    p_in, p_out = edit(a.protected)
    grid = Image.new("RGB", (512 * 4, 512))
    for i, im in enumerate([o_in, p_in, o_out, p_out]):
        grid.paste(im, (512 * i, 0))
    grid.save(a.out)
    print(f"saved {a.out}  (왼쪽부터: 원본 | 보호본 | 원본 편집 결과 | 보호본 편집 결과)")


if __name__ == "__main__":
    main()
