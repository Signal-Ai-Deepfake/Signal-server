package com.signal.domain.report.service;

import com.signal.domain.report.dto.request.CreateReportRequest;
import com.signal.domain.report.dto.request.UpdateReportRequest;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.generator.ReportDocumentGenerator;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportDocumentGenerator reportDocumentGenerator;

    @Transactional
    public Report createReport(Long userId, CreateReportRequest request) {
        validateRequired(request.description(), request.sourceUrls());

        String documentUrl = reportDocumentGenerator.generate(ReportStatus.DRAFT);

        Report report = Report.builder()
                .userId(userId)
                .incidentDate(request.incidentDate())
                .discoveryRoute(request.discoveryRoute())
                .damageType(request.damageType())
                .description(request.description())
                .sourceUrls(request.sourceUrls())
                .evidenceIds(request.evidenceIds() != null ? request.evidenceIds() : List.of())
                .targetAgencyType(request.targetAgencyType())
                .documentUrl(documentUrl)
                .build();

        return reportRepository.save(report);
    }

    public Report getReport(Long userId, Long reportId) {
        return getOwnedReport(userId, reportId);
    }

    @Transactional
    public Report updateReport(Long userId, Long reportId, UpdateReportRequest request) {
        Report report = getOwnedReport(userId, reportId);
        validateDraft(report);

        String documentUrl = reportDocumentGenerator.generate(ReportStatus.DRAFT);
        report.applyPatch(
                request.incidentDate(), request.discoveryRoute(), request.damageType(),
                request.description(), request.sourceUrls(), request.evidenceIds(),
                request.targetAgencyType(), documentUrl);

        return report;
    }

    @Transactional
    public Report finalizeReport(Long userId, Long reportId) {
        Report report = getOwnedReport(userId, reportId);
        validateDraft(report);

        String documentUrl = reportDocumentGenerator.generate(ReportStatus.FINALIZED);
        report.markFinalized(documentUrl);

        return report;
    }

    private Report getOwnedReport(Long userId, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        if (!report.isOwnedBy(userId)) {
            throw new SignalException(ErrorCode.FORBIDDEN);
        }

        return report;
    }

    private void validateDraft(Report report) {
        if (report.getStatus() != ReportStatus.DRAFT) {
            throw new SignalException(ErrorCode.REPORT_ALREADY_FINALIZED);
        }
    }

    private void validateRequired(String description, List<String> sourceUrls) {
        if (!StringUtils.hasText(description) || sourceUrls == null || sourceUrls.isEmpty()) {
            throw new SignalException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }
}
