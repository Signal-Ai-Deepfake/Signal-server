import pytest

from fetch_model import check_config, check_revision, check_sha256, file_sha256

GOOD = {"num_labels": 1, "num_attention_heads": 6, "intermediate_size": 1536, "hidden_size": 384}


def test_정정된_설정은_통과():
    check_config(GOOD)


@pytest.mark.parametrize("field,value", [("num_labels", 2), ("num_attention_heads", 12), ("intermediate_size", 3072)])
def test_옛_설정이면_예외(field, value):
    with pytest.raises(ValueError, match="옛 설정"):
        check_config({**GOOD, field: value})


def test_필드가_없어도_예외():
    with pytest.raises(ValueError):
        check_config({})


COMMIT = "0123456789abcdef0123456789abcdef01234567"


def test_커밋_해시면_통과():
    check_revision(COMMIT)


@pytest.mark.parametrize("revision", ["", "main", "v1.0", COMMIT[:7], COMMIT.upper(), COMMIT + "0"])
def test_해시가_아니면_예외(revision):
    with pytest.raises(ValueError, match="DETECT_MODEL_REVISION"):
        check_revision(revision)


def test_sha256이_맞으면_통과_다르면_예외(tmp_path):
    f = tmp_path / "model.onnx"
    f.write_bytes(b"weights")
    good = file_sha256(f)
    check_sha256(f, good)
    with pytest.raises(ValueError, match="sha256"):
        check_sha256(f, "0" * 64)


def test_sha256을_주지_않으면_확인하지_않는다(tmp_path):
    f = tmp_path / "model.onnx"
    f.write_bytes(b"weights")
    check_sha256(f, "")
