package com.signal.domain.deepfakedetection.entity;

/** 등급을 무엇으로 정했는가. */
public enum AssessmentBasis {
    /** 전용 탐지 모델의 점수. */
    MODEL,
    /** 파일 메타데이터에서 발견한 생성 도구의 흔적(점수와 무관하게 등급을 높임). */
    PROVENANCE
}
