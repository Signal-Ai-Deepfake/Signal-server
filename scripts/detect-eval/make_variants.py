#!/usr/bin/env python3
"""평가 세트의 원본 이미지에서 "실제로 유통되는 모습"의 변형 버전을 만든다.

메신저·SNS를 거치면 이미지가 재압축되고 줄어든다. 탐지기가 원본에서만 잘 맞고 변형에서 무너지는지 보려고 쓴다.
Pillow가 필요하다(pip install pillow). 원본 매니페스트에 변형 행(variant, variant_of)을 더한 새 매니페스트를 쓴다.

    python3 scripts/detect-eval/make_variants.py --manifest eval/manifest.csv --out-dir eval/variants \\
        --out-manifest eval/manifest.with-variants.csv

변형 종류(특정 서비스를 그대로 재현한 것이 아니라 비슷한 열화를 주는 근사다):
  jpeg70      JPEG 품질 70으로 재압축
  resize512   긴 변 512px로 축소(PNG)
  messenger   긴 변 1280px로 축소 + JPEG 품질 75
  screenshot  1080x2340 화면 안에 가로 폭을 맞춰 넣은 캡처(위아래 여백 포함, PNG)
"""
import argparse
import csv
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import manifest as manifest_mod  # noqa: E402

VARIANTS = ["jpeg70", "resize512", "messenger", "screenshot"]


def _flatten(img):
    from PIL import Image
    if img.mode in ("RGBA", "LA", "P"):
        base = Image.new("RGB", img.size, (255, 255, 255))
        base.paste(img.convert("RGBA"), mask=img.convert("RGBA").split()[-1])
        return base
    return img.convert("RGB")


def _fit_long_edge(img, long_edge):
    from PIL import Image
    w, h = img.size
    scale = long_edge / max(w, h)
    if scale >= 1:
        return img
    return img.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)


def make(img, variant, out_path):
    from PIL import Image
    img = _flatten(img)
    if variant == "jpeg70":
        img.save(out_path, "JPEG", quality=70)
    elif variant == "resize512":
        _fit_long_edge(img, 512).save(out_path, "PNG")
    elif variant == "messenger":
        _fit_long_edge(img, 1280).save(out_path, "JPEG", quality=75)
    elif variant == "screenshot":
        screen_w, screen_h = 1080, 2340
        scaled = img.resize((screen_w, max(1, round(img.height * screen_w / img.width))), Image.LANCZOS)
        scaled = scaled.crop((0, 0, screen_w, min(scaled.height, screen_h)))
        canvas = Image.new("RGB", (screen_w, screen_h), (18, 18, 18))
        canvas.paste(scaled, (0, (screen_h - scaled.height) // 2))
        canvas.save(out_path, "PNG")
    else:
        raise ValueError(variant)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--manifest", required=True)
    p.add_argument("--images-root", help="기본: 매니페스트가 있는 폴더")
    p.add_argument("--out-dir", required=True, help="변형 이미지를 쓸 폴더(images-root 아래여야 함)")
    p.add_argument("--out-manifest", required=True)
    p.add_argument("--variants", nargs="+", default=VARIANTS, choices=VARIANTS)
    args = p.parse_args(argv)

    try:
        from PIL import Image
    except ImportError:
        print("Pillow가 필요합니다: pip install pillow", file=sys.stderr)
        return 2

    items = manifest_mod.load(args.manifest)
    root = Path(args.images_root or Path(args.manifest).resolve().parent).resolve()
    out_dir = Path(args.out_dir).resolve()
    try:
        out_dir.relative_to(root)
    except ValueError:
        print("--out-dir은 --images-root 아래여야 합니다(매니페스트 path가 상대 경로라서).", file=sys.stderr)
        return 2
    out_dir.mkdir(parents=True, exist_ok=True)

    rows = []
    for it in items:
        rows.append(it)
        if it.variant != "original":
            continue
        with Image.open(root / it.path) as img:
            img.load()
            for variant in args.variants:
                stem = Path(it.path).with_suffix("").as_posix().replace("/", "__")
                suffix = ".jpg" if variant in ("jpeg70", "messenger") else ".png"
                out = out_dir / f"{stem}.{variant}{suffix}"
                make(img, variant, out)
                rows.append(type(it)(path=out.relative_to(root).as_posix(), label=it.label, variant=variant,
                                     variant_of=it.path, generator=it.generator, source=it.source, license=it.license))

    with open(args.out_manifest, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["path", "label", "variant", "variant_of", "generator", "source", "license"])
        for it in rows:
            w.writerow([it.path, "fake" if it.label else "real", it.variant, it.variant_of, it.generator,
                        it.source, it.license])
    print(f"원본 {sum(1 for i in items if i.variant == 'original')}장 -> 변형 포함 {len(rows)}행: {args.out_manifest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
