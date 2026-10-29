"""평가 세트 매니페스트(CSV) 읽기와 검증.

필수 열: path, label(real|fake)
선택 열: variant(original|jpeg70|...; 기본 original), variant_of(원본 path), generator, source, license
path는 --images-root 기준 상대 경로다. 이미지 파일은 레포에 넣지 않는다.
"""
from __future__ import annotations

import csv
from dataclasses import dataclass
from pathlib import Path

LABELS = {"real": 0, "fake": 1}


@dataclass
class Item:
    path: str
    label: int
    variant: str = "original"
    variant_of: str = ""
    generator: str = ""
    source: str = ""
    license: str = ""


def load(manifest_path):
    items = []
    with open(manifest_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        missing = {"path", "label"} - set(reader.fieldnames or [])
        if missing:
            raise ValueError(f"매니페스트에 필수 열이 없습니다: {sorted(missing)}")
        for lineno, row in enumerate(reader, start=2):
            label = (row.get("label") or "").strip().lower()
            if label not in LABELS:
                raise ValueError(f"{manifest_path}:{lineno} label은 real 또는 fake여야 합니다: {label!r}")
            path = (row.get("path") or "").strip()
            if not path:
                raise ValueError(f"{manifest_path}:{lineno} path가 비어 있습니다")
            items.append(Item(
                path=path, label=LABELS[label],
                variant=(row.get("variant") or "original").strip() or "original",
                variant_of=(row.get("variant_of") or "").strip(),
                generator=(row.get("generator") or "").strip(),
                source=(row.get("source") or "").strip(),
                license=(row.get("license") or "").strip()))
    return items


def check(items, images_root):
    """문제 목록(문자열)을 돌려준다. 비어 있으면 통과."""
    problems = []
    root = Path(images_root)
    seen = set()
    paths = {i.path for i in items}
    for it in items:
        if it.path in seen:
            problems.append(f"중복 path: {it.path}")
        seen.add(it.path)
        if not (root / it.path).is_file():
            problems.append(f"파일이 없습니다: {it.path}")
        if it.variant != "original" and it.variant_of not in paths:
            problems.append(f"변형의 원본(variant_of)이 매니페스트에 없습니다: {it.path} -> {it.variant_of!r}")
        if it.variant == "original" and not it.source:
            problems.append(f"출처(source)가 비어 있습니다: {it.path}")
        if it.variant == "original" and not it.license:
            problems.append(f"라이선스(license)가 비어 있습니다: {it.path}")
        if it.label == 1 and it.variant == "original" and not it.generator:
            problems.append(f"합성 이미지인데 생성기(generator)가 비어 있습니다: {it.path}")
    return problems
