package com.signal.domain.protection.dto.response;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionStatus;

public record ProtectionResponse(
        Long protectionId,
        ProtectionStatus status,
        String originalImageUrl,
        String protectedImageUrl,
        Double visualDifference
) {

    public static ProtectionResponse from(Protection protection) {
        return new ProtectionResponse(
                protection.getId(),
                protection.getStatus(),
                protection.getOriginalImageUrl(),
                protection.getProtectedImageUrl(),
                protection.getVisualDifference());
    }
}
