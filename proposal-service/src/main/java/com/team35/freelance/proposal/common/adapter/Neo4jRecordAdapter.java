package com.team35.freelance.proposal.common.adapter;

import com.team35.freelance.proposal.dto.FeeEstimateDTO;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDTO;
import com.team35.freelance.proposal.dto.ProposalDetailsDTO;
import com.team35.freelance.proposal.model.ProposalStatus;

import java.util.Map;

public class Neo4jRecordAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Object neo4jRecord, Class<T> targetType) {
        if (neo4jRecord == null) {
            return null;
        }

        Map<String, Object> source = extractSource(neo4jRecord);
        if (source == null) {
            source = new java.util.HashMap<>();
        }

        if (targetType == FeeEstimateDTO.class) {
            FeeEstimateDTO.Builder builder = FeeEstimateDTO.builder();

            Object bidAmount = source.get("bidAmount");
            if (bidAmount instanceof Number) {
                builder.bidAmount(((Number) bidAmount).doubleValue());
            }

            Object platformFee = source.get("platformFee");
            if (platformFee instanceof Number) {
                builder.platformFee(((Number) platformFee).doubleValue());
            }

            Object freelancerPayout = source.get("freelancerPayout");
            if (freelancerPayout instanceof Number) {
                builder.freelancerPayout(((Number) freelancerPayout).doubleValue());
            }

            Object feePercentage = source.get("feePercentage");
            if (feePercentage instanceof Number) {
                builder.feePercentage(((Number) feePercentage).doubleValue());
            }

            Object estimatedDailyRate = source.get("estimatedDailyRate");
            if (estimatedDailyRate instanceof Number) {
                builder.estimatedDailyRate(((Number) estimatedDailyRate).doubleValue());
            }

            return (T) builder.build();
        }

        if (targetType == ProposalAnalyticsDTO.class) {
            ProposalAnalyticsDTO.Builder builder = ProposalAnalyticsDTO.builder();

            Object total = source.get("totalProposals");
            if (total instanceof Number) {
                builder.totalProposals(((Number) total).longValue());
            }

            Object accepted = source.get("acceptedProposals");
            if (accepted instanceof Number) {
                builder.acceptedProposals(((Number) accepted).longValue());
            }

            Object rejected = source.get("rejectedProposals");
            if (rejected instanceof Number) {
                builder.rejectedProposals(((Number) rejected).longValue());
            }

            Object totalBid = source.get("totalBidValue");
            if (totalBid instanceof Number) {
                builder.totalBidValue(((Number) totalBid).doubleValue());
            }

            Object averageBid = source.get("averageBid");
            if (averageBid instanceof Number) {
                builder.averageBid(((Number) averageBid).doubleValue());
            }

            Object acceptanceRate = source.get("acceptanceRate");
            if (acceptanceRate instanceof Number) {
                builder.acceptanceRate(((Number) acceptanceRate).doubleValue());
            }

            return (T) builder.build();
        }

        if (targetType == ProposalDetailsDTO.class) {
            ProposalDetailsDTO.Builder builder = ProposalDetailsDTO.builder();

            Object proposalId = source.get("proposalId");
            if (proposalId instanceof Number) {
                builder.proposalId(((Number) proposalId).longValue());
            }

            Object jobId = source.get("jobId");
            if (jobId instanceof Number) {
                builder.jobId(((Number) jobId).longValue());
            }

            Object freelancerId = source.get("freelancerId");
            if (freelancerId instanceof Number) {
                builder.freelancerId(((Number) freelancerId).longValue());
            }

            Object status = source.get("status");
            if (status instanceof String) {
                builder.status(ProposalStatus.valueOf((String) status));
            }

            Object bidAmount = source.get("bidAmount");
            if (bidAmount instanceof Number) {
                builder.bidAmount(((Number) bidAmount).doubleValue());
            }

            Object totalMilestones = source.get("totalMilestones");
            if (totalMilestones instanceof Number) {
                builder.totalMilestones(((Number) totalMilestones).intValue());
            }

            Object completedMilestones = source.get("completedMilestones");
            if (completedMilestones instanceof Number) {
                builder.completedMilestones(((Number) completedMilestones).longValue());
            }

            return (T) builder.build();
        }

        return buildEmpty(targetType);
    }

    private Map<String, Object> extractSource(Object record) {
        if (record instanceof Map<?, ?>) {
            return (Map<String, Object>) record;
        }

        try {
            Object value = record.getClass().getMethod("asMap").invoke(record);
            if (value instanceof Map<?, ?>) {
                return (Map<String, Object>) value;
            }
        } catch (Exception ignored) {
        }

        return null;
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