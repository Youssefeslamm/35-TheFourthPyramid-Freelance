package com.team35.freelance.wallet.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "payout_audit_trail")
public class PayoutAuditEvent implements MongoEvent {

    @Id
    private String id;

    private Long payoutId;
    private Long contractId;
    private Long freelancerId;

    private String action;
    private LocalDateTime timestamp;

    private String method;
    private Double amount;

    private Map<String, Object> details;

    public PayoutAuditEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public PayoutAuditEvent(Long payoutId,
                            Long contractId,
                            Long freelancerId,
                            String action,
                            String method,
                            Double amount,
                            Map<String, Object> details) {
        this.payoutId = payoutId;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.action = action;
        this.method = method;
        this.amount = amount;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public Long getPayoutId() {
        return payoutId;
    }

    public Long getContractId() {
        return contractId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMethod() {
        return method;
    }

    public Double getAmount() {
        return amount;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}