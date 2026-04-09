package com.team35.freelance.proposal.dto;
import com.team35.freelance.proposal.model.ProposalStatus;
import java.util.List;
import java.util.Map;
public class ProposalDetailsDTO {
    private Long proposalId;
    private Long jobId;
    private Long freelancerId;
    private ProposalStatus status;
    private Double bidAmount;
    private Map<String, Object> metadata;
    private List<MilestoneDTO> milestones;
    private int totalMilestones;
    private long completedMilestones;

    public ProposalDetailsDTO(Long proposalId, Long jobId, Long freelancerId,
                              ProposalStatus status, Double bidAmount,
                              Map<String, Object> metadata,
                              List<MilestoneDTO> milestones,
                              int totalMilestones, long completedMilestones) {
        this.proposalId = proposalId;
        this.jobId = jobId;
        this.freelancerId = freelancerId;
        this.status = status;
        this.bidAmount = bidAmount;
        this.metadata = metadata;
        this.milestones = milestones;
        this.totalMilestones = totalMilestones;
        this.completedMilestones = completedMilestones;
    }
    public Long getProposalId() { return proposalId; }
    public Long getJobId() { return jobId; }
    public Long getFreelancerId() { return freelancerId; }
    public ProposalStatus getStatus() { return status; }
    public Double getBidAmount() { return bidAmount; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<MilestoneDTO> getMilestones() { return milestones; }
    public int getTotalMilestones() { return totalMilestones; }
    public long getCompletedMilestones() { return completedMilestones; }
}
