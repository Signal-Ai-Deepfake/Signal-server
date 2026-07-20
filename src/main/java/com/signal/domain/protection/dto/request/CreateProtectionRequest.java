package com.signal.domain.protection.dto.request;

import com.signal.domain.protection.entity.ProtectionLevel;
import jakarta.validation.constraints.NotNull;

public record CreateProtectionRequest(
        @NotNull Long assessmentId,
        ProtectionLevel protectionLevel
) {
}
