import json
import tempfile
import unittest

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import compare_reports  # noqa: E402
import manifest as manifest_mod  # noqa: E402
import run_eval  # noqa: E402
from detectors import Result  # noqa: E402


class CompareTest(unittest.TestCase):
    def test_표를_출력한다(self):
        with tempfile.TemporaryDirectory() as tmp:
            res = [Result(score=s) for s in (0.1, 0.2, 0.9, 0.8)]
            items = [manifest_mod.Item("a", 0), manifest_mod.Item("b", 0), manifest_mod.Item("c", 1, generator="g"),
                     manifest_mod.Item("d", 1, generator="g")]
            report = run_eval.evaluate(items, res, 0.5)
            path = Path(tmp) / "one.json"
            path.write_text(json.dumps(report), encoding="utf-8")
            self.assertEqual(compare_reports.main([str(path)]), 0)


if __name__ == "__main__":
    unittest.main()
