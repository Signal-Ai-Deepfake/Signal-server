package com.signal.domain.deepfakedetection.entity;

/**
 * 딥페이크 탐지가 FAILED로 끝난 이유. 프론트가 사용자에게 "다시 시도" 외의 구체적인 안내를 띄울 수 있도록
 * 응답에 그대로 노출한다.
 */
public enum DetectionFailureReason {
    NO_FACE_DETECTED,
    VIDEO_NOT_SUPPORTED,
    AI_SERVER_ERROR,
    TIMEOUT
}
