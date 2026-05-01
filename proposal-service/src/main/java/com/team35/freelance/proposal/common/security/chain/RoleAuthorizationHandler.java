package com.team35.freelance.proposal.common.security.chain;

import jakarta.servlet.http.HttpServletResponse;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        if (ctx.getUser() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String requiredRole = ctx.getRequiredRole();

        if (requiredRole == null) {
            return proceed(ctx, response);
        }

        return proceed(ctx, response);
    }

    private boolean proceed(AuthContext ctx, HttpServletResponse response) {
        if (next != null) {
            return next.handle(ctx, response);
        }
        return true;
    }
}

