package com.team35.freelance.user.common.event;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {

        switch (type) {
            case AUTH:
                return new AuthEvent(params);

            default:
                throw new IllegalArgumentException("Invalid event type");
        }
    }
}