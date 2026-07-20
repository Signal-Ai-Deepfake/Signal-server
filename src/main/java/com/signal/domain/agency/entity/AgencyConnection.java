package com.signal.domain.agency.entity;

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
@Table(name = "agency_connections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long agencyId;

    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionType connectionType;

    @Column(nullable = false)
    private String agencyName;

    @Column(nullable = false)
    private String phone;

    private String attachedReportUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AgencyConnection(Long userId, Long agencyId, Long reportId, ConnectionType connectionType,
                             String agencyName, String phone, String attachedReportUrl) {
        this.userId = userId;
        this.agencyId = agencyId;
        this.reportId = reportId;
        this.connectionType = connectionType;
        this.agencyName = agencyName;
        this.phone = phone;
        this.attachedReportUrl = attachedReportUrl;
        this.createdAt = LocalDateTime.now();
    }
}
