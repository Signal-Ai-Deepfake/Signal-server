package com.signal.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.signal.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid.access.token";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication doFilterWithBearer(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void 유효한_액세스_토큰이고_사용자가_존재하면_인증을_설정한다() throws Exception {
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isRefreshToken(TOKEN)).thenReturn(false);
        when(jwtProvider.getUserId(TOKEN)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);

        Authentication authentication = doFilterWithBearer(TOKEN);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
    }

    @Test
    void 탈퇴한_사용자의_토큰이면_인증하지_않는다() throws Exception {
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isRefreshToken(TOKEN)).thenReturn(false);
        when(jwtProvider.getUserId(TOKEN)).thenReturn(99L);
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThat(doFilterWithBearer(TOKEN)).isNull();
    }

    @Test
    void 리프레시_토큰으로는_인증하지_않는다() throws Exception {
        when(jwtProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtProvider.isRefreshToken(TOKEN)).thenReturn(true);

        assertThat(doFilterWithBearer(TOKEN)).isNull();
        verify(userRepository, never()).existsById(anyLong());
    }

    @Test
    void 서명이_유효하지_않은_토큰으로는_인증하지_않는다() throws Exception {
        when(jwtProvider.validateToken(TOKEN)).thenReturn(false);

        assertThat(doFilterWithBearer(TOKEN)).isNull();
    }

    @Test
    void 토큰이_없으면_인증하지_않는다() throws Exception {
        assertThat(doFilterWithBearer(null)).isNull();
    }
}
