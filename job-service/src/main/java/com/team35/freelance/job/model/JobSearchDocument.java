package com.team35.freelance.job.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "jobs", createIndex = false)
public class JobSearchDocument {
    @Id
    private String id;
    private String title;
    private String description;
    private String category;
    private Double budgetMin;
    private Double budgetMax;
    private String status;

    public JobSearchDocument() {}

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setBudgetMin(Double budgetMin) { this.budgetMin = budgetMin; }
    public void setBudgetMax(Double budgetMax) { this.budgetMax = budgetMax; }
    public void setStatus(String status) { this.status = status; }

    // Manual Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Double getBudgetMin() { return budgetMin; }
    public Double getBudgetMax() { return budgetMax; }
    public String getStatus() { return status; }
}
