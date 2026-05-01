package com.team35.freelance.proposal.common.security.chain;

import com.team35.freelance.proposal.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;

public class UserLoaderHandler extends AuthHandler {

    private final JwtService jwtService;

    public UserLoaderHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        try {
            Claims claims = jwtService.extractClaims(ctx.getToken());
            String email = claims.getSubject();

            if (email == null || email.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            ctx.setUser(email);

            if (next != null) {
                return next.handle(ctx, response);
            }

            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}

