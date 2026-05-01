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

    private RegisterRequest(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.password = builder.password;
        this.phone = builder.phone;
        this.role = builder.role;
        this.preferences = builder.preferences;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String email;
        private String password;
        private String phone;
        private Role role;
        private Map<String, Object> preferences;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(this);
        }
    }

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