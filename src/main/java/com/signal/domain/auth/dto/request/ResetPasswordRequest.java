package com.signal.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank String verificationToken,
        @NotBlank @Size(min = 8, max = 72) String newPassword,
        @NotBlank String newPasswordConfirm
) {
}
