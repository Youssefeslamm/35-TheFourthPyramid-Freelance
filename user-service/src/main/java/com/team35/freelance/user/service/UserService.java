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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final MongoEventLogger mongoEventLogger;


    public UserService(UserRepository userRepository,
                       UserSkillRepository userSkillRepository,
                       MongoEventLogger mongoEventLogger) {

        this.userRepository = userRepository;
        this.userSkillRepository = userSkillRepository;
        this.mongoEventLogger = mongoEventLogger;

        // register observer
        this.observers.add(mongoEventLogger);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");        }

        if (user.getStatus() == null) {
            user.setStatus(Status.ACTIVE);
        }

        User savedUser = userRepository.save(user);

        // 🔥 REQUIRED FOR CC-4 (Observer trigger)
        notifyObservers("REGISTERED", savedUser);

        return savedUser;    }

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");            }
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

    public User updateUserPreferences(Long id, Map<String, Object> newPreferences) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> existingPreferences = user.getPreferences();

        if (existingPreferences == null) {
            existingPreferences = new HashMap<>();
        }

        if (newPreferences != null) {
            existingPreferences.putAll(newPreferences); // 🔥 MERGE
        }

        user.setPreferences(existingPreferences);

        return userRepository.save(user);
    }

    public List<User> searchUsersByPreference(String key, String value) {

        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be blank");
        }

        return userRepository.findUsersByPreference(key, value);
    }

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
    public User setPrimarySkill(Long userId, Long skillId) {

        // 1. Find user (404)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        // 2. Find skill (404)
        UserSkill skill = userSkillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Skill not found"
                ));

        // 3. Verify ownership (400)
        if (skill.getUser() == null || !skill.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Skill does not belong to this user"
            );
        }

        // 4. Reset all user skills
        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);

        for (UserSkill s : userSkills) {
            s.setIsPrimary(false);
        }

        // 5. Set target skill as primary
        skill.setIsPrimary(true);

        // 6. Save changes
        userSkillRepository.saveAll(userSkills);

        // 7. Return updated user
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));
    }

    public UserProfileDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        List<UserSkill> userSkills = userSkillRepository.findByUserId(id);
        List<UserSkillProfileDTO> skillDTOs = new ArrayList<>();

        for (UserSkill skill : userSkills) {
            UserSkillProfileDTO skillDTO = new UserSkillProfileDTO(
                    skill.getSkillName(),
                    skill.getCategory(),
                    skill.getYearsOfExperience(),
                    skill.getProficiencyLevel(),
                    skill.getIsPrimary(),
                    skill.getMetadata()
            );
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

    // ===================== S1-F4: Deactivate User Account =====================

    @Transactional
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
        return userRepository.save(user);
    }

    // ===================== S1-F6: Top Freelancers by Earnings =====================

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


    // ===================== S1-F9: Users by Language + Min Completed Contracts =====================

    public List<User> findUsersByLanguageAndMinContracts(String lang, int minContracts) {
        if (lang == null || lang.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lang must not be blank");
        }
        return userRepository.findUsersByLanguageAndMinContracts(lang, minContracts);
    }



    public User updateUserRole(Long id, String role) {

        // 1. Find user (404)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        // 2. Validate role (400)
        Role newRole;
        try {
            newRole = Role.valueOf(role.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid role"
            );
        }

        // 3. Update role
        Role oldRole = user.getRole();
        user.setRole(newRole);

        User savedUser = userRepository.save(user);

        // 🔥 (IMPORTANT FOR CC-2)
        // Later you will add:
        // - Mongo event: ROLE_CHANGED
        // - Redis invalidation

        return savedUser;
    }

}
