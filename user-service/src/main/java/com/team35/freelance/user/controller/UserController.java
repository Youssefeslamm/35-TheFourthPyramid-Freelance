package com.team35.freelance.user.controller;

import com.team35.freelance.user.model.User;
import com.team35.freelance.user.model.UserSkill;
import com.team35.freelance.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}