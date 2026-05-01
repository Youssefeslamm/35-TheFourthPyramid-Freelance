package com.team35.freelance.wallet.common.event;

import java.util.Map;

public class EventFactory {

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {

        return switch (type) {
            case PAYOUT_AUDIT -> new PayoutAuditEvent(
                    (String) params.get("action"),
                    params
            );
        };
    }
}