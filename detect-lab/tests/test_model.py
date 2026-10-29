import math

import numpy as np
import pytest

from conftest import SCALE
from model import OnnxModel, sigmoid
from preprocess import MEAN, STD


def test_sigmoid는_극단값에서도_안정적():
    assert sigmoid(0) == 0.5
    assert sigmoid(1000) == 1.0
    assert sigmoid(-1000) == pytest.approx(0.0)
    assert sigmoid(2.0) == pytest.approx(1 / (1 + math.exp(-2.0)))


def test_모델_예측은_로짓에_sigmoid를_적용한다(tiny_onnx):
    model = OnnxModel(tiny_onnx, "tiny", "t1")
    batch = np.full((1, 3, 384, 384), 0.1, dtype=np.float32)
    assert model.predict(batch) == pytest.approx(sigmoid(0.1 * SCALE), abs=1e-3)  # float32 평균 누적 오차


def test_버전에_가중치_해시가_붙는다(tiny_onnx):
    model = OnnxModel(tiny_onnx, "tiny", "t1")
    assert model.version.startswith("t1+")
    assert len(model.version.split("+")[1]) == 12
    assert OnnxModel(tiny_onnx, "tiny").version.startswith("unversioned+")


def test_모델_파일이_없으면_FileNotFoundError(tmp_path):
    with pytest.raises(FileNotFoundError):
        OnnxModel(str(tmp_path / "none.onnx"), "x")
