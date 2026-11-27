package com.cramless.model;

import java.time.LocalDateTime;

public class TopicProgress {
    private final LocalDateTime completedAt;
    private final Integer actualTimeSpent; // minutes, optional

    public TopicProgress(LocalDateTime completedAt, Integer actualTimeSpent) {
        this.completedAt = completedAt;
        this.actualTimeSpent = actualTimeSpent;
    }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public Integer getActualTimeSpent() { return actualTimeSpent; }
    public boolean isCompleted() { return completedAt != null; }
}
