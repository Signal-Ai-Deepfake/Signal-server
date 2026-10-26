package com.signal.domain.deepfakedetection.entity;

public enum Verdict {
    REAL, FAKE, SUSPICIOUS;

    /** riskScore(0~100) 구간으로 판정한다: 30 미만 REAL, 70 이하 SUSPICIOUS, 초과 FAKE. */
    public static Verdict fromRiskScore(int riskScore) {
        if (riskScore < 30) {
            return REAL;
        }
        if (riskScore <= 70) {
            return SUSPICIOUS;
        }
        return FAKE;
    }
}
