package com.team35.freelance.user.dto;

import java.util.List;
import java.util.Map;

public class UserProfileDTO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Map<String, Object> preferences;
    private List<UserSkillProfileDTO> skills;
    private Integer totalSkills;

    public UserProfileDTO() {
    }

    public UserProfileDTO(Long userId,
                          String name,
                          String email,
                          String phone,
                          Map<String, Object> preferences,
                          List<UserSkillProfileDTO> skills,
                          Integer totalSkills) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.skills = skills;
        this.totalSkills = totalSkills;
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

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public List<UserSkillProfileDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<UserSkillProfileDTO> skills) {
        this.skills = skills;
    }

    public Integer getTotalSkills() {
        return totalSkills;
    }

    public void setTotalSkills(Integer totalSkills) {
        this.totalSkills = totalSkills;
    }
}