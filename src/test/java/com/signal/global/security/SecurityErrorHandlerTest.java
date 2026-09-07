package com.signal.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.global.exception.ErrorCode;
import com.signal.global.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 인증되지_않은_요청에는_401_UNAUTHORIZED를_JSON으로_반환한다() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("인증 정보 없음"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.code()).isEqualTo(ErrorCode.UNAUTHORIZED.name());
        assertThat(body.message()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void 권한이_없는_요청에는_403_FORBIDDEN을_JSON으로_반환한다() throws Exception {
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("권한 없음"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.code()).isEqualTo(ErrorCode.FORBIDDEN.name());
    }

    @Test
    void 응답_본문이_비어있지_않다() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("인증 정보 없음"));

        // 기존에는 Spring Security 기본값이 403을 빈 본문으로 내려줘서 원인 파악이 불가능했다.
        assertThat(response.getContentAsString()).isNotBlank();
    }
}
