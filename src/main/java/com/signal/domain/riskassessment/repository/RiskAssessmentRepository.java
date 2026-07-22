package com.signal.domain.riskassessment.repository;

import com.signal.domain.riskassessment.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    long countByAnonymousId(String anonymousId);
}
