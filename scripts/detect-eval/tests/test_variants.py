import csv
import tempfile
import unittest

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import manifest as manifest_mod  # noqa: E402


try:
    import PIL  # noqa: F401
    HAS_PIL = True
except ImportError:
    HAS_PIL = False


@unittest.skipUnless(HAS_PIL, "Pillow 없음")
class VariantsTest(unittest.TestCase):
    def test_변형을_만들고_원본에_연결한다(self):
        import make_variants
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            Image.new("RGB", (640, 480), (200, 30, 30)).save(Path(tmp) / "a.png")
            with open(Path(tmp) / "manifest.csv", "w", newline="", encoding="utf-8") as f:
                w = csv.writer(f)
                w.writerow(["path", "label", "variant", "variant_of", "generator", "source", "license"])
                w.writerow(["a.png", "fake", "original", "", "sdxl", "s", "l"])
            out_manifest = str(Path(tmp) / "m2.csv")
            code = make_variants.main(["--manifest", str(Path(tmp) / "manifest.csv"),
                                       "--out-dir", str(Path(tmp) / "variants"), "--out-manifest", out_manifest])
            self.assertEqual(code, 0)
            items = manifest_mod.load(out_manifest)
            self.assertEqual([i.variant for i in items], ["original", "jpeg70", "resize512", "messenger", "screenshot"])
            self.assertEqual(manifest_mod.check(items, tmp), [])
            with Image.open(Path(tmp) / next(i.path for i in items if i.variant == "screenshot")) as shot:
                self.assertEqual(shot.size, (1080, 2340))
            with Image.open(Path(tmp) / next(i.path for i in items if i.variant == "resize512")) as small:
                self.assertEqual(max(small.size), 512)


if __name__ == "__main__":
    unittest.main()
