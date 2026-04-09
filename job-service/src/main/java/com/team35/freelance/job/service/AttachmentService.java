package com.team35.freelance.job.service;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.AttachmentType; // Fixes AttachmentType
import org.springframework.data.domain.Page;               // Fixes Page
import java.util.List;
public interface AttachmentService {
    JobAttachment saveAttachment(JobAttachment attachment);
    Page<JobAttachment> getAttachmentsPaged(Long jobId, int page, int size);
    List<JobAttachment> getVerifiedAttachments(Long jobId, AttachmentType type);
}