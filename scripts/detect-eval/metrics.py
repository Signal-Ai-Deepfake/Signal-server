"""탐지 평가 지표. 표준 라이브러리만 사용한다.

점수는 모두 "합성(가짜)일 가능성"이고 0~1이다(클수록 가짜). 라벨은 1=fake, 0=real.
"""
from __future__ import annotations


def auc(scores, labels):
    """ROC AUC (Mann-Whitney U, 동점은 0.5로 계산). 한쪽 라벨이 없으면 None."""
    pos = [s for s, y in zip(scores, labels) if y == 1]
    neg = [s for s, y in zip(scores, labels) if y == 0]
    if not pos or not neg:
        return None
    wins = 0.0
    neg_sorted = sorted(neg)
    for p in pos:
        lo = _bisect_left(neg_sorted, p)
        hi = _bisect_right(neg_sorted, p)
        wins += lo + 0.5 * (hi - lo)
    return wins / (len(pos) * len(neg))


def _bisect_left(a, x):
    lo, hi = 0, len(a)
    while lo < hi:
        mid = (lo + hi) // 2
        if a[mid] < x:
            lo = mid + 1
        else:
            hi = mid
    return lo


def _bisect_right(a, x):
    lo, hi = 0, len(a)
    while lo < hi:
        mid = (lo + hi) // 2
        if x < a[mid]:
            hi = mid
        else:
            lo = mid + 1
    return lo


def confusion(scores, labels, threshold):
    """score >= threshold 이면 fake로 판정한다."""
    tp = fp = tn = fn = 0
    for s, y in zip(scores, labels):
        predicted_fake = s >= threshold
        if y == 1 and predicted_fake:
            tp += 1
        elif y == 1:
            fn += 1
        elif predicted_fake:
            fp += 1
        else:
            tn += 1
    return {"tp": tp, "fp": fp, "tn": tn, "fn": fn}


def rates(conf):
    """오탐률(FPR: real을 fake로), 미탐률(FNR: fake를 놓침), 정확도. 분모가 0이면 None."""
    tp, fp, tn, fn = conf["tp"], conf["fp"], conf["tn"], conf["fn"]
    n_real, n_fake = fp + tn, tp + fn
    total = n_real + n_fake
    return {
        "fpr": fp / n_real if n_real else None,
        "fnr": fn / n_fake if n_fake else None,
        "accuracy": (tp + tn) / total if total else None,
    }


def tpr_at_fpr(scores, labels, max_fpr):
    """오탐률이 max_fpr 이하인 임계값 중 탐지율(TPR)이 가장 높은 지점. (tpr, threshold) 또는 None.

    real이 적으면 max_fpr=0.01 같은 값은 사실상 "오탐 0개"를 뜻해서 불안정하다. 항상 n_real과 함께 해석한다.
    """
    n_real = sum(1 for y in labels if y == 0)
    n_fake = sum(1 for y in labels if y == 1)
    if not n_real or not n_fake:
        return None
    best = None
    for t in sorted(set(scores), reverse=True):
        r = rates(confusion(scores, labels, t))
        if r["fpr"] is not None and r["fpr"] <= max_fpr:
            tpr = 1.0 - r["fnr"]
            if best is None or tpr > best[0] or (tpr == best[0] and t < best[1]):
                best = (tpr, t)
    if best is None:  # 가장 높은 점수보다 높게 잡으면 아무것도 fake로 판정하지 않는다(오탐 0, 탐지 0)
        return (0.0, max(scores) + 1e-9)
    return best


def summarize(scores, labels, threshold):
    """한 그룹의 요약 지표."""
    n_real = sum(1 for y in labels if y == 0)
    n_fake = sum(1 for y in labels if y == 1)
    out = {"n": len(scores), "n_real": n_real, "n_fake": n_fake, "threshold": threshold}
    out["auc"] = auc(scores, labels)
    out.update(rates(confusion(scores, labels, threshold)))
    for name, fpr in (("tpr_at_fpr_1", 0.01), ("tpr_at_fpr_5", 0.05)):
        hit = tpr_at_fpr(scores, labels, fpr)
        out[name] = hit[0] if hit else None
    return out


def percentile(values, q):
    """선형 보간 없는 단순 백분위수(q: 0~100). 빈 목록이면 None."""
    if not values:
        return None
    ordered = sorted(values)
    idx = min(len(ordered) - 1, max(0, round(q / 100 * (len(ordered) - 1))))
    return ordered[idx]
