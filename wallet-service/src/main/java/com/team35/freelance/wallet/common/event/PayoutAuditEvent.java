package com.team35.freelance.wallet.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "payout_audit_trail")
public class PayoutAuditEvent implements MongoEvent {

    @Id
    private final String id;
    private final LocalDateTime timestamp;
    private final String action;
    private final Map<String, Object> details;

    private final String method;
    private final Double amount;

    public PayoutAuditEvent(String action, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;
        this.method = details != null ? (String) details.get("method") : null;
        Object amt = details != null ? details.get("amount") : null;
        this.amount = amt instanceof Number ? ((Number) amt).doubleValue() : null;
    }

    @Override
    public String getId() { return id; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getAction() { return action; }

    @Override
    public Map<String, Object> getDetails() { return details; }

    public String getMethod() { return method; }

    public Double getAmount() { return amount; }
}