#!/usr/bin/env python3
"""딥페이크 탐지 평가 러너.

같은 평가 세트(매니페스트)로 탐지기를 재서 오탐률·미탐률·AUC·변형별 성능을 낸다.
탐지기가 달라도 같은 세트·같은 지표라서 결과(JSON)를 compare_reports.py로 나란히 볼 수 있다.

    # 매니페스트만 검증(라벨, 변형의 원본 연결, 출처·라이선스·생성기 기록)
    python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.csv --check

    # 추론 서비스(detect-lab)를 직접 평가
    python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.csv --detector http \\
        --base-url http://localhost:8000 --report reports/community-forensics.json

    # 현재 LLM 탐지(기준선) 또는 서버 전체를 평가 (로컬/개발 서버에서만)
    python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.csv --detector server \\
        --base-url http://localhost:8080 --report reports/baseline-llm.json

점수는 "합성일 가능성"(0~1)이다. 임계값 이상이면 fake로 판정한다. 의존성 없음(표준 라이브러리만).
이미지는 레포에 넣지 않고 --images-root 아래에 둔다. 실제 피해자 이미지는 평가 세트에 쓰지 않는다.
"""
import argparse
import json
import sys
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import manifest as manifest_mod  # noqa: E402
import metrics  # noqa: E402
from detectors import HttpDetector, ServerDetector  # noqa: E402


def evaluate(items, results, threshold, abstain_low=None, abstain_high=None):
    """items와 같은 순서의 results로 지표를 계산해 리포트 dict를 만든다."""
    scored, failed = [], defaultdict(int)
    for it, res in zip(items, results):
        if res.score is None:
            failed[res.error or "unknown"] += 1
            continue
        scored.append((it, res))

    abstained = 0
    if abstain_low is not None and abstain_high is not None:
        kept = []
        for it, res in scored:
            if abstain_low <= res.score <= abstain_high:
                abstained += 1  # 판단 보류 구간: 지표에서 빼고 보류율로 따로 센다
            else:
                kept.append((it, res))
        scored = kept

    def group(rows):
        return metrics.summarize([r.score for _, r in rows], [i.label for i, _ in rows], threshold)

    by_variant = defaultdict(list)
    by_generator = defaultdict(list)
    for it, res in scored:
        by_variant[it.variant].append((it, res))
        if it.label == 1 and it.generator:
            by_generator[it.generator].append((it, res))

    scores_by_path = {it.path: res.score for it, res in scored}
    robustness = {}
    for variant, rows in by_variant.items():
        if variant == "original":
            continue
        deltas, flips = [], 0
        for it, res in rows:
            original = scores_by_path.get(it.variant_of)
            if original is None:
                continue
            deltas.append(abs(res.score - original))
            if (res.score >= threshold) != (original >= threshold):
                flips += 1
        if deltas:
            robustness[variant] = {"pairs": len(deltas), "mean_abs_score_delta": sum(deltas) / len(deltas),
                                   "verdict_flip_rate": flips / len(deltas)}

    latencies = [res.latency_ms for _, res in zip(items, results)]
    models = sorted({f"{r.model}@{r.model_version}" if r.model_version else (r.model or "")
                     for r in results if r.model})
    return {
        "threshold": threshold,
        "models": models,
        "total": len(items),
        "failed": {"count": sum(failed.values()), "rate": sum(failed.values()) / len(items) if items else None,
                   "reasons": dict(failed)},
        "abstained": {"count": abstained, "low": abstain_low, "high": abstain_high},
        "overall": group(scored) if scored else None,
        "by_variant": {v: group(rows) for v, rows in sorted(by_variant.items())},
        "by_generator_fake_only": {g: {"n": len(rows), "fnr": metrics.rates(metrics.confusion(
            [r.score for _, r in rows], [1] * len(rows), threshold))["fnr"]} for g, rows in sorted(by_generator.items())},
        "robustness_vs_original": robustness,
        "latency_ms": {"p50": metrics.percentile(latencies, 50), "p95": metrics.percentile(latencies, 95)},
    }


def print_report(report, name):
    def pct(x):
        return "-" if x is None else f"{x * 100:.1f}%"

    def num(x):
        return "-" if x is None else f"{x:.3f}"

    o = report["overall"]
    print(f"\n== {name} (임계값 {report['threshold']}) ==")
    print(f"전체 {report['total']}장 | 실패 {report['failed']['count']} ({pct(report['failed']['rate'])})"
          f" | 판단 보류 {report['abstained']['count']}")
    if report["failed"]["reasons"]:
        print("  실패 이유:", report["failed"]["reasons"])
    if o:
        print(f"  AUC {num(o['auc'])} | 오탐률 {pct(o['fpr'])} (real {o['n_real']}장) | 미탐률 {pct(o['fnr'])} "
              f"(fake {o['n_fake']}장) | 정확도 {pct(o['accuracy'])}")
        print(f"  오탐률 1%일 때 탐지율 {pct(o['tpr_at_fpr_1'])} | 5%일 때 {pct(o['tpr_at_fpr_5'])}"
              f"  (real이 적으면 이 값은 불안정합니다)")
    print("  변형별:")
    for variant, s in report["by_variant"].items():
        print(f"    {variant:<12} n={s['n']:<4} AUC {num(s['auc'])}  오탐률 {pct(s['fpr'])}  미탐률 {pct(s['fnr'])}")
    if report["robustness_vs_original"]:
        print("  원본 대비 점수 변화:")
        for variant, r in report["robustness_vs_original"].items():
            print(f"    {variant:<12} 평균 |Δ점수| {r['mean_abs_score_delta']:.3f}  판정 뒤집힘 {pct(r['verdict_flip_rate'])}")
    if report["by_generator_fake_only"]:
        print("  생성기별 미탐률(fake만):")
        for g, s in report["by_generator_fake_only"].items():
            print(f"    {g:<20} n={s['n']:<4} 미탐률 {pct(s['fnr'])}")
    lat = report["latency_ms"]
    print(f"  지연 p50 {lat['p50']:.0f}ms / p95 {lat['p95']:.0f}ms" if lat["p50"] is not None else "")


def make_detector(args):
    if args.detector == "http":
        return HttpDetector(args.base_url, timeout=args.timeout)
    return ServerDetector(args.base_url, timeout=args.timeout)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--manifest", required=True)
    p.add_argument("--images-root", help="이미지 폴더(기본: 매니페스트가 있는 폴더)")
    p.add_argument("--check", action="store_true", help="매니페스트만 검증하고 끝낸다")
    p.add_argument("--detector", choices=["http", "server"])
    p.add_argument("--base-url", help="http: 추론 서비스 주소, server: Signal 서버 주소")
    p.add_argument("--threshold", type=float, default=0.5, help="이 점수 이상이면 fake (기본 0.5, 모델 카드 기본값)")
    p.add_argument("--abstain-low", type=float, help="판단 보류 구간 하한(이 구간의 점수는 지표에서 제외)")
    p.add_argument("--abstain-high", type=float, help="판단 보류 구간 상한")
    p.add_argument("--timeout", type=int, default=120)
    p.add_argument("--workers", type=int, default=1, help="동시 요청 수(지연 측정이 목적이면 1)")
    p.add_argument("--limit", type=int, help="앞에서 N장만(연기 테스트용)")
    p.add_argument("--report", help="JSON 리포트 저장 경로")
    args = p.parse_args(argv)

    items = manifest_mod.load(args.manifest)
    root = args.images_root or str(Path(args.manifest).resolve().parent)
    problems = manifest_mod.check(items, root)
    n_real = sum(1 for i in items if i.label == 0 and i.variant == "original")
    n_fake = sum(1 for i in items if i.label == 1 and i.variant == "original")
    print(f"매니페스트: 원본 {n_real + n_fake}장 (real {n_real}, fake {n_fake}), 변형 "
          f"{sum(1 for i in items if i.variant != 'original')}장")
    for problem in problems:
        print("  문제:", problem)
    if args.check:
        print("검증 통과" if not problems else f"문제 {len(problems)}건")
        return 1 if problems else 0
    if problems:
        print("매니페스트 문제를 먼저 고치거나 --check로 확인하세요.", file=sys.stderr)
        return 1
    if not args.detector or not args.base_url:
        p.error("--detector와 --base-url이 필요합니다(매니페스트만 보려면 --check)")
    if (args.abstain_low is None) != (args.abstain_high is None):
        p.error("--abstain-low와 --abstain-high는 함께 줘야 합니다")

    if args.limit:
        items = items[:args.limit]
    detector = make_detector(args)
    paths = [str(Path(root) / i.path) for i in items]
    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        results = list(pool.map(detector.detect, paths))

    report = evaluate(items, results, args.threshold, args.abstain_low, args.abstain_high)
    report["detector"] = args.detector
    report["base_url"] = args.base_url
    print_report(report, f"{args.detector} @ {args.base_url}")
    if args.report:
        report["items"] = [{"path": i.path, "label": "fake" if i.label else "real", "variant": i.variant,
                            "score": r.score, "error": r.error, "latency_ms": round(r.latency_ms, 1)}
                           for i, r in zip(items, results)]
        Path(args.report).parent.mkdir(parents=True, exist_ok=True)
        Path(args.report).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"리포트 저장: {args.report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
