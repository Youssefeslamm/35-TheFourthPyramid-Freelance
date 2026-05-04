package com.team35.freelance.job.common.adapter;

import com.team35.freelance.job.dto.JobAttachmentAlertDTO;
import com.team35.freelance.job.dto.JobProposalSummaryDTO;
import com.team35.freelance.job.dto.TopBudgetJobDTO;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;

import java.util.List;
import java.util.Map;

public class ElasticsearchHitAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Object hit, Class<T> targetType) {
        if (hit == null) {
            return null;
        }

        Map<String, Object> source = extractSource(hit);
        if (source == null) {
            source = new java.util.HashMap<>();
        }

        if (targetType == JobProposalSummaryDTO.class) {
            JobProposalSummaryDTO.Builder builder = JobProposalSummaryDTO.builder();

            Object jobId = source.get("jobId");
            if (jobId instanceof Number) {
                builder.jobId(((Number) jobId).longValue());
            }

            Object title = source.get("title");
            if (title instanceof String) {
                builder.title((String) title);
            }

            Object total = source.get("totalProposals");
            if (total instanceof Number) {
                builder.totalProposals(((Number) total).longValue());
            }

            Object average = source.get("averageBidAmount");
            if (average instanceof Number) {
                builder.averageBidAmount(((Number) average).doubleValue());
            }

            Object lowest = source.get("lowestBid");
            if (lowest instanceof Number) {
                builder.lowestBid(((Number) lowest).doubleValue());
            }

            Object highest = source.get("highestBid");
            if (highest instanceof Number) {
                builder.highestBid(((Number) highest).doubleValue());
            }

            return (T) builder.build();
        }

        if (targetType == TopBudgetJobDTO.class) {
            TopBudgetJobDTO dto = new TopBudgetJobDTO();

            Object jobId = source.get("jobId");
            if (jobId instanceof Number) {
                dto.setJobId(((Number) jobId).longValue());
            }

            Object title = source.get("title");
            if (title instanceof String) {
                dto.setTitle((String) title);
            }

            Object budgetMax = source.get("budgetMax");
            if (budgetMax instanceof Number) {
                dto.setBudgetMax(((Number) budgetMax).doubleValue());
            }

            Object totalProposals = source.get("totalProposals");
            if (totalProposals instanceof Number) {
                dto.setTotalProposals(((Number) totalProposals).longValue());
            }

            return (T) dto;
        }

        if (targetType == JobAttachmentAlertDTO.class) {
            JobAttachmentAlertDTO.Builder builder = JobAttachmentAlertDTO.builder();

            Object jobId = source.get("jobId");
            if (jobId instanceof Number) {
                builder.jobId(((Number) jobId).longValue());
            }

            Object title = source.get("jobTitle");
            if (title instanceof String) {
                builder.jobTitle((String) title);
            }

            Object status = source.get("jobStatus");
            if (status instanceof String) {
                builder.jobStatus(JobStatus.valueOf((String) status));
            }

            Object attachments = source.get("expiredAttachments");
            if (attachments instanceof List<?>) {
                builder.expiredAttachments((List<JobAttachment>) attachments);
            }

            Object expiredCount = source.get("expiredCount");
            if (expiredCount instanceof Number) {
                builder.expiredCount(((Number) expiredCount).intValue());
            }

            return (T) builder.build();
        }

        return buildEmpty(targetType);
    }

    private Map<String, Object> extractSource(Object hit) {
        if (hit instanceof Map<?, ?>) {
            return (Map<String, Object>) hit;
        }

        try {
            Object source = hit.getClass().getMethod("getSourceAsMap").invoke(hit);
            if (source instanceof Map<?, ?>) {
                return (Map<String, Object>) source;
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