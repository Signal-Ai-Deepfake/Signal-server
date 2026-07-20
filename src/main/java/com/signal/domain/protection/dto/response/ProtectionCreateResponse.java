package com.signal.domain.protection.dto.response;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionStatus;

public record ProtectionCreateResponse(Long protectionId, ProtectionStatus status) {

    public static ProtectionCreateResponse from(Protection protection) {
        return new ProtectionCreateResponse(protection.getId(), protection.getStatus());
    }
}
