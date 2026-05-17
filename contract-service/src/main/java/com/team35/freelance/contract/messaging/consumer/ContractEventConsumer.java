package com.team35.freelance.contract.messaging.consumer;

import com.team35.freelance.contract.feign.OutboundFeignClients;
import com.team35.freelance.contract.messaging.publisher.ContractEventPublisher;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.repository.ContractRepository;
import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.events.ContractCancelledEvent;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.UserDeactivatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ContractEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContractEventConsumer.class);

    private final ContractRepository contractRepository;
    private final ContractEventPublisher contractEventPublisher;
    private final OutboundFeignClients outboundFeignClients;

    public ContractEventConsumer(ContractRepository contractRepository,
                                 ContractEventPublisher contractEventPublisher,
                                 OutboundFeignClients outboundFeignClients) {
        this.contractRepository = contractRepository;
        this.contractEventPublisher = contractEventPublisher;
        this.outboundFeignClients = outboundFeignClients;
    }

    /**
     * proposal.accepted -> create ACTIVE contract -> publish contract.created
     */
    @Transactional
    @RabbitListener(queues = "proposal.accepted.contract.queue")
    public void handleProposalAccepted(ProposalAcceptedEvent event) {
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());

        log.info("Received proposal.accepted event proposalId={} jobId={} freelancerId={}",
                event.proposalId(), event.jobId(), event.freelancerId());

        try {
            boolean alreadyExists = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .isPresent();

            if (alreadyExists) {
                log.warn("Skipping proposal.accepted because ACTIVE contract already exists proposalId={}",
                        event.proposalId());
                return;
            }

            Long clientId = outboundFeignClients.tryFetchJobById(event.jobId())
                    .map(JobDTO::getClientId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot create contract because job-service did not return job/client for jobId=" + event.jobId()
                    ));

            Contract contract = new Contract();
            contract.setProposalId(event.proposalId());
            contract.setJobId(event.jobId());
            contract.setFreelancerId(event.freelancerId());
            contract.setClientId(clientId);
            contract.setAgreedAmount(toDouble(event.bidAmount()));
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setStartDate(LocalDateTime.now());
            contract.setMetadata(Map.of(
                    "sourceEvent", "proposal.accepted",
                    "createdBy", "contract-service-event-consumer"
            ));

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            log.info("Created ACTIVE contract contractId={} proposalId={}", saved.getId(), saved.getProposalId());

            contractEventPublisher.publishCreated(new ContractCreatedEvent(
                    saved.getId(),
                    saved.getProposalId(),
                    saved.getJobId(),
                    saved.getFreelancerId(),
                    BigDecimal.valueOf(saved.getAgreedAmount())
            ));

            log.info("Published contract.created event contractId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to process proposal.accepted proposalId={}", event.proposalId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * proposal.completed -> mark ACTIVE contract COMPLETED -> publish contract.status-changed
     */
    @Transactional
    @RabbitListener(queues = "proposal.completed.contract.queue")
    public void handleProposalCompleted(ProposalCompletedEvent event) {
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());
        if (event.contractId() != null) {
            MDC.put("contractId", String.valueOf(event.contractId()));
        }

        log.info("Received proposal.completed event proposalId={} contractId={}",
                event.proposalId(), event.contractId());

        try {
            Contract contract = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .orElse(null);

            if (contract == null) {
                log.warn("Skipping proposal.completed because no ACTIVE contract found proposalId={}",
                        event.proposalId());
                return;
            }

            ContractStatus oldStatus = contract.getStatus();

            contract.setStatus(ContractStatus.COMPLETED);
            contract.setEndDate(LocalDateTime.now());

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            log.info("Marked contract COMPLETED contractId={} proposalId={}",
                    saved.getId(), saved.getProposalId());

            contractEventPublisher.publishStatusChanged(new ContractStatusChangedEvent(
                    saved.getId(),
                    oldStatus.name(),
                    ContractStatus.COMPLETED.name()
            ));

            log.info("Published contract.status-changed event contractId={} oldStatus={} newStatus={}",
                    saved.getId(), oldStatus.name(), ContractStatus.COMPLETED.name());
        } catch (Exception e) {
            log.error("Failed to process proposal.completed proposalId={}", event.proposalId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * proposal.cancelled -> mark ACTIVE contract TERMINATED -> publish contract.cancelled
     */
    @Transactional
    @RabbitListener(queues = "proposal.cancelled.contract.queue")
    public void handleProposalCancelled(ProposalCancelledEvent event) {
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());

        log.info("Received proposal.cancelled event proposalId={} reason={}",
                event.proposalId(), event.reason());

        try {
            Contract contract = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .orElse(null);

            if (contract == null) {
                log.warn("Skipping proposal.cancelled because no ACTIVE contract found proposalId={}",
                        event.proposalId());
                return;
            }

            contract.setStatus(ContractStatus.TERMINATED);
            contract.setEndDate(LocalDateTime.now());

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            log.info("Marked contract TERMINATED contractId={} proposalId={}",
                    saved.getId(), saved.getProposalId());

            contractEventPublisher.publishCancelled(new ContractCancelledEvent(
                    saved.getId(),
                    saved.getProposalId()
            ));

            log.info("Published contract.cancelled event contractId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to process proposal.cancelled proposalId={}", event.proposalId(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * user.deactivated -> audit only.
     * No hard state change here to avoid touching Ebrahim's read APIs or other members' business logic.
     */
    @RabbitListener(queues = "user.deactivated.contract.queue")
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        MDC.put("userId", String.valueOf(event.userId()));
        try {
            log.info("Received user.deactivated event userId={} - audit logged by contract-service",
                    event.userId());
        } finally {
            MDC.clear();
        }
    }

    private void putProposalMdc(Long proposalId, Long jobId, Long freelancerId) {
        if (proposalId != null) {
            MDC.put("proposalId", String.valueOf(proposalId));
        }
        if (jobId != null) {
            MDC.put("jobId", String.valueOf(jobId));
        }
        if (freelancerId != null) {
            MDC.put("userId", String.valueOf(freelancerId));
        }
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
