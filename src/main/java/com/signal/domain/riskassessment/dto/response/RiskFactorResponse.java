package com.signal.domain.riskassessment.dto.response;

import com.signal.domain.riskassessment.analyzer.RiskTexts;
import com.signal.domain.riskassessment.entity.RiskFactor;
import com.signal.global.i18n.LanguageContext;

public record RiskFactorResponse(String type, String label, int score, String description) {

    /**
     * label은 저장된 이름이 아니라 type과 요청 언어(Accept-Language)로 정해 이전에 저장된 진단도 현재 언어로 보인다.
     * description은 AI가 쓴 문장이라 저장 당시 언어로 내려간다.
     */
    public static RiskFactorResponse from(RiskFactor riskFactor) {
        String label = RiskTexts.label(riskFactor.getType(), LanguageContext.current(), riskFactor.getLabel());
        return new RiskFactorResponse(riskFactor.getType(), label, riskFactor.getScore(), riskFactor.getDescription());
    }
}
