package com.signal.domain.monitoring.dto.response;

import com.signal.domain.monitoring.entity.Monitoring;
import com.signal.domain.monitoring.entity.MonitoringStatus;

public record MonitoringCreateResponse(Long monitoringId, MonitoringStatus status) {

    public static MonitoringCreateResponse from(Monitoring monitoring) {
        return new MonitoringCreateResponse(monitoring.getId(), monitoring.getStatus());
    }
}
