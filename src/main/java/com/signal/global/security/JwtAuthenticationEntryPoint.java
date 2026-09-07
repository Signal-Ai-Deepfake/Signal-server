package com.signal.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 요청에 401을 반환한다.
 *
 * <p>formLogin/httpBasic을 모두 끄면 등록된 AuthenticationEntryPoint가 없어
 * Spring Security 기본값이 403을 빈 본문으로 내려준다. 그러면 클라이언트가
 * "미로그인"과 "권한 없음"을 구분할 수 없고, 액세스 토큰 만료 시점을 알 수 없어
 * 토큰 재발급 처리도 할 수 없다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(response, objectMapper, ErrorCode.UNAUTHORIZED);
    }
}
