package com.team35.freelance.user.common.security.chain;

import jakarta.servlet.http.HttpServletRequest;
import com.team35.freelance.user.model.User;

public class AuthContext {

    private HttpServletRequest request;
    private String token;
    private User user;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() { return request; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}