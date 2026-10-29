"""딥페이크 탐지 추론 서비스 (FastAPI + onnxruntime).

실행:  uvicorn app:app --host 0.0.0.0 --port 8000
요청:  curl -F file=@photo.jpg http://localhost:8000/detect
응답:  {"score": 0.0~1.0(가짜일 확률), "model": ..., "modelVersion": ..., "width": ..., "height": ...}

점수는 보정되지 않은 모델 출력(sigmoid)이다. 구간·판단 불가 같은 해석은 서버가 평가 결과로 정한다.
"""
from __future__ import annotations

import logging
import os
import threading
from contextlib import asynccontextmanager
from typing import Callable, Optional

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse

from preprocess import ImageTooLarge, InvalidImage, MAX_PIXELS as DEFAULT_MAX_PIXELS, load_image, preprocess

log = logging.getLogger("detect-lab")

MODEL_NAME = os.getenv("DETECT_MODEL_NAME", "CommunityForensics-DeepfakeDet-ViT")
MODEL_VERSION = os.getenv("DETECT_MODEL_VERSION", "onnx-fp32")
MODEL_PATH = os.getenv("DETECT_MODEL_PATH", "/models/onnx/model.onnx")
# 서버(DeepfakeDetectionService)가 이미지를 최대 100MB까지 받으므로 같은 값으로 맞춘다. 더 작게 두면 서버가 받은 사진이 여기서 거절된다.
MAX_UPLOAD_MB = int(os.getenv("DETECT_MAX_UPLOAD_MB", "100"))
MAX_PIXELS = int(os.getenv("DETECT_MAX_PIXELS", str(DEFAULT_MAX_PIXELS)))
MAX_CONCURRENCY = int(os.getenv("DETECT_MAX_CONCURRENCY", "2"))
THREADS = int(os.getenv("DETECT_THREADS", "2"))


def _default_model():
    from model import OnnxModel

    return OnnxModel(MODEL_PATH, MODEL_NAME, MODEL_VERSION, threads=THREADS)


def create_app(model_factory: Optional[Callable[[], object]] = None, max_upload_bytes: Optional[int] = None,
               max_pixels: int = MAX_PIXELS, max_concurrency: Optional[int] = None) -> FastAPI:
    state: dict = {"model": None, "error": None}
    limit = max_upload_bytes if max_upload_bytes is not None else MAX_UPLOAD_MB * 1024 * 1024
    # 메모리 보호: 파일 읽기, 디코딩(가장 큼), 전처리, 추론을 모두 이 안에서 한다. 밖에서 디코딩하면 큰 사진 여러 장이 동시에 메모리에 올라간다.
    slots = threading.BoundedSemaphore(max_concurrency or MAX_CONCURRENCY)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        try:
            state["model"] = (model_factory or _default_model)()
        except Exception as e:  # 모델이 없어도 서비스는 떠서 /health가 이유를 알려준다
            state["error"] = f"{type(e).__name__}: {e}"
            log.error("모델을 불러오지 못했습니다: %s", state["error"])
        yield

    app = FastAPI(title="Signal Detect Lab", lifespan=lifespan)

    @app.get("/health")
    def health():
        model = state["model"]
        if model is None:
            return JSONResponse(status_code=503, content={"status": "model_unavailable", "error": state["error"]})
        return {"status": "ok", "model": model.name, "modelVersion": model.version}

    @app.post("/detect")
    def detect(file: UploadFile = File(...)):
        model = state["model"]
        if model is None:
            raise HTTPException(status_code=503, detail="모델을 사용할 수 없습니다")
        with slots:
            data = file.file.read(limit + 1)
            if len(data) > limit:
                raise HTTPException(status_code=413, detail=f"파일이 {limit // (1024 * 1024)}MB를 넘습니다")
            try:
                img = load_image(data, max_pixels=max_pixels)
            except ImageTooLarge as e:
                raise HTTPException(status_code=413, detail=str(e))
            except InvalidImage as e:
                raise HTTPException(status_code=400, detail=str(e))
            width, height = img.size
            score = model.predict(preprocess(img))
        return {"score": round(score, 6), "model": model.name, "modelVersion": model.version,
                "width": width, "height": height}

    return app


app = create_app()
