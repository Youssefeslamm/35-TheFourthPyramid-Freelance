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
import com.team35.freelance.contracts.observability.RabbitObservability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
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

    @Transactional
    @RabbitListener(queues = "proposal.accepted.contract.queue")
    public void handleProposalAccepted(ProposalAcceptedEvent event, Message message) {
        final String routingKey = "proposal.accepted";
        RabbitObservability.applyInboundMdc(message);
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());
        try {
            RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());

            boolean alreadyExists = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .isPresent();

            if (alreadyExists) {
                log.warn("Skipping proposal.accepted because ACTIVE contract already exists proposalId={}",
                        event.proposalId());
                RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
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
                    "sourceEvent", routingKey,
                    "createdBy", "contract-service-event-consumer"
            ));

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            contractEventPublisher.publishCreated(new ContractCreatedEvent(
                    saved.getId(),
                    saved.getProposalId(),
                    saved.getJobId(),
                    saved.getFreelancerId(),
                    BigDecimal.valueOf(saved.getAgreedAmount())
            ));

            RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw e;
        } finally {
            RabbitObservability.clearConsumerMdc("proposalId", "jobId", "userId", "contractId");
        }
    }

    @Transactional
    @RabbitListener(queues = "proposal.completed.contract.queue")
    public void handleProposalCompleted(ProposalCompletedEvent event, Message message) {
        final String routingKey = "proposal.completed";
        RabbitObservability.applyInboundMdc(message);
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());
        if (event.contractId() != null) {
            MDC.put("contractId", String.valueOf(event.contractId()));
        }
        try {
            RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());

            Contract contract = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .orElse(null);

            if (contract == null) {
                log.warn("Skipping proposal.completed because no ACTIVE contract found proposalId={}",
                        event.proposalId());
                RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                return;
            }

            ContractStatus oldStatus = contract.getStatus();
            contract.setStatus(ContractStatus.COMPLETED);
            contract.setEndDate(LocalDateTime.now());

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            contractEventPublisher.publishStatusChanged(new ContractStatusChangedEvent(
                    saved.getId(),
                    oldStatus.name(),
                    ContractStatus.COMPLETED.name()
            ));

            RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw e;
        } finally {
            RabbitObservability.clearConsumerMdc("proposalId", "jobId", "userId", "contractId");
        }
    }

    @Transactional
    @RabbitListener(queues = "proposal.cancelled.contract.queue")
    public void handleProposalCancelled(ProposalCancelledEvent event, Message message) {
        final String routingKey = "proposal.cancelled";
        RabbitObservability.applyInboundMdc(message);
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId());
        try {
            RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());

            Contract contract = contractRepository
                    .findFirstByProposalIdAndStatusOrderByCreatedAtDesc(event.proposalId(), ContractStatus.ACTIVE)
                    .orElse(null);

            if (contract == null) {
                log.warn("Skipping proposal.cancelled because no ACTIVE contract found proposalId={}",
                        event.proposalId());
                RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                return;
            }

            contract.setStatus(ContractStatus.TERMINATED);
            contract.setEndDate(LocalDateTime.now());

            Contract saved = contractRepository.save(contract);
            MDC.put("contractId", String.valueOf(saved.getId()));

            contractEventPublisher.publishCancelled(new ContractCancelledEvent(
                    saved.getId(),
                    saved.getProposalId()
            ));

            RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw e;
        } finally {
            RabbitObservability.clearConsumerMdc("proposalId", "jobId", "userId", "contractId");
        }
    }

    @RabbitListener(queues = "user.deactivated.contract.queue")
    public void handleUserDeactivated(UserDeactivatedEvent event, Message message) {
        final String routingKey = "user.deactivated";
        RabbitObservability.applyInboundMdc(message);
        MDC.put("userId", String.valueOf(event.userId()));
        try {
            RabbitObservability.logConsuming(routingKey, "userId", event.userId());
            log.info("Audit logged user.deactivated userId={}", event.userId());
            RabbitObservability.logProcessed(routingKey, "userId", event.userId());
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw e;
        } finally {
            RabbitObservability.clearConsumerMdc("userId");
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
