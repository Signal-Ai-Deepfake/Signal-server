package com.signal.domain.protection.controller;

import com.signal.domain.protection.dto.request.CreateProtectionRequest;
import com.signal.domain.protection.dto.response.ProtectionCreateResponse;
import com.signal.domain.protection.dto.response.ProtectionResponse;
import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.service.ProtectionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/protections")
@RequiredArgsConstructor
public class ProtectionController {

    private final ProtectionService protectionService;

    @Operation(summary = "이미지 보호 처리 요청",
            description = "[현재 스텁] 실제 노이즈 삽입은 구현되지 않았으며 원본 이미지가 그대로 복사됩니다. 응답의 simulated=true 로 구분하세요.")
    @PostMapping
    public ResponseEntity<ProtectionCreateResponse> createProtection(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateProtectionRequest request) {
        Protection protection = protectionService.createProtection(
                userId, request.assessmentId(), request.protectionLevel());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProtectionCreateResponse.from(protection));
    }

    @Operation(summary = "보호 처리 결과 조회",
            description = "[현재 스텁] simulated=true 이면 시연용 결과이며 visualDifference 는 제공되지 않습니다.")
    @GetMapping("/{protectionId}")
    public ResponseEntity<ProtectionResponse> getProtection(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long protectionId) {
        Protection protection = protectionService.getProtection(userId, protectionId);
        return ResponseEntity.ok(ProtectionResponse.from(protection));
    }

    @Operation(summary = "보호 이미지 다운로드",
            description = "[현재 스텁] 원본과 동일한 이미지가 내려갑니다(보호 효과 없음).")
    @GetMapping("/{protectionId}/download")
    public ResponseEntity<byte[]> downloadProtectedImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long protectionId) {
        byte[] image = protectionService.downloadProtectedImage(userId, protectionId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
    }
}
