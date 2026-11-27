package com.cramless.model;

import com.cramless.util.DateRange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Course {
    private final String name;
    private final String courseCode; // optional
    private String section;          // optional
    private final LocalDate week1Date;
    private String targetGrade;      // optional
    private final List<Topic> topics = new ArrayList<>();
    private final List<Assessment> assessments = new ArrayList<>();
    private final GradeScale gradeScale = GradeScale.defaultScale();
    private Student owner;
    private String lastError = "";

    public Course(String name, String courseCode, LocalDate week1Date) {
        this.name = name;
        this.courseCode = courseCode;
        this.week1Date = week1Date;
    }

    // ---- building the course (Creator: a Course creates its Topics and Assessments) ----

    public Topic addTopic(int weekNumber, String title) {
        Topic t = new Topic(this, weekNumber, title);
        topics.add(t);
        topics.sort(Comparator.comparingInt(Topic::getWeekNumber));
        return t;
    }

    /** type: quiz, assignment, mid-term, final or exam. */
    public Assessment addAssessment(String type, String assessmentName, LocalDate date, double weightage) {
        checkName(assessmentName, null);
        String t = type == null ? "" : type.toLowerCase();
        Assessment a;
        if (t.startsWith("quiz")) a = new Quiz(assessmentName, date, weightage);
        else if (t.startsWith("mid")) a = new Exam("mid-term", assessmentName, date, weightage);
        else if (t.startsWith("final")) a = new Exam("final", assessmentName, date, weightage);
        else if (t.startsWith("exam")) a = new Exam("exam", assessmentName, date, weightage);
        else a = new Assignment(assessmentName, date, weightage);
        assessments.add(a);
        sortAssessments();
        return a;
    }

    /** Checks a new assessment: date present, weightage between 0 and 100, and the course total stays within 100. */
    public boolean validateChange(LocalDate date, double weightage) {
        return validateChange(null, date, weightage);
    }

    /** As above, ignoring the assessment that is being edited. */
    public boolean validateChange(Assessment editing, LocalDate date, double weightage) {
        if (date == null) return fail("Please give a date.");
        if (weightage <= 0 || weightage > 100) return fail("Weightage must be more than 0 and at most 100.");
        double others = 0;
        for (Assessment a : assessments) if (a != editing) others += a.getWeightage();
        if (others + weightage > 100.0001) {
            return fail("Weightages of this course would add up to " + round1(others + weightage)
                    + "%. The total cannot be more than 100%.");
        }
        lastError = "";
        return true;
    }

    private boolean fail(String msg) { lastError = msg; return false; }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    public String getLastError() { return lastError; }

    public void updateAssessment(Assessment a, String newName, LocalDate date, double weightage) {
        checkName(newName, a);
        a.update(newName, date, weightage);
        sortAssessments();
    }

    public void removeAssessment(Assessment a) { assessments.remove(a); }

    private void checkName(String assessmentName, Assessment editing) {
        if (assessmentName == null || assessmentName.isBlank()) throw new com.cramless.util.ValidationException("Please enter a name.");
        Assessment same = getAssessment(assessmentName.trim());
        if (same != null && same != editing) {
            throw new com.cramless.util.ValidationException("This course already has \"" + assessmentName.trim() + "\".");
        }
    }

    public Assessment getAssessment(String assessmentName) {
        for (Assessment a : assessments) if (a.getName().equalsIgnoreCase(assessmentName)) return a;
        return null;
    }

    // ---- coverage of topics by assessments ----

    public int countTopicsCoveredBy(Assessment a) {
        int n = 0;
        for (Topic t : topics) if (a.getDate() != null && !t.getWeekStart().isAfter(a.getDate())) n++;
        return n;
    }

    public List<Assessment> getAssessmentsCovering(Topic t, boolean graded) {
        List<Assessment> out = new ArrayList<>();
        for (Assessment a : assessments) {
            if (a.isGraded() != graded) continue;
            if (a.getDate() != null && !t.getWeekStart().isAfter(a.getDate())) out.add(a);
        }
        return out;
    }

    // ---- grades ----

    /** Weighted average of the graded assessments, 0-100 (0 when nothing is graded yet). */
    public double getCurrentPercentage() {
        double w = 0, earned = 0;
        for (Assessment a : assessments) {
            if (!a.isGraded()) continue;
            w += a.getWeightage();
            earned += a.getWeightage() * a.getScore().getPercentage() / 100.0;
        }
        return w <= 0 ? 0 : earned / w * 100.0;
    }

    public boolean hasScores() { return getGradedWeight() > 0; }

    public double getGradedWeight() {
        double w = 0;
        for (Assessment a : assessments) if (a.isGraded()) w += a.getWeightage();
        return w;
    }

    public double getTotalWeight() {
        double w = 0;
        for (Assessment a : assessments) w += a.getWeightage();
        return w;
    }

    public GradeScale getGradeScale() { return gradeScale; }

    // ---- progress ----

    public int countDueTopics(LocalDate today) {
        int n = 0;
        for (Topic t : topics) if (t.getPlannedByDate().isBefore(today)) n++;
        return n;
    }

    public int countCompletedTopics() {
        int n = 0;
        for (Topic t : topics) if (t.isCompleted()) n++;
        return n;
    }

    public List<Topic> getOverdueTopics(LocalDate today) {
        List<Topic> out = new ArrayList<>();
        for (Topic t : topics) if (t.getPlannedByDate().isBefore(today) && !t.isCompleted()) out.add(t);
        return out;
    }

    public List<Score> getScoresIn(DateRange period) {
        List<Score> out = new ArrayList<>();
        for (Assessment a : assessments) {
            if (a.isGraded() && (period == null || period.contains(a.getScore().getDate()))) out.add(a.getScore());
        }
        out.sort(Comparator.comparing(Score::getDate));
        return out;
    }

    /** Study hours per day, from the time recorded when topics were completed. */
    public Map<LocalDate, Double> getStudyTimeByDay(DateRange period) {
        Map<LocalDate, Double> out = new TreeMap<>();
        for (Topic t : topics) {
            if (!t.isCompleted() || t.getProgress().getActualTimeSpent() == null) continue;
            LocalDate d = t.getProgress().getCompletedAt().toLocalDate();
            if (period != null && !period.contains(d)) continue;
            out.merge(d, t.getProgress().getActualTimeSpent() / 60.0, Double::sum);
        }
        return out;
    }

    public double getStudyTimeIn(DateRange period) {
        double h = 0;
        for (double v : getStudyTimeByDay(period).values()) h += v;
        return h;
    }

    // ---- plain getters ----

    public String getName() { return name; }
    public String getCourseCode() { return courseCode; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public LocalDate getWeek1Date() { return week1Date; }
    public String getTargetGrade() { return targetGrade; }
    public void setTargetGrade(String targetGrade) { this.targetGrade = targetGrade; }
    public List<Topic> getTopics() { return topics; }
    public List<Assessment> getAssessments() { return assessments; }

    /** Keeps assessments in date order; called whenever one is added or its date changes. */
    public void sortAssessments() {
        assessments.sort(Comparator.comparing(Assessment::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
    }
    public Student getOwner() { return owner; }
    void setOwner(Student owner) { this.owner = owner; }

    @Override
    public String toString() { return name; }
}
