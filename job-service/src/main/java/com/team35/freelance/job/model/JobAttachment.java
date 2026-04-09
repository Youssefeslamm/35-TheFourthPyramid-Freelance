package com.team35.freelance.job.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "job_attachments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A reference to the Job this attachment belongs to (Cross-service style)
    @Column(nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    private AttachmentType type;

    @Column(nullable = false)
    private String fileUrl;

    private LocalDateTime expiryDate;

    private boolean verified;

    /* This is the JSONB field for 7.2.2.
       It stores: file size, format, version, and notes.
    */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}
