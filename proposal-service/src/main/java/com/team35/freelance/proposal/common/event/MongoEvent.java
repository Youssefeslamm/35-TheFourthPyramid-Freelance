package com.team35.freelance.proposal.common.event;

import java.time.Instant;
import java.util.Map;

public interface MongoEvent {

    String getId();

    Instant getTimestamp();

    String getAction();

    Map<String, Object> getDetails();
}