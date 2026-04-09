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
}
