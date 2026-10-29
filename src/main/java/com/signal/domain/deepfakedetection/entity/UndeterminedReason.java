package com.signal.domain.deepfakedetection.entity;

/** 등급이 {@link DetectionBand#UNDETERMINED}(판단 불가)인 이유. */
public enum UndeterminedReason {
    /** 이미지가 너무 작아 모델 점수를 믿기 어렵다. */
    LOW_RESOLUTION
}
