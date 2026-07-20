package com.signal.domain.report.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private LocalDate incidentDate;

    private String discoveryRoute;

    private String damageType;

    @Column(nullable = false, length = 3000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_source_urls", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "source_url_order")
    @Column(name = "source_url", nullable = false, length = 1000)
    private List<String> sourceUrls;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_evidence_ids", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "evidence_id_order")
    @Column(name = "evidence_id", nullable = false)
    private List<Long> evidenceIds;

    private String targetAgencyType;

    private String documentUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_timeline", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "timeline_order")
    private List<TimelineEntry> timeline;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Report(Long userId, LocalDate incidentDate, String discoveryRoute, String damageType,
                  String description, List<String> sourceUrls, List<Long> evidenceIds,
                  String targetAgencyType, String documentUrl) {
        this.userId = userId;
        this.status = ReportStatus.DRAFT;
        this.incidentDate = incidentDate;
        this.discoveryRoute = discoveryRoute;
        this.damageType = damageType;
        this.description = description;
        this.sourceUrls = sourceUrls;
        this.evidenceIds = evidenceIds;
        this.targetAgencyType = targetAgencyType;
        this.documentUrl = documentUrl;
        this.timeline = new ArrayList<>();
        this.timeline.add(TimelineEntry.builder().event(TimelineEvent.CREATED).occurredAt(LocalDateTime.now()).build());
        this.createdAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void applyPatch(LocalDate incidentDate, String discoveryRoute, String damageType,
                            String description, List<String> sourceUrls, List<Long> evidenceIds,
                            String targetAgencyType, String documentUrl) {
        if (incidentDate != null) {
            this.incidentDate = incidentDate;
        }
        if (discoveryRoute != null) {
            this.discoveryRoute = discoveryRoute;
        }
        if (damageType != null) {
            this.damageType = damageType;
        }
        if (description != null) {
            this.description = description;
        }
        if (sourceUrls != null) {
            this.sourceUrls = sourceUrls;
        }
        if (evidenceIds != null) {
            this.evidenceIds = evidenceIds;
        }
        if (targetAgencyType != null) {
            this.targetAgencyType = targetAgencyType;
        }
        this.documentUrl = documentUrl;
        this.timeline.add(TimelineEntry.builder().event(TimelineEvent.UPDATED).occurredAt(LocalDateTime.now()).build());
    }

    public void markFinalized(String documentUrl) {
        this.status = ReportStatus.FINALIZED;
        this.documentUrl = documentUrl;
        this.timeline.add(TimelineEntry.builder().event(TimelineEvent.FINALIZED).occurredAt(LocalDateTime.now()).build());
    }
}
