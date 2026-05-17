package com.team35.freelance.contracts.dto;

import java.util.Map;

public class UserProfileDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private Map<String, Object> preferences;

    public UserProfileDTO() {
    }

    public UserProfileDTO(Long userId, String name, String email, String phone, Map<String, Object> preferences) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status;  }

    public void setStatus(String status) { this.status = status;}

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }
}

