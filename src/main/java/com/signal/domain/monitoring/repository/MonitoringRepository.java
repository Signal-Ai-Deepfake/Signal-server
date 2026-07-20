package com.signal.domain.monitoring.repository;

import com.signal.domain.monitoring.entity.Monitoring;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringRepository extends JpaRepository<Monitoring, Long> {
}
