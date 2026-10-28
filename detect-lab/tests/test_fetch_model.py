import pytest

from fetch_model import check_config

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
