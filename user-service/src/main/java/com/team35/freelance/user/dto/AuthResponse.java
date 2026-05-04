package com.team35.freelance.user.dto;

public class AuthResponse {
    private String token;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String role;

    public AuthResponse(String token,Long expiresIn, Long userId, String email, String role) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }


    public AuthResponse(String token, Long userId, String email, String role) {
        this.token = token;
        this.expiresIn = null;
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}