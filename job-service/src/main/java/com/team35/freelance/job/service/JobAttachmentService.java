package com.team35.freelance.job.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.team35.freelance.job.dto.VerifyAttachmentRequestDTO;
import com.team35.freelance.job.exception.BadRequestException;
import com.team35.freelance.job.exception.ForbiddenException;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;

import jakarta.transaction.Transactional;

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

    @Cacheable(value = "job-service::attachment", key = "#attachmentId")
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


    @CacheEvict(value = {
            "job-service::attachment",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
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

    @Transactional
    public Job verifyAttachment(Long jobId, Long attachmentId, VerifyAttachmentRequestDTO request) {
        Job job = jobRepository.findByIdWithAttachments(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        JobAttachment attachment = jobAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (request == null || request.getVerifiedBy() == null) {
            throw new BadRequestException("verifiedBy is required");
        }

        if (attachment.getJob() == null || !jobId.equals(attachment.getJob().getId())) {
            throw new BadRequestException("Attachment does not belong to this job");
        }

        if (attachment.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Attachment is expired and cannot be verified");
        }

        String role = jobRepository.findUserRoleById(request.getVerifiedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Verifier user not found"));

        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Only ADMIN users can verify job attachments");
        }

        attachment.setVerified(true);

        Map<String, Object> metadata = attachment.getMetadata() == null
                ? new HashMap<>()
                : new HashMap<>(attachment.getMetadata());

        metadata.put("verifiedAt", LocalDateTime.now().toString());
        metadata.put("verifiedBy", request.getVerifiedBy());

        attachment.setMetadata(metadata);

        jobAttachmentRepository.save(attachment);

        return jobRepository.findByIdWithAttachments(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }
}