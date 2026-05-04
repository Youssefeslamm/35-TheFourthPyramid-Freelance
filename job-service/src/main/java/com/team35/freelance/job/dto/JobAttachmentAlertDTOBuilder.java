package com.team35.freelance.job.dto;

import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;

import java.util.List;

public class JobAttachmentAlertDTOBuilder {

    private Long jobId;
    private String jobTitle;
    private JobStatus jobStatus;
    private List<JobAttachment> expiredAttachments;
    private Integer expiredCount;

    public JobAttachmentAlertDTOBuilder jobId(Long val) {
        this.jobId = val;
        return this;
    }

    public JobAttachmentAlertDTOBuilder jobTitle(String val) {
        this.jobTitle = val;
        return this;
    }

    public JobAttachmentAlertDTOBuilder jobStatus(JobStatus val) {
        this.jobStatus = val;
        return this;
    }

    public JobAttachmentAlertDTOBuilder expiredAttachments(List<JobAttachment> val) {
        this.expiredAttachments = val;
        return this;
    }

    public JobAttachmentAlertDTOBuilder expiredCount(Integer val) {
        this.expiredCount = val;
        return this;
    }

    public JobAttachmentAlertDTO build() {
        return new JobAttachmentAlertDTO(
                jobId,
                jobTitle,
                jobStatus,
                expiredAttachments,
                expiredCount
        );
    }
}