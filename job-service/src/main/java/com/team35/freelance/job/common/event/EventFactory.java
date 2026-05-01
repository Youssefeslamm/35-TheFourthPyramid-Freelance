package com.team35.freelance.job.common.event;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {

        if (type == EventType.JOB) {
            return new JobEvent(
                    (String) params.get("action"),
                    params
            );
        }

        throw new IllegalArgumentException("Unsupported event type: " + type);
    }
}