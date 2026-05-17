package com.team35.freelance.proposal.saga;

import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SagaAbandonmentReaper {

    private static final Logger log = LoggerFactory.getLogger(SagaAbandonmentReaper.class);

    private final ProposalRepository proposalRepository;
    private final ProposalEventPublisher proposalEventPublisher;
    private final Duration abandonAfter;

    public SagaAbandonmentReaper(ProposalRepository proposalRepository,
                                 ProposalEventPublisher proposalEventPublisher,
                                 @Value("${saga.payout.abandon-after:PT72H}") String abandonAfter) {
        this.proposalRepository = proposalRepository;
        this.proposalEventPublisher = proposalEventPublisher;
        this.abandonAfter = Duration.parse(abandonAfter);
    }

    @Scheduled(fixedDelayString = "${saga.payout.reaper-interval:PT15M}")
    @Transactional
    public void reapAbandonedPayouts() {
        LocalDateTime cutoff = LocalDateTime.now().minus(abandonAfter);
        List<Proposal> stuckProposals = proposalRepository.findByStatusAndAcceptedAtBefore(
                ProposalStatus.PAYMENT_PENDING, cutoff);

        if (stuckProposals.isEmpty()) {
            return;
        }

        log.warn("Saga reaper found {} proposals stuck in PAYMENT_PENDING beyond {}",
                stuckProposals.size(), abandonAfter);

        for (Proposal proposal : stuckProposals) {
            try {
                MDC.put("proposalId", String.valueOf(proposal.getId()));
                log.warn("Reaping abandoned proposal {} stuck in PAYMENT_PENDING since acceptedAt={}",
                        proposal.getId(), proposal.getAcceptedAt());

                // Mirror exactly what onPaymentFailed does:
                // 1. Set status to PAYMENT_FAILED
                proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
                proposalRepository.save(proposal);

                log.info("Proposal {} transitioning PAYMENT_PENDING -> PAYMENT_FAILED (reaper)",
                        proposal.getId());

                // 2. Publish proposal.cancelled to trigger compensation cascade
                proposalEventPublisher.publishCancelled(
                        new ProposalCancelledEvent(
                                proposal.getId(),
                                proposal.getJobId(),
                                proposal.getFreelancerId(),
                                "payout_abandoned"
                        )
                );

            } catch (Exception e) {
                log.error("Failed to reap proposal {}: {}", proposal.getId(), e.getMessage(), e);
            } finally {
                MDC.remove("proposalId");
            }
        }
    }
}