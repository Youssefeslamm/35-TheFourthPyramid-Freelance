package com.team35.freelance.proposal.neo4j;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
public class ProposedToRelationship {

    private Integer proposalCount;
    private LocalDateTime lastProposalDate;

    @TargetNode
    private JobNode job;

    public ProposedToRelationship() {
    }

    public ProposedToRelationship(Integer proposalCount, LocalDateTime lastProposalDate, JobNode job) {
        this.proposalCount = proposalCount;
        this.lastProposalDate = lastProposalDate;
        this.job = job;
    }

    public Integer getProposalCount() {
        return proposalCount;
    }

    public LocalDateTime getLastProposalDate() {
        return lastProposalDate;
    }

    public JobNode getJob() {
        return job;
    }

    public void setProposalCount(Integer proposalCount) {
        this.proposalCount = proposalCount;
    }

    public void setLastProposalDate(LocalDateTime lastProposalDate) {
        this.lastProposalDate = lastProposalDate;
    }

    public void setJob(JobNode job) {
        this.job = job;
    }
}