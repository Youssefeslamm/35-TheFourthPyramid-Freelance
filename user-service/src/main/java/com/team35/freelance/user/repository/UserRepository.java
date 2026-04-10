package com.team35.freelance.user.repository;

import com.team35.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    @Query(value = """
    SELECT * FROM users u
    WHERE (:name IS NULL OR u.name ILIKE CONCAT('%', :name, '%'))
      AND (:email IS NULL OR u.email ILIKE CONCAT('%', :email, '%'))
      AND (:role IS NULL OR u.role = CAST(:role AS user_role_enum))
""", nativeQuery = true)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role
    );

    @Query(value = """
    SELECT * FROM users u
    WHERE u.preferences ->> :key = :value
""", nativeQuery = true)
    List<User> findUsersByPreference(
            @Param("key") String key,
            @Param("value") String value
    );

    // ===================== S1-F4: Deactivate User =====================

    @Query(value = """
    SELECT COUNT(*) FROM contracts
    WHERE (freelancer_id = :userId OR client_id = :userId)
      AND status = 'ACTIVE'
""", nativeQuery = true)
    Long countActiveContractsForUser(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE proposals SET status = 'WITHDRAWN'
    WHERE freelancer_id = :userId AND status = 'SUBMITTED'
""", nativeQuery = true)
    int withdrawSubmittedProposalsForUser(@Param("userId") Long userId);
}
