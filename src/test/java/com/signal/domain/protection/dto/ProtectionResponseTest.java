package com.signal.domain.protection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.protection.dto.response.ProtectionResponse;
import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.entity.ProtectionLevel;
import org.junit.jupiter.api.Test;

class ProtectionResponseTest {

    @Test
    void 시연용_보호결과는_simulated이고_가짜_visualDifference를_노출하지_않는다() {
        Protection protection = Protection.builder().userId(1L).assessmentId(1L)
                .originalImageUrl("/uploads/a.png").protectionLevel(ProtectionLevel.NORMAL).build();
        protection.complete("/uploads/protections/p.png", 0.04);

        ProtectionResponse response = ProtectionResponse.from(protection);

        assertThat(response.simulated()).isTrue();
        assertThat(response.visualDifference()).isNull();
    }
}
