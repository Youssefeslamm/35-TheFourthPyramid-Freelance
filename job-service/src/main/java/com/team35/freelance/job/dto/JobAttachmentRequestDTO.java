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

    private JobAttachmentRequestDTO(Builder builder) {
        this.type = builder.type;
        this.fileUrl = builder.fileUrl;
        this.expiryDate = builder.expiryDate;
        this.verified = builder.verified;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JobAttachmentType type;
        private String fileUrl;
        private LocalDate expiryDate;
        private Boolean verified;
        private Map<String, Object> metadata;

        public Builder type(JobAttachmentType type) {
            this.type = type;
            return this;
        }

        public Builder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public JobAttachmentRequestDTO build() {
            return new JobAttachmentRequestDTO(this);
        }
    }

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