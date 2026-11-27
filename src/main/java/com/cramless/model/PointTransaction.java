package com.cramless.model;

import java.time.LocalDateTime;

public class PointTransaction {
    private final int pointsChange;
    private final String reason;
    private final LocalDateTime transactionDateTime;
    private final String source; // topic or score this reward came from (optional)

    public PointTransaction(int pointsChange, String reason, LocalDateTime when, String source) {
        this.pointsChange = pointsChange;
        this.reason = reason;
        this.transactionDateTime = when;
        this.source = source;
    }

    public int getPointsChange() { return pointsChange; }
    public String getReason() { return reason; }
    public LocalDateTime getTransactionDateTime() { return transactionDateTime; }
    public String getSource() { return source; }
}
