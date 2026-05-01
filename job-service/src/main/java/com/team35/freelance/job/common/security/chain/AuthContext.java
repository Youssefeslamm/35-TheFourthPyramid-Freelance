package com.team35.freelance.job.common.security.chain;

import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {

    private HttpServletRequest request;
    private String token;
    private Object user;
    private String requiredRole;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() { return request; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Object getUser() { return user; }
    public void setUser(Object user) { this.user = user; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }
}

