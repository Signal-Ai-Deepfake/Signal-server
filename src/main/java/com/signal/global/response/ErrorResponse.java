package com.signal.global.response;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message);
    }
}
