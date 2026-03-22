package com.team35.freelance.user.repository;

import com.team35.freelance.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}