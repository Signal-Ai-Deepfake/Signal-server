package com.signal.domain.riskassessment.dto.response;

import com.signal.domain.riskassessment.entity.RiskAssessment;
import com.signal.domain.riskassessment.entity.RiskLevel;
import java.util.List;

public record RiskAssessmentResponse(
        Long assessmentId,
        RiskLevel overallRiskLevel,
        int overallScore,
        List<RiskFactorResponse> factors,
        List<String> recommendations,
        boolean faceDetected,
        List<FaceResponse> faces
) {

    public static RiskAssessmentResponse from(RiskAssessment riskAssessment) {
        return new RiskAssessmentResponse(
                riskAssessment.getId(),
                riskAssessment.getOverallRiskLevel(),
                riskAssessment.getOverallScore(),
                riskAssessment.getFactors().stream().map(RiskFactorResponse::from).toList(),
                riskAssessment.getRecommendations(),
                riskAssessment.isFaceDetected(),
                riskAssessment.getFaces().stream().map(FaceResponse::from).toList());
    }
}
