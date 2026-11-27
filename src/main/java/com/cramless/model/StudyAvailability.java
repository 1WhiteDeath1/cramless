package com.cramless.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** How many hours the student can study on one day of the week, and when the study block starts. */
public class StudyAvailability {
    public static final LocalTime DEFAULT_START = LocalTime.of(17, 0);

    private final String dayOfWeek; // MONDAY ... SUNDAY
    private double availableHours;
    private final List<AvailabilitySlot> slots = new ArrayList<>();

    public StudyAvailability(String dayOfWeek, double availableHours) {
        this(dayOfWeek, availableHours, DEFAULT_START);
    }

    public StudyAvailability(String dayOfWeek, double availableHours, LocalTime start) {
        this.dayOfWeek = dayOfWeek;
        setHours(availableHours, start);
    }

    public final void setHours(double hours, LocalTime start) {
        this.availableHours = hours;
        slots.clear();
        if (hours > 0) {
            slots.add(new AvailabilitySlot(start, start.plusMinutes(Math.round(hours * 60)), "study"));
        }
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public double getAvailableHours() { return availableHours; }
    public boolean isDayOff() { return availableHours <= 0; }
    public List<AvailabilitySlot> getSlots() { return slots; }

    public LocalTime getStartTime() {
        return slots.isEmpty() ? DEFAULT_START : slots.get(0).getStartTime();
    }
}
