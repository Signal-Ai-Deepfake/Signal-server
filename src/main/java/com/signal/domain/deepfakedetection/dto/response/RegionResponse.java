package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.deepfakedetection.entity.Region;

public record RegionResponse(int x, int y, int width, int height) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getX(), region.getY(), region.getWidth(), region.getHeight());
    }
}
