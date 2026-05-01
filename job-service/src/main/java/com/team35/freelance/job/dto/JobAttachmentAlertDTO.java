package com.team35.freelance.job.dto;

import java.util.List;

import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;

public class JobAttachmentAlertDTO {

    private Long jobId;
    private String jobTitle;
    private JobStatus jobStatus;
    private List<JobAttachment> expiredAttachments;
    private Integer expiredCount;

    JobAttachmentAlertDTO(Long jobId,
                          String jobTitle,
                          JobStatus jobStatus,
                          List<JobAttachment> expiredAttachments,
                          Integer expiredCount) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.jobStatus = jobStatus;
        this.expiredAttachments = expiredAttachments;
        this.expiredCount = expiredCount;
    }

    private JobAttachmentAlertDTO(Builder builder) {
        this.jobId = builder.jobId;
        this.jobTitle = builder.jobTitle;
        this.jobStatus = builder.jobStatus;
        this.expiredAttachments = builder.expiredAttachments;
        this.expiredCount = builder.expiredCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long jobId;
        private String jobTitle;
        private JobStatus jobStatus;
        private List<JobAttachment> expiredAttachments;
        private Integer expiredCount;

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder jobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public Builder jobStatus(JobStatus jobStatus) {
            this.jobStatus = jobStatus;
            return this;
        }

        public Builder expiredAttachments(List<JobAttachment> expiredAttachments) {
            this.expiredAttachments = expiredAttachments;
            return this;
        }

        public Builder expiredCount(Integer expiredCount) {
            this.expiredCount = expiredCount;
            return this;
        }

        public JobAttachmentAlertDTO build() {
            return new JobAttachmentAlertDTO(this);
        }
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public List<JobAttachment> getExpiredAttachments() {
        return expiredAttachments;
    }

    public Integer getExpiredCount() {
        return expiredCount;
    }
}