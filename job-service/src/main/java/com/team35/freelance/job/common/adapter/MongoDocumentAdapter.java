package com.team35.freelance.job.common.adapter;

import com.team35.freelance.job.dto.JobAttachmentAlertDTO;
import com.team35.freelance.job.dto.JobProposalSummaryDTO;
import com.team35.freelance.job.dto.TopBudgetJobDTO;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;
import org.bson.Document;

import java.util.List;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Document mongoDocument, Class<T> targetType) {
        if (mongoDocument == null) {
            return null;
        }

        if (targetType == JobProposalSummaryDTO.class) {
            JobProposalSummaryDTO.Builder builder = JobProposalSummaryDTO.builder();

            Number jobId = (Number) mongoDocument.get("jobId");
            if (jobId != null) {
                builder.jobId(jobId.longValue());
            }

            String title = mongoDocument.getString("title");
            if (title != null) {
                builder.title(title);
            }

            Number total = (Number) mongoDocument.get("totalProposals");
            if (total != null) {
                builder.totalProposals(total.longValue());
            }

            Double average = mongoDocument.getDouble("averageBidAmount");
            if (average != null) {
                builder.averageBidAmount(average);
            }

            Double lowest = mongoDocument.getDouble("lowestBid");
            if (lowest != null) {
                builder.lowestBid(lowest);
            }

            Double highest = mongoDocument.getDouble("highestBid");
            if (highest != null) {
                builder.highestBid(highest);
            }

            return (T) builder.build();
        }

        if (targetType == TopBudgetJobDTO.class) {
            TopBudgetJobDTO dto = new TopBudgetJobDTO();

            Number jobId = (Number) mongoDocument.get("jobId");
            if (jobId != null) {
                dto.setJobId(jobId.longValue());
            }

            String title = mongoDocument.getString("title");
            if (title != null) {
                dto.setTitle(title);
            }

            Double budgetMax = mongoDocument.getDouble("budgetMax");
            if (budgetMax != null) {
                dto.setBudgetMax(budgetMax);
            }

            Number totalProposals = (Number) mongoDocument.get("totalProposals");
            if (totalProposals != null) {
                dto.setTotalProposals(totalProposals.longValue());
            }

            return (T) dto;
        }

        if (targetType == JobAttachmentAlertDTO.class) {
            JobAttachmentAlertDTO.Builder builder = JobAttachmentAlertDTO.builder();

            Number jobId = (Number) mongoDocument.get("jobId");
            if (jobId != null) {
                builder.jobId(jobId.longValue());
            }

            String title = mongoDocument.getString("jobTitle");
            if (title != null) {
                builder.jobTitle(title);
            }

            String status = mongoDocument.getString("jobStatus");
            if (status != null) {
                builder.jobStatus(JobStatus.valueOf(status));
            }

            Object attachments = mongoDocument.get("expiredAttachments");
            if (attachments instanceof List<?>) {
                builder.expiredAttachments((List<JobAttachment>) attachments);
            }

            Number expiredCount = (Number) mongoDocument.get("expiredCount");
            if (expiredCount != null) {
                builder.expiredCount(expiredCount.intValue());
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
