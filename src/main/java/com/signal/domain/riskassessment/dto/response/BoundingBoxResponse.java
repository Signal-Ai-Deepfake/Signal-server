package com.signal.domain.riskassessment.dto.response;

import com.signal.domain.riskassessment.entity.BoundingBox;

public record BoundingBoxResponse(int x, int y, int width, int height) {

    public static BoundingBoxResponse from(BoundingBox boundingBox) {
        return new BoundingBoxResponse(
                boundingBox.getX(), boundingBox.getY(), boundingBox.getWidth(), boundingBox.getHeight());
    }
}
