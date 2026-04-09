package com.team35.freelance.job.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data // If you use Lombok, otherwise generate Getters/Setters
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    // Required for 9.2.1 search: OPEN, IN_PROGRESS, etc.
    private String status;

    // Required for 9.2.1 budget filtering
    @Column(name = "budget_max")
    private Double budgetMax;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}