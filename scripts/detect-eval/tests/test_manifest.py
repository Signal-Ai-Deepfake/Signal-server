import csv
import tempfile
import unittest

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import manifest as manifest_mod  # noqa: E402


class ManifestTest(unittest.TestCase):
    def write(self, tmp, rows, create=()):
        for name in create:
            (Path(tmp) / name).parent.mkdir(parents=True, exist_ok=True)
            (Path(tmp) / name).write_bytes(b"x")
        path = Path(tmp) / "manifest.csv"
        with open(path, "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["path", "label", "variant", "variant_of", "generator", "source", "license"])
            w.writerows(rows)
        return path

    def test_정상_매니페스트는_통과(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [
                ["a.jpg", "real", "original", "", "", "팀원", "내부"],
                ["b.png", "fake", "original", "", "sdxl", "데이터셋", "CC"],
                ["a.v.jpg", "real", "jpeg70", "a.jpg", "", "", ""]], create=["a.jpg", "b.png", "a.v.jpg"])
            items = manifest_mod.load(path)
            self.assertEqual(manifest_mod.check(items, tmp), [])
            self.assertEqual([i.label for i in items], [0, 1, 0])

    def test_문제를_모두_알려준다(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [
                ["a.jpg", "real", "original", "", "", "", ""],
                ["a.jpg", "real", "original", "", "", "x", "y"],
                ["b.png", "fake", "original", "", "", "x", "y"],
                ["c.jpg", "real", "jpeg70", "없는원본.jpg", "", "", ""]], create=["a.jpg", "b.png"])
            problems = "\n".join(manifest_mod.check(manifest_mod.load(path), tmp))
            for expected in ("중복 path", "파일이 없습니다: c.jpg", "원본(variant_of)", "출처(source)",
                             "라이선스(license)", "생성기(generator)"):
                self.assertIn(expected, problems)

    def test_잘못된_라벨은_예외(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.write(tmp, [["a.jpg", "maybe", "original", "", "", "", ""]])
            with self.assertRaises(ValueError):
                manifest_mod.load(path)


if __name__ == "__main__":
    unittest.main()
