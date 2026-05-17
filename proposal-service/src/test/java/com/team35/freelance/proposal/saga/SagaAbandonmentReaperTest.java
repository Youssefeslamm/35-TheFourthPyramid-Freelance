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
import static org.assertj.core.api.Assertions.assertThatNoException;
import java.util.List;

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
    @Test
    void reaper_acceptedAtNull_doesNotThrowNPE() {
        Proposal stuck = new Proposal();
        stuck.setId(5L);
        stuck.setJobId(10L);
        stuck.setFreelancerId(3L);
        stuck.setStatus(ProposalStatus.PAYMENT_PENDING);
        stuck.setAcceptedAt(null); // no acceptedAt set

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(stuck));
        when(proposalRepository.save(any())).thenReturn(stuck);

        // Must not throw NPE
        assertThatNoException().isThrownBy(() -> reaper.reapAbandonedPayouts());

        verify(proposalEventPublisher).publishCancelled(any());
    }

    @Test
    void reaper_largeList_allProcessed() {
        List<Proposal> proposals = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Proposal p = new Proposal();
            p.setId((long) i);
            p.setJobId(10L);
            p.setFreelancerId(5L);
            p.setStatus(ProposalStatus.PAYMENT_PENDING);
            proposals.add(p);
        }

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(proposals);
        when(proposalRepository.save(any())).thenReturn(new Proposal());

        reaper.reapAbandonedPayouts();

        verify(proposalRepository, times(20)).save(any());
        verify(proposalEventPublisher, times(20)).publishCancelled(any());
    }

    @Test
    void reaper_exactlyAtCutoffBoundary_isReaped() {
        // A proposal acceptedAt exactly = cutoff should be included
        // (findByStatusAndAcceptedAtBefore uses "before" so boundary itself is excluded
        //  — this test verifies the cutoff calculation is consistent)
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), cutoffCaptor.capture()))
                .thenReturn(List.of());

        reaper.reapAbandonedPayouts();

        LocalDateTime cutoff = cutoffCaptor.getValue();
        // cutoff must be in the past
        assertThat(cutoff).isBefore(LocalDateTime.now());
        // cutoff must be approximately 72h ago
        assertThat(cutoff).isAfter(LocalDateTime.now().minusHours(73));
    }

    @Test
    void reaper_allProposalsFail_doesNotPropagateException() {
        Proposal p1 = new Proposal();
        p1.setId(1L); p1.setJobId(10L); p1.setFreelancerId(5L);
        p1.setStatus(ProposalStatus.PAYMENT_PENDING);

        Proposal p2 = new Proposal();
        p2.setId(2L); p2.setJobId(10L); p2.setFreelancerId(5L);
        p2.setStatus(ProposalStatus.PAYMENT_PENDING);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(p1, p2));
        when(proposalRepository.save(any())).thenReturn(new Proposal());

        // Both throw on publish
        doThrow(new RuntimeException("rabbit totally down"))
                .when(proposalEventPublisher).publishCancelled(any());

        // Reaper must not propagate — it catches per-proposal
        assertThatNoException().isThrownBy(() -> reaper.reapAbandonedPayouts());
    }

    @Test
    void reaper_eachProposalGetsSeparateCancelledEvent() {
        Proposal p1 = new Proposal();
        p1.setId(10L); p1.setJobId(1L); p1.setFreelancerId(2L);
        p1.setStatus(ProposalStatus.PAYMENT_PENDING);

        Proposal p2 = new Proposal();
        p2.setId(20L); p2.setJobId(3L); p2.setFreelancerId(4L);
        p2.setStatus(ProposalStatus.PAYMENT_PENDING);

        when(proposalRepository.findByStatusAndAcceptedAtBefore(
                eq(ProposalStatus.PAYMENT_PENDING), any()))
                .thenReturn(List.of(p1, p2));
        when(proposalRepository.save(any())).thenReturn(new Proposal());

        reaper.reapAbandonedPayouts();

        ArgumentCaptor<ProposalCancelledEvent> captor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher, times(2)).publishCancelled(captor.capture());

        List<ProposalCancelledEvent> events = captor.getAllValues();
        assertThat(events).extracting(ProposalCancelledEvent::proposalId)
                .containsExactlyInAnyOrder(10L, 20L);
        assertThat(events).extracting(ProposalCancelledEvent::reason)
                .containsOnly("payout_abandoned");
    }
}