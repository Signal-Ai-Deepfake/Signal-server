package com.signal.domain.chat.dto.request;

public record CreateChatSessionRequest(Boolean saveConsent) {

    public boolean saveConsentOrDefault() {
        return saveConsent != null && saveConsent;
    }
}
