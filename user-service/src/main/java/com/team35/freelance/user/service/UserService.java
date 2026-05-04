package com.team35.freelance.user.service;

import com.team35.freelance.user.common.adapter.UserContractSummaryAdapter;
import com.team35.freelance.user.model.Role;
import com.team35.freelance.user.model.Status;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.model.UserSkill;
import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.repository.UserSkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.team35.freelance.user.dto.UserProfileDTO;
import com.team35.freelance.user.dto.UserSkillProfileDTO;
import com.team35.freelance.user.dto.UserContractSummaryDTO;
import com.team35.freelance.user.dto.TopFreelancerDTO;
import com.team35.freelance.user.common.observer.EntityObserver;
import com.team35.freelance.user.common.observer.MongoEventLogger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.team35.freelance.user.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final MongoEventLogger mongoEventLogger;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository,
                       UserSkillRepository userSkillRepository,
                       MongoEventLogger mongoEventLogger,PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.mongoEventLogger = mongoEventLogger;
        this.passwordEncoder = passwordEncoder;

        // register observer
        this.observers.add(mongoEventLogger);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        if (user.getStatus() == null) {
            user.setStatus(Status.ACTIVE);
        }

        User savedUser = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REGISTERED");
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());

        notifyObservers("REGISTERED", payload);

        return savedUser;
    }

    // ✅ CACHE DTO INSTEAD OF ENTITY
    @Cacheable(value = "user-service::user", key = "#id")
    public UserProfileDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return buildUserProfileDTO(user);
    }

    // ✅ NO CACHING - Returns entities which shouldn't be cached
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updatedUser.getName() != null && !updatedUser.getName().isBlank())
            user.setName(updatedUser.getName());

        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            if (!updatedUser.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
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

        User saved = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "USER_UPDATED");
        payload.put("userId", saved.getId());

        notifyObservers("USER_UPDATED", payload);

        return saved;
    }

    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "USER_DELETED");
        payload.put("userId", id);

        notifyObservers("USER_DELETED", payload);
    }

    // ===================== USER SKILLS =====================


    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public UserSkill addSkillToUser(Long userId, UserSkill skill) {
        if (skill.getSkillName() == null || skill.getSkillName().isBlank() ||
                skill.getCategory() == null || skill.getCategory().isBlank() ||
                skill.getYearsOfExperience() == null ||
                skill.getProficiencyLevel() == null) {
            throw new RuntimeException("Invalid skill data");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        skill.setUser(user);

        UserSkill saved = userSkillRepository.save(skill);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "SKILL_ADDED");
        payload.put("userId", userId);
        payload.put("skillId", saved.getId());

        notifyObservers("SKILL_ADDED", payload);

        return saved;
    }

    public List<UserSkill> getUserSkills(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userSkillRepository.findByUserId(userId);
    }

    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
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

        UserSkill saved = userSkillRepository.save(skill);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "SKILL_UPDATED");
        payload.put("skillId", saved.getId());

        notifyObservers("SKILL_UPDATED", payload);

        return saved;
    }

    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public void deleteUserSkill(Long skillId) {
        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        userSkillRepository.delete(skill);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "SKILL_DELETED");
        payload.put("skillId", skillId);

        notifyObservers("SKILL_DELETED", payload);
    }


    @Cacheable(value = "user-service::S1-F1", key = "#name + ':' + #email + ':' + #role")
    public List<User> searchUsers(String name, String email, String role) {
        return userRepository.searchUsers(name, email, role);
    }



    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public User updateUserPreferences(Long id, Map<String, Object> newPreferences) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> existingPreferences = user.getPreferences();

        if (existingPreferences == null) {
            existingPreferences = new HashMap<>();
        }

        if (newPreferences != null) {
            existingPreferences.putAll(newPreferences);
        }

        user.setPreferences(existingPreferences);   // 🔥 MISSING LINE
        User saved = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PREFERENCES_UPDATED");
        payload.put("userId", saved.getId());

        notifyObservers("PREFERENCES_UPDATED", payload);

        return saved;
    }


    @Cacheable(value = "user-service::S1-F5", key = "#key + ':' + #value")
    public List<User> searchUsersByPreference(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be blank");
        }

        return userRepository.findUsersByPreference(key, value);
    }

    @Cacheable(value = "user-service::S1-F3", key = "#userId")
    public UserContractSummaryDTO getUserContractSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        Object[] row = ((Object[]) userRepository.getUserContractSummary(userId)[0]);
        UserContractSummaryAdapter adapter = new UserContractSummaryAdapter();
        return adapter.adapt(row);
    }

    @Transactional
    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public User setPrimarySkill(Long userId, Long skillId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found"
                ));

        if (skill.getUser() == null || !skill.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Skill does not belong to this user"
            );
        }

        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);

        for (UserSkill s : userSkills) {
            s.setIsPrimary(false);
        }

        skill.setIsPrimary(true);
        userSkillRepository.saveAll(userSkills);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PRIMARY_SKILL_SET");
        payload.put("userId", userId);
        payload.put("skillId", skillId);

        notifyObservers("PRIMARY_SKILL_SET", payload);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));
    }

    // ✅ CACHE DTO INSTEAD OF ENTITY
    @Cacheable(value = "user-service::user", key = "#id")
    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));
        return buildUserProfileDTO(user);
    }

    // ✅ HELPER METHOD TO BUILD DTO
    private UserProfileDTO buildUserProfileDTO(User user) {
        List<UserSkill> userSkills = userSkillRepository.findByUserId(user.getId());
        List<UserSkillProfileDTO> skillDTOs = new ArrayList<>();

        for (UserSkill skill : userSkills) {
            UserSkillProfileDTO skillDTO = UserSkillProfileDTO.builder()
                    .skillName(skill.getSkillName())
                    .category(skill.getCategory())
                    .yearsOfExperience(skill.getYearsOfExperience())
                    .proficiencyLevel(skill.getProficiencyLevel())
                    .isPrimary(skill.getIsPrimary())
                    .metadata(skill.getMetadata())
                    .build();
            skillDTOs.add(skillDTO);
        }

        return UserProfileDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(user.getPreferences())
                .skills(skillDTOs)
                .totalSkills(skillDTOs.size())
                .build();
    }

    // ===================== DEACTIVATE USER ACCOUNT =====================

    @Transactional
    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public User deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Long activeContracts = userRepository.countActiveContractsForUser(id);
        if (activeContracts != null && activeContracts > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot deactivate: user has active contracts");
        }

        user.setStatus(Status.DEACTIVATED);
        userRepository.withdrawSubmittedProposalsForUser(id);
        User saved = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "USER_DEACTIVATED");
        payload.put("userId", saved.getId());

        notifyObservers("USER_DEACTIVATED", payload);

        return saved;
    }

    // ===================== TOP FREELANCERS BY EARNINGS =====================

    @Cacheable(value = "user-service::S1-F6", key = "#startDate + ':' + #endDate + ':' + #limit")
    public List<TopFreelancerDTO> getTopFreelancersByEarnings(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
        List<Object[]> rows = userRepository.findTopFreelancersByEarnings(
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59), limit);
        List<TopFreelancerDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(
                    TopFreelancerDTO.builder()
                            .userId(((Number) row[0]).longValue())
                            .name((String) row[1])
                            .totalEarnings(((Number) row[2]).doubleValue())
                            .contractCount(((Number) row[3]).longValue())
                            .build()
            );
        }
        return result;
    }

    // ===================== USERS BY LANGUAGE + MIN COMPLETED CONTRACTS =====================

    @Cacheable(value = "user-service::S1-F9", key = "#lang + ':' + #minContracts")
    public List<User> findUsersByLanguageAndMinContracts(String lang, int minContracts) {
        if (lang == null || lang.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lang must not be blank");
        }
        return userRepository.findUsersByLanguageAndMinContracts(lang, minContracts);
    }


    @CacheEvict(value = {
            "user-service::user",
            "user-service::S1-F1",
            "user-service::S1-F3",
            "user-service::S1-F5",
            "user-service::S1-F6",
            "user-service::S1-F9"
    }, allEntries = true)
    public User updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        Role newRole;
        try {
            newRole = Role.valueOf(role.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid role"
            );
        }

        user.setRole(newRole);
        User saved = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ROLE_CHANGED");
        payload.put("userId", saved.getId());
        payload.put("newRole", newRole.toString());

        notifyObservers("ROLE_CHANGED", payload);

        return saved;
    }


    public User registerUser(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String phone = request.getPhone().trim();

        if (userRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);

        if (request.getRole() != null &&
                (request.getRole() == Role.CLIENT || request.getRole() == Role.FREELANCER)) {
            user.setRole(request.getRole());
        } else {
            user.setRole(Role.CLIENT);
        }

        user.setStatus(Status.ACTIVE);
        user.setPreferences(request.getPreferences());

        User savedUser = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REGISTERED");
        payload.put("userId", savedUser.getId());
        payload.put("email", savedUser.getEmail());

        notifyObservers("REGISTERED", payload);

        return savedUser;
    }
}
