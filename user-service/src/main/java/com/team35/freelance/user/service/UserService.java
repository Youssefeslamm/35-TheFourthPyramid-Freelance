package com.team35.freelance.user.service;

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


    public User createUser(User user) {
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

        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        user.setPassword(updatedUser.getPassword());
        user.setPhone(updatedUser.getPhone());
        user.setRole(updatedUser.getRole());
        user.setStatus(updatedUser.getStatus());
        user.setPreferences(updatedUser.getPreferences());

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }


    public UserSkill addSkillToUser(Long userId, UserSkill skill) {
        User user = getUserById(userId);
        skill.setUser(user);
        return userSkillRepository.save(skill);
    }

    public List<UserSkill> getUserSkills(Long userId) {
        return userSkillRepository.findByUserId(userId);
    }

    public UserSkill updateUserSkill(Long skillId, UserSkill updatedSkill) {
        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        skill.setSkillName(updatedSkill.getSkillName());
        skill.setCategory(updatedSkill.getCategory());
        skill.setYearsOfExperience(updatedSkill.getYearsOfExperience());
        skill.setProficiencyLevel(updatedSkill.getProficiencyLevel());
        skill.setIsPrimary(updatedSkill.getIsPrimary());
        skill.setMetadata(updatedSkill.getMetadata());

        return userSkillRepository.save(skill);
    }

    public void deleteUserSkill(Long skillId) {
        userSkillRepository.deleteById(skillId);
    }
}