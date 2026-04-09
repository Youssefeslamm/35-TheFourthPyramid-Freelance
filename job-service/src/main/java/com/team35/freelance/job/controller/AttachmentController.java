package com.team35.freelance.job.controller;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/jobs/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<JobAttachment> upload(@RequestBody JobAttachment attachment) {
        return ResponseEntity.ok(attachmentService.saveAttachment(attachment));
    }
}