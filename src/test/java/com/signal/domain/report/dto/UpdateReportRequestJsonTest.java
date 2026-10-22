package com.signal.domain.report.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.report.dto.request.UpdateReportRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class UpdateReportRequestJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 보내지_않은_필드는_null_null로_보낸_필드는_빈_Optional이_된다() throws Exception {
        UpdateReportRequest request = objectMapper.readValue(
                "{\"discoveryRoute\":null,\"sourceUrls\":[\"https://a\"],\"additionalNotes\":\"메모\"}",
                UpdateReportRequest.class);

        assertThat(request.getDamageType()).isNull();
        assertThat(request.getDiscoveryRoute()).isEmpty();
        assertThat(request.getSourceUrls()).contains(List.of("https://a"));
        assertThat(request.getAdditionalNotes()).isEqualTo(Optional.of("메모"));
    }
}
