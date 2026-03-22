package com.team35.freelance.user.service;

import com.team35.freelance.user.model.Role;
import com.team35.freelance.user.model.Status;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.model.UserSkill;
import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;

    public UserService(UserRepository userRepository, UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
    }

    // ===================== USER =====================

    public User createUser(User user) {

        if (user.getName() == null || user.getName().isBlank() ||
                user.getEmail() == null || user.getEmail().isBlank() ||
                user.getPassword() == null || user.getPassword().isBlank() ||
                user.getPhone() == null || user.getPhone().isBlank() ||
                user.getRole() == null) {

            throw new RuntimeException("Missing or invalid required fields");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (user.getStatus() == null) {
            user.setStatus(Status.ACTIVE);
        }

        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        User user = getUserById(id);

        if (updatedUser.getName() != null && !updatedUser.getName().isBlank())
            user.setName(updatedUser.getName());

        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            if (!updatedUser.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank())
            user.setPassword(updatedUser.getPassword());

        if (updatedUser.getPhone() != null && !updatedUser.getPhone().isBlank())
            user.setPhone(updatedUser.getPhone());

        if (updatedUser.getRole() != null)
            user.setRole(updatedUser.getRole());

        if (updatedUser.getStatus() != null)
            user.setStatus(updatedUser.getStatus());

        if (updatedUser.getPreferences() != null)
            user.setPreferences(updatedUser.getPreferences());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    // ===================== USER SKILLS =====================

    public UserSkill addSkillToUser(Long userId, UserSkill skill) {
        if (skill.getSkillName() == null || skill.getSkillName().isBlank() ||
                skill.getCategory() == null || skill.getCategory().isBlank() ||
                skill.getYearsOfExperience() == null ||
                skill.getProficiencyLevel() == null) {

            throw new RuntimeException("Invalid skill data");
        }
        User user = getUserById(userId);
        skill.setUser(user);
        return userSkillRepository.save(skill);
    }

    public List<UserSkill> getUserSkills(Long userId) {
        // ensure user exists
        getUserById(userId);
        return userSkillRepository.findByUserId(userId);
    }

    public UserSkill updateUserSkill(Long skillId, UserSkill updatedSkill) {
        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        if (updatedSkill.getSkillName() != null && !updatedSkill.getSkillName().isBlank())
            skill.setSkillName(updatedSkill.getSkillName());

        if (updatedSkill.getCategory() != null && !updatedSkill.getCategory().isBlank())
            skill.setCategory(updatedSkill.getCategory());

        if (updatedSkill.getYearsOfExperience() != null)
            skill.setYearsOfExperience(updatedSkill.getYearsOfExperience());

        if (updatedSkill.getProficiencyLevel() != null)
            skill.setProficiencyLevel(updatedSkill.getProficiencyLevel());

        if (updatedSkill.getIsPrimary() != null)
            skill.setIsPrimary(updatedSkill.getIsPrimary());

        if (updatedSkill.getMetadata() != null)
            skill.setMetadata(updatedSkill.getMetadata());

        return userSkillRepository.save(skill);
    }

    public void deleteUserSkill(Long skillId) {
        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        userSkillRepository.delete(skill);
    }

    public List<User> searchUsers(String name, String email, String role) {
        return userRepository.searchUsers(name, email, role);
    }
}