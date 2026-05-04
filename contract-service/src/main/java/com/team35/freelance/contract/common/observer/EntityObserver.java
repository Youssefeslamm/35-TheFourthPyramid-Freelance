package com.team35.freelance.contract.common.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}

