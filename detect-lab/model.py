"""ONNX 모델 실행. 출력은 로짓 1개이고 sigmoid를 거친 값이 "가짜일 확률"이다(모델 카드)."""
from __future__ import annotations

import hashlib
import math
from pathlib import Path

import numpy as np


def sigmoid(x: float) -> float:
    if x >= 0:
        return 1.0 / (1.0 + math.exp(-x))
    e = math.exp(x)
    return e / (1.0 + e)


def file_sha256(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


class OnnxModel:
    def __init__(self, path: str, name: str, version: str | None = None, threads: int = 2):
        import onnxruntime as ort

        if not Path(path).is_file():
            raise FileNotFoundError(f"모델 파일이 없습니다: {path}")
        options = ort.SessionOptions()
        options.intra_op_num_threads = threads
        self.session = ort.InferenceSession(path, options, providers=["CPUExecutionProvider"])
        self.input_name = self.session.get_inputs()[0].name
        self.name = name
        # 어떤 가중치로 낸 점수인지 추적할 수 있게 파일 해시를 버전에 붙인다.
        self.version = f"{version or 'unversioned'}+{file_sha256(path)[:12]}"

    def predict(self, batch: np.ndarray) -> float:
        logits = self.session.run(None, {self.input_name: batch})[0]
        return sigmoid(float(np.ravel(logits)[0]))
