package com.signal.domain.protection.controller;

import com.signal.domain.protection.dto.request.CreateProtectionRequest;
import com.signal.domain.protection.dto.response.ProtectionCreateResponse;
import com.signal.domain.protection.dto.response.ProtectionResponse;
import com.signal.domain.protection.entity.Protection;
import com.signal.domain.protection.service.ProtectionService;
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

    @PostMapping
    public ResponseEntity<ProtectionCreateResponse> createProtection(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateProtectionRequest request) {
        Protection protection = protectionService.createProtection(
                userId, request.assessmentId(), request.protectionLevel());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProtectionCreateResponse.from(protection));
    }

    @GetMapping("/{protectionId}")
    public ResponseEntity<ProtectionResponse> getProtection(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long protectionId) {
        Protection protection = protectionService.getProtection(userId, protectionId);
        return ResponseEntity.ok(ProtectionResponse.from(protection));
    }

    @GetMapping("/{protectionId}/download")
    public ResponseEntity<byte[]> downloadProtectedImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long protectionId) {
        byte[] image = protectionService.downloadProtectedImage(userId, protectionId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
    }
}
