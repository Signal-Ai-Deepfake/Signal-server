package com.signal.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank String verificationToken,
        @NotBlank String newPassword,
        @NotBlank String newPasswordConfirm
) {
}
