package com.signal.domain.report.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.report.entity.ReportStatus;
import org.junit.jupiter.api.Test;

class StubReportDocumentGeneratorTest {

    private final StubReportDocumentGenerator generator = new StubReportDocumentGenerator();

    @Test
    void 상태에_따라_다른_URL을_생성한다() {
        String draftUrl = generator.generate(ReportStatus.DRAFT);
        String finalizedUrl = generator.generate(ReportStatus.FINALIZED);

        assertThat(draftUrl).contains("draft").endsWith(".pdf");
        assertThat(finalizedUrl).contains("finalized").endsWith(".pdf");
    }

    @Test
    void 매번_호출할_때마다_다른_URL을_생성한다() {
        String first = generator.generate(ReportStatus.DRAFT);
        String second = generator.generate(ReportStatus.DRAFT);

        assertThat(first).isNotEqualTo(second);
    }
}
