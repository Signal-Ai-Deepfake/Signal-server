# detect-eval

딥페이크 탐지기를 같은 평가 세트로 재는 도구다. 탐지기를 바꿀지 판단하는 근거(오탐률, 미탐률, AUC, 변형별 성능)를 만든다.
표준 라이브러리만 쓴다(`make_variants.py`만 Pillow 필요). 설계 배경은 `docs/deepfake-detection.md`.

## 순서

1. **평가 세트 준비.** 이미지는 레포에 넣지 않고 별도 폴더에 둔다. `manifest.example.csv` 형식의 CSV를 만든다.
   - 실제 피해자 이미지, 실존 인물 얼굴로 새로 만든 합성 이미지, 성적 이미지는 넣지 않는다.
   - 이미지마다 정답(real/fake), 출처, 라이선스, 생성기(fake인 경우)를 적는다.
2. **검증.** `python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.csv --check`
3. **변형 만들기(선택).** 메신저·캡처를 거친 모습의 변형을 원본에 연결해 추가한다.
   `python3 scripts/detect-eval/make_variants.py --manifest eval/manifest.csv --out-dir eval/variants --out-manifest eval/manifest.with-variants.csv`
4. **측정.** 탐지기마다 같은 매니페스트로 돌려 리포트를 저장한다.
   ```bash
   # 현재 LLM 탐지(기준선): 로컬/개발 서버에서만. 이미지마다 새 익명 ID를 쓴다.
   python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.with-variants.csv \
       --detector server --base-url http://localhost:8080 --report reports/baseline-llm.json

   # 추론 서비스(detect-lab): 후보 모델
   python3 scripts/detect-eval/run_eval.py --manifest eval/manifest.with-variants.csv \
       --detector http --base-url http://localhost:8000 --report reports/community-forensics.json
   ```
5. **비교.** `python3 scripts/detect-eval/compare_reports.py reports/*.json`

## 읽는 법

- 점수는 "합성일 가능성"(0~1)이고, 임계값(기본 0.5, `--threshold`) 이상이면 fake로 판정한다.
- **오탐률**은 real을 fake로 본 비율, **미탐률**은 fake를 놓친 비율이다. 임계값이 다른 탐지기끼리는 이 둘을 직접 비교하지 말고
  AUC와 "오탐률 1%·5%일 때 탐지율"을 같이 본다. real이 100장 미만이면 오탐률 1% 지점은 사실상 "오탐 0개"라 불안정하다.
- **실패**(분석하지 못함)는 지표에서 빼고 따로 센다. 실패가 많은 탐지기는 지표가 좋아 보일 수 있으니 실패율을 함께 본다.
- **판단 보류 구간**(`--abstain-low/--abstain-high`)에 든 점수는 지표에서 빼고 보류 수로 센다. 서비스에서 "판단 불가"를 둘 구간을
  정할 때 임계값을 바꿔 가며 오탐·미탐이 얼마나 줄고 보류가 얼마나 느는지 본다.
- **원본 대비 점수 변화**는 같은 이미지의 변형이 원본과 다른 판정을 받는 비율이다. 높으면 재압축·캡처에 약하다는 뜻이다.

## 테스트

```bash
python3 -m unittest discover -s scripts/detect-eval/tests
```
