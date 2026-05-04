package com.team35.freelance.contract.common.security.chain;

import com.team35.freelance.contract.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;

public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean handle(AuthContext ctx, HttpServletResponse response) {

        if (!jwtService.isTokenValid(ctx.getToken())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (next != null) {
            return next.handle(ctx, response);
        }

        return true;
    }
}

