package com.signal.domain.riskassessment.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "risk_assessments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String anonymousId;

    @Column(nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel overallRiskLevel;

    @Column(nullable = false)
    private int overallScore;

    @Column(nullable = false)
    private boolean faceDetected;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "risk_assessment_factors", joinColumns = @JoinColumn(name = "risk_assessment_id"))
    @OrderColumn(name = "factor_order")
    private List<RiskFactor> factors;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "risk_assessment_recommendations", joinColumns = @JoinColumn(name = "risk_assessment_id"))
    @OrderColumn(name = "recommendation_order")
    @Column(name = "recommendation", length = 500)
    private List<String> recommendations;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "risk_assessment_faces", joinColumns = @JoinColumn(name = "risk_assessment_id"))
    @OrderColumn(name = "face_order")
    private List<DetectedFace> faces;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RiskAssessment(Long userId, String anonymousId, String imageUrl, RiskLevel overallRiskLevel,
                           int overallScore, boolean faceDetected, List<RiskFactor> factors,
                           List<String> recommendations, List<DetectedFace> faces) {
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.imageUrl = imageUrl;
        this.overallRiskLevel = overallRiskLevel;
        this.overallScore = overallScore;
        this.faceDetected = faceDetected;
        this.factors = factors;
        this.recommendations = recommendations;
        this.faces = faces;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId, String anonymousId) {
        if (this.userId != null) {
            return this.userId.equals(userId);
        }
        return Objects.equals(this.anonymousId, anonymousId);
    }
}
