package com.signal.domain.monitoring.monitor;

import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.repository.DetectionRepository;
import com.signal.domain.monitoring.repository.MonitoringRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 실제 AI 서버 연동 전까지 사용하는 스텁 구현체. 기준 이미지 URL의 해시로 결정론적인 초기 탐지
 * 결과(0~3건)를 생성한다. 실서버 연동 시 이 클래스를 교체한다.
 *
 * MonitoringService(→FaceMonitor) 의존 방향의 순환을 피하기 위해 탐지 결과 반영 시
 * MonitoringService가 아닌 MonitoringRepository/DetectionRepository에 직접 접근한다.
 */
@Slf4j
@Component
public class StubFaceMonitor implements FaceMonitor {

    private static final int MAX_INITIAL_DETECTIONS = 3;

    private final MonitoringRepository monitoringRepository;
    private final DetectionRepository detectionRepository;
    private final long processingDelayMs;

    public StubFaceMonitor(MonitoringRepository monitoringRepository,
                            DetectionRepository detectionRepository,
                            @Value("${monitoring.processing-delay-ms:3000}") long processingDelayMs) {
        this.monitoringRepository = monitoringRepository;
        this.detectionRepository = detectionRepository;
        this.processingDelayMs = processingDelayMs;
    }

    @Override
    @Async
    public void startMonitoring(Long monitoringId, String referenceImageUrl) {
        try {
            Thread.sleep(processingDelayMs);

            if (monitoringRepository.findById(monitoringId).isEmpty()) {
                return;
            }

            int seed = Math.abs(referenceImageUrl.hashCode());
            int detectionCount = seed % (MAX_INITIAL_DETECTIONS + 1);

            for (int i = 0; i < detectionCount; i++) {
                detectionRepository.save(buildDetection(monitoringId, seed + i * 31));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("얼굴 모니터링 초기 탐지 실패: monitoringId={}", monitoringId, e);
        }
    }

    private Detection buildDetection(Long monitoringId, int detectionSeed) {
        String key = Integer.toHexString(Math.abs(detectionSeed));
        return Detection.builder()
                .monitoringId(monitoringId)
                .sourceUrl("https://stub.signal.local/detections/source/" + key)
                .thumbnailUrl("https://stub.signal.local/detections/thumbnail/" + key)
                .similarity(0.7 + (Math.abs(detectionSeed) % 30) / 100.0)
                .build();
    }
}
