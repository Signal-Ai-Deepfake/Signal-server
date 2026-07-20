package com.signal.domain.agency.dto.response;

import com.signal.domain.agency.entity.Agency;
import java.util.List;

public record AgencyResponse(
        Long agencyId,
        String name,
        String phone,
        String website,
        String availableHours,
        List<String> supportedActions
) {

    public static AgencyResponse from(Agency agency) {
        return new AgencyResponse(
                agency.getId(),
                agency.getName(),
                agency.getPhone(),
                agency.getWebsite(),
                agency.getAvailableHours(),
                agency.getSupportedActions());
    }
}
