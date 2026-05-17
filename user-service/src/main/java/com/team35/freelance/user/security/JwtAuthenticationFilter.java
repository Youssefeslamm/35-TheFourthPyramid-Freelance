package com.team35.freelance.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.team35.freelance.user.common.security.chain.*;
import com.team35.freelance.user.repository.UserRepository;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ 1. INTERNAL CALL BYPASS (MOST IMPORTANT)
        String internal = request.getHeader("X-INTERNAL-CALL");
        if ("true".equals(internal)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // ✅ 2. PUBLIC ENDPOINTS
        if (path.startsWith("/api/auth/") || path.contains("/health")
                || path.equals("/actuator/prometheus") || path.equals("/actuator/info")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 3. NORMAL AUTH FLOW (ONLY FOR REAL USERS)
        AuthContext ctx = new AuthContext(request);

        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService, userRepository))
                .setNext(new RoleAuthorizationHandler());

        boolean success = chain.handle(ctx, response);
        if (!success) return;

        // ✅ 4. SET SECURITY CONTEXT
        String email = ctx.getUser().getEmail();
        String role = ctx.getUser().getRole().name();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                )
        );

        filterChain.doFilter(request, response);
    }
}
