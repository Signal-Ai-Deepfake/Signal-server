package com.signal.domain.riskassessment.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DetectedFace {

    private int faceIndex;

    @Embedded
    private BoundingBox boundingBox;

    @Builder
    public DetectedFace(int faceIndex, BoundingBox boundingBox) {
        this.faceIndex = faceIndex;
        this.boundingBox = boundingBox;
    }
}
