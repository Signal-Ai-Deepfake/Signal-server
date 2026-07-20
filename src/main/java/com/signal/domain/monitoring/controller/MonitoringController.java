package com.signal.domain.monitoring.controller;

import com.signal.domain.monitoring.dto.response.DetectionPageResponse;
import com.signal.domain.monitoring.dto.response.MonitoringCreateResponse;
import com.signal.domain.monitoring.entity.Detection;
import com.signal.domain.monitoring.entity.Monitoring;
import com.signal.domain.monitoring.service.MonitoringService;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MonitoringCreateResponse> createMonitoring(
            @AuthenticationPrincipal Long userId,
            @RequestPart("referenceImage") MultipartFile referenceImage) {
        Monitoring monitoring = monitoringService.createMonitoring(userId, referenceImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(MonitoringCreateResponse.from(monitoring));
    }

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
