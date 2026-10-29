package com.signal.domain.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monitoringId;

    @Column(nullable = false, length = 2048)
    private String sourceUrl;

    @Column(length = 2048)
    private String thumbnailUrl;

    @Column(length = 500)
    private String title;

    @Column(nullable = false)
    private double similarity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Builder
    public Detection(Long monitoringId, String sourceUrl, String thumbnailUrl, String title, double similarity) {
        this.monitoringId = monitoringId;
        this.sourceUrl = sourceUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.similarity = similarity;
        this.detectedAt = LocalDateTime.now();
    }
}
