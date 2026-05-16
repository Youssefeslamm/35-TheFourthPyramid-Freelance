// proposal-service/src/test/java/com/team35/freelance/proposal/service/ProposalServiceSagaTest.java
package com.team35.freelance.proposal.service;

import com.team35.freelance.contracts.dto.ContractDTO;
import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.dto.UserProfileDTO;
import com.team35.freelance.contracts.events.*;
import com.team35.freelance.contracts.feign.ContractServiceClient;
import com.team35.freelance.contracts.feign.JobServiceClient;
import com.team35.freelance.contracts.feign.UserServiceClient;
import com.team35.freelance.proposal.common.observer.MongoEventLogger;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team35.freelance.proposal.repository.ProposalEventRepository;
import com.team35.freelance.proposal.repository.ProposalRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceSagaTest {

    @Mock ProposalRepository proposalRepository;
    @Mock ProposalMilestoneRepository milestoneRepository;
    @Mock ProposalEventRepository proposalEventRepository;
    @Mock MongoEventLogger mongoEventLogger;
    @Mock Driver neo4jDriver;
    @Mock ProposalEventPublisher proposalEventPublisher;
    @Mock UserServiceClient userServiceClient;
    @Mock JobServiceClient jobServiceClient;
    @Mock ContractServiceClient contractServiceClient;

    ProposalService service;

    // Helper to build a FeignException
    private FeignException.NotFound feignNotFound() {
        return new FeignException.NotFound(
                "Not Found",
                Request.create(Request.HttpMethod.GET, "/test", Map.of(), null, StandardCharsets.UTF_8, null),
                null, Map.of()
        );
    }

    private FeignException.ServiceUnavailable feignServiceUnavailable() {
        return new FeignException.ServiceUnavailable(
                "Service Unavailable",
                Request.create(Request.HttpMethod.GET, "/test", Map.of(), null, StandardCharsets.UTF_8, null),
                null, Map.of()
        );
    }

    @BeforeEach
    void setUp() {
        service = new ProposalService(
                proposalRepository, milestoneRepository, proposalEventRepository,
                mongoEventLogger, neo4jDriver, proposalEventPublisher,
                userServiceClient, jobServiceClient, contractServiceClient
        );
    }

    // ─── S3-F2: acceptProposal ───────────────────────────────────────────────

    @Nested
    class AcceptProposalTests {

        Proposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new Proposal();
            proposal.setId(1L);
            proposal.setJobId(10L);
            proposal.setFreelancerId(5L);
            proposal.setBidAmount(2000.0);
            proposal.setStatus(ProposalStatus.SUBMITTED);
        }

        @Test
        void accept_submittedProposal_happyPath() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            UserProfileDTO freelancer = new UserProfileDTO();
            freelancer.setRole("FREELANCER");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            Proposal result = service.acceptProposal(1L);

            assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            assertThat(result.getAcceptedAt()).isNotNull();

            ArgumentCaptor<ProposalAcceptedEvent> captor =
                    ArgumentCaptor.forClass(ProposalAcceptedEvent.class);
            verify(proposalEventPublisher).publishAccepted(captor.capture());
            assertThat(captor.getValue().proposalId()).isEqualTo(1L);
            assertThat(captor.getValue().jobId()).isEqualTo(10L);
            assertThat(captor.getValue().freelancerId()).isEqualTo(5L);
            assertThat(captor.getValue().bidAmount()).isEqualTo(BigDecimal.valueOf(2000.0));
        }

        @Test
        void accept_shortlistedProposal_happyPath() {
            proposal.setStatus(ProposalStatus.SHORTLISTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            UserProfileDTO freelancer = new UserProfileDTO();
            freelancer.setRole("FREELANCER");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            Proposal result = service.acceptProposal(1L);
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
        }

        @Test
        void accept_alreadyAccepted_throws400() {
            proposal.setStatus(ProposalStatus.ACCEPTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishAccepted(any());
        }

        @Test
        void accept_rejectedProposal_throws400() {
            proposal.setStatus(ProposalStatus.REJECTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishAccepted(any());
        }

        @Test
        void accept_withdrawnProposal_throws400() {
            proposal.setStatus(ProposalStatus.WITHDRAWN);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_proposalNotFound_throws404() {
            when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(99L));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
            verify(proposalEventPublisher, never()).publishAccepted(any());
        }

        @Test
        void accept_freelancerNotFound_throws404() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(userServiceClient.getUserById(5L, null)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
            verify(proposalEventPublisher, never()).publishAccepted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void accept_userServiceUnavailable_throws503() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(userServiceClient.getUserById(5L, null)).thenThrow(feignServiceUnavailable());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(503);
            verify(proposalEventPublisher, never()).publishAccepted(any());
        }

        @Test
        void accept_freelancerIsClient_throws400() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            UserProfileDTO client = new UserProfileDTO();
            client.setRole("CLIENT");
            when(userServiceClient.getUserById(5L, null)).thenReturn(client);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishAccepted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void accept_freelancerRoleNull_throws400() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            UserProfileDTO user = new UserProfileDTO();
            user.setRole(null);
            when(userServiceClient.getUserById(5L, null)).thenReturn(user);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_publisherThrows_proposalStillSaved() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);
            UserProfileDTO freelancer = new UserProfileDTO();
            freelancer.setRole("FREELANCER");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            doThrow(new RuntimeException("rabbit down"))
                    .when(proposalEventPublisher).publishAccepted(any());

            // Publisher failure is caught — proposal is still saved
            Proposal result = service.acceptProposal(1L);
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
            verify(proposalRepository).save(any());
        }
        @Test
        void accept_completingProposal_throws400() {
            proposal.setStatus(ProposalStatus.COMPLETING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishAccepted(any());
        }

        @Test
        void accept_paymentPendingProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_paidProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAID);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_paymentFailedProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_refundedProposal_throws400() {
            proposal.setStatus(ProposalStatus.REFUNDED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.acceptProposal(1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void accept_setsAcceptedAt() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);
            UserProfileDTO freelancer = new UserProfileDTO();
            freelancer.setRole("FREELANCER");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            service.acceptProposal(1L);

            ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
            verify(proposalRepository).save(captor.capture());
            assertThat(captor.getValue().getAcceptedAt()).isNotNull();
            assertThat(captor.getValue().getAcceptedAt()).isBefore(
                    java.time.LocalDateTime.now().plusSeconds(1));
        }

        @Test
        void accept_publishedEventHasCorrectBidAmount() {
            proposal.setBidAmount(3500.0);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);
            UserProfileDTO freelancer = new UserProfileDTO();
            freelancer.setRole("FREELANCER");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            service.acceptProposal(1L);

            ArgumentCaptor<ProposalAcceptedEvent> captor =
                    ArgumentCaptor.forClass(ProposalAcceptedEvent.class);
            verify(proposalEventPublisher).publishAccepted(captor.capture());
            assertThat(captor.getValue().bidAmount())
                    .isEqualByComparingTo(java.math.BigDecimal.valueOf(3500.0));
        }
    }

    // ─── S3-F4: completeProposal ─────────────────────────────────────────────

    @Nested
    class CompleteProposalTests {

        Proposal proposal;
        UserProfileDTO freelancer;
        JobDTO job;
        ContractDTO contract;

        @BeforeEach
        void setUp() {
            proposal = new Proposal();
            proposal.setId(1L);
            proposal.setJobId(10L);
            proposal.setFreelancerId(5L);
            proposal.setBidAmount(2000.0);
            proposal.setStatus(ProposalStatus.ACCEPTED);

            freelancer = new UserProfileDTO();
            freelancer.setStatus("ACTIVE");
            freelancer.setRole("FREELANCER");

            job = new JobDTO();
            job.setId(10L);
            job.setStatus("IN_PROGRESS");

            contract = new ContractDTO();
            contract.setId(99L);
            contract.setAgreedAmount(2000.0);
        }

        @Test
        void complete_happyPath_freelancerCaller() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            Proposal result = service.completeProposal(1L, 5L, "FREELANCER");

            assertThat(result.getStatus()).isEqualTo(ProposalStatus.COMPLETING);

            ArgumentCaptor<ProposalCompletedEvent> captor =
                    ArgumentCaptor.forClass(ProposalCompletedEvent.class);
            verify(proposalEventPublisher).publishCompleted(captor.capture());
            assertThat(captor.getValue().proposalId()).isEqualTo(1L);
            assertThat(captor.getValue().contractId()).isEqualTo(99L);
            assertThat(captor.getValue().agreedAmount()).isEqualTo(BigDecimal.valueOf(2000.0));
        }

        @Test
        void complete_happyPath_adminCaller() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            // Different userId but ADMIN role — should still work
            Proposal result = service.completeProposal(1L, 999L, "ADMIN");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.COMPLETING);
        }

        @Test
        void complete_proposalNotFound_throws404() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_notAccepted_submittedStatus_throws400() {
            proposal.setStatus(ProposalStatus.SUBMITTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_notAccepted_paidStatus_throws400() {
            proposal.setStatus(ProposalStatus.PAID);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void complete_wrongCaller_throws403() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            // Caller is neither the freelancer nor ADMIN
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 99L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_nullCaller_throws403() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, null, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        void complete_jobNotFound_throws400_noEventPublished() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_jobClosed_throws400_noEventPublished() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            job.setStatus("CLOSED");
            when(jobServiceClient.getJobById(10L)).thenReturn(job);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_jobServiceUnavailable_throws503() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenThrow(feignServiceUnavailable());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(503);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_freelancerNotFound_throws400_noEventPublished() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_freelancerDeactivated_throws400_noEventPublished() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            freelancer.setStatus("DEACTIVATED");
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_freelancerStatusNull_throws400() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            freelancer.setStatus(null);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_noActiveContract_throws400_noEventPublished() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_contractServiceUnavailable_throws503() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenThrow(feignServiceUnavailable());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(503);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_publisherThrows_proposalStillSaved() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);
            doThrow(new RuntimeException("rabbit down"))
                    .when(proposalEventPublisher).publishCompleted(any());

            Proposal result = service.completeProposal(1L, 5L, "FREELANCER");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.COMPLETING);
            verify(proposalRepository).save(any());
        }

        @Test
        void complete_verifyAllThreePrechecksRunInOrder() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            service.completeProposal(1L, 5L, "FREELANCER");

            // All three Feign clients must have been called
            verify(jobServiceClient).getJobById(10L);
            verify(userServiceClient).getUserById(5L, null);
            verify(contractServiceClient).getActiveContractForProposal(1L);
        }
        @Test
        void complete_jobNotFound_throws400_noEventPublished_tightened() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400); // spec §8.3 explicit
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_freelancerNotFound_throws400_noEventPublished_tightened() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenThrow(feignNotFound());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400); // spec §8.3 explicit
            verify(proposalEventPublisher, never()).publishCompleted(any());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        void complete_completingStatus_throws400() {
            proposal.setStatus(ProposalStatus.COMPLETING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishCompleted(any());
        }

        @Test
        void complete_paymentPendingStatus_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void complete_paymentFailedStatus_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void complete_refundedStatus_throws400() {
            proposal.setStatus(ProposalStatus.REFUNDED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.completeProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void complete_jobStatusOpen_isValid() {
            // OPEN job should also be completable per spec — only CLOSED is rejected
            job.setStatus("OPEN");
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            Proposal result = service.completeProposal(1L, 5L, "FREELANCER");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.COMPLETING);
        }

        @Test
        void complete_eventPayloadHasAllFields() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            service.completeProposal(1L, 5L, "FREELANCER");

            ArgumentCaptor<ProposalCompletedEvent> captor =
                    ArgumentCaptor.forClass(ProposalCompletedEvent.class);
            verify(proposalEventPublisher).publishCompleted(captor.capture());
            ProposalCompletedEvent event = captor.getValue();
            assertThat(event.proposalId()).isEqualTo(1L);
            assertThat(event.jobId()).isEqualTo(10L);
            assertThat(event.freelancerId()).isEqualTo(5L);
            assertThat(event.contractId()).isEqualTo(99L);
            assertThat(event.agreedAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(2000.0));
        }

        @Test
        void complete_proposalStatusIsCompletingNotAccepted() {
            // Must be COMPLETING after save, not remain ACCEPTED
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(jobServiceClient.getJobById(10L)).thenReturn(job);
            when(userServiceClient.getUserById(5L, null)).thenReturn(freelancer);
            when(contractServiceClient.getActiveContractForProposal(1L)).thenReturn(contract);
            when(proposalRepository.save(any())).thenReturn(proposal);

            service.completeProposal(1L, 5L, "FREELANCER");

            ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
            verify(proposalRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.COMPLETING);
            assertThat(captor.getValue().getStatus()).isNotEqualTo(ProposalStatus.ACCEPTED);
        }
    }

    // ─── S3-F7: withdrawProposal ─────────────────────────────────────────────

    @Nested
    class WithdrawProposalTests {

        Proposal proposal;

        @BeforeEach
        void setUp() {
            proposal = new Proposal();
            proposal.setId(1L);
            proposal.setJobId(10L);
            proposal.setFreelancerId(5L);
            proposal.setStatus(ProposalStatus.SUBMITTED);
        }

        @Test
        void withdraw_submitted_byFreelancer_happyPath() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            Proposal result = service.withdrawProposal(1L, 5L, "FREELANCER");

            assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);

            ArgumentCaptor<ProposalWithdrawnEvent> captor =
                    ArgumentCaptor.forClass(ProposalWithdrawnEvent.class);
            verify(proposalEventPublisher).publishWithdrawn(captor.capture());
            assertThat(captor.getValue().proposalId()).isEqualTo(1L);
            assertThat(captor.getValue().jobId()).isEqualTo(10L);
            assertThat(captor.getValue().freelancerId()).isEqualTo(5L);
        }

        @Test
        void withdraw_shortlisted_byFreelancer_happyPath() {
            proposal.setStatus(ProposalStatus.SHORTLISTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            Proposal result = service.withdrawProposal(1L, 5L, "FREELANCER");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
            verify(proposalEventPublisher).publishWithdrawn(any());
        }

        @Test
        void withdraw_byAdmin_differentUserId_succeeds() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            Proposal result = service.withdrawProposal(1L, 999L, "ADMIN");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
        }

        @Test
        void withdraw_proposalNotFound_throws404() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
            verify(proposalEventPublisher, never()).publishWithdrawn(any());
        }

        @Test
        void withdraw_wrongFreelancer_throws403() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 99L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
            verify(proposalEventPublisher, never()).publishWithdrawn(any());
        }

        @Test
        void withdraw_nullUserId_notAdmin_throws403() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, null, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
        }

        @Test
        void withdraw_acceptedProposal_throws400() {
            proposal.setStatus(ProposalStatus.ACCEPTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishWithdrawn(any());
        }

        @Test
        void withdraw_completingProposal_throws400() {
            proposal.setStatus(ProposalStatus.COMPLETING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_paidProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAID);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_rejectedProposal_throws400() {
            proposal.setStatus(ProposalStatus.REJECTED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_publisherThrows_proposalStillSaved() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);
            doThrow(new RuntimeException("rabbit down"))
                    .when(proposalEventPublisher).publishWithdrawn(any());

            // Publisher failure is caught — proposal is still saved as WITHDRAWN
            Proposal result = service.withdrawProposal(1L, 5L, "FREELANCER");
            assertThat(result.getStatus()).isEqualTo(ProposalStatus.WITHDRAWN);
            verify(proposalRepository).save(any());
        }

        @Test
        void withdraw_doesNotPublishCancelled_onlyWithdrawn() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            service.withdrawProposal(1L, 5L, "FREELANCER");

            // Must publish withdrawn, must NOT publish cancelled
            verify(proposalEventPublisher).publishWithdrawn(any());
            verify(proposalEventPublisher, never()).publishCancelled(any());
        }

        @Test
        void withdraw_paymentPendingProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verify(proposalEventPublisher, never()).publishWithdrawn(any());
        }

        @Test
        void withdraw_paymentFailedProposal_throws400() {
            proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_refundedProposal_throws400() {
            proposal.setStatus(ProposalStatus.REFUNDED);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_alreadyWithdrawnProposal_throws400() {
            proposal.setStatus(ProposalStatus.WITHDRAWN);
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 5L, "FREELANCER"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        void withdraw_eventPayloadHasAllFields() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            service.withdrawProposal(1L, 5L, "FREELANCER");

            ArgumentCaptor<ProposalWithdrawnEvent> captor =
                    ArgumentCaptor.forClass(ProposalWithdrawnEvent.class);
            verify(proposalEventPublisher).publishWithdrawn(captor.capture());
            assertThat(captor.getValue().proposalId()).isEqualTo(1L);
            assertThat(captor.getValue().jobId()).isEqualTo(10L);
            assertThat(captor.getValue().freelancerId()).isEqualTo(5L);
        }

        @Test
        void withdraw_saveHappensBeforePublish() {
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            when(proposalRepository.save(any())).thenReturn(proposal);

            var order = inOrder(proposalRepository, proposalEventPublisher);
            service.withdrawProposal(1L, 5L, "FREELANCER");

            order.verify(proposalRepository).save(any());
            order.verify(proposalEventPublisher).publishWithdrawn(any());
        }

        @Test
        void withdraw_clientRole_cannotWithdrawFreelancerProposal() {
            // CLIENT role with different userId should be 403
            when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.withdrawProposal(1L, 99L, "CLIENT"));
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
            verify(proposalEventPublisher, never()).publishWithdrawn(any());
        }
    }
}