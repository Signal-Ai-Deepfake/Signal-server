package com.signal.domain.report.generator;

import com.signal.domain.report.entity.ReportStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 실제 문서 생성 서버 연동 전까지 사용하는 스텁 구현체. 실제 문서를 만들지 않고 URL만 생성한다.
 * 실서버 연동 시 이 클래스를 교체한다.
 */
@Component
public class StubReportDocumentGenerator implements ReportDocumentGenerator {

    private static final String BASE_URL = "https://stub.signal.local/reports/documents/";

    @Override
    public String generate(ReportStatus status) {
        return BASE_URL + status.name().toLowerCase() + "-" + UUID.randomUUID() + ".pdf";
    }
}
