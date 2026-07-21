package com.signal.domain.deepfakedetection.entity;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deepfake_detections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeepfakeDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String anonymousId;

    @Column(nullable = false)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeepfakeDetectionStatus status;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    private Double confidence;

    private Integer riskScore;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deepfake_detection_evidences", joinColumns = @JoinColumn(name = "deepfake_detection_id"))
    @OrderColumn(name = "evidence_order")
    private List<Evidence> evidences;

    private String highlightedResultUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public DeepfakeDetection(Long userId, String anonymousId, String fileUrl) {
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.fileUrl = fileUrl;
        this.status = DeepfakeDetectionStatus.PROCESSING;
        this.evidences = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId, String anonymousId) {
        if (this.userId != null) {
            return this.userId.equals(userId);
        }
        return Objects.equals(this.anonymousId, anonymousId);
    }

    public void complete(Verdict verdict, double confidence, int riskScore,
                          List<Evidence> evidences, String highlightedResultUrl) {
        this.status = DeepfakeDetectionStatus.COMPLETED;
        this.verdict = verdict;
        this.confidence = confidence;
        this.riskScore = riskScore;
        this.evidences = evidences;
        this.highlightedResultUrl = highlightedResultUrl;
    }

    public void fail() {
        this.status = DeepfakeDetectionStatus.FAILED;
    }
}
