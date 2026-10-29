package com.signal.domain.agency.dto.response;

import com.signal.domain.agency.entity.Agency;
import com.signal.domain.agency.i18n.AgencyTexts;
import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

public record AgencyResponse(
        Long agencyId,
        String name,
        String phone,
        String website,
        String availableHours,
        List<String> supportedActions
) {

    public static AgencyResponse from(Agency agency, ResponseLanguage language) {
        return new AgencyResponse(
                agency.getId(),
                AgencyTexts.name(agency.getName(), language),
                agency.getPhone(),
                agency.getWebsite(),
                AgencyTexts.availableHours(agency.getAvailableHours(), language),
                AgencyTexts.supportedActions(agency.getSupportedActions(), language));
    }
}
