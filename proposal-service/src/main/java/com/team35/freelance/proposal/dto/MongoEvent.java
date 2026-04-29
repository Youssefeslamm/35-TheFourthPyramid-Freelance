package com.team35.freelance.proposal.dto;

import java.time.LocalDateTime;
import java.util.Map;

public interface MongoEvent {
    String getId();

    LocalDateTime getTimestamp();

    String getAction();

    Map<String, Object> getDetails();
}