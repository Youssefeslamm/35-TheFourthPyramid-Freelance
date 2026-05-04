package com.team35.freelance.user.dto;

import java.util.List;
import java.util.Map;

public class ActivityFeedResponseDTO {

    private List<ActivityEventDTO> content;
    private int page;
    private int size;
    private long totalElements;

    public ActivityFeedResponseDTO() {}

    public ActivityFeedResponseDTO(List<ActivityEventDTO> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }

    public List<ActivityEventDTO> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }

    // ===================== INNER EVENT DTO =====================

    public static class ActivityEventDTO {
        private String action;
        private String timestamp;
        private Map<String, Object> details;

        public ActivityEventDTO() {}

        public ActivityEventDTO(String action, String timestamp, Map<String, Object> details) {
            this.action = action;
            this.timestamp = timestamp;
            this.details = details;
        }

        public String getAction() { return action; }
        public String getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return details; }
    }
}

