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

        String path = request.getRequestURI();

        // ✅ Skip authentication for public endpoints
        if (path.startsWith("/api/auth/") || path.contains("/health")) {
            filterChain.doFilter(request, response);
            return;
        }
// ✅ Create context
        AuthContext ctx = new AuthContext(request);

// ✅ Build chain
        AuthHandler chain = new TokenExtractionHandler();
        chain.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService, userRepository))
                .setNext(new RoleAuthorizationHandler());

// ✅ Execute chain
        boolean success = chain.handle(ctx, response);

// ❌ Stop if failed
      if (!success) {
    return;
}

String email = ctx.getUser().getEmail();
String role = ctx.getUser().getRole().name();

// ✅ create authentication object
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );



SecurityContextHolder.getContext().setAuthentication(authentication);

filterChain.doFilter(request, response);
}
