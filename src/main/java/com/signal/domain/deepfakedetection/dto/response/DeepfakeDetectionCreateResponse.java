package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.deepfakedetection.entity.DeepfakeDetection;
import com.signal.domain.deepfakedetection.entity.DeepfakeDetectionStatus;

public record DeepfakeDetectionCreateResponse(Long detectionId, DeepfakeDetectionStatus status) {

    public static DeepfakeDetectionCreateResponse from(DeepfakeDetection detection) {
        return new DeepfakeDetectionCreateResponse(detection.getId(), detection.getStatus());
    }
}
