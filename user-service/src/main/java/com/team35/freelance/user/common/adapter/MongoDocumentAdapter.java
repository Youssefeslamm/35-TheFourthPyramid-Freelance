package com.team35.freelance.user.common.adapter;

import com.team35.freelance.user.dto.TopFreelancerDTO;
import com.team35.freelance.user.dto.UserContractSummaryDTO;
import com.team35.freelance.user.dto.UserProfileDTO;
import com.team35.freelance.user.dto.UserSkillProfileDTO;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Document mongoDocument, Class<T> targetType) {
        if (mongoDocument == null) {
            return null;
        }

        if (targetType == UserProfileDTO.class) {
            UserProfileDTO.Builder builder = UserProfileDTO.builder();

            Number userId = (Number) mongoDocument.get("userId");
            if (userId != null) {
                builder.userId(userId.longValue());
            }

            String name = mongoDocument.getString("name");
            if (name != null) {
                builder.name(name);
            }

            String email = mongoDocument.getString("email");
            if (email != null) {
                builder.email(email);
            }

            String phone = mongoDocument.getString("phone");
            if (phone != null) {
                builder.phone(phone);
            }

            Object preferences = mongoDocument.get("preferences");
            if (preferences instanceof Map<?, ?>) {
                builder.preferences((Map<String, Object>) preferences);
            }

            Object skills = mongoDocument.get("skills");
            if (skills instanceof List<?>) {
                builder.skills((List<UserSkillProfileDTO>) skills);
                builder.totalSkills(((List<?>) skills).size());
            }

            return (T) builder.build();
        }

        if (targetType == UserContractSummaryDTO.class) {
            UserContractSummaryDTO.Builder builder = UserContractSummaryDTO.builder();

            Number userId = (Number) mongoDocument.get("userId");
            if (userId != null) {
                builder.userId(userId.longValue());
            }

            String name = mongoDocument.getString("name");
            if (name != null) {
                builder.name(name);
            }

            Number totalContracts = (Number) mongoDocument.get("totalContracts");
            if (totalContracts != null) {
                builder.totalContracts(totalContracts.longValue());
            }

            Number completedContracts = (Number) mongoDocument.get("completedContracts");
            if (completedContracts != null) {
                builder.completedContracts(completedContracts.longValue());
            }

            Number terminatedContracts = (Number) mongoDocument.get("terminatedContracts");
            if (terminatedContracts != null) {
                builder.terminatedContracts(terminatedContracts.longValue());
            }

            Double totalEarnings = mongoDocument.getDouble("totalEarnings");
            if (totalEarnings != null) {
                builder.totalEarnings(totalEarnings);
            }

            Double averageContractValue = mongoDocument.getDouble("averageContractValue");
            if (averageContractValue != null) {
                builder.averageContractValue(averageContractValue);
            }

            return (T) builder.build();
        }

        if (targetType == TopFreelancerDTO.class) {
            TopFreelancerDTO.Builder builder = TopFreelancerDTO.builder();

            Number userId = (Number) mongoDocument.get("userId");
            if (userId != null) {
                builder.userId(userId.longValue());
            }

            String name = mongoDocument.getString("name");
            if (name != null) {
                builder.name(name);
            }

            Double totalEarnings = mongoDocument.getDouble("totalEarnings");
            if (totalEarnings != null) {
                builder.totalEarnings(totalEarnings);
            }

            Number contractCount = (Number) mongoDocument.get("contractCount");
            if (contractCount != null) {
                builder.contractCount(contractCount.longValue());
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

