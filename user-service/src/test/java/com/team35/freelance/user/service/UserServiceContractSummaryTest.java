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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceContractSummaryTest {

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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                userSkillRepository,
                mongoEventLogger,
                passwordEncoder,
                authEventRepository,
                jwtService,
                userEventPublisher,
                contractServiceClient
        );
    }

    @Test
    void getUserContractSummary_Success() {
        User user = new User();
        user.setId(1L);
        user.setName("Ahmed");
        user.setEmail("ahmed@test.com");
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        com.team35.freelance.contracts.dto.UserContractSummaryDTO contractSummary =
            new com.team35.freelance.contracts.dto.UserContractSummaryDTO(
                1L, null, 5L, 3L, 1L, 3000.00, 1000.00
            );
        when(contractServiceClient.getUserContractSummary(1L)).thenReturn(contractSummary);

        UserContractSummaryDTO result = userService.getUserContractSummary(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Ahmed", result.getName());
        assertEquals(5L, result.getTotalContracts());
        assertEquals(3L, result.getCompletedContracts());
        assertEquals(1L, result.getTerminatedContracts());
        assertEquals(3000.00, result.getTotalEarnings());
        assertEquals(1000.00, result.getAverageContractValue());

        verify(userRepository).findById(1L);
        verify(contractServiceClient).getUserContractSummary(1L);
    }

    @Test
    void getUserContractSummary_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> userService.getUserContractSummary(1L));

        verify(userRepository).findById(1L);
        verify(contractServiceClient, never()).getUserContractSummary(anyLong());
    }

    @Test
    void getUserContractSummary_ContractServiceUnavailable() {
        User user = new User();
        user.setId(1L);
        user.setName("Ahmed");
        user.setEmail("ahmed@test.com");
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(contractServiceClient.getUserContractSummary(1L))
                .thenThrow(new RuntimeException("Service unavailable"));

        UserContractSummaryDTO result = userService.getUserContractSummary(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Ahmed", result.getName());
        assertEquals(0L, result.getTotalContracts());
        assertEquals(0L, result.getCompletedContracts());
        assertEquals(0L, result.getTerminatedContracts());
        assertEquals(0.0, result.getTotalEarnings());
        assertEquals(0.0, result.getAverageContractValue());
    }

    @Test
    void getUserContractSummary_CalculatesAverageCorrectly() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        com.team35.freelance.contracts.dto.UserContractSummaryDTO contractSummary =
            new com.team35.freelance.contracts.dto.UserContractSummaryDTO(
                1L, null, 4L, 2L, 1L, 4000.00, 1000.00
            );
        when(contractServiceClient.getUserContractSummary(1L)).thenReturn(contractSummary);

        UserContractSummaryDTO result = userService.getUserContractSummary(1L);

        assertEquals(4L, result.getTotalContracts());
        assertEquals(4000.00, result.getTotalEarnings());
        assertEquals(1000.00, result.getAverageContractValue());
    }
}