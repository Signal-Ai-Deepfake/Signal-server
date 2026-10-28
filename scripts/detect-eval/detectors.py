"""평가 대상 탐지기. 모두 이미지 한 장을 받아 "가짜일 가능성(0~1)"을 돌려준다.

- HttpDetector:   detect-lab 같은 추론 서비스의 POST {base}/detect 를 직접 호출한다.
- ServerDetector: Signal 서버의 POST /api/v1/deepfake-detections 를 호출하고 완료될 때까지 조회한다.
                  현재 LLM 탐지기(기준선)나 mode=http 서버 전체를 같은 방식으로 잴 때 쓴다.
                  서버는 점수를 riskScore(0~100)로 내려주므로 score = riskScore / 100 이다.
"""
from __future__ import annotations

import json
import mimetypes
import time
import uuid
from dataclasses import dataclass
from urllib import request as urlrequest
from urllib.error import HTTPError, URLError


@dataclass
class Result:
    score: float | None = None      # None이면 점수를 얻지 못함(실패/분석 불가)
    error: str | None = None
    model: str | None = None
    model_version: str | None = None
    latency_ms: float = 0.0


def multipart(field, filename, content, content_type):
    boundary = "----detecteval" + uuid.uuid4().hex
    head = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{filename}\"\r\n"
            f"Content-Type: {content_type}\r\n\r\n").encode()
    tail = f"\r\n--{boundary}--\r\n".encode()
    return head + content + tail, f"multipart/form-data; boundary={boundary}"


def _content_type(path):
    return mimetypes.guess_type(path)[0] or "application/octet-stream"


def _http_json(req, timeout):
    try:
        with urlrequest.urlopen(req, timeout=timeout) as res:
            return res.status, json.loads(res.read().decode() or "null")
    except HTTPError as e:
        body = e.read().decode(errors="replace")[:200]
        raise RuntimeError(f"HTTP {e.code}: {body}")
    except URLError as e:
        raise RuntimeError(f"연결 실패: {e.reason}")


class HttpDetector:
    name = "http"

    def __init__(self, base_url, timeout=60):
        self.base = base_url.rstrip("/")
        self.timeout = timeout

    def detect(self, path):
        started = time.monotonic()
        try:
            with open(path, "rb") as f:
                content = f.read()
            body, ctype = multipart("file", path.rsplit("/", 1)[-1], content, _content_type(path))
            req = urlrequest.Request(self.base + "/detect", data=body, method="POST",
                                     headers={"Content-Type": ctype})
            _, data = _http_json(req, self.timeout)
            score = data.get("score")
            if not isinstance(score, (int, float)) or not 0.0 <= score <= 1.0:
                raise RuntimeError(f"score가 0~1 숫자가 아닙니다: {score!r}")
            return Result(score=float(score), model=data.get("model"), model_version=data.get("modelVersion"),
                          latency_ms=(time.monotonic() - started) * 1000)
        except Exception as e:  # 한 장의 실패가 전체 평가를 멈추지 않게 한다
            return Result(error=str(e), latency_ms=(time.monotonic() - started) * 1000)


class ServerDetector:
    name = "server"

    def __init__(self, base_url, timeout=120, poll_interval=1.0):
        self.base = base_url.rstrip("/")
        self.timeout = timeout
        self.poll_interval = poll_interval

    def detect(self, path):
        started = time.monotonic()
        anon = str(uuid.uuid4())  # 익명 탐지 한도(익명 ID당 5회) 때문에 이미지마다 새 ID를 쓴다
        headers = {"X-Anonymous-Id": anon, "Accept-Language": "ko"}
        try:
            with open(path, "rb") as f:
                content = f.read()
            body, ctype = multipart("file", path.rsplit("/", 1)[-1], content, _content_type(path))
            create = urlrequest.Request(self.base + "/api/v1/deepfake-detections", data=body, method="POST",
                                        headers={**headers, "Content-Type": ctype})
            _, created = _http_json(create, self.timeout)
            detection_id = created["detectionId"]
            deadline = time.monotonic() + self.timeout
            while True:
                get = urlrequest.Request(f"{self.base}/api/v1/deepfake-detections/{detection_id}", headers=headers)
                _, data = _http_json(get, self.timeout)
                status = data.get("status")
                if status == "COMPLETED":
                    risk = data.get("riskScore")
                    if risk is None:
                        raise RuntimeError("COMPLETED인데 riskScore가 없습니다")
                    return Result(score=float(risk) / 100.0, model="signal-server",
                                  model_version="fallback" if data.get("fallbackUsed") else None,
                                  latency_ms=(time.monotonic() - started) * 1000)
                if status == "FAILED":
                    return Result(error=f"FAILED:{data.get('failureReason')}",
                                  latency_ms=(time.monotonic() - started) * 1000)
                if time.monotonic() > deadline:
                    raise RuntimeError("완료 대기 시간 초과")
                time.sleep(self.poll_interval)
        except Exception as e:
            return Result(error=str(e), latency_ms=(time.monotonic() - started) * 1000)
