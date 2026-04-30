package com.team35.freelance.user.common.security.chain;

import jakarta.servlet.http.HttpServletResponse;

public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        // ❌ No user → forbidden
        if (ctx.getUser() == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        // (Optional: later you can check roles here)

        // ✅ Continue chain
        if (next != null) {
            return next.handle(ctx, response);
        }

        return true;
    }
}