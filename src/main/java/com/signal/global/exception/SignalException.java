package com.signal.global.exception;

import lombok.Getter;

@Getter
public class SignalException extends RuntimeException {

    private final ErrorCode errorCode;

    public SignalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
