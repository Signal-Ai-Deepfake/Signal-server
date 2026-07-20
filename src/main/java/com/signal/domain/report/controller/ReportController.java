package com.signal.domain.report.controller;

import com.signal.domain.report.dto.request.CreateReportRequest;
import com.signal.domain.report.dto.request.UpdateReportRequest;
import com.signal.domain.report.dto.response.ReportResponse;
import com.signal.domain.report.dto.response.ReportStatusResponse;
import com.signal.domain.report.entity.Report;
import com.signal.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportStatusResponse> createReport(
            @AuthenticationPrincipal Long userId,
            @RequestBody CreateReportRequest request) {
        Report report = reportService.createReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportStatusResponse.from(report));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reportId) {
        Report report = reportService.getReport(userId, reportId);
        return ResponseEntity.ok(ReportResponse.from(report));
    }

    @PatchMapping("/{reportId}")
    public ResponseEntity<ReportResponse> updateReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reportId,
            @RequestBody UpdateReportRequest request) {
        Report report = reportService.updateReport(userId, reportId, request);
        return ResponseEntity.ok(ReportResponse.from(report));
    }

    @PostMapping("/{reportId}/finalize")
    public ResponseEntity<ReportStatusResponse> finalizeReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long reportId) {
        Report report = reportService.finalizeReport(userId, reportId);
        return ResponseEntity.ok(ReportStatusResponse.from(report));
    }
}
