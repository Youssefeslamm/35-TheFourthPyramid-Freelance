package com.team35.freelance.user.common.security.chain;

import jakarta.servlet.http.HttpServletResponse;

public class TokenExtractionHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        String authHeader = ctx.getRequest().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        ctx.setToken(token);

        if (next != null) {
            return next.handle(ctx, response);
        }

        return true;
    }
}