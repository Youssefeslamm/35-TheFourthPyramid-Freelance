package com.team35.freelance.contract.common.event;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class EventFactory {

    public MongoEvent createEvent(EventType type, Map<String, Object> params) {

        if (type == EventType.CONTRACT) {
            return new ContractEvent(
                    (String) params.get("action"),
                    params
            );
        }

        throw new IllegalArgumentException("Unsupported event type: " + type);
    }
}

