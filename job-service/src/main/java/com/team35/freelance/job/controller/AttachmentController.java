package com.team35.freelance.job.controller;

import com.team35.freelance.job.dto.AttachmentRequestDTO;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.team35.freelance.job.model.AttachmentType; // Fixes red 'AttachmentType'
import org.springframework.data.domain.Page;               // Fixes red 'Page'
import org.springframework.web.bind.annotation.GetMapping; // Fixes red 'GetMapping'
import org.springframework.web.bind.annotation.PathVariable; // Fixes red 'PathVariable'
import org.springframework.web.bind.annotation.RequestParam;  // Fixes red 'RequestParam'
import java.util.List;                                     // Fixes red 'List'
@RestController
@RequestMapping("/api/jobs/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<JobAttachment> upload(@RequestBody AttachmentRequestDTO requestDto) { // Changed name to requestDto
        JobAttachment attachment = new JobAttachment();
        attachment.setJobId(requestDto.getJobId());
        attachment.setType(requestDto.getType());
        attachment.setFileUrl(requestDto.getFileUrl());
        attachment.setMetadata(requestDto.getMetadata());

        return ResponseEntity.ok(attachmentService.saveAttachment(attachment));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<Page<JobAttachment>> getJobAttachments(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(attachmentService.getAttachmentsPaged(jobId, page, size));
    }

    @GetMapping("/job/{jobId}/verified")
    public ResponseEntity<List<JobAttachment>> getVerified(
            @PathVariable Long jobId,
            @RequestParam AttachmentType type) {

        return ResponseEntity.ok(attachmentService.getVerifiedAttachments(jobId, type));
    }
}
