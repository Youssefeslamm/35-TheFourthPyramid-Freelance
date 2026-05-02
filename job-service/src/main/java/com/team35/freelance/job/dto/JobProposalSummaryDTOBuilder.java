package com.team35.freelance.job.dto;

public class JobProposalSummaryDTOBuilder {

    private Long jobId;
    private String title;
    private Long totalProposals;
    private Double averageBidAmount;
    private Double lowestBid;
    private Double highestBid;

    public JobProposalSummaryDTOBuilder jobId(Long val) {
        this.jobId = val;
        return this;
    }

    public JobProposalSummaryDTOBuilder title(String val) {
        this.title = val;
        return this;
    }

    public JobProposalSummaryDTOBuilder totalProposals(Long val) {
        this.totalProposals = val;
        return this;
    }

    public JobProposalSummaryDTOBuilder averageBidAmount(Double val) {
        this.averageBidAmount = val;
        return this;
    }

    public JobProposalSummaryDTOBuilder lowestBid(Double val) {
        this.lowestBid = val;
        return this;
    }

    public JobProposalSummaryDTOBuilder highestBid(Double val) {
        this.highestBid = val;
        return this;
    }

    public JobProposalSummaryDTO build() {
        return JobProposalSummaryDTO.builder()
                .jobId(jobId)
                .title(title)
                .totalProposals(totalProposals)
                .averageBidAmount(averageBidAmount)
                .lowestBid(lowestBid)
                .highestBid(highestBid)
                .build();
    }
}