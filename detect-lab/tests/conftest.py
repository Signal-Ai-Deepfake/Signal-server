import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

onnx = pytest.importorskip("onnx")
from onnx import TensorProto, helper  # noqa: E402

SCALE = 10.0


@pytest.fixture(scope="session")
def tiny_onnx(tmp_path_factory):
    """입력 평균에 10을 곱한 값을 로짓으로 내는 작은 가짜 모델.

    실제 가중치 없이 전처리와 API를 숫자로 검증하려는 것이다. 단색 이미지라면 점수를 계산해서 맞출 수 있다.
    """
    x = helper.make_tensor_value_info("pixel_values", TensorProto.FLOAT, [1, 3, 384, 384])
    y = helper.make_tensor_value_info("logits", TensorProto.FLOAT, [1, 1])
    scale = helper.make_tensor("scale", TensorProto.FLOAT, [], [SCALE])
    shape = helper.make_tensor("shape", TensorProto.INT64, [2], [1, 1])
    nodes = [
        helper.make_node("ReduceMean", ["pixel_values"], ["mean"], axes=[1, 2, 3], keepdims=0),
        helper.make_node("Mul", ["mean", "scale"], ["scaled"]),
        helper.make_node("Reshape", ["scaled", "shape"], ["logits"]),
    ]
    graph = helper.make_graph(nodes, "tiny", [x], [y], initializer=[scale, shape])
    model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)])
    model.ir_version = 8
    onnx.checker.check_model(model)
    path = tmp_path_factory.mktemp("model") / "tiny.onnx"
    onnx.save(model, str(path))
    return str(path)
