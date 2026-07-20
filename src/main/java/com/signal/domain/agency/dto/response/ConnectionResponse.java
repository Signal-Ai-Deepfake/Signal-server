package com.signal.domain.agency.dto.response;

import com.signal.domain.agency.entity.AgencyConnection;
import com.signal.domain.agency.entity.ConnectionType;

public record ConnectionResponse(
        Long connectionId,
        String agencyName,
        ConnectionType connectionType,
        String phone,
        String attachedReportUrl
) {

    public static ConnectionResponse from(AgencyConnection connection) {
        return new ConnectionResponse(
                connection.getId(),
                connection.getAgencyName(),
                connection.getConnectionType(),
                connection.getPhone(),
                connection.getAttachedReportUrl());
    }
}
