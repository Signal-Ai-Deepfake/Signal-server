package com.signal.domain.report.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.report.dto.response.ReportResponse;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.entity.ReportStatus;
import com.signal.domain.report.generator.StubReportDocumentGenerator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportResponseTest {

    @Test
    void 스텁_문서URL이면_documentSimulated가_true이고_URL은_숨긴다() {
        String stubUrl = new StubReportDocumentGenerator().generate(ReportStatus.FINALIZED);
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl(stubUrl).build();

        ReportResponse response = ReportResponse.from(report);

        assertThat(response.documentSimulated()).isTrue();
        assertThat(response.documentUrl()).isNull();
    }

    @Test
    void 실제_문서URL이면_그대로_내려준다() {
        Report report = Report.builder()
                .userId(1L).description("설명").sourceUrls(List.of("https://a")).evidenceIds(List.of())
                .documentUrl("/uploads/reports/final.pdf").build();

        ReportResponse response = ReportResponse.from(report);

        assertThat(response.documentSimulated()).isFalse();
        assertThat(response.documentUrl()).isEqualTo("/uploads/reports/final.pdf");
    }
}
