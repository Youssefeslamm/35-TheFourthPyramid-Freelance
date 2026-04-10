package com.team35.freelance.user.controller;

import com.team35.freelance.user.model.Role;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.model.UserSkill;
import com.team35.freelance.user.service.UserService;
import com.team35.freelance.user.dto.TopFreelancerDTO;
import org.springframework.web.bind.annotation.*;
import com.team35.freelance.user.dto.UserProfileDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    // ===================== USER SKILLS =====================

    @PostMapping("/{userId}/skills")
    public UserSkill addSkill(
            @PathVariable Long userId,
            @RequestBody UserSkill skill
    ) {
        return userService.addSkillToUser(userId, skill);
    }

    @GetMapping("/{userId}/skills")
    public List<UserSkill> getSkills(@PathVariable Long userId) {
        return userService.getUserSkills(userId);
    }

    @PutMapping("/skills/{skillId}")
    public UserSkill updateSkill(
            @PathVariable Long skillId,
            @RequestBody UserSkill skill
    ) {
        return userService.updateUserSkill(skillId, skill);
    }

    @DeleteMapping("/skills/{skillId}")
    public void deleteSkill(@PathVariable Long skillId) {
        userService.deleteUserSkill(skillId);
    }

    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role
    ) {
        if (role != null) {
            role = role.toUpperCase();
        }

        return userService.searchUsers(name, email, role);
    }

    @PutMapping("/{id}/preferences")
    public User updatePreferences(
            @PathVariable Long id,
            @RequestBody Map<String, Object> preferences
    ) {
        return userService.updateUserPreferences(id, preferences);
    }

    @GetMapping("/preferences/search")
    public List<User> searchByPreference(
            @RequestParam String key,
            @RequestParam String value
    ) {
        return userService.searchUsersByPreference(key, value);
    }
    @PutMapping("/{userId}/skills/{skillId}/primary")
    public User setPrimarySkill(
            @PathVariable Long userId,
            @PathVariable Long skillId
    ) {
        return userService.setPrimarySkill(userId, skillId);
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getUserProfile(@PathVariable Long id) {
        return userService.getUserProfile(id);
    }

    // ===================== S1-F4: Deactivate User Account =====================

    @PutMapping("/{id}/deactivate")
    public User deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    // ===================== S1-F6: Top Freelancers by Earnings =====================

    @GetMapping("/reports/top-freelancers")
    public List<TopFreelancerDTO> getTopFreelancers(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam int limit) {
        return userService.getTopFreelancersByEarnings(
                LocalDate.parse(startDate), LocalDate.parse(endDate), limit);
    }
}
