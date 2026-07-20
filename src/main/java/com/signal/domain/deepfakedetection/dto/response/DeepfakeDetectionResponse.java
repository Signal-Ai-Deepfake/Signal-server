package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.deepfakedetection.entity.DeepfakeDetection;
import com.signal.domain.deepfakedetection.entity.DeepfakeDetectionStatus;
import com.signal.domain.deepfakedetection.entity.Verdict;
import java.util.List;

public record DeepfakeDetectionResponse(
        Long detectionId,
        DeepfakeDetectionStatus status,
        Verdict verdict,
        Double confidence,
        Integer riskScore,
        List<EvidenceResponse> evidences,
        String highlightedResultUrl
) {

    public static DeepfakeDetectionResponse from(DeepfakeDetection detection) {
        return new DeepfakeDetectionResponse(
                detection.getId(),
                detection.getStatus(),
                detection.getVerdict(),
                detection.getConfidence(),
                detection.getRiskScore(),
                detection.getEvidences().stream().map(EvidenceResponse::from).toList(),
                detection.getHighlightedResultUrl());
    }
}
