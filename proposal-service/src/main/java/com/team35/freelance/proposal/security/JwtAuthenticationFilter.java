package com.team35.freelance.proposal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.team35.freelance.proposal.common.security.chain.*;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.contains("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

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
        String role = claims.get("role", String.class);
        Object uidObj = claims.get("userId");
        Long uid = uidObj != null ? ((Number) uidObj).longValue() : null;

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        uid,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}