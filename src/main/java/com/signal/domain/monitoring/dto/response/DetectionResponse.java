package com.signal.domain.monitoring.dto.response;

import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.monitor.StubFaceMonitor;
import java.net.URI;
import java.time.LocalDateTime;

/**
 * simulated=true 이면 실제 검색으로 찾은 결과가 아닌 시연용 예시다. 이 경우 존재하지 않는 출처 링크는
 * 내리지 않고(sourceUrl=null), 썸네일은 자체 포함된 "SAMPLE" 플레이스홀더 이미지로 대체한다.
 */
public record DetectionResponse(
        Long detectionId,
        String sourceUrl,
        String thumbnailUrl,
        String title,
        String sourceDomain,
        double similarity,
        LocalDateTime detectedAt,
        boolean simulated
) {

    private static String domainOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static DetectionResponse from(Detection detection) {
        boolean simulated = StubFaceMonitor.isSimulatedUrl(detection.getSourceUrl());
        return new DetectionResponse(
                detection.getId(),
                simulated ? null : detection.getSourceUrl(),
                simulated ? StubFaceMonitor.PLACEHOLDER_THUMBNAIL : detection.getThumbnailUrl(),
                detection.getTitle(),
                simulated ? null : domainOf(detection.getSourceUrl()),
                detection.getSimilarity(),
                detection.getDetectedAt(),
                simulated);
    }
}
