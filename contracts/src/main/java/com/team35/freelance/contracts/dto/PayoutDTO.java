package com.team35.freelance.contracts.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class PayoutDTO {
    private Long id;
    private Long contractId;
    private Long freelancerId;
    private Double amount;
    private String method;
    private String status;
    private LocalDateTime createdAt;
    private Map<String, Object> transactionDetails;

    public PayoutDTO() {
    }

    public PayoutDTO(Long id,
                     Long contractId,
                     Long freelancerId,
                     Double amount,
                     String method,
                     String status,
                     LocalDateTime createdAt,
                     Map<String, Object> transactionDetails) {
        this.id = id;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.createdAt = createdAt;
        this.transactionDetails = transactionDetails;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getTransactionDetails() {
        return transactionDetails;
    }

    public void setTransactionDetails(Map<String, Object> transactionDetails) {
        this.transactionDetails = transactionDetails;
    }
}

