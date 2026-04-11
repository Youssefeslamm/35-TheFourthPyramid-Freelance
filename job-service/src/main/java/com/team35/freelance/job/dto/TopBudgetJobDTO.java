package com.team35.freelance.job.dto;

public class TopBudgetJobDTO {

    private Long jobId;
    private String title;
    private Double budgetMax;
    private Long totalProposals;

    public TopBudgetJobDTO() {
    }

    public TopBudgetJobDTO(Long jobId, String title, Double budgetMax, Long totalProposals) {
        this.jobId = jobId;
        this.title = title;
        this.budgetMax = budgetMax;
        this.totalProposals = totalProposals;
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

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public Long getTotalProposals() {
        return totalProposals;
    }

    public void setTotalProposals(Long totalProposals) {
        this.totalProposals = totalProposals;
    }
}