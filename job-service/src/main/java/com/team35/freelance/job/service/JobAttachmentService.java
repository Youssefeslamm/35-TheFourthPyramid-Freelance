package com.team35.freelance.job.service;

import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobAttachmentService {

    private final JobAttachmentRepository jobAttachmentRepository;
    private final JobRepository jobRepository;

    public JobAttachmentService(JobAttachmentRepository jobAttachmentRepository,
                                JobRepository jobRepository) {
        this.jobAttachmentRepository = jobAttachmentRepository;
        this.jobRepository = jobRepository;
    }

    public JobAttachment createAttachment(Long jobId, JobAttachment attachment) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        validateAttachment(attachment);

        attachment.setJob(job);
        return jobAttachmentRepository.save(attachment);
    }

    public List<JobAttachment> getAttachmentsByJob(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        return jobAttachmentRepository.findByJobId(jobId);
    }

    public JobAttachment getAttachmentById(Long jobId, Long attachmentId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        return jobAttachmentRepository.findByIdAndJobId(attachmentId, jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    public JobAttachment updateAttachment(Long jobId, Long attachmentId, JobAttachment updatedAttachment) {
        JobAttachment existing = getAttachmentById(jobId, attachmentId);

        validateAttachment(updatedAttachment);

        existing.setType(updatedAttachment.getType());
        existing.setFileUrl(updatedAttachment.getFileUrl());
        existing.setExpiryDate(updatedAttachment.getExpiryDate());
        existing.setMetadata(updatedAttachment.getMetadata());

        if (updatedAttachment.getVerified() != null) {
            existing.setVerified(updatedAttachment.getVerified());
        }

        return jobAttachmentRepository.save(existing);
    }

    public void deleteAttachment(Long jobId, Long attachmentId) {
        JobAttachment existing = getAttachmentById(jobId, attachmentId);
        jobAttachmentRepository.delete(existing);
    }

    private void validateAttachment(JobAttachment attachment) {
        if (attachment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment request body is required");
        }

        if (attachment.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }

        if (attachment.getFileUrl() == null || attachment.getFileUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileUrl is required");
        }

        if (attachment.getExpiryDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiryDate is required");
        }
    }
}