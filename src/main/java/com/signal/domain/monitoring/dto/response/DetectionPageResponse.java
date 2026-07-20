package com.signal.domain.monitoring.dto.response;

import com.signal.domain.monitoring.entity.Detection;
import java.util.List;
import org.springframework.data.domain.Page;

public record DetectionPageResponse(
        List<DetectionResponse> content,
        long totalElements,
        int totalPages
) {

    public static DetectionPageResponse from(Page<Detection> page) {
        return new DetectionPageResponse(
                page.getContent().stream().map(DetectionResponse::from).toList(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
