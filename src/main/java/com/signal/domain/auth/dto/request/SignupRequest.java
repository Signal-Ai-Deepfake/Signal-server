package com.signal.domain.auth.dto.request;

import com.signal.domain.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String verificationToken,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(1) @Max(150) Integer age,
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
