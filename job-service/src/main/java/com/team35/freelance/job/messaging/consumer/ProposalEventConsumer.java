package com.team35.freelance.job.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.JobStatusChangedEvent;
import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.ProposalWithdrawnEvent;
import com.team35.freelance.contracts.feign.ProposalServiceClient;
import com.team35.freelance.job.config.RabbitConsumerTopologyConfig;
import com.team35.freelance.job.messaging.publisher.JobEventPublisher;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobStatus;
import com.team35.freelance.job.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import feign.FeignException;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProposalEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProposalEventConsumer.class);

    private final JobRepository jobRepository;
    private final JobEventPublisher jobEventPublisher;
    private final ObjectMapper objectMapper;
    private final ProposalServiceClient proposalServiceClient;

    public ProposalEventConsumer(JobRepository jobRepository,
                                 JobEventPublisher jobEventPublisher,
                                 ObjectMapper objectMapper,
                                 ProposalServiceClient proposalServiceClient) {
        this.jobRepository = jobRepository;
        this.jobEventPublisher = jobEventPublisher;
        this.objectMapper = objectMapper;
        this.proposalServiceClient = proposalServiceClient;
    }

    @RabbitListener(queues = RabbitConsumerTopologyConfig.JOB_PROPOSAL_SAGA_QUEUE)
    @Transactional
    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public void handleProposalEvent(Message message) throws Exception {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        try {
            MDC.put("routingKey", routingKey);

            Object correlationId = message.getMessageProperties().getHeaders().get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId.toString());
            }

            log.info("Consuming proposal event with routingKey={}", routingKey);

            if (RabbitConsumerTopologyConfig.PROPOSAL_ACCEPTED_ROUTING_KEY.equals(routingKey)) {
                ProposalAcceptedEvent event = objectMapper.readValue(message.getBody(), ProposalAcceptedEvent.class);
                MDC.put("proposalId", String.valueOf(event.proposalId()));
                MDC.put("jobId", String.valueOf(event.jobId()));
                onProposalAccepted(event);
                return;
            }

            if (RabbitConsumerTopologyConfig.PROPOSAL_COMPLETED_ROUTING_KEY.equals(routingKey)) {
                ProposalCompletedEvent event = objectMapper.readValue(message.getBody(), ProposalCompletedEvent.class);
                MDC.put("proposalId", String.valueOf(event.proposalId()));
                MDC.put("jobId", String.valueOf(event.jobId()));
                onProposalCompleted(event);
                return;
            }

            if (RabbitConsumerTopologyConfig.PROPOSAL_CANCELLED_ROUTING_KEY.equals(routingKey)) {
                ProposalCancelledEvent event = objectMapper.readValue(message.getBody(), ProposalCancelledEvent.class);
                MDC.put("proposalId", String.valueOf(event.proposalId()));
                MDC.put("jobId", String.valueOf(event.jobId()));
                onProposalCancelled(event);
                return;
            }

            if (RabbitConsumerTopologyConfig.PROPOSAL_WITHDRAWN_ROUTING_KEY.equals(routingKey)) {
                ProposalWithdrawnEvent event = objectMapper.readValue(message.getBody(), ProposalWithdrawnEvent.class);
                MDC.put("proposalId", String.valueOf(event.proposalId()));
                MDC.put("jobId", String.valueOf(event.jobId()));
                onProposalWithdrawn(event);
                return;
            }

            log.warn("Ignoring unsupported proposal routingKey={}", routingKey);

        } finally {
            MDC.remove("routingKey");
            MDC.remove("correlationId");
            MDC.remove("proposalId");
            MDC.remove("jobId");
        }
    }

    private void onProposalAccepted(ProposalAcceptedEvent event) {
        updateJobStatus(event.jobId(), JobStatus.IN_PROGRESS, "proposal.accepted");
    }

    private void onProposalCompleted(ProposalCompletedEvent event) {
        updateJobStatus(event.jobId(), JobStatus.CLOSED, "proposal.completed");
    }

    private void onProposalCancelled(ProposalCancelledEvent event) {
        Job job = jobRepository.findById(event.jobId()).orElse(null);

        if (job == null) {
            log.warn("Job {} not found while processing proposal.cancelled", event.jobId());
            return;
        }

        if (job.getStatus() == JobStatus.CLOSED) {
            updateJobStatus(event.jobId(), JobStatus.IN_PROGRESS, "proposal.cancelled");
            return;
        }

        if (job.getStatus() == JobStatus.IN_PROGRESS) {
            updateJobStatus(event.jobId(), JobStatus.OPEN, "proposal.cancelled");
            return;
        }

        log.info("No job status change needed for job {} on proposal.cancelled", event.jobId());
    }

    private void onProposalWithdrawn(ProposalWithdrawnEvent event) {
        Job job = jobRepository.findById(event.jobId()).orElse(null);

        if (job == null) {
            log.warn("Job {} not found while processing proposal.withdrawn", event.jobId());
            return;
        }

        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            log.info("No job status change needed for job {} on proposal.withdrawn because status is {}",
                    event.jobId(), job.getStatus());
            return;
        }

        try {
            com.team35.freelance.contracts.dto.JobProposalSummaryDTO summary =
                    proposalServiceClient.getJobProposalSummary(
                            event.jobId(),
                            "1970-01-01",
                            "2999-12-31"
                    );

            Long acceptedProposals = summary.getAcceptedProposals() == null
                    ? 0L
                    : summary.getAcceptedProposals();

            if (acceptedProposals == 0L) {
                updateJobStatus(event.jobId(), JobStatus.OPEN, "proposal.withdrawn");
                return;
            }

            log.info("Job {} remains IN_PROGRESS after proposal.withdrawn because acceptedProposals={}",
                    event.jobId(), acceptedProposals);

        } catch (FeignException e) {
            log.error("Failed to check proposal summary for job {} while processing proposal.withdrawn: {}",
                    event.jobId(), e.getMessage());
            throw e;
        }
    }

    private void updateJobStatus(Long jobId, JobStatus newStatus, String sourceRoutingKey) {
        Job job = jobRepository.findById(jobId).orElse(null);

        if (job == null) {
            log.warn("Job {} not found while processing {}", jobId, sourceRoutingKey);
            return;
        }

        JobStatus oldStatus = job.getStatus();

        if (oldStatus == newStatus) {
            log.info("Job {} already has status {}, no update needed", jobId, newStatus);
            return;
        }

        job.setStatus(newStatus);
        jobRepository.save(job);

        jobEventPublisher.publishStatusChanged(
                new JobStatusChangedEvent(job.getId(), oldStatus == null ? null : oldStatus.name(), newStatus.name())
        );

        if (newStatus == JobStatus.CLOSED) {
            jobEventPublisher.publishJobClosed(new JobClosedEvent(job.getId(), job.getClientId()));
        }

        log.info("Job {} status changed from {} to {} after {}", jobId, oldStatus, newStatus, sourceRoutingKey);
    }
}
