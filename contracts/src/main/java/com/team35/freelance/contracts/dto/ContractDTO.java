package com.team35.freelance.contracts.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ContractDTO {
    private Long id;
    private Long jobId;
    private Long freelancerId;
    private Long clientId;
    private Double agreedAmount;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Map<String, Object> metadata;

    public ContractDTO() {
    }

    public ContractDTO(Long id,
                       Long jobId,
                       Long freelancerId,
                       Long clientId,
                       Double agreedAmount,
                       String status,
                       LocalDateTime startDate,
                       LocalDateTime endDate,
                       Map<String, Object> metadata) {
        this.id = id;
        this.jobId = jobId;
        this.freelancerId = freelancerId;
        this.clientId = clientId;
        this.agreedAmount = agreedAmount;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Double getAgreedAmount() {
        return agreedAmount;
    }

    public void setAgreedAmount(Double agreedAmount) {
        this.agreedAmount = agreedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

