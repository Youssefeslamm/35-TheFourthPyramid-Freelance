package com.team35.freelance.contracts.dto;

public class JobProposalSummaryDTO implements java.io.Serializable {
    private Long jobId;
    private String title;
    private Long totalProposals;
    private Long acceptedProposals;
    private Double averageBidAmount;
    private Double lowestBid;
    private Double highestBid;

    public JobProposalSummaryDTO() {
    }

    public JobProposalSummaryDTO(Long jobId,
                                 String title,
                                 Long totalProposals,
                                 Long acceptedProposals,
                                 Double averageBidAmount,
                                 Double lowestBid,
                                 Double highestBid) {
        this.jobId = jobId;
        this.title = title;
        this.totalProposals = totalProposals;
        this.acceptedProposals = acceptedProposals;
        this.averageBidAmount = averageBidAmount;
        this.lowestBid = lowestBid;
        this.highestBid = highestBid;
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

    public Double getLowestBid() {
        return lowestBid;
    }

    public void setLowestBid(Double lowestBid) {
        this.lowestBid = lowestBid;
    }

    public Double getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Double highestBid) {
        this.highestBid = highestBid;
    }
}