package com.team35.freelance.job.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "jobs")
public class JobSearchDocument {

    @Id
    private Long id;

    private String title;
    private String description;
    private String category;
    private Double budgetMin;
    private Double budgetMax;
    private Double rating;
    private String status;

    public JobSearchDocument() {
    }

    public JobSearchDocument(Long id, String title, String description, String category,
                             Double budgetMin, Double budgetMax, Double rating, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.rating = rating;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public Double getRating() {
        return rating;
    }

    public String getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}