package com.team35.freelance.proposal.model.neo4j;

import org.springframework.data.neo4j.core.schema.*;
import java.time.LocalDateTime;

@RelationshipProperties
public class ProposedToRelationship {

    @Id @GeneratedValue
    private Long id;

    @Property("proposalCount")
    private Integer proposalCount = 1;

    @Property("lastProposalDate")
    private LocalDateTime lastProposalDate;

    @Property("recordedProposalIdsStr")
    private String recordedProposalIdsStr = "";

    @TargetNode
    private JobNode job;

    public ProposedToRelationship() {}

    public ProposedToRelationship(JobNode job, Long firstProposalId) {
        this.job = job;
        this.proposalCount = 1;
        this.lastProposalDate = LocalDateTime.now();
        this.recordedProposalIdsStr = String.valueOf(firstProposalId);
    }

    public boolean hasRecorded(Long proposalId) {
        if (recordedProposalIdsStr == null || recordedProposalIdsStr.isBlank()) return false;
        return java.util.Arrays.asList(recordedProposalIdsStr.split(","))
                .contains(String.valueOf(proposalId));
    }

    public void addRecordedProposalId(Long proposalId) {
        if (recordedProposalIdsStr == null || recordedProposalIdsStr.isBlank()) {
            recordedProposalIdsStr = String.valueOf(proposalId);
        } else {
            recordedProposalIdsStr += "," + proposalId;
        }
    }

    public Long getId() { return id; }
    public Integer getProposalCount() { return proposalCount; }
    public void setProposalCount(Integer proposalCount) { this.proposalCount = proposalCount; }
    public LocalDateTime getLastProposalDate() { return lastProposalDate; }
    public void setLastProposalDate(LocalDateTime lastProposalDate) { this.lastProposalDate = lastProposalDate; }
    public String getRecordedProposalIdsStr() { return recordedProposalIdsStr; }
    public void setRecordedProposalIdsStr(String s) { this.recordedProposalIdsStr = s; }
    public JobNode getJob() { return job; }
    public void setJob(JobNode job) { this.job = job; }
}