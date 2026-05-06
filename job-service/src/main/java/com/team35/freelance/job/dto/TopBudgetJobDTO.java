package com.team35.freelance.job.dto;

import java.io.Serializable;

public class TopBudgetJobDTO implements Serializable {

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long jobId;
        private String title;
        private Double budgetMax;
        private Long totalProposals;

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder budgetMax(Double budgetMax) {
            this.budgetMax = budgetMax;
            return this;
        }

        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        public TopBudgetJobDTO build() {
            return new TopBudgetJobDTO(jobId, title, budgetMax, totalProposals);
        }
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
