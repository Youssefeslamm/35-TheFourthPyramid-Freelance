package com.team35.freelance.proposal.common.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}