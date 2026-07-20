package com.signal.domain.riskassessment.dto.response;

import com.signal.domain.riskassessment.entity.RiskFactor;

public record RiskFactorResponse(String type, String label, int score, String description) {

    public static RiskFactorResponse from(RiskFactor riskFactor) {
        return new RiskFactorResponse(
                riskFactor.getType(), riskFactor.getLabel(), riskFactor.getScore(), riskFactor.getDescription());
    }
}
