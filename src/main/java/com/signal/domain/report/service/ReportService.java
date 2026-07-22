package com.signal.domain.report.service;

import com.signal.domain.report.dto.request.CreateReportRequest;
import com.signal.domain.report.dto.request.UpdateReportRequest;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportEvidence;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.generator.ReportDocumentGenerator;
import com.signal.domain.report.repository.ReportEvidenceRepository;
import com.signal.domain.report.repository.ReportRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final long MAX_EVIDENCE_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EVIDENCE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");
    private static final String EVIDENCE_DIRECTORY = "report-evidences";

    private final ReportRepository reportRepository;
    private final ReportEvidenceRepository reportEvidenceRepository;
    private final ReportDocumentGenerator reportDocumentGenerator;
    private final FileStorage fileStorage;

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

    public List<Report> getMyReports(Long userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
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

    @Transactional
    public ReportEvidence uploadEvidence(Long userId, MultipartFile file) {
        validateEvidenceFile(file);

        String fileUrl = fileStorage.store(file, EVIDENCE_DIRECTORY);
        ReportEvidence evidence = ReportEvidence.builder()
                .userId(userId)
                .fileUrl(fileUrl)
                .build();

        return reportEvidenceRepository.save(evidence);
    }

    private void validateEvidenceFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SignalException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > MAX_EVIDENCE_FILE_SIZE) {
            throw new SignalException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_EVIDENCE_CONTENT_TYPES.contains(file.getContentType())) {
            throw new SignalException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
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
