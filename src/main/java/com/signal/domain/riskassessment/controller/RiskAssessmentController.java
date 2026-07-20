package com.signal.domain.riskassessment.controller;

import com.signal.domain.riskassessment.dto.response.RiskAssessmentResponse;
import com.signal.domain.riskassessment.entity.RiskAssessment;
import com.signal.domain.riskassessment.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/risk-assessments")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RiskAssessmentResponse> createAssessment(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        RiskAssessment riskAssessment = riskAssessmentService.createAssessment(userId, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(RiskAssessmentResponse.from(riskAssessment));
    }

    @GetMapping("/{assessmentId}")
    public ResponseEntity<RiskAssessmentResponse> getAssessment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long assessmentId) {
        RiskAssessment riskAssessment = riskAssessmentService.getAssessment(userId, assessmentId);
        return ResponseEntity.ok(RiskAssessmentResponse.from(riskAssessment));
    }
}
