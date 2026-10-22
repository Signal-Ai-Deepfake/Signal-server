package com.signal.domain.report.dto.response;

import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.generator.StubReportDocumentGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long reportId,
        ReportStatus status,
        LocalDate incidentDate,
        String discoveryRoute,
        String damageType,
        String description,
        List<String> sourceUrls,
        List<Long> evidenceIds,
        String targetAgencyType,
        String additionalNotes,
        String documentUrl,
        boolean documentSimulated,
        List<TimelineEntryResponse> timeline,
        LocalDateTime createdAt
) {

    public static ReportResponse from(Report report) {
        boolean documentSimulated = StubReportDocumentGenerator.isSimulatedUrl(report.getDocumentUrl());
        return new ReportResponse(
                report.getId(),
                report.getStatus(),
                report.getIncidentDate(),
                report.getDiscoveryRoute(),
                report.getDamageType(),
                report.getDescription(),
                report.getSourceUrls(),
                report.getEvidenceIds(),
                report.getTargetAgencyType(),
                report.getAdditionalNotes(),
                documentSimulated ? null : report.getDocumentUrl(),
                documentSimulated,
                report.getTimeline().stream().map(TimelineEntryResponse::from).toList(),
                report.getCreatedAt());
    }
}
