package com.team35.freelance.wallet.common.event;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {

        return switch (type) {

            case PAYOUT_AUDIT -> new PayoutAuditEvent(
                    (String) params.get("action"),
                    params
            );

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + type
            );
        };
    }
}