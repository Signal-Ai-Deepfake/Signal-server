package com.signal.domain.agency.entity;

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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실제 지원 기관 참고 데이터. 사용자별 리소스가 아니라 data.sql로 시드되는 공용 참조 데이터라
 * 다른 엔티티들과 달리 createdAt을 두지 않는다.
 */
@Entity
@Table(name = "agencies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String website;

    private String availableHours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agency_supported_actions", joinColumns = @JoinColumn(name = "agency_id"))
    @OrderColumn(name = "supported_action_order")
    @Column(name = "supported_action", nullable = false, length = 200)
    private List<String> supportedActions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agency_supported_situation_types", joinColumns = @JoinColumn(name = "agency_id"))
    @OrderColumn(name = "supported_situation_type_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "supported_situation_type", nullable = false, length = 50)
    private List<AgencySituationType> supportedSituationTypes;

    @Builder
    public Agency(String name, String phone, String website, String availableHours,
                  List<String> supportedActions, List<AgencySituationType> supportedSituationTypes) {
        this.name = name;
        this.phone = phone;
        this.website = website;
        this.availableHours = availableHours;
        this.supportedActions = supportedActions;
        this.supportedSituationTypes = supportedSituationTypes;
    }
}
