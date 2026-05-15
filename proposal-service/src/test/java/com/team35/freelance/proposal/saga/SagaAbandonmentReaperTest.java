// proposal-service/src/test/java/com/team35/freelance/proposal/saga/SagaAbandonmentReaperTest.java
package com.team35.freelance.proposal.saga;

import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaAbandonmentReaperTest {

    @Mock ProposalRepository proposalRepository;
    @Mock ProposalEventPublisher proposalEventPublisher;

    SagaAbandonmentReaper reaper;

    @BeforeEach
    void setUp() {
        reaper = new SagaAbandonmentReaper(proposalRepository, proposalEventPublisher, "PT72H");
    }

    @Test
    void noStuckProposals_doesNothing() {
        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of());

        reaper.reapAbandonedPayouts();

        verify(proposalRepository, never()).save(any());
        verify(proposalEventPublisher, never()).publishCancelled(any());
    }

    @Test
    void stuckProposal_setsPaymentFailedAndPublishesCancelled() {
        Proposal stuck = new Proposal();
        stuck.setId(42L);
        stuck.setJobId(10L);
        stuck.setFreelancerId(5L);
        stuck.setStatus(ProposalStatus.PAYMENT_PENDING);
        stuck.setAcceptedAt(LocalDateTime.now().minusHours(100));

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(stuck));

        reaper.reapAbandonedPayouts();

        // Status must be set to PAYMENT_FAILED
        ArgumentCaptor<Proposal> savedCaptor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);

        // Must publish proposal.cancelled with reason payout_abandoned
        ArgumentCaptor<ProposalCancelledEvent> eventCaptor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher).publishCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().proposalId()).isEqualTo(42L);
        assertThat(eventCaptor.getValue().reason()).isEqualTo("payout_abandoned");
    }

    @Test
    void oneProposalFails_othersStillProcessed() {
        Proposal good = new Proposal();
        good.setId(1L); good.setJobId(10L); good.setFreelancerId(5L);
        good.setStatus(ProposalStatus.PAYMENT_PENDING);

        Proposal bad = new Proposal();
        bad.setId(2L); bad.setJobId(20L); bad.setFreelancerId(6L);
        bad.setStatus(ProposalStatus.PAYMENT_PENDING);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(good, bad));

        // save() returns Proposal so use when().thenReturn() not doNothing()
        when(proposalRepository.save(any())).thenReturn(new Proposal());

        // First publishCancelled throws, second succeeds
        doThrow(new RuntimeException("rabbit down"))
                .doNothing()
                .when(proposalEventPublisher).publishCancelled(any());

        // Should not throw — reaper catches per-proposal exceptions
        reaper.reapAbandonedPayouts();

        // Both proposals attempted — publishCancelled called twice
        verify(proposalEventPublisher, times(2)).publishCancelled(any());
    }
    // Add these to SagaAbandonmentReaperTest.java

    @Test
    void reaper_usesCorrectCutoffTime() {
        // Verify the reaper queries with a cutoff = now - abandonAfter
        // i.e. PT72H means cutoff should be ~72 hours ago
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), cutoffCaptor.capture()))
                .thenReturn(List.of());

        reaper.reapAbandonedPayouts();

        LocalDateTime captured = cutoffCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusHours(72);

        // Should be within 5 seconds of expected
        assertThat(captured).isBetween(
                expectedCutoff.minusSeconds(5),
                expectedCutoff.plusSeconds(5)
        );
    }

    @Test
    void reaper_setsStatusBeforePublishing() {
        // Status must be PAYMENT_FAILED BEFORE publishing cancelled
        // to avoid compensation firing on wrong status
        Proposal stuck = new Proposal();
        stuck.setId(1L);
        stuck.setJobId(10L);
        stuck.setFreelancerId(5L);
        stuck.setStatus(ProposalStatus.PAYMENT_PENDING);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(stuck));
        when(proposalRepository.save(any())).thenReturn(stuck);

        var order = inOrder(proposalRepository, proposalEventPublisher);

        reaper.reapAbandonedPayouts();

        order.verify(proposalRepository).save(any()); // save happens first
        order.verify(proposalEventPublisher).publishCancelled(any()); // then publish
    }

    @Test
    void reaper_publishesWithPayoutAbandonedReason() {
        Proposal stuck = new Proposal();
        stuck.setId(99L);
        stuck.setJobId(20L);
        stuck.setFreelancerId(7L);
        stuck.setStatus(ProposalStatus.PAYMENT_PENDING);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(stuck));
        when(proposalRepository.save(any())).thenReturn(stuck);

        reaper.reapAbandonedPayouts();

        ArgumentCaptor<ProposalCancelledEvent> captor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher).publishCancelled(captor.capture());

        assertThat(captor.getValue().reason()).isEqualTo("payout_abandoned");
        assertThat(captor.getValue().proposalId()).isEqualTo(99L);
        assertThat(captor.getValue().jobId()).isEqualTo(20L);
        assertThat(captor.getValue().freelancerId()).isEqualTo(7L);
    }
}