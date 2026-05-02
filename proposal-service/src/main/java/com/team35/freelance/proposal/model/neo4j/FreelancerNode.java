package com.team35.freelance.proposal.model.neo4j;

import org.springframework.data.neo4j.core.schema.*;
import java.util.ArrayList;
import java.util.List;

@Node("Freelancer")
public class FreelancerNode {

    @Id
    @Property("userId")
    private Long userId;

    @Property("name")
    private String name;

    @Relationship(type = "PROPOSED_TO", direction = Relationship.Direction.OUTGOING)
    private List<ProposedToRelationship> proposedTo = new ArrayList<>();

    public FreelancerNode() {}

    public FreelancerNode(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ProposedToRelationship> getProposedTo() { return proposedTo; }
    public void setProposedTo(List<ProposedToRelationship> proposedTo) { this.proposedTo = proposedTo; }
}