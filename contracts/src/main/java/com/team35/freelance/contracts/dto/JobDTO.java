package com.team35.freelance.contracts.dto;

public class JobDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String status;
    private Double budgetMin;
    private Double budgetMax;
    private Long clientId;

    public JobDTO() {
    }

    public JobDTO(Long id,
                  String title,
                  String description,
                  String category,
                  String status,
                  Double budgetMin,
                  Double budgetMax,
                  Long clientId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBudgetMin() {
        return budgetMin;
    }

    public void setBudgetMin(Double budgetMin) {
        this.budgetMin = budgetMin;
    }

    public Double getBudgetMax() {
        return budgetMax;
    }

    public void setBudgetMax(Double budgetMax) {
        this.budgetMax = budgetMax;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}

