package com.signal.domain.riskassessment.entity;

public enum RiskLevel {
    HIGH, MEDIUM, LOW;

    public static RiskLevel fromScore(int score) {
        if (score >= 70) {
            return HIGH;
        }
        if (score >= 40) {
            return MEDIUM;
        }
        return LOW;
    }
}
