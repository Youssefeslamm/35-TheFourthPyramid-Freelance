package com.team35.freelance.user.common.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}