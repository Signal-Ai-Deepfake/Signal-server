package com.signal.domain.auth.dto.request;

import com.signal.domain.auth.service.VerificationService.Purpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyCodeRequest(
        @Email @NotBlank String email,
        @NotBlank String code,
        @NotNull Purpose purpose
) {
}
