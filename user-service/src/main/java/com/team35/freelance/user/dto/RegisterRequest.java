package com.team35.freelance.user.dto;

import com.team35.freelance.user.model.Role;
import java.util.Map;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    private Role role;
    private Map<String, Object> preferences;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }
}