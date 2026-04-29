package com.team35.freelance.contract.document;

import com.team35.freelance.contract.dto.ContractAnalyticsDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "contract_analytics_snapshots")
public class ContractAnalyticsSnapshot {

    @Id
    private String id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;
    private ContractAnalyticsDTO analytics;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public ContractAnalyticsDTO getAnalytics() {
        return analytics;
    }

    public void setAnalytics(ContractAnalyticsDTO analytics) {
        this.analytics = analytics;
    }
}
