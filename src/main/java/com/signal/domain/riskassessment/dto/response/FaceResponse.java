package com.signal.domain.riskassessment.dto.response;

import com.signal.domain.riskassessment.entity.DetectedFace;

public record FaceResponse(int index, BoundingBoxResponse boundingBox) {

    public static FaceResponse from(DetectedFace detectedFace) {
        return new FaceResponse(detectedFace.getFaceIndex(), BoundingBoxResponse.from(detectedFace.getBoundingBox()));
    }
}
