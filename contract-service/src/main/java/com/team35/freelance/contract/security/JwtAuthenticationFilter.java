package com.team35.freelance.contract.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.team35.freelance.contract.common.security.chain.*;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final RequestMatcher[] PUBLIC_INTERNAL_GET_ENDPOINTS = {
            get("^/actuator/health$"),
            get("^/api/health$"),
            get("^/api/contracts/[0-9]+$"),
            get("^/api/contracts/user/[0-9]+/summary$"),
            get("^/api/contracts/user/[0-9]+/active-count$"),
            get("^/api/contracts/user/[0-9]+/completed-count$"),
            get("^/api/contracts/job/[0-9]+/active-count$"),
            get("^/api/contracts/proposal/[0-9]+/active$")
    };

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("CONTRACT JWT FILTER PATH = {}", path);

        for (RequestMatcher matcher : PUBLIC_INTERNAL_GET_ENDPOINTS) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Create context
        AuthContext ctx = new AuthContext(request);

        // Build chain
        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService))
                .setNext(new RoleAuthorizationHandler());

        // Execute chain
        boolean success = chain.handle(ctx, response);

        if (!success) {
            return;
        }

        // Set Spring Security context
        Claims claims = jwtService.extractClaims(ctx.getToken());
        String email = claims.getSubject();
        String role = normalizeRole(claims.get("role", String.class));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private static RequestMatcher get(String pattern) {
        return RegexRequestMatcher.regexMatcher(HttpMethod.GET, pattern);
    }
}
