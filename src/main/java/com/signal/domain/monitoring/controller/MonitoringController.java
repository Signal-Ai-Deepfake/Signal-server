package com.signal.domain.monitoring.controller;

import com.signal.domain.monitoring.dto.response.DetectionPageResponse;
import com.signal.domain.monitoring.dto.response.MonitoringCreateResponse;
import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.entity.Monitoring;
import com.signal.domain.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/monitorings")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Operation(summary = "얼굴 도용 모니터링 시작",
            description = "[현재 스텁] 실제 검색은 수행되지 않고 시연용 예시 탐지 결과가 생성됩니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MonitoringCreateResponse> createMonitoring(
            @AuthenticationPrincipal Long userId,
            @RequestPart("referenceImage") MultipartFile referenceImage) {
        Monitoring monitoring = monitoringService.createMonitoring(userId, referenceImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(MonitoringCreateResponse.from(monitoring));
    }

    @Operation(summary = "탐지 결과 조회",
            description = "[현재 스텁] simulated=true 항목은 시연용 예시이며 sourceUrl 은 null, thumbnailUrl 은 SAMPLE 이미지입니다.")
    @GetMapping("/{monitoringId}/detections")
    public ResponseEntity<DetectionPageResponse> getDetections(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long monitoringId,
            Pageable pageable) {
        Page<Detection> detections = monitoringService.getDetections(userId, monitoringId, pageable);
        return ResponseEntity.ok(DetectionPageResponse.from(detections));
    }

    @DeleteMapping("/{monitoringId}")
    public ResponseEntity<Void> deleteMonitoring(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long monitoringId) {
        monitoringService.deleteMonitoring(userId, monitoringId);
        return ResponseEntity.noContent().build();
    }
}
