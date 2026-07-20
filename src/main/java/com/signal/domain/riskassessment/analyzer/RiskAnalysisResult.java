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
        List<DetectedFace> faces
) {

    public static RiskAnalysisResult faceNotDetected() {
        return new RiskAnalysisResult(false, null, 0, List.of(), List.of(), List.of());
    }
}
