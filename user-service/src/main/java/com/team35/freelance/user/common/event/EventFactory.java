package com.team35.freelance.user.common.event;

import java.util.Map;

public class EventFactory {

    public static MongoEvent createEvent(EventType type, Map<String, Object> params) {

        switch (type) {
            case AUTH:
                return new AuthEvent(params);

            // we will add the rest later
            default:
                throw new IllegalArgumentException("Invalid event type");
        }
    }
}