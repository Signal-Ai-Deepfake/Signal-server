package com.signal.domain.riskassessment.analyzer;

import com.signal.domain.riskassessment.entity.DetectedFace;
import com.signal.domain.riskassessment.entity.RiskFactor;
import com.signal.domain.riskassessment.entity.RiskLevel;
import java.util.List;

public record RiskAnalysisResult(
        boolean faceDetected,
        RiskLevel overallRiskLevel,
        int overallScore,
        List<RiskFactor> factors,
        List<String> recommendations,
        List<DetectedFace> faces,
        boolean fallbackUsed
) {

    /**
     * @param fallbackUsed true면 {@link StubRiskAnalyzer}(룰 기반 폴백)가 내린 판단, false면 실제
     *                      비전 LLM이 얼굴을 못 찾았다고 답한 것.
     */
    public static RiskAnalysisResult faceNotDetected(boolean fallbackUsed) {
        return new RiskAnalysisResult(false, null, 0, List.of(), List.of(), List.of(), fallbackUsed);
    }
}
