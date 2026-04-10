package com.team35.freelance.job.dto;

import java.util.List;

import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;

public record JobAttachmentAlertDTO(
        Long jobId,
        String jobTitle,
        JobStatus jobStatus,
        List<JobAttachment> expiredAttachments,
        Integer expiredCount
) {
}