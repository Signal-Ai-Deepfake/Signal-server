package com.signal.global.i18n;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 모든 요청의 Accept-Language 헤더를 읽어 {@link LanguageContext}에 담고, 요청이 끝나면 정리한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LanguageContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LanguageContext.set(ResponseLanguage.fromAcceptLanguage(request.getHeader("Accept-Language")));
        try {
            chain.doFilter(request, response);
        } finally {
            LanguageContext.clear();
        }
    }
}
