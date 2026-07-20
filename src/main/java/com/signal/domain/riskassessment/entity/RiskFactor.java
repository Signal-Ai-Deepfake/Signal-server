package com.signal.domain.riskassessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskFactor {

    private String type;
    private String label;
    private int score;

    @Column(length = 500)
    private String description;

    @Builder
    public RiskFactor(String type, String label, int score, String description) {
        this.type = type;
        this.label = label;
        this.score = score;
        this.description = description;
    }
}
