package com.signal.domain.protection.dto.response;

import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionStatus;

/**
 * simulated=true 이면 실제 노이즈 삽입이 이루어지지 않은 시연용 결과다(StubImageProtector).
 * 이 경우 visualDifference는 실측값이 아니므로 null로 내려간다. 실제 구현체로 교체하면 함께 수정한다.
 */
public record ProtectionResponse(
        Long protectionId,
        ProtectionStatus status,
        String originalImageUrl,
        String protectedImageUrl,
        Double visualDifference,
        boolean simulated
) {

    private static final boolean SIMULATED = true;

    public static ProtectionResponse from(Protection protection) {
        return new ProtectionResponse(
                protection.getId(),
                protection.getStatus(),
                protection.getOriginalImageUrl(),
                protection.getProtectedImageUrl(),
                SIMULATED ? null : protection.getVisualDifference(),
                SIMULATED);
    }
}
