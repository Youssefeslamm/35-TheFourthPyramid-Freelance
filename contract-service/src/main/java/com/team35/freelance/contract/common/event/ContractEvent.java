package com.team35.freelance.contract.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "contract_events")
public class ContractEvent implements MongoEvent {

    @Id
    private final String id;
    private final LocalDateTime timestamp;
    private final String action;
    private final Map<String, Object> details;

    public ContractEvent(String action, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }
}

