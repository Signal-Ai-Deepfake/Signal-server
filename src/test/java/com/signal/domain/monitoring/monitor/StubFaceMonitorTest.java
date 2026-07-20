package com.signal.domain.monitoring.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.entity.Monitoring;
import com.signal.domain.monitoring.repository.DetectionRepository;
import com.signal.domain.monitoring.repository.MonitoringRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StubFaceMonitorTest {

    @Mock
    private MonitoringRepository monitoringRepository;

    @Mock
    private DetectionRepository detectionRepository;

    private StubFaceMonitor stubFaceMonitor;

    @BeforeEach
    void setUp() {
        stubFaceMonitor = new StubFaceMonitor(monitoringRepository, detectionRepository, 0);
    }

    @Test
    void 동일한_기준이미지_URL은_매번_동일한_탐지결과를_생성한다() {
        Monitoring monitoring = Monitoring.builder().userId(1L).referenceImageUrl("/uploads/monitorings/a.png").build();
        when(monitoringRepository.findById(1L)).thenReturn(Optional.of(monitoring));

        stubFaceMonitor.startMonitoring(1L, "/uploads/monitorings/a.png");
        List<Detection> firstRun = captureSaved();

        stubFaceMonitor.startMonitoring(1L, "/uploads/monitorings/a.png");
        List<Detection> secondRun = captureSaved();

        assertThat(secondRun).hasSize(firstRun.size() * 2);
        for (int i = 0; i < firstRun.size(); i++) {
            assertThat(secondRun.get(i).getSourceUrl()).isEqualTo(firstRun.get(i).getSourceUrl());
            assertThat(secondRun.get(i).getSimilarity()).isEqualTo(firstRun.get(i).getSimilarity());
        }
    }

    private List<Detection> captureSaved() {
        ArgumentCaptor<Detection> captor = ArgumentCaptor.forClass(Detection.class);
        verify(detectionRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void 모니터링이_이미_삭제되었으면_탐지를_생성하지_않는다() {
        when(monitoringRepository.findById(1L)).thenReturn(Optional.empty());

        stubFaceMonitor.startMonitoring(1L, "/uploads/monitorings/a.png");

        verify(detectionRepository, never()).save(any());
    }

    @Test
    void 탐지가_생성되면_해당_모니터링ID로_저장된다() {
        Monitoring monitoring = Monitoring.builder().userId(1L)
                .referenceImageUrl("/uploads/monitorings/many-detections-seed.png").build();
        when(monitoringRepository.findById(42L)).thenReturn(Optional.of(monitoring));

        stubFaceMonitor.startMonitoring(42L, "/uploads/monitorings/many-detections-seed.png");

        ArgumentCaptor<Detection> captor = ArgumentCaptor.forClass(Detection.class);
        verify(detectionRepository, org.mockito.Mockito.atMost(3)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(detection ->
                assertThat(detection.getMonitoringId()).isEqualTo(42L));
    }
}
