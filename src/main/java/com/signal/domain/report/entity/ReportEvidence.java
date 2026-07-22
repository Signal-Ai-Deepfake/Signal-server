package com.signal.domain.report.entity;

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
@Table(name = "report_evidences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ReportEvidence(Long userId, String fileUrl) {
        this.userId = userId;
        this.fileUrl = fileUrl;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
