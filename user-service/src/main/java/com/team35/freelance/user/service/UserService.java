package com.team35.freelance.user.service;

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

        // Partial update (avoid null overwrite)
        if (updatedUser.getName() != null)
            user.setName(updatedUser.getName());

        if (updatedUser.getEmail() != null)
            user.setEmail(updatedUser.getEmail());

        if (updatedUser.getPassword() != null)
            user.setPassword(updatedUser.getPassword());

        if (updatedUser.getPhone() != null)
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

        // Partial update
        if (updatedSkill.getSkillName() != null)
            skill.setSkillName(updatedSkill.getSkillName());

        if (updatedSkill.getCategory() != null)
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
}