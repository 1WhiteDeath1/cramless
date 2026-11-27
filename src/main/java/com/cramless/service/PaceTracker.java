package com.cramless.service;

import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.model.StudyAvailability;
import com.cramless.util.AppClock;
import java.time.LocalDate;
import java.util.List;

/** Compares finished topics with the topics the syllabus says should be finished by now. */
public class PaceTracker {
    private final double behindThreshold = 10; // percent of due topics left unfinished

    public String getPaceStatus(Course c, LocalDate today) {
        return status(c.countDueTopics(today), c.countCompletedTopics());
    }

    /** Positive = topics ahead of the syllabus, negative = topics behind. */
    public int getTopicsAheadOrBehind(Course c, LocalDate today) {
        return c.countCompletedTopics() - c.countDueTopics(today);
    }

    public String getOverallStatus(List<Course> courses) {
        LocalDate today = AppClock.today();
        int due = 0, done = 0;
        for (Course c : courses) {
            due += c.countDueTopics(today);
            done += c.countCompletedTopics();
        }
        return status(due, done);
    }

    /** Hours per day, on top of the plan, needed to finish the overdue topics before the next assessment. */
    public double extraHoursPerDay(Course c, List<StudyAvailability> avail) {
        LocalDate today = AppClock.today();
        double hoursNeeded = c.getOverdueTopics(today).size(); // about one hour per topic
        if (hoursNeeded == 0) return 0;

        LocalDate next = null;
        for (Assessment a : c.getAssessments()) {
            if (a.getDate() != null && !a.getDate().isBefore(today) && (next == null || a.getDate().isBefore(next))) {
                next = a.getDate();
            }
        }
        if (next == null) next = today.plusDays(7);

        int studyDays = 0;
        for (LocalDate d = today; d.isBefore(next); d = d.plusDays(1)) {
            for (StudyAvailability a : avail) {
                if (a.getDayOfWeek().equals(d.getDayOfWeek().name()) && !a.isDayOff()) studyDays++;
            }
        }
        double perDay = hoursNeeded / Math.max(1, studyDays);
        return Math.round(perDay * 100.0) / 100.0;
    }

    private String status(int due, int done) {
        int unfinished = due - done;
        if (unfinished < 0) return "Ahead";
        if (due > 0 && unfinished * 100.0 / due > behindThreshold) return "Behind";
        return "On track";
    }

    public double getBehindThreshold() { return behindThreshold; }
}
