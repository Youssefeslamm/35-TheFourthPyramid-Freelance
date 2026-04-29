package com.team35.freelance.wallet.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "wallet_events")
public class WalletEvent implements MongoEvent {

    @Id
    private String id;

    private Long payoutId;
    private Long contractId;
    private Long freelancerId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public WalletEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public WalletEvent(Long payoutId, Long contractId, Long freelancerId, String action, Map<String, Object> details) {
        this.payoutId = payoutId;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.action = action;
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

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}