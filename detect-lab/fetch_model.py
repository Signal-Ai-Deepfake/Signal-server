"""모델 가중치(ONNX FP32)와 config.json을 내려받고, 모델 카드가 안내한 정정본인지 확인한다.

    python fetch_model.py

이미 있으면 다시 받지 않는다. 기본값은 CommunityForensics ViT-S의 FP32 ONNX이며, 양자화 ONNX는 FP32와 점수가
크게 달라질 수 있어(모델 카드) 기본으로 쓰지 않는다. 버전을 고정하려면 DETECT_MODEL_REVISION에 커밋 해시를 준다.
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

REPO = os.getenv("DETECT_MODEL_REPO", "buildborderless/CommunityForensics-DeepfakeDet-ViT")
FILE = os.getenv("DETECT_MODEL_FILE", "onnx/model.onnx")
REVISION = os.getenv("DETECT_MODEL_REVISION", "main")
MODEL_DIR = Path(os.getenv("DETECT_MODEL_DIR", "/models"))


def check_config(config: dict) -> None:
    """2026-07 정정 이전의 옛 설정이면 예외. 모델 카드의 확인 방법과 같다."""
    expected = {"num_labels": 1, "num_attention_heads": 6, "intermediate_size": 1536}
    wrong = {k: config.get(k) for k, v in expected.items() if config.get(k) != v}
    if wrong:
        raise ValueError(f"옛 설정(정정 이전)으로 보입니다. 다시 받으세요: 기대 {expected}, 실제 {wrong}")


def main() -> int:
    target = MODEL_DIR / FILE
    config_path = MODEL_DIR / "config.json"
    if target.is_file() and config_path.is_file():
        check_config(json.loads(config_path.read_text(encoding="utf-8")))
        print(f"이미 있습니다: {target}")
        return 0
    from huggingface_hub import hf_hub_download

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    config_file = hf_hub_download(REPO, "config.json", revision=REVISION, local_dir=str(MODEL_DIR))
    check_config(json.loads(Path(config_file).read_text(encoding="utf-8")))
    model_file = hf_hub_download(REPO, FILE, revision=REVISION, local_dir=str(MODEL_DIR))
    print(f"받았습니다: {model_file} (repo={REPO}, revision={REVISION})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
