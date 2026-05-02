package com.team35.freelance.proposal.model.neo4j;

import org.springframework.data.neo4j.core.schema.*;

@Node("Job")
public class JobNode {

    @Id
    @Property("jobId")
    private Long jobId;

    @Property("title")
    private String title;

    @Property("category")
    private String category;

    public JobNode() {}

    public JobNode(Long jobId, String title, String category) {
        this.jobId = jobId;
        this.title = title;
        this.category = category;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}