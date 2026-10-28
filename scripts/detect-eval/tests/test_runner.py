import csv
import json
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import manifest as manifest_mod  # noqa: E402
import run_eval  # noqa: E402
from detectors import HttpDetector, Result, ServerDetector, multipart  # noqa: E402


class EvaluateTest(unittest.TestCase):
    def items(self):
        return [
            manifest_mod.Item("r1", 0), manifest_mod.Item("r2", 0),
            manifest_mod.Item("f1", 1, generator="sdxl"), manifest_mod.Item("f2", 1, generator="flux"),
            manifest_mod.Item("r1.j", 0, variant="jpeg70", variant_of="r1"),
            manifest_mod.Item("f1.j", 1, variant="jpeg70", variant_of="f1", generator="sdxl"),
        ]

    def test_지표와_변형_생성기_견고성(self):
        results = [Result(score=s) for s in (0.1, 0.2, 0.9, 0.3, 0.6, 0.4)]
        report = run_eval.evaluate(self.items(), results, 0.5)
        self.assertEqual(report["overall"]["n"], 6)
        self.assertEqual(report["by_generator_fake_only"]["flux"]["fnr"], 1.0)
        self.assertEqual(report["by_generator_fake_only"]["sdxl"]["fnr"], 0.5)
        rob = report["robustness_vs_original"]["jpeg70"]
        self.assertEqual(rob["pairs"], 2)
        self.assertEqual(rob["verdict_flip_rate"], 1.0)  # r1: 0.1->0.6 뒤집힘, f1: 0.9->0.4 뒤집힘
        self.assertAlmostEqual(rob["mean_abs_score_delta"], 0.5)

    def test_실패는_지표에서_빼고_따로_센다(self):
        results = [Result(score=0.1), Result(error="FAILED:TIMEOUT"), Result(score=0.9), Result(score=0.3),
                   Result(error="FAILED:TIMEOUT"), Result(score=0.4)]
        report = run_eval.evaluate(self.items(), results, 0.5)
        self.assertEqual(report["failed"]["count"], 2)
        self.assertEqual(report["failed"]["reasons"], {"FAILED:TIMEOUT": 2})
        self.assertEqual(report["overall"]["n"], 4)

    def test_판단_보류_구간은_지표에서_제외(self):
        results = [Result(score=s) for s in (0.1, 0.45, 0.9, 0.55, 0.1, 0.9)]
        report = run_eval.evaluate(self.items(), results, 0.5, 0.4, 0.6)
        self.assertEqual(report["abstained"]["count"], 2)
        self.assertEqual(report["overall"]["n"], 4)
        self.assertEqual(report["overall"]["fpr"], 0.0)


class _FakeServer(BaseHTTPRequestHandler):
    """/detect: 본문 첫 바이트가 점수(0~100). 상태 조회 API: 첫 바이트 255는 FAILED."""
    stored = {}

    def log_message(self, *args):
        pass

    def _first_byte(self, body):
        head_end = body.index(b"\r\n\r\n") + 4
        return body[head_end]

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        value = self._first_byte(body)
        if self.path == "/detect":
            self._json(200, {"score": value / 100.0, "model": "fake", "modelVersion": "t1"})
        elif self.path == "/api/v1/deepfake-detections":
            assert self.headers["X-Anonymous-Id"]
            _FakeServer.stored[len(_FakeServer.stored) + 1] = value
            self._json(202, {"detectionId": len(_FakeServer.stored), "status": "PROCESSING"})
        else:
            self._json(404, {})

    def do_GET(self):
        detection_id = int(self.path.rsplit("/", 1)[1])
        value = _FakeServer.stored[detection_id]
        if value == 255:
            self._json(200, {"status": "FAILED", "failureReason": "NO_FACE_DETECTED"})
        else:
            self._json(200, {"status": "COMPLETED", "riskScore": value, "fallbackUsed": False})

    def _json(self, status, payload):
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


class EndToEndTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = HTTPServer(("127.0.0.1", 0), _FakeServer)
        cls.base = f"http://127.0.0.1:{cls.httpd.server_address[1]}"
        threading.Thread(target=cls.httpd.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()

    def make_set(self, tmp):
        rows = [("r1.jpg", "real", 10), ("r2.jpg", "real", 60), ("f1.png", "fake", 90), ("f2.png", "fake", 30),
                ("f3.png", "fake", 255)]
        with open(Path(tmp) / "manifest.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["path", "label", "variant", "variant_of", "generator", "source", "license"])
            for name, label, value in rows:
                (Path(tmp) / name).write_bytes(bytes([value]) + b"payload")
                w.writerow([name, label, "original", "", "g" if label == "fake" else "", "s", "l"])
        return str(Path(tmp) / "manifest.csv")

    def test_http_탐지기와_server_탐지기로_리포트가_나온다(self):
        for detector in ("http", "server"):
            with tempfile.TemporaryDirectory() as tmp:
                manifest = self.make_set(tmp)
                report_path = str(Path(tmp) / "r.json")
                code = run_eval.main(["--manifest", manifest, "--detector", detector, "--base-url", self.base,
                                      "--report", report_path, "--timeout", "10"])
                self.assertEqual(code, 0)
                report = json.loads(Path(report_path).read_text(encoding="utf-8"))
                self.assertEqual(report["total"], 5)
                # 점수: r1 0.10, r2 0.60(오탐), f1 0.90, f2 0.30(미탐), f3 255
                if detector == "http":
                    self.assertEqual(report["failed"]["count"], 1)  # 255/100=2.55 -> score 범위 밖이라 실패로 센다
                else:
                    self.assertEqual(report["failed"]["reasons"], {"FAILED:NO_FACE_DETECTED": 1})
                    self.assertEqual(report["overall"]["n"], 4)
                    self.assertEqual(report["overall"]["fpr"], 0.5)
                    self.assertEqual(report["overall"]["fnr"], 0.5)

    def test_http_탐지기는_범위밖_점수를_실패로_센다(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "x.png"
            p.write_bytes(bytes([255]) + b"x")
            res = HttpDetector(self.base).detect(str(p))
            self.assertIsNone(res.score)
            self.assertIn("0~1", res.error)

    def test_연결_실패는_예외가_아니라_실패_결과(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "x.png"
            p.write_bytes(b"x")
            res = ServerDetector("http://127.0.0.1:1", timeout=2).detect(str(p))
            self.assertIsNone(res.score)
            self.assertTrue(res.error)

    def test_check_모드는_문제가_있으면_1(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = self.make_set(tmp)
            self.assertEqual(run_eval.main(["--manifest", manifest, "--check"]), 0)
            (Path(tmp) / "r1.jpg").unlink()
            self.assertEqual(run_eval.main(["--manifest", manifest, "--check"]), 1)


class MultipartTest(unittest.TestCase):
    def test_본문과_경계(self):
        body, ctype = multipart("file", "a.png", b"BYTES", "image/png")
        boundary = ctype.split("boundary=")[1].encode()
        self.assertTrue(body.startswith(b"--" + boundary))
        self.assertIn(b'name="file"; filename="a.png"', body)
        self.assertIn(b"\r\n\r\nBYTES\r\n--" + boundary + b"--", body)


if __name__ == "__main__":
    unittest.main()
