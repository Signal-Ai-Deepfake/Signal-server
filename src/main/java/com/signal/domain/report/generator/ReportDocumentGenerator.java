package com.signal.domain.report.generator;

import com.signal.domain.report.entity.ReportStatus;

public interface ReportDocumentGenerator {

    /**
     * 신고 문서를 생성하고 접근 가능한 URL을 반환한다. 실제 문서 생성 서버 연동 전까지는
     * 스텁 구현체가 URL만 생성해 대신한다.
     */
    String generate(ReportStatus status);
}
