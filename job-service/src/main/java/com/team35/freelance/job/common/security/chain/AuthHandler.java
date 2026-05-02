package com.team35.freelance.job.common.security.chain;

import jakarta.servlet.http.HttpServletResponse;

public abstract class AuthHandler {

    protected AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public abstract boolean handle(AuthContext ctx, HttpServletResponse response);
}

