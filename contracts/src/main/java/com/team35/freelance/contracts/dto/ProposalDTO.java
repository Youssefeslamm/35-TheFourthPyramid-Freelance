package com.team35.freelance.contracts.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ProposalDTO {
    private Long id;
    private Long jobId;
    private Long freelancerId;
    private String status;
    private Double bidAmount;
    private Integer estimatedDays;
    private LocalDateTime submittedAt;
    private Map<String, Object> metadata;

    public ProposalDTO() {
    }

    public ProposalDTO(Long id,
                       Long jobId,
                       Long freelancerId,
                       String status,
                       Double bidAmount,
                       Integer estimatedDays,
                       LocalDateTime submittedAt,
                       Map<String, Object> metadata) {
        this.id = id;
        this.jobId = jobId;
        this.freelancerId = freelancerId;
        this.status = status;
        this.bidAmount = bidAmount;
        this.estimatedDays = estimatedDays;
        this.submittedAt = submittedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Integer getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(Integer estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

