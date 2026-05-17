package com.team35.freelance.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        log.info("Received {} {}", request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("Returning {} for {} {}", response.getStatus(), request.getMethod(), request.getRequestURI());
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            if (elapsedMs > 1000) {
                log.warn("Slow {} took {}ms", request.getRequestURI(), elapsedMs);
            }
        }
    }
}
