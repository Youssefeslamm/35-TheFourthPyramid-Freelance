package com.team35.freelance.contracts.dto;

public class JobDashboardDTO {
    private Long jobId;
    private String title;
    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Long activeAttachments;
    private Double rating;

    public JobDashboardDTO() {
    }

    public JobDashboardDTO(Long jobId,
                           String title,
                           Long totalProposals,
                           Long acceptedProposals,
                           Double averageBidAmount,
                           Long activeAttachments,
                           Double rating) {
        this.jobId = jobId;
        this.title = title;
        this.totalProposals = totalProposals;
        this.acceptedProposals = acceptedProposals;
        this.averageBidAmount = averageBidAmount;
        this.activeAttachments = activeAttachments;
        this.rating = rating;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTotalProposals() {
        return totalProposals;
    }

    public void setTotalProposals(Long totalProposals) {
        this.totalProposals = totalProposals;
    }

    public Long getAcceptedProposals() {
        return acceptedProposals;
    }

    public void setAcceptedProposals(Long acceptedProposals) {
        this.acceptedProposals = acceptedProposals;
    }

    public Double getAverageBidAmount() {
        return averageBidAmount;
    }

    public void setAverageBidAmount(Double averageBidAmount) {
        this.averageBidAmount = averageBidAmount;
    }

    public Long getActiveAttachments() {
        return activeAttachments;
    }

    public void setActiveAttachments(Long activeAttachments) {
        this.activeAttachments = activeAttachments;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }
}

