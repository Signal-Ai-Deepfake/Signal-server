package com.signal.domain.report.dto.request;

import java.time.LocalDate;
import java.util.List;

public record CreateReportRequest(
        LocalDate incidentDate,
        String discoveryRoute,
        String damageType,
        String description,
        List<String> sourceUrls,
        List<Long> evidenceIds,
        String targetAgencyType
) {
}
