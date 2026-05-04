package com.team35.freelance.proposal.dto;

import java.io.Serializable;

public class JobRecommendationDTO implements Serializable {
    private Long jobId;
    private String title;
    private String category;
    private Integer score;

    private JobRecommendationDTO() {}

    public Long getJobId() { return jobId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public Integer getScore() { return score; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final JobRecommendationDTO dto = new JobRecommendationDTO();

        public Builder jobId(Long jobId) { dto.jobId = jobId; return this; }
        public Builder title(String title) { dto.title = title; return this; }
        public Builder category(String category) { dto.category = category; return this; }
        public Builder score(Integer score) { dto.score = score; return this; }
        public JobRecommendationDTO build() { return dto; }
    }
}