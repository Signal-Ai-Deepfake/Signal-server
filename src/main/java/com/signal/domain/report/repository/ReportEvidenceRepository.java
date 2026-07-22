package com.signal.domain.report.repository;

import com.signal.domain.report.entity.ReportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportEvidenceRepository extends JpaRepository<ReportEvidence, Long> {
}
