package com.signal.domain.agency.dto.response;

import com.signal.domain.agency.entity.Agency;
import java.util.List;

public record AgencyListResponse(List<AgencyResponse> agencies) {

    public static AgencyListResponse from(List<Agency> agencies) {
        return new AgencyListResponse(agencies.stream().map(AgencyResponse::from).toList());
    }
}
