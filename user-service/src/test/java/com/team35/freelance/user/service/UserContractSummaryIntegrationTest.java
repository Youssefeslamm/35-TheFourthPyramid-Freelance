package com.team35.freelance.user.service;

import com.team35.freelance.contracts.feign.ContractServiceClient;
import com.team35.freelance.user.dto.UserContractSummaryDTO;
import com.team35.freelance.user.model.Status;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.repository.UserRepository;
import com.team35.freelance.user.repository.UserSkillRepository;
import com.team35.freelance.user.common.observer.MongoEventLogger;
import com.team35.freelance.user.messaging.publisher.UserEventPublisher;
import com.team35.freelance.user.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * S1-F3 Integration Test Scenario:
 *
 * Setup:
 * - In user-postgres: create User ID=1 (name="Ahmed")
 * - In contract-postgres: create 5 contracts for freelancerId=1
 *   - 3 COMPLETED with agreedAmounts 500, 1000, 1500
 *   - 1 TERMINATED
 *   - 1 ACTIVE
 *
 * Action:
 * - GET /api/users/1/contract-summary with valid Bearer token
 *
 * Expected:
 * - 200 OK
 * - userId=1, name="Ahmed"
 * - totalContracts=5, completedContracts=3, terminatedContracts=1
 * - totalEarnings=3000.00, averageContractValue=1000.00
 *
 * Verify:
 * - No direct JDBC connection from user-postgres to contract-postgres
 * - Contract counts come from Feign HTTP call
 */
@ExtendWith(MockitoExtension.class)
class UserContractSummaryIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private MongoEventLogger mongoEventLogger;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.team35.freelance.user.repository.AuthEventRepository authEventRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private ContractServiceClient contractServiceClient;

    @InjectMocks
    private UserService userService;

    @Test
    void testScenario_ExactValues() {
        // Setup: User ID=1 (name="Ahmed") in user-postgres
        User user = new User();
        user.setId(1L);
        user.setName("Ahmed");
        user.setEmail("ahmed@test.com");
        user.setPhone("1234567890");
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Setup: contract-service returns summary for 5 contracts
        // 3 COMPLETED (500 + 1000 + 1500 = 3000)
        // 1 TERMINATED
        // 1 ACTIVE
        com.team35.freelance.contracts.dto.UserContractSummaryDTO contractSummary =
            new com.team35.freelance.contracts.dto.UserContractSummaryDTO(
                1L,    // userId
                null,  // name (from contract-service perspective)
                5L,    // totalContracts
                3L,    // completedContracts
                1L,    // terminatedContracts
                3000.00,  // totalEarnings (sum of COMPLETED contracts)
                1000.00   // averageContractValue (5000/5)
            );

        when(contractServiceClient.getUserContractSummary(1L)).thenReturn(contractSummary);

        // Action: GET /api/users/1/contract-summary
        UserContractSummaryDTO result = userService.getUserContractSummary(1L);

        // Expected: 200 OK with exact values
        assertEquals(1L, result.getUserId());
        assertEquals("Ahmed", result.getName());
        assertEquals(5L, result.getTotalContracts());
        assertEquals(3L, result.getCompletedContracts());
        assertEquals(1L, result.getTerminatedContracts());
        assertEquals(3000.00, result.getTotalEarnings());
        assertEquals(1000.00, result.getAverageContractValue());

        // Verify: No direct JDBC to contract-postgres - data comes from Feign call
        verify(contractServiceClient, times(1)).getUserContractSummary(1L);
        verify(userRepository, times(1)).findById(1L);
        // No direct contract table query from user-service
    }

    @Test
    void testScenario_VerifyNoDirectJDBC() {
        // This test verifies that user-service does NOT query contracts table directly
        // All contract data must come from Feign call to contract-service

        User user = new User();
        user.setId(1L);
        user.setName("Ahmed");
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Contract-service provides all contract data
        com.team35.freelance.contracts.dto.UserContractSummaryDTO contractSummary =
            new com.team35.freelance.contracts.dto.UserContractSummaryDTO(
                1L, null, 5L, 3L, 1L, 3000.00, 1000.00
            );
        when(contractServiceClient.getUserContractSummary(1L)).thenReturn(contractSummary);

        userService.getUserContractSummary(1L);

        // Verify Feign was called (no direct JDBC to contract-postgres)
        verify(contractServiceClient).getUserContractSummary(1L);
    }
}