package com.signal.domain.auth.dto.request;

import com.signal.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String verificationToken,
        @NotBlank String name,
        @NotNull Integer age,
        @NotNull User.Gender gender,
        @NotNull Agreements agreements
) {
    public record Agreements(
            boolean termsOfService,
            boolean privacyPolicy
    ) {
        public boolean allAgreed() {
            return termsOfService && privacyPolicy;
        }
    }
}
