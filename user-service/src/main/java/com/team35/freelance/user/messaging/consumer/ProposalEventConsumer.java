package com.team35.freelance.user.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.user.model.User;
import com.team35.freelance.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProposalEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProposalEventConsumer.class);

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProposalEventConsumer(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "proposal.completed.user.queue")
    @Transactional
    public void onProposalCompleted(Message message) {
        ProposalCompletedEvent event = readEvent(message, ProposalCompletedEvent.class);
        if (event == null || event.freelancerId() == null) {
            log.warn("proposal.completed event missing freelancerId");
            return;
        }
        userRepository.findById(event.freelancerId()).ifPresentOrElse(user -> {
            incrementPreference(user, "completedProposals", 1L);
            addPreferenceAmount(user, "totalEarnings", event.agreedAmount());
            userRepository.save(user);
        }, () -> log.warn("Freelancer not found for proposal.completed freelancerId={}", event.freelancerId()));
    }

    @RabbitListener(queues = "proposal.cancelled.user.queue")
    @Transactional
    public void onProposalCancelled(Message message) {
        ProposalCancelledEvent event = readEvent(message, ProposalCancelledEvent.class);
        if (event == null || event.freelancerId() == null) {
            log.warn("proposal.cancelled event missing freelancerId");
            return;
        }
        userRepository.findById(event.freelancerId()).ifPresentOrElse(user -> {
            incrementPreference(user, "cancelledProposals", 1L);
            userRepository.save(user);
        }, () -> log.warn("Freelancer not found for proposal.cancelled freelancerId={}", event.freelancerId()));
    }

    private <T> T readEvent(Message message, Class<T> eventType) {
        try {
            return objectMapper.readValue(message.getBody(), eventType);
        } catch (Exception ex) {
            log.error("Failed to parse event {}: {}", eventType.getSimpleName(), ex.getMessage(), ex);
            throw new IllegalStateException("Failed to parse event " + eventType.getSimpleName(), ex);
        }
    }

    private void incrementPreference(User user, String key, long delta) {
        Map<String, Object> preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        Object existing = preferences.get(key);
        long current = existing instanceof Number number ? number.longValue() : 0L;
        preferences.put(key, current + delta);
        user.setPreferences(preferences);
    }

    private void addPreferenceAmount(User user, String key, BigDecimal amount) {
        Map<String, Object> preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        Object existing = preferences.get(key);
        double current = existing instanceof Number number ? number.doubleValue() : 0.0;
        double delta = amount == null ? 0.0 : amount.doubleValue();
        preferences.put(key, current + delta);
        user.setPreferences(preferences);
    }
}

