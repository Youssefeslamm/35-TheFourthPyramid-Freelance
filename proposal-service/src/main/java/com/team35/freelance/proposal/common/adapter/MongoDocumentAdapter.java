package com.team35.freelance.proposal.common.adapter;

import com.team35.freelance.proposal.dto.FeeEstimateDTO;
import com.team35.freelance.proposal.dto.MilestoneDTO;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team35.freelance.proposal.dto.ProposalDetailsDTO;
import com.team35.freelance.proposal.model.ProposalStatus;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Document mongoDocument, Class<T> targetType) {
        if (mongoDocument == null) {
            return null;
        }

        if (targetType == FeeEstimateDTO.class) {
            FeeEstimateDTO.Builder builder = FeeEstimateDTO.builder();

            Double bidAmount = mongoDocument.getDouble("bidAmount");
            if (bidAmount != null) {
                builder.bidAmount(bidAmount);
            }

            Double platformFee = mongoDocument.getDouble("platformFee");
            if (platformFee != null) {
                builder.platformFee(platformFee);
            }

            Double freelancerPayout = mongoDocument.getDouble("freelancerPayout");
            if (freelancerPayout != null) {
                builder.freelancerPayout(freelancerPayout);
            }

            Double feePercentage = mongoDocument.getDouble("feePercentage");
            if (feePercentage != null) {
                builder.feePercentage(feePercentage);
            }

            Double estimatedDailyRate = mongoDocument.getDouble("estimatedDailyRate");
            if (estimatedDailyRate != null) {
                builder.estimatedDailyRate(estimatedDailyRate);
            }

            return (T) builder.build();
        }

        if (targetType == ProposalAnalyticsDTO.class) {
            ProposalAnalyticsDTO.Builder builder = ProposalAnalyticsDTO.builder();

            Number total = (Number) mongoDocument.get("totalProposals");
            if (total != null) {
                builder.totalProposals(total.longValue());
            }

            Number accepted = (Number) mongoDocument.get("acceptedProposals");
            if (accepted != null) {
                builder.acceptedProposals(accepted.longValue());
            }

            Number rejected = (Number) mongoDocument.get("rejectedProposals");
            if (rejected != null) {
                builder.rejectedProposals(rejected.longValue());
            }

            Double totalBid = mongoDocument.getDouble("totalBidValue");
            if (totalBid != null) {
                builder.totalBidValue(totalBid);
            }

            Double averageBid = mongoDocument.getDouble("averageBid");
            if (averageBid != null) {
                builder.averageBid(averageBid);
            }

            Double acceptanceRate = mongoDocument.getDouble("acceptanceRate");
            if (acceptanceRate != null) {
                builder.acceptanceRate(acceptanceRate);
            }

            return (T) builder.build();
        }

        if (targetType == ProposalDetailsDTO.class) {
            ProposalDetailsDTO.Builder builder = ProposalDetailsDTO.builder();

            Number proposalId = (Number) mongoDocument.get("proposalId");
            if (proposalId != null) {
                builder.proposalId(proposalId.longValue());
            }

            Number jobId = (Number) mongoDocument.get("jobId");
            if (jobId != null) {
                builder.jobId(jobId.longValue());
            }

            Number freelancerId = (Number) mongoDocument.get("freelancerId");
            if (freelancerId != null) {
                builder.freelancerId(freelancerId.longValue());
            }

            String status = mongoDocument.getString("status");
            if (status != null) {
                builder.status(ProposalStatus.valueOf(status));
            }

            Double bidAmount = mongoDocument.getDouble("bidAmount");
            if (bidAmount != null) {
                builder.bidAmount(bidAmount);
            }

            Object metadata = mongoDocument.get("metadata");
            if (metadata instanceof Map<?, ?>) {
                builder.metadata((Map<String, Object>) metadata);
            }

            Object milestones = mongoDocument.get("milestones");
            if (milestones instanceof List<?>) {
                builder.milestones((List<MilestoneDTO>) milestones);
            }

            Number totalMilestones = (Number) mongoDocument.get("totalMilestones");
            if (totalMilestones != null) {
                builder.totalMilestones(totalMilestones.intValue());
            }

            Number completedMilestones = (Number) mongoDocument.get("completedMilestones");
            if (completedMilestones != null) {
                builder.completedMilestones(completedMilestones.longValue());
            }

            return (T) builder.build();
        }

        return buildEmpty(targetType);
    }

    private <T> T buildEmpty(Class<T> targetType) {
        try {
            Object builder = targetType.getMethod("builder").invoke(null);
            return (T) builder.getClass().getMethod("build").invoke(builder);
        } catch (NoSuchMethodException ignored) {
            try {
                return targetType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Unsupported target type: " + targetType.getName(), e);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to build target type: " + targetType.getName(), e);
        }
    }
}
