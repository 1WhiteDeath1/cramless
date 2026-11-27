package com.cramless.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The semester-wide plan: a list of study sessions across all of the student's courses. */
public class StudyPlan {
    public static final String ACTIVE = "active";
    public static final String OUTDATED = "outdated";

    private final LocalDateTime generatedAt;
    private String status = ACTIVE;
    private final List<StudySession> sessions = new ArrayList<>();
    private Student owner;

    public StudyPlan(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    /** Creator: the plan contains its sessions, so it creates them. */
    public StudySession addSession(Topic topic, LocalDate date, LocalTime startTime, int duration,
                                   double priorityScore, String reason) {
        StudySession s = new StudySession(topic, date, startTime, duration, priorityScore, reason);
        sessions.add(s);
        return s;
    }

    public List<StudySession> getSessionsFor(LocalDate date) {
        List<StudySession> out = new ArrayList<>();
        for (StudySession s : sessions) if (s.getScheduledDate().equals(date)) out.add(s);
        out.sort(Comparator.comparing(StudySession::getStartTime));
        return out;
    }

    public StudySession getSession(LocalDate date, LocalTime startTime) {
        for (StudySession s : sessions) {
            if (s.getScheduledDate().equals(date) && s.getStartTime().equals(startTime)) return s;
        }
        return null;
    }

    /** Minutes planned on a date. */
    public int getTotalPlannedTime(LocalDate date) {
        int total = 0;
        for (StudySession s : sessions) if (s.getScheduledDate().equals(date)) total += s.getPlannedDuration();
        return total;
    }

    public int getDoneTime(LocalDate date) {
        int total = 0;
        for (StudySession s : sessions) if (s.getScheduledDate().equals(date) && s.isDone()) total += s.getPlannedDuration();
        return total;
    }

    public void markOutdated() { status = OUTDATED; }

    /** Planned sessions before today that were not finished (carried over). */
    public List<StudySession> getCarriedOver(LocalDate today) {
        List<StudySession> out = new ArrayList<>();
        for (StudySession s : sessions) {
            if (s.getScheduledDate().isBefore(today) && !s.isDone()) out.add(s);
        }
        out.sort(Comparator.comparing(StudySession::getScheduledDate).thenComparing(StudySession::getStartTime));
        return out;
    }

    /** Drops every not-yet-done session from the given date on (used when the plan is rebalanced). */
    public void removePlannedFrom(LocalDate date) {
        sessions.removeIf(s -> !s.isDone() && !s.getScheduledDate().isBefore(date));
    }

    /** A completed topic needs no more sessions: past ones count as done, future ones are dropped. */
    public void completeSessionsFor(Topic topic, LocalDate today) {
        sessions.removeIf(s -> s.getTopic() == topic && s.getScheduledDate().isAfter(today));
        for (StudySession s : sessions) if (s.getTopic() == topic) s.setStatus(StudySession.DONE);
    }

    /** Undo of a completion: today's sessions of the topic are planned again. */
    public void reopenSessionsFor(Topic topic, LocalDate today) {
        for (StudySession s : sessions) {
            if (s.getTopic() == topic && s.getScheduledDate().equals(today)) s.setStatus(StudySession.PLANNED);
        }
    }

    public LocalDate getFirstDate() {
        LocalDate d = null;
        for (StudySession s : sessions) if (d == null || s.getScheduledDate().isBefore(d)) d = s.getScheduledDate();
        return d;
    }

    public LocalDate getLastDate() {
        LocalDate d = null;
        for (StudySession s : sessions) if (d == null || s.getScheduledDate().isAfter(d)) d = s.getScheduledDate();
        return d;
    }

    /** The first planned session from today on, used when nothing is planned for the shown day (UC-07 4a). */
    public StudySession getNextSession(LocalDate today) {
        StudySession best = null;
        for (StudySession s : sessions) {
            if (s.getScheduledDate().isBefore(today) || s.isDone()) continue;
            if (best == null || s.getScheduledDate().isBefore(best.getScheduledDate())
                    || (s.getScheduledDate().equals(best.getScheduledDate()) && s.getStartTime().isBefore(best.getStartTime()))) {
                best = s;
            }
        }
        return best;
    }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isOutdated() { return OUTDATED.equals(status); }
    public List<StudySession> getSessions() { return sessions; }
    public Student getOwner() { return owner; }
    void setOwner(Student owner) { this.owner = owner; }
}
