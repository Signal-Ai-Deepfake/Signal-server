package com.signal.domain.protection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "protections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Protection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtectionLevel protectionLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtectionStatus status;

    @Column(nullable = false)
    private String originalImageUrl;

    private String protectedImageUrl;

    private Double visualDifference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Protection(Long userId, Long assessmentId, ProtectionLevel protectionLevel, String originalImageUrl) {
        this.userId = userId;
        this.assessmentId = assessmentId;
        this.protectionLevel = protectionLevel;
        this.status = ProtectionStatus.PROCESSING;
        this.originalImageUrl = originalImageUrl;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void complete(String protectedImageUrl, double visualDifference) {
        this.status = ProtectionStatus.COMPLETED;
        this.protectedImageUrl = protectedImageUrl;
        this.visualDifference = visualDifference;
    }

    public void fail() {
        this.status = ProtectionStatus.FAILED;
    }
}
