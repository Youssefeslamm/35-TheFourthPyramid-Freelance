package com.team35.freelance.wallet.common.observer;

public interface EntityObserver {
    void onEvent(String eventType, Object payload);
}