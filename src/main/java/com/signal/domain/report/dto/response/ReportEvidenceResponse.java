package com.signal.domain.report.dto.response;

import com.signal.domain.report.entity.ReportEvidence;

public record ReportEvidenceResponse(Long evidenceId, String fileUrl) {

    public static ReportEvidenceResponse from(ReportEvidence evidence) {
        return new ReportEvidenceResponse(evidence.getId(), evidence.getFileUrl());
    }
}
