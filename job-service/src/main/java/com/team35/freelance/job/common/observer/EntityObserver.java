package com.team35.freelance.job.common.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}