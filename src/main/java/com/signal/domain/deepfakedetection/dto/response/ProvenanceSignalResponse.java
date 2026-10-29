package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.deepfakedetection.entity.ProvenanceSignal;

/** 파일에서 찾은 출처 신호. strong이 true면 합성 가능성 등급을 높이는 근거로 쓰였다. */
public record ProvenanceSignalResponse(String code, String message, boolean strong) {

    public static ProvenanceSignalResponse from(ProvenanceSignal signal) {
        return new ProvenanceSignalResponse(signal.name(), signal.getMessage(), signal.isStrong());
    }
}
