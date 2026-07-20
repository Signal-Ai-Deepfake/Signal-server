package com.signal.domain.riskassessment.analyzer;

import org.springframework.web.multipart.MultipartFile;

public interface RiskAnalyzer {

    /**
     * 이미지를 분석해 위험도 진단 결과를 반환한다. 실제 AI 서버 연동 전까지는 스텁 구현체가 대신한다.
     */
    RiskAnalysisResult analyze(MultipartFile image);
}
