package com.team35.freelance.job.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team35.freelance.job.dto.JobAttachmentRequestDTO;
import com.team35.freelance.job.dto.VerifyAttachmentRequestDTO;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.service.JobAttachmentService;

@RestController
@RequestMapping("/api/jobs/{jobId}/attachments")
public class JobAttachmentController {

    private final JobAttachmentService jobAttachmentService;

    public JobAttachmentController(JobAttachmentService jobAttachmentService) {
        this.jobAttachmentService = jobAttachmentService;
    }

    @PostMapping
    public ResponseEntity<JobAttachment> createAttachment(@PathVariable Long jobId,
                                                          @RequestBody JobAttachmentRequestDTO dto) {
        JobAttachment attachment = new JobAttachment();
        attachment.setType(dto.getType());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setExpiryDate(dto.getExpiryDate());
        attachment.setVerified(dto.getVerified());
        attachment.setMetadata(dto.getMetadata());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobAttachmentService.createAttachment(jobId, attachment));
    }

    @GetMapping
    public ResponseEntity<List<JobAttachment>> getAttachmentsByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobAttachmentService.getAttachmentsByJob(jobId));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<JobAttachment> getAttachmentById(@PathVariable Long jobId,
                                                           @PathVariable Long attachmentId) {
        return ResponseEntity.ok(jobAttachmentService.getAttachmentById(jobId, attachmentId));
    }

    @PutMapping("/{attachmentId}")
    public ResponseEntity<JobAttachment> updateAttachment(@PathVariable Long jobId,
                                                          @PathVariable Long attachmentId,
                                                          @RequestBody JobAttachmentRequestDTO dto) {
        JobAttachment updatedAttachment = new JobAttachment();
        updatedAttachment.setType(dto.getType());
        updatedAttachment.setFileUrl(dto.getFileUrl());
        updatedAttachment.setExpiryDate(dto.getExpiryDate());
        updatedAttachment.setVerified(dto.getVerified());
        updatedAttachment.setMetadata(dto.getMetadata());

        return ResponseEntity.ok(
                jobAttachmentService.updateAttachment(jobId, attachmentId, updatedAttachment)
        );
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long jobId,
                                                 @PathVariable Long attachmentId) {
        jobAttachmentService.deleteAttachment(jobId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{attachmentId}/verify")
    public ResponseEntity<Job> verifyAttachment(@PathVariable Long jobId,
                                                @PathVariable Long attachmentId,
                                                @RequestBody VerifyAttachmentRequestDTO request) {
        return ResponseEntity.ok(jobAttachmentService.verifyAttachment(jobId, attachmentId, request));
    }

    
}