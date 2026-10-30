package com.signal.domain.deepfakedetection.controller;

import com.signal.domain.deepfakedetection.dto.response.DeepfakeDetectionCreateResponse;
import com.signal.domain.deepfakedetection.dto.response.DeepfakeDetectionResponse;
import com.signal.domain.deepfakedetection.entity.DeepfakeDetection;
import com.signal.domain.deepfakedetection.service.DeepfakeDetectionService;
import com.signal.domain.websearch.entity.WebSearch;
import com.signal.domain.websearch.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/deepfake-detections")
@RequiredArgsConstructor
public class DeepfakeDetectionController {

    private final DeepfakeDetectionService deepfakeDetectionService;
    private final WebSearchService webSearchService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DeepfakeDetectionCreateResponse> createDetection(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @RequestPart("file") MultipartFile file) {
        DeepfakeDetection detection = deepfakeDetectionService.createDetection(userId, anonymousId, file);
        // 합성 분석과 별개로 웹 검색을 시작한다(설정이 꺼져 있으면 아무 일도 하지 않는다).
        webSearchService.start(detection.getId(), detection.getFileUrl(),
                file.getContentType() != null && file.getContentType().startsWith("video/"));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DeepfakeDetectionCreateResponse.from(detection));
    }

    @GetMapping("/{detectionId}")
    public ResponseEntity<DeepfakeDetectionResponse> getDetection(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @PathVariable Long detectionId) {
        DeepfakeDetection detection = deepfakeDetectionService.getDetection(userId, anonymousId, detectionId);
        return ResponseEntity.ok(DeepfakeDetectionResponse.from(detection, webSearchService.find(detectionId)));
    }
}
