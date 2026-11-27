package com.cramless.model;

import java.time.LocalTime;

public class AvailabilitySlot {
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String slotType;

    public AvailabilitySlot(LocalTime startTime, LocalTime endTime, String slotType) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotType = slotType;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getSlotType() { return slotType; }
}
