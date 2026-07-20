package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.deepfakedetection.entity.Evidence;

public record EvidenceResponse(String type, String description, RegionResponse region, Integer frame) {

    public static EvidenceResponse from(Evidence evidence) {
        return new EvidenceResponse(
                evidence.getType(),
                evidence.getDescription(),
                RegionResponse.from(evidence.getRegion()),
                evidence.getFrame());
    }
}
