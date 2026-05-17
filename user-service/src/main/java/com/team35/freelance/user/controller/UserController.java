package com.team35.freelance.user.controller;

import com.team35.freelance.user.dto.ActivityFeedResponseDTO;
import com.team35.freelance.user.dto.TopFreelancerDTO;
import com.team35.freelance.user.dto.UserContractSummaryDTO;
import com.team35.freelance.user.dto.UserProfileDTO;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.model.UserSkill;
import com.team35.freelance.user.security.JwtService;
import com.team35.freelance.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public UserProfileDTO getUserById(@PathVariable Long id, HttpServletRequest request) {
        if (request.getHeader("X-INTERNAL-CALL") == null) {
            assertOwnerOrAdmin(id, request);
        }
        return userService.getUserById(id);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user, HttpServletRequest request) {
        assertOwnerOrAdmin(id, request);
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id, HttpServletRequest request) {
        assertOwnerOrAdmin(id, request);
        userService.deleteUser(id);
    }

    @PostMapping("/{userId}/skills")
    public UserSkill addSkill(@PathVariable Long userId, @RequestBody UserSkill skill) {
        return userService.addSkillToUser(userId, skill);
    }

    @GetMapping("/{userId}/skills")
    public List<UserSkill> getSkills(@PathVariable Long userId) {
        return userService.getUserSkills(userId);
    }

    @PutMapping("/skills/{skillId}")
    public UserSkill updateSkill(@PathVariable Long skillId, @RequestBody UserSkill skill) {
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
            @RequestParam(required = false) String role) {
        if (role != null) {
            role = role.toUpperCase();
        }
        return userService.searchUsers(name, email, role);
    }

    @PutMapping("/{id}/preferences")
    public User updatePreferences(@PathVariable Long id, @RequestBody Map<String, Object> preferences) {
        return userService.updateUserPreferences(id, preferences);
    }

    @GetMapping("/preferences/search")
    public List<User> searchByPreference(@RequestParam String key, @RequestParam String value) {
        return userService.searchUsersByPreference(key, value);
    }

    @GetMapping("/{id}/contract-summary")
    public UserContractSummaryDTO getUserContractSummary(@PathVariable Long id, HttpServletRequest request) {
        assertOwnerOrAdmin(id, request);
        return userService.getUserContractSummary(id);
    }

    @PutMapping("/{userId}/skills/{skillId}/primary")
    public User setPrimarySkill(@PathVariable Long userId, @PathVariable Long skillId) {
        return userService.setPrimarySkill(userId, skillId);
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getUserProfile(@PathVariable Long id) {
        return userService.getUserProfile(id);
    }

    @PutMapping("/{id}/deactivate")
    public User deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    @GetMapping("/reports/top-freelancers")
    public List<TopFreelancerDTO> getTopFreelancers(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDate start = startDate == null || startDate.isBlank()
                ? LocalDate.of(1970, 1, 1)
                : LocalDate.parse(startDate);
        LocalDate end = endDate == null || endDate.isBlank()
                ? LocalDate.now().plusDays(1)
                : LocalDate.parse(endDate);
        return userService.getTopFreelancersByEarnings(start, end, limit);
    }

    @GetMapping("/preferences/language")
    public List<User> getUsersByLanguageAndContracts(
            @RequestParam String lang,
            @RequestParam(defaultValue = "0") int minContracts) {
        return userService.findUsersByLanguageAndMinContracts(lang, minContracts);
    }

    @PutMapping("/{id}/role")
    public User updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        return userService.updateUserRole(id, request.get("role"));
    }

    @GetMapping("/{id}/activity")
    public ActivityFeedResponseDTO getUserActivityFeed(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Claims claims = extractClaims(request);
        Long callerUserId = claimAsLong(claims, "userId");
        String callerRole = claims.get("role", String.class);
        return userService.getUserActivityFeed(id, callerUserId, callerRole, page, size);
    }

    private void assertOwnerOrAdmin(Long targetUserId, HttpServletRequest request) {
        Claims claims = extractClaims(request);
        Long callerUserId = claimAsLong(claims, "userId");
        String callerRole = claims.get("role", String.class);

        if ("ADMIN".equalsIgnoreCase(callerRole)) {
            return;
        }
        if (callerUserId != null && callerUserId.equals(targetUserId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    private Claims extractClaims(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return jwtService.extractClaims(authHeader.substring(7));
    }

    private Long claimAsLong(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            return Long.valueOf(s);
        }
        return null;
    }
}
