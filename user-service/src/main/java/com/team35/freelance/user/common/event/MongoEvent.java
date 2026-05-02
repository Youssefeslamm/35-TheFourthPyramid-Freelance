package com.team35.freelance.user.common.event;

import java.util.Map;

public interface MongoEvent {
    String getId();
    String getTimestamp();
    String getAction();
    Map<String, Object> getDetails();
}