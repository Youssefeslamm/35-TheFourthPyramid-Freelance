package com.team35.freelance.job.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_attachments")
public class JobAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "job_attachment_type_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private JobAttachmentType type;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private Boolean verified = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnore
    private Job job;

    public JobAttachment() {
    }

    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
        if (this.verified == null) {
            this.verified = false;
        }
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }

    public Long getId() {
        return id;
    }

    public JobAttachmentType getType() {
        return type;
    }

    public void setType(JobAttachmentType type) {
        this.type = type;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}