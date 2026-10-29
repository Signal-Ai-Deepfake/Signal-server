"""모델 가중치(ONNX FP32)와 config.json을 내려받고, 모델 카드가 안내한 정정본인지 확인한다.

    python fetch_model.py

이미 있으면 다시 받지 않는다. 기본값은 CommunityForensics ViT-S의 FP32 ONNX이며, 양자화 ONNX는 FP32와 점수가
크게 달라질 수 있어(모델 카드) 기본으로 쓰지 않는다.

새로 받을 때는 DETECT_MODEL_REVISION에 커밋 해시(40자리)를 반드시 줘야 한다. main 같은 브랜치 이름이면 컨테이너를 다시 만들 때마다
가중치가 바뀔 수 있어 평가(#35)로 고른 모델과 운영 모델이 달라진다. DETECT_MODEL_SHA256을 주면 내려받은(또는 이미 있는) 파일의 해시도 확인한다.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import sys
from pathlib import Path

REPO = os.getenv("DETECT_MODEL_REPO", "buildborderless/CommunityForensics-DeepfakeDet-ViT")
FILE = os.getenv("DETECT_MODEL_FILE", "onnx/model.onnx")
REVISION = os.getenv("DETECT_MODEL_REVISION", "").strip()
EXPECTED_SHA256 = os.getenv("DETECT_MODEL_SHA256", "").strip().lower()
MODEL_DIR = Path(os.getenv("DETECT_MODEL_DIR", "/models"))


def check_config(config: dict) -> None:
    """2026-07 정정 이전의 옛 설정이면 예외. 모델 카드의 확인 방법과 같다."""
    expected = {"num_labels": 1, "num_attention_heads": 6, "intermediate_size": 1536}
    wrong = {k: config.get(k) for k, v in expected.items() if config.get(k) != v}
    if wrong:
        raise ValueError(f"옛 설정(정정 이전)으로 보입니다. 다시 받으세요: 기대 {expected}, 실제 {wrong}")


def check_revision(revision: str) -> None:
    """브랜치·태그 이름이 아닌 커밋 해시(40자리 16진수)만 받는다."""
    if not re.fullmatch(r"[0-9a-f]{40}", revision or ""):
        raise ValueError(
            "DETECT_MODEL_REVISION에 모델 저장소의 커밋 해시(40자리)를 지정하세요. "
            f"main 같은 이름은 받을 때마다 가중치가 달라질 수 있어 쓰지 않습니다. (현재 값: {revision!r})")


def file_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def check_sha256(path: Path, expected: str) -> None:
    """expected가 비어 있으면 건너뛴다. 다르면 예외."""
    if not expected:
        return
    actual = file_sha256(path)
    if actual != expected:
        raise ValueError(f"가중치 sha256이 기대와 다릅니다: 기대 {expected}, 실제 {actual} ({path})")


def main() -> int:
    target = MODEL_DIR / FILE
    config_path = MODEL_DIR / "config.json"
    if target.is_file() and config_path.is_file():
        check_config(json.loads(config_path.read_text(encoding="utf-8")))
        check_sha256(target, EXPECTED_SHA256)
        print(f"이미 있습니다: {target}")
        return 0
    check_revision(REVISION)
    from huggingface_hub import hf_hub_download

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    config_file = hf_hub_download(REPO, "config.json", revision=REVISION, local_dir=str(MODEL_DIR))
    check_config(json.loads(Path(config_file).read_text(encoding="utf-8")))
    model_file = hf_hub_download(REPO, FILE, revision=REVISION, local_dir=str(MODEL_DIR))
    check_sha256(Path(model_file), EXPECTED_SHA256)
    print(f"받았습니다: {model_file} (repo={REPO}, revision={REVISION})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
