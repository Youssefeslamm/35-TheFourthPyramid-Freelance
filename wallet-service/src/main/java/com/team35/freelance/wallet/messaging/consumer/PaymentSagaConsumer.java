package com.team35.freelance.wallet.messaging.consumer;

import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.wallet.messaging.publisher.PaymentEventPublisher;
import com.team35.freelance.wallet.repository.PayoutRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;

import java.math.BigDecimal;
import java.util.List;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;

@Component
public class PaymentSagaConsumer {
    private final PayoutRepository payoutRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentSagaConsumer(PayoutRepository payoutRepository,
                               PaymentEventPublisher paymentEventPublisher) {
        this.payoutRepository = payoutRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }
    @RabbitListener(queues = "payment.saga-listener")
    public void handleSagaEvent(
            String payload,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        switch (routingKey) {

            case "proposal.completed" -> {
                ProposalCompletedEvent event =
                        mapper.readValue(payload, ProposalCompletedEvent.class);

                handleProposalCompleted(event);
            }

            case "proposal.cancelled" -> {
                ProposalCancelledEvent event =
                        mapper.readValue(payload, ProposalCancelledEvent.class);

                handleProposalCancelled(event);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported routing key: " + routingKey
            );
        }
    }
    public void handleProposalCompleted(ProposalCompletedEvent event) {
        Payout payout = new Payout();

        payout.setContractId(event.contractId());
        payout.setFreelancerId(event.freelancerId());
        payout.setAmount(event.agreedAmount().doubleValue());

        payout.setStatus(PayoutStatus.PENDING);
        payout.setMethod(null);

        payout.setCreatedAt(LocalDateTime.now());

        Payout savedPayout = payoutRepository.save(payout);

        paymentEventPublisher.publishInitiated(
                new PaymentInitiatedEvent(
                        savedPayout.getId(),
                        event.proposalId(),
                        event.contractId(),
                        event.agreedAmount()
                )
        );
    }

    public void handleProposalCancelled(ProposalCancelledEvent event) {
        List<Payout> payouts = payoutRepository.findByFreelancerIdAndStatusIn(
                event.freelancerId(),
                List.of(PayoutStatus.PENDING, PayoutStatus.COMPLETED)
        );

        for (Payout payout : payouts) {
            payout.setStatus(PayoutStatus.REFUNDED);
            Payout savedPayout = payoutRepository.save(payout);

            paymentEventPublisher.publishRefunded(
                    new PaymentRefundedEvent(
                            savedPayout.getId(),
                            event.proposalId(),
                            savedPayout.getContractId(),
                            BigDecimal.valueOf(savedPayout.getAmount())
                    )
            );
        }
    }

}
