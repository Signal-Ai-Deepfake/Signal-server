package com.signal.domain.monitoring.dto.response;

import com.signal.domain.monitoring.entity.Detection;
import java.time.LocalDateTime;

public record DetectionResponse(
        Long detectionId,
        String sourceUrl,
        String thumbnailUrl,
        double similarity,
        LocalDateTime detectedAt
) {

    public static DetectionResponse from(Detection detection) {
        return new DetectionResponse(
                detection.getId(),
                detection.getSourceUrl(),
                detection.getThumbnailUrl(),
                detection.getSimilarity(),
                detection.getDetectedAt());
    }
}
