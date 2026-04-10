package com.team35.freelance.job.dto;

import com.team35.freelance.job.model.JobAttachmentType;

import java.time.LocalDate;
import java.util.Map;

public class JobAttachmentRequestDTO {

    private JobAttachmentType type;
    private String fileUrl;
    private LocalDate expiryDate;
    private Boolean verified;
    private Map<String, Object> metadata;

    public JobAttachmentType getType() {
        return type;
    }

    public void setType(JobAttachmentType type) {
        this.type = type;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}