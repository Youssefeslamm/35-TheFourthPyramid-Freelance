package com.team35.freelance.job.dto;

import com.team35.freelance.job.model.AttachmentType;

import java.util.Map;

public class AttachmentRequestDTO {
    private Long jobId;
    private AttachmentType type;
    private String fileUrl;
    private Map<String, Object> metadata;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
