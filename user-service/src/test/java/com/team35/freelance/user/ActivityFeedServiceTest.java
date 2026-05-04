package com.team35.freelance.user;

import com.team35.freelance.user.common.event.AuthEvent;
import com.team35.freelance.user.common.observer.MongoEventLogger;
import com.team35.freelance.user.dto.ActivityFeedResponseDTO;
import com.team35.freelance.user.model.Role;
import com.team35.freelance.user.model.Status;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.repository.AuthEventRepository;
import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.repository.UserSkillRepository;
import com.team35.freelance.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserSkillRepository userSkillRepository;
    @Mock MongoEventLogger mongoEventLogger;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthEventRepository authEventRepository;

    UserService userService;

    User mockUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, userSkillRepository,
                mongoEventLogger, passwordEncoder, authEventRepository
        );

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Basil Ayman");
        mockUser.setEmail("basil@example.com");
        mockUser.setPassword("hashed");
        mockUser.setPhone("+201234567890");
        mockUser.setRole(Role.CLIENT);
        mockUser.setStatus(Status.ACTIVE);
    }

    // ─────────────────────────────────────────────
    // Helper: make an AuthEvent with a userId
    // ─────────────────────────────────────────────
    private AuthEvent makeEvent(Long userId, String action) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("userId", userId);
        params.put("action", action);
        return new AuthEvent(params);
    }

    // ─────────────────────────────────────────────
    // TEST 1: Owner gets their own feed — returns events newest-first
    // ─────────────────────────────────────────────
    @Test
    void ownerCanGetActivityFeed() {
        AuthEvent e1 = makeEvent(1L, "REGISTERED");
        AuthEvent e2 = makeEvent(1L, "LOGGED_IN");
        var page = new PageImpl<>(List.of(e2, e1), PageRequest.of(0, 10), 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(page);

        ActivityFeedResponseDTO result = userService.getUserActivityFeed(1L, 1L, "CLIENT", 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("LOGGED_IN");
        assertThat(result.getContent().get(1).getAction()).isEqualTo("REGISTERED");
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    // ─────────────────────────────────────────────
    // TEST 2: ADMIN can access any user's feed
    // ─────────────────────────────────────────────
    @Test
    void adminCanGetAnyUserFeed() {
        var page = new PageImpl<>(List.of(makeEvent(1L, "REGISTERED")), PageRequest.of(0, 10), 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(page);

        // callerUserId=999 (different user), but role=ADMIN
        ActivityFeedResponseDTO result = userService.getUserActivityFeed(1L, 999L, "ADMIN", 0, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────
    // TEST 3: Different non-admin user gets 403
    // ─────────────────────────────────────────────
    @Test
    void differentUserGetsForbidden() {
        // callerUserId=2, target=1, role=CLIENT → 403
        assertThatThrownBy(() ->
                userService.getUserActivityFeed(1L, 2L, "CLIENT", 0, 10)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ─────────────────────────────────────────────
    // TEST 4: User not found → 404
    // ─────────────────────────────────────────────
    @Test
    void userNotFoundReturns404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.getUserActivityFeed(99L, 99L, "CLIENT", 0, 10)
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ─────────────────────────────────────────────
    // TEST 5: size > 100 is clamped to 100
    // ─────────────────────────────────────────────
    @Test
    void sizeIsCappedAt100() {
        PageImpl<AuthEvent> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(page);

        ActivityFeedResponseDTO result = userService.getUserActivityFeed(1L, 1L, "CLIENT", 0, 500);

        // Verify MongoDB was called with page size 100, not 500
        verify(authEventRepository).findByUserIdOrderByTimestampDesc(
                eq(1L), argThat(p -> p.getPageSize() == 100)
        );
        assertThat(result.getSize()).isEqualTo(100);
    }

    // ─────────────────────────────────────────────
    // TEST 6: Empty feed returns empty list, not null
    // ─────────────────────────────────────────────
    @Test
    void emptyFeedReturnsEmptyList() {
        var emptyPage = new PageImpl<AuthEvent>(List.of(), PageRequest.of(0, 10), 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(emptyPage);

        ActivityFeedResponseDTO result = userService.getUserActivityFeed(1L, 1L, "CLIENT", 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────
    // TEST 7: AuthEvent correctly extracts userId from params
    // ─────────────────────────────────────────────
    @Test
    void authEventExtractsUserId() {
        AuthEvent event = makeEvent(42L, "LOGGED_IN");
        assertThat(event.getUserId()).isEqualTo(42L);
        assertThat(event.getAction()).isEqualTo("LOGGED_IN");
    }

    // ─────────────────────────────────────────────
    // TEST 8: Pagination works correctly
    // ─────────────────────────────────────────────
    @Test
    void paginationParametersAreRespected() {
        AuthEvent event = makeEvent(1L, "ROLE_CHANGED");
        var page = new PageImpl<>(List.of(event), PageRequest.of(1, 1), 3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(authEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any()))
                .thenReturn(page);

        ActivityFeedResponseDTO result = userService.getUserActivityFeed(1L, 1L, "CLIENT", 1, 1);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(1);
    }
}


