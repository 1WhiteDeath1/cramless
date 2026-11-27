package com.cramless.model;

import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * One week-wise topic of a course. A topic is covered by every assessment dated on or after the
 * start of its week; an assessment's weightage is shared equally between the topics it covers.
 */
public class Topic {
    private final Course course;
    private final int weekNumber;
    private final String title;
    private TopicProgress progress; // 0..1

    public Topic(Course course, int weekNumber, String title) {
        this.course = course;
        this.weekNumber = weekNumber;
        this.title = title;
    }

    public Course getCourse() { return course; }
    public int getWeekNumber() { return weekNumber; }
    public String getTitle() { return title; }

    /** Unique, readable key such as "Data Structures :: Linked Lists". */
    public String getKey() { return course.getName() + " :: " + title; }

    public LocalDate getWeekStart() { return course.getWeek1Date().plusDays(7L * (weekNumber - 1)); }

    /** The date by which the plan expects this topic to be finished (end of its week). */
    public LocalDate getPlannedByDate() { return getWeekStart().plusDays(6); }

    /** Share of the final grade that is still open for this topic (percentage points). */
    public double getWeightage() {
        double total = 0;
        for (Assessment a : course.getAssessmentsCovering(this, false)) {
            int covered = Math.max(1, course.countTopicsCoveredBy(a));
            total += a.getWeightage() / covered;
        }
        return total;
    }

    /** Date of the nearest upcoming assessment that covers this topic, or null if none. */
    public LocalDate getDueDate() {
        LocalDate today = AppClock.today();
        LocalDate best = null;
        for (Assessment a : course.getAssessmentsCovering(this, false)) {
            if (a.getDate() == null || a.getDate().isBefore(today)) continue;
            if (best == null || a.getDate().isBefore(best)) best = a.getDate();
        }
        return best;
    }

    public int getDaysLeft(LocalDate today) {
        LocalDate due = getDueDate();
        if (due == null) return 365;
        return (int) Math.max(0, ChronoUnit.DAYS.between(today, due));
    }

    /** 0 (not weak) to 1 (very weak), from the scores of graded assessments that cover this topic. */
    public double getWeaknessLevel() {
        List<Assessment> graded = course.getAssessmentsCovering(this, true);
        if (graded.isEmpty()) return 0;
        double sum = 0;
        for (Assessment a : graded) sum += a.getScore().getPercentage();
        double avg = sum / graded.size();
        double threshold = course.getOwner() == null ? 60 : course.getOwner().getSettings().getWeakScoreThreshold();
        return avg >= threshold ? 0 : (100.0 - avg) / 100.0;
    }

    /** Text such as "Quiz 1 scored 45%" for the most recent graded assessment that covers this topic, or null. */
    public String getLatestScoreNote() {
        Assessment latest = null;
        for (Assessment a : course.getAssessmentsCovering(this, true)) {
            if (latest == null || a.getScore().getDate().isAfter(latest.getScore().getDate())) latest = a;
        }
        if (latest == null) return null;
        return latest.getName() + " scored " + Math.round(latest.getScore().getPercentage()) + "%";
    }

    /** Creator: a Topic creates its own TopicProgress when it is completed. */
    public TopicProgress complete(Integer actualTimeSpent) {
        if (isCompleted()) throw new ValidationException("\"" + title + "\" is already marked as completed.");
        if (actualTimeSpent != null && actualTimeSpent < 0) {
            throw new ValidationException("Time spent cannot be negative.");
        }
        progress = new TopicProgress(AppClock.now(), actualTimeSpent);
        return progress;
    }

    /** UC-05 extension *a: the same day, the completion can be undone and the topic is pending again. */
    public void undoCompletion(LocalDate today) {
        if (!isCompleted()) throw new ValidationException("\"" + title + "\" is not completed.");
        if (!progress.getCompletedAt().toLocalDate().equals(today)) {
            throw new ValidationException("A completion can only be undone on the day it was recorded.");
        }
        progress = null;
    }

    /** Used when loading from the database. */
    public void restoreProgress(LocalDateTime completedAt, Integer actualTimeSpent) {
        progress = new TopicProgress(completedAt, actualTimeSpent);
    }

    public TopicProgress getProgress() { return progress; }
    public boolean isCompleted() { return progress != null && progress.isCompleted(); }

    /** Points for finishing this topic: 10, plus up to 10 more for topics worth more marks. */
    public int getPointsReward() {
        return 10 + (int) Math.min(10, Math.round(getWeightage()));
    }

    @Override
    public String toString() { return title; }
}
