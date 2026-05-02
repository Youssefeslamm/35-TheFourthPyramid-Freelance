package com.team35.freelance.user.common.security.chain;

import jakarta.servlet.http.HttpServletResponse;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        // ❌ No user → 401 (NOT 403)
        if (ctx.getUser() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String requiredRole = ctx.getRequiredRole();

        // ✅ No role required → allow
        if (requiredRole == null) {
            return proceed(ctx, response);
        }

        String userRole = ctx.getUser().getRole().name();

        // ❌ Role mismatch → 403
        if (!userRole.equals(requiredRole)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
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