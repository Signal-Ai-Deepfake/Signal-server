package com.signal.domain.agency.dto.response;

import com.signal.domain.agency.entity.Agency;
import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

public record AgencyListResponse(List<AgencyResponse> agencies) {

    public static AgencyListResponse from(List<Agency> agencies, ResponseLanguage language) {
        return new AgencyListResponse(agencies.stream().map(agency -> AgencyResponse.from(agency, language)).toList());
    }
}
