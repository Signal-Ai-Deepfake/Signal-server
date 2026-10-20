package com.signal.domain.monitoring.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.monitoring.dto.response.DetectionResponse;
import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.monitor.StubFaceMonitor;
import org.junit.jupiter.api.Test;

class DetectionResponseTest {

    @Test
    void 시연용_탐지결과는_출처링크를_숨기고_플레이스홀더_썸네일과_simulated를_내려준다() {
        Detection detection = Detection.builder().monitoringId(1L)
                .sourceUrl(StubFaceMonitor.SIMULATED_URL_PREFIX + "source/ab")
                .thumbnailUrl(StubFaceMonitor.SIMULATED_URL_PREFIX + "thumbnail/ab")
                .similarity(0.8).build();

        DetectionResponse response = DetectionResponse.from(detection);

        assertThat(response.simulated()).isTrue();
        assertThat(response.sourceUrl()).isNull();
        assertThat(response.thumbnailUrl()).isEqualTo(StubFaceMonitor.PLACEHOLDER_THUMBNAIL);
    }

    @Test
    void 기존_stub_signal_local_데이터도_시연용으로_처리한다() {
        Detection detection = Detection.builder().monitoringId(1L)
                .sourceUrl("https://stub.signal.local/detections/source/ab")
                .thumbnailUrl("https://stub.signal.local/detections/thumbnail/ab")
                .similarity(0.8).build();

        assertThat(DetectionResponse.from(detection).simulated()).isTrue();
    }

    @Test
    void 실제_URL은_그대로_내려준다() {
        Detection detection = Detection.builder().monitoringId(1L)
                .sourceUrl("https://example.com/a").thumbnailUrl("https://example.com/t.png")
                .similarity(0.9).build();

        DetectionResponse response = DetectionResponse.from(detection);

        assertThat(response.simulated()).isFalse();
        assertThat(response.sourceUrl()).isEqualTo("https://example.com/a");
    }
}
