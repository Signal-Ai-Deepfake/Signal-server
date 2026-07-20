package com.signal.domain.report.dto.request;

import java.time.LocalDate;
import java.util.List;

/**
 * PATCH 부분 수정 요청. null이 아닌 필드만 반영된다.
 */
public record UpdateReportRequest(
        LocalDate incidentDate,
        String discoveryRoute,
        String damageType,
        String description,
        List<String> sourceUrls,
        List<Long> evidenceIds,
        String targetAgencyType
) {
}
