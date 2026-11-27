package com.cramless.controller;

import com.cramless.model.Course;
import com.cramless.model.Student;
import com.cramless.model.StudyAvailability;
import com.cramless.model.StudyPlan;
import com.cramless.model.StudySession;
import com.cramless.model.Topic;
import com.cramless.service.PriorityAdvisor;
import com.cramless.service.StudyPlanGenerator;
import com.cramless.storage.PersistentStorage;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Session controller for UC-04 (generate plan), UC-05 (complete topic), UC-07 (daily plan) and UC-08 (priority). */
public class StudyPlanController {
    private final Student student;
    private final StudyPlanGenerator generator;
    private final PriorityAdvisor advisor;
    private final PersistentStorage store;

    private Topic selectedTopic;
    private LocalDate shownDate = AppClock.today();

    // Results the screens show next to the return values.
    private int lastStreak;
    private Topic lastNextTopic;
    private List<String> lastWarnings = new ArrayList<>();
    private boolean lastStreakRestarted;
    private String lastCourseCovered;
    private Topic lastCompleted;
    private int plannedMinutes;
    private double selectedWeightage;
    private LocalDate selectedDueDate;
    private String selectedReason;
    private int selectedDaysLeft;
    private Topic lastTop;
    private String lastReason;

    public StudyPlanController(Student student, StudyPlanGenerator generator, PriorityAdvisor advisor, PersistentStorage store) {
        this.student = student;
        this.generator = generator;
        this.advisor = advisor;
        this.store = store;
    }

    // ------------------------------------------------------------------ UC-04

    /** UC-04 step 1: returns the last saved availability (1.1). 5a: nothing to plan yet. */
    public List<StudyAvailability> generateStudyPlan() {
        if (student.getCourses().isEmpty()) {
            throw new ValidationException("Import a course syllabus first. The plan is built from your topics and exam dates.");
        }
        if (student.getPendingTopics().isEmpty()) {
            throw new ValidationException("There are no unfinished topics with an upcoming exam or deadline to plan.");
        }
        return student.getAvailability();                                               // 1.1
    }

    /** UC-04 step 2, following its sequence diagram (2.1 to 2.4). The new plan replaces the old one. */
    public StudyPlan enterAvailability(double availableHours, Collection<String> daysOff) {
        student.setAvailability(availableHours, daysOff);                               // 2.1
        StudyPlan p = generator.generate(student);                                      // 2.2
        student.addPlan(p);                                                             // 2.3
        lastWarnings = new ArrayList<>(generator.getWarnings());
        store.save(p);                                                                  // 2.4
        return p;
    }

    // ------------------------------------------------------------------ UC-05

    /** UC-05 step 1: 1.1 findTopic, 1.2 getWeightage, 1.3 getDueDate. */
    public Topic selectTopic(String topicTitle) {
        Topic t = student.findTopic(topicTitle);                                        // 1.1
        if (t == null) throw new ValidationException("That topic could not be found.");
        selectedWeightage = t.getWeightage();                                           // 1.2
        selectedDueDate = t.getDueDate();                                               // 1.3
        selectedTopic = t;
        return t;
    }

    /**
     * UC-05 step 2, as in its sequence diagram: 2.1 complete, 2.2 getPointsReward, 2.3 addPoints,
     * 2.4 getCurrentStreak, 2.5 getActivePlan, 2.6 rebalance, 2.7 getTopTopic, 2.8 save. Returns the points earned.
     */
    public Integer completeTopic(Integer actualTimeSpent) {
        if (selectedTopic == null) throw new IllegalStateException("No topic selected.");
        Topic t = selectedTopic;
        if (t.isCompleted()) throw new ValidationException("\"" + t.getTitle() + "\" is already completed. No points were added."); // 3a
        boolean wasBroken = student.hasBrokenStreak();
        t.complete(actualTimeSpent);                                                     // 2.1
        int pts = t.getPointsReward();                                                   // 2.2
        student.addPoints(pts, Student.REASON_TOPIC, t.getKey());                        // 2.3
        lastStreak = student.getCurrentStreak();                                         // 2.4
        lastStreakRestarted = wasBroken && lastStreak == 1;                              // 4a
        Course course = t.getCourse();
        lastCourseCovered = course.countCompletedTopics() == course.getTopics().size() ? course.getName() : null; // 5a

        StudyPlan p = student.getActivePlan();                                           // 2.5
        if (p != null) {
            try {
                generator.rebalance(p, student);                                         // 2.6
            } catch (ValidationException e) {
                p.markOutdated();
            }
        }
        lastNextTopic = advisor.getTopTopic(student);                                    // 2.7
        store.save(student);                                                             // 2.8
        lastCompleted = t;
        selectedTopic = null;
        return pts;
    }

    /** UC-05 extension *a: later the same day, the completion is undone and its points are taken back. */
    public void undoCompletion(String topicTitle) {
        Topic t = student.findTopic(topicTitle);
        if (t == null) throw new ValidationException("That topic could not be found.");
        t.undoCompletion(AppClock.today());
        student.addPoints(-t.getPointsReward(), Student.REASON_UNDO, t.getKey());
        StudyPlan p = student.getActivePlan();
        if (p != null) p.reopenSessionsFor(t, AppClock.today());
        lastStreak = student.getCurrentStreak();
        store.save(student);
    }

    // ------------------------------------------------------------------ UC-07

    /** UC-07 step 1: 1.1 getActivePlan, 1.2 getSessionsFor(today), 1.3 getTotalPlannedTime(today). */
    public List<StudySession> viewDailyPlan() {
        LocalDate today = AppClock.today();
        StudyPlan p = student.getActivePlan();                                          // 1.1
        if (p == null) throw new ValidationException("You have no study plan yet. Generate one first.");
        shownDate = today;
        List<StudySession> sessions = p.getSessionsFor(today);                          // 1.2
        plannedMinutes = p.getTotalPlannedTime(today);                                  // 1.3
        return sessions;
    }

    /** UC-07 step 2 (loop): 2.1 getSessionsFor(date). */
    public List<StudySession> viewPlanForDate(LocalDate date) {
        StudyPlan p = student.getActivePlan();
        if (p == null) throw new ValidationException("You have no study plan yet. Generate one first.");
        shownDate = date;
        plannedMinutes = p.getTotalPlannedTime(date);
        return p.getSessionsFor(date);                                                  // 2.1
    }

    /** UC-07 step 3: 3.1 getSession, 3.2 getTopic, 3.3 getReason, 3.4 getWeightage, 3.5 getDaysLeft. */
    public StudySession selectSession(LocalTime startTime) {
        StudyPlan p = student.getActivePlan();
        StudySession s = p == null ? null : p.getSession(shownDate, startTime);          // 3.1
        if (s == null) throw new ValidationException("That session could not be found.");
        Topic t = s.getTopic();                                                         // 3.2
        selectedReason = s.getReason();                                                 // 3.3
        selectedWeightage = t.getWeightage();                                           // 3.4
        selectedDaysLeft = t.getDaysLeft(AppClock.today());                             // 3.5
        selectedTopic = t;
        return s;
    }

    /** Total planned minutes for the day last shown by viewDailyPlan or viewPlanForDate. */
    public int getPlannedMinutes(LocalDate date) {
        if (date.equals(shownDate)) return plannedMinutes;
        StudyPlan p = student.getActivePlan();
        return p == null ? 0 : p.getTotalPlannedTime(date);
    }

    public int getDoneMinutes(LocalDate date) {
        StudyPlan p = student.getActivePlan();
        return p == null ? 0 : p.getDoneTime(date);
    }

    public List<StudySession> getCarriedOver() {
        StudyPlan p = student.getActivePlan();
        return p == null ? new ArrayList<>() : p.getCarriedOver(AppClock.today());
    }

    /** Sessions today's plan lists for the next upcoming day, used when nothing is planned today. */
    public StudySession getNextUpcomingSession() {
        StudyPlan p = student.getActivePlan();
        return p == null ? null : p.getNextSession(AppClock.today());
    }

    // ------------------------------------------------------------------ UC-08

    /** UC-08 step 1: 1.1 getTopTopic, 1.2 explain(top). 3b: nothing pending. */
    public Topic requestNextTopic() {
        Topic top = advisor.getTopTopic(student);                                       // 1.1
        if (top == null) throw new ValidationException("You are all caught up. No unfinished topics have an upcoming exam.");
        lastReason = advisor.explain(top);                                              // 1.2
        lastTop = top;
        return top;
    }

    /** The reason for any topic in the ranking, worked out the same way as 1.2. */
    public String explain(Topic t) { return advisor.explain(t); }

    /** The reason returned with the last requestNextTopic (1.2). */
    public String getLastReason() { return lastReason; }

    public double scoreOf(Topic t) { return advisor.scoreOf(t, student); }

    /** UC-08 step 2: 2.1 rankTopics. */
    public List<Topic> viewFullRanking() { return advisor.rankTopics(student); }        // 2.1

    /** Changes the weights of the Smart Priority Advisor (they are normalised to add up to 1). */
    public void updateSettings(double weightage, double urgency, double weakness) {
        student.getSettings().setFactors(weightage, urgency, weakness);
        StudyPlan p = student.getActivePlan();
        if (p != null) p.markOutdated();
        store.save(student);
    }

    // ------------------------------------------------------------------ results for the screens

    public Topic getSelectedTopic() { return selectedTopic; }
    public double getSelectedWeightage() { return selectedWeightage; }
    public LocalDate getSelectedDueDate() { return selectedDueDate; }
    public String getSelectedReason() { return selectedReason; }
    public int getSelectedDaysLeft() { return selectedDaysLeft; }
    public LocalDate getShownDate() { return shownDate; }
    public int getLastStreak() { return lastStreak; }
    public Topic getLastNextTopic() { return lastNextTopic; }
    public boolean isLastStreakRestarted() { return lastStreakRestarted; }
    public String getLastCourseCovered() { return lastCourseCovered; }
    public Topic getLastCompleted() { return lastCompleted; }
    public List<String> getLastWarnings() { return lastWarnings; }
    public Student getStudent() { return student; }
}
