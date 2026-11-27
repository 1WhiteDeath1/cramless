package com.cramless.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One block of study time (30 minutes by default) planned for a topic. */
public class StudySession {
    public static final String PLANNED = "planned";
    public static final String DONE = "done";

    private final Topic topic;
    private final LocalDate scheduledDate;
    private final LocalTime startTime;
    private final int plannedDuration; // minutes
    private final double priorityScore;
    private final String reason;
    private String status = PLANNED;

    public StudySession(Topic topic, LocalDate scheduledDate, LocalTime startTime, int plannedDuration,
                        double priorityScore, String reason) {
        this.topic = topic;
        this.scheduledDate = scheduledDate;
        this.startTime = startTime;
        this.plannedDuration = plannedDuration;
        this.priorityScore = priorityScore;
        this.reason = reason;
    }

    public Topic getTopic() { return topic; }
    public String getReason() { return reason; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public LocalTime getStartTime() { return startTime; }
    public int getPlannedDuration() { return plannedDuration; }
    public double getPriorityScore() { return priorityScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDone() { return DONE.equals(status); }
    public String getCourseName() { return topic.getCourse().getName(); }
}
