package com.team35.freelance.proposal.common.event;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {

        String action = (String) params.get("action");

        if (type == EventType.PROPOSAL) {
            return new ProposalEvent(
                    params.get("proposalId") instanceof Number
                            ? ((Number) params.get("proposalId")).longValue()
                            : null,
                    action,
                    params
            );
        }

        throw new IllegalArgumentException("Unknown event type: " + type);
    }
}