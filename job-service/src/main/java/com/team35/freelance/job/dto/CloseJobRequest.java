package com.team35.freelance.job.dto;

import com.team35.freelance.job.model.JobStatus;

public class CloseJobRequest {

    private JobStatus status;

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }
}