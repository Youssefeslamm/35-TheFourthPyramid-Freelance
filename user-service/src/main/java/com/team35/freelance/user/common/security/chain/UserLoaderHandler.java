package com.team35.freelance.user.common.security.chain;

import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;

public class UserLoaderHandler extends AuthHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserLoaderHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        try {
            // ✅ extract claims
            Claims claims = jwtService.extractClaims(ctx.getToken());

            Long userId = claims.get("userId", Long.class);

            // ❌ user not found
            return userRepository.findById(userId)
                    .map(user -> {
                        ctx.setUser(user);

                        // 👉 continue chain
                        if (next != null) {
                            return next.handle(ctx, response);
                        }
                        return true;
                    })
                    .orElseGet(() -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return false;
                    });

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}