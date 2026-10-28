#!/usr/bin/env python3
"""run_eval.py가 만든 JSON 리포트 여러 개를 한 표로 비교한다.

    python3 scripts/detect-eval/compare_reports.py reports/baseline-llm.json reports/community-forensics.json

같은 평가 세트(같은 매니페스트)로 잰 리포트끼리만 비교해야 한다. 임계값이 다르면 오탐·미탐률은 직접 비교할 수 없으니
AUC와 "오탐률 1%·5%일 때 탐지율"을 함께 본다.
"""
import json
import sys
from pathlib import Path


def pct(x):
    return "-" if x is None else f"{x * 100:.1f}%"


def main(paths):
    if len(paths) < 1:
        print(__doc__)
        return 2
    reports = [(Path(p).stem, json.loads(Path(p).read_text(encoding="utf-8"))) for p in paths]
    header = ["탐지기", "n", "실패", "AUC", "오탐률", "미탐률", "TPR@FPR1%", "TPR@FPR5%", "지연p50(ms)", "임계값"]
    rows = []
    for name, r in reports:
        o = r.get("overall") or {}
        rows.append([name, str(r.get("total")), pct(r["failed"]["rate"]),
                     "-" if o.get("auc") is None else f"{o['auc']:.3f}", pct(o.get("fpr")), pct(o.get("fnr")),
                     pct(o.get("tpr_at_fpr_1")), pct(o.get("tpr_at_fpr_5")),
                     "-" if r["latency_ms"]["p50"] is None else f"{r['latency_ms']['p50']:.0f}", str(r["threshold"])])
    widths = [max(len(header[i]), *(len(row[i]) for row in rows)) for i in range(len(header))]
    line = lambda cells: "  ".join(c.ljust(w) for c, w in zip(cells, widths))
    print(line(header))
    print(line(["-" * w for w in widths]))
    for row in rows:
        print(line(row))
    # 변형별 AUC
    variants = sorted({v for _, r in reports for v in r["by_variant"]})
    if variants:
        print("\n변형별 AUC")
        print("  ".join(["탐지기".ljust(widths[0])] + [v.ljust(12) for v in variants]))
        for name, r in reports:
            cells = []
            for v in variants:
                s = r["by_variant"].get(v)
                cells.append(("-" if not s or s["auc"] is None else f"{s['auc']:.3f}").ljust(12))
            print("  ".join([name.ljust(widths[0])] + cells))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
