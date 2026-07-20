package com.signal.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.global.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void 요청_본문_파싱에_실패하면_400_INVALID_INPUT을_반환한다() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("파싱 실패", (HttpInputMessage) null);

        ResponseEntity<ErrorResponse> response =
                globalExceptionHandler.handleHttpMessageNotReadableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INVALID_INPUT.name());
    }
}
