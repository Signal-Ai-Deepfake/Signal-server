import unittest

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import metrics  # noqa: E402


class MetricsTest(unittest.TestCase):
    def test_auc_완벽한_분리는_1_뒤집으면_0(self):
        self.assertEqual(metrics.auc([0.1, 0.2, 0.8, 0.9], [0, 0, 1, 1]), 1.0)
        self.assertEqual(metrics.auc([0.9, 0.8, 0.2, 0.1], [0, 0, 1, 1]), 0.0)

    def test_auc_동점은_절반으로_계산(self):
        self.assertEqual(metrics.auc([0.5, 0.5], [0, 1]), 0.5)

    def test_auc_알려진_값(self):
        # pos 0.8, 0.4 / neg 0.6, 0.2 -> 3/4
        self.assertAlmostEqual(metrics.auc([0.8, 0.4, 0.6, 0.2], [1, 1, 0, 0]), 0.75)

    def test_auc_한쪽_라벨이_없으면_None(self):
        self.assertIsNone(metrics.auc([0.1, 0.2], [0, 0]))

    def test_임계값_판정과_비율(self):
        conf = metrics.confusion([0.9, 0.6, 0.4, 0.1], [1, 0, 1, 0], 0.5)
        self.assertEqual(conf, {"tp": 1, "fp": 1, "tn": 1, "fn": 1})
        r = metrics.rates(conf)
        self.assertEqual((r["fpr"], r["fnr"], r["accuracy"]), (0.5, 0.5, 0.5))

    def test_분모가_0이면_None(self):
        r = metrics.rates({"tp": 0, "fp": 0, "tn": 0, "fn": 0})
        self.assertEqual((r["fpr"], r["fnr"], r["accuracy"]), (None, None, None))

    def test_tpr_at_fpr(self):
        scores = [0.95, 0.9, 0.7, 0.3, 0.8, 0.2, 0.1, 0.05]
        labels = [1, 1, 1, 1, 0, 0, 0, 0]
        tpr, threshold = metrics.tpr_at_fpr(scores, labels, 0.0)
        self.assertEqual((tpr, threshold), (0.5, 0.9))  # 0.8짜리 real보다 높게 잡아야 오탐 0
        # 임계값 0.3이면 fake 4장을 모두 잡고 real은 0.8짜리 1장만 오탐(25%)
        tpr, threshold = metrics.tpr_at_fpr(scores, labels, 0.25)
        self.assertEqual((tpr, threshold), (1.0, 0.3))

    def test_tpr_at_fpr_오탐을_피할_수_없으면_탐지율_0(self):
        tpr, _ = metrics.tpr_at_fpr([0.1, 0.9], [1, 0], 0.0)
        self.assertEqual(tpr, 0.0)

    def test_percentile(self):
        self.assertEqual(metrics.percentile([5, 1, 3], 50), 3)
        self.assertIsNone(metrics.percentile([], 50))


if __name__ == "__main__":
    unittest.main()
