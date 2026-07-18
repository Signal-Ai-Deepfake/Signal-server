package com.signal.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
