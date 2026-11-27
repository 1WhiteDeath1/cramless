package com.cramless.service;

import com.cramless.model.Student;
import com.cramless.model.StudyAvailability;
import com.cramless.model.StudyPlan;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the single semester-wide plan. Topics are taken best-priority first and given 30-minute
 * blocks on the earliest free days that fall before their exam, within the student's daily hours.
 */
public class StudyPlanGenerator {
    private static final int HORIZON_DAYS = 400;

    private final PriorityAdvisor advisor;
    private final int blockMinutes = 30;
    private final List<String> warnings = new ArrayList<>();

    public StudyPlanGenerator(PriorityAdvisor advisor) {
        this.advisor = advisor;
    }

    public StudyPlan generate(Student s) {
        StudyPlan p = new StudyPlan(AppClock.now());
        warnings.clear();
        schedule(p, s);
        return p;
    }

    /** Re-plans everything that is not finished, after a topic was completed or a score was logged. */
    public void rebalance(StudyPlan p, Student s) {
        warnings.clear();
        java.util.Set<com.cramless.model.Topic> done = new java.util.LinkedHashSet<>();
        for (com.cramless.model.StudySession ss : p.getSessions()) if (ss.getTopic().isCompleted()) done.add(ss.getTopic());
        for (com.cramless.model.Topic t : done) p.completeSessionsFor(t, AppClock.today());
        p.removePlannedFrom(LocalDate.MIN);
        schedule(p, s);
        p.setStatus(StudyPlan.ACTIVE);
    }

    private void schedule(StudyPlan p, Student s) {
        double totalHours = 0;
        for (StudyAvailability a : s.getAvailability()) totalHours += a.getAvailableHours();
        if (totalHours <= 0) {
            throw new ValidationException("You have no study hours on any day. Give at least one day some hours.");
        }

        LocalDate today = AppClock.today();
        java.util.Map<com.cramless.model.Course, Integer> shortMinutes = new java.util.LinkedHashMap<>();
        java.util.Map<com.cramless.model.Course, Integer> shortTopics = new java.util.LinkedHashMap<>();
        java.util.Map<com.cramless.model.Course, LocalDate> firstDue = new java.util.LinkedHashMap<>();
        for (Topic t : advisor.rankTopics(s)) {
            LocalDate due = t.getDueDate();
            if (due == null) continue;
            LocalDate last = due.isAfter(today) ? due.minusDays(1) : today; // study before the exam day
            LocalDate horizon = today.plusDays(HORIZON_DAYS);
            if (last.isAfter(horizon)) last = horizon;

            int wanted = blocksFor(t);
            double score = advisor.scoreOf(t, s);
            String reason = advisor.explain(t);
            int placed = 0;
            for (LocalDate d = today; !d.isAfter(last) && placed < wanted; d = d.plusDays(1)) {
                StudyAvailability av = s.getAvailabilityFor(d.getDayOfWeek());
                if (av == null || av.isDayOff()) continue;
                int slots = (int) (av.getAvailableHours() * 60) / blockMinutes;
                for (int k = 0; k < slots && placed < wanted; k++) {
                    LocalTime start = av.getStartTime().plusMinutes((long) k * blockMinutes);
                    if (p.getSession(d, start) != null) continue; // already taken
                    p.addSession(t, d, start, blockMinutes, score, reason);
                    placed++;
                }
            }
            if (placed < wanted) {
                shortMinutes.merge(t.getCourse(), (wanted - placed) * blockMinutes, Integer::sum);
                shortTopics.merge(t.getCourse(), 1, Integer::sum);
                firstDue.merge(t.getCourse(), due, (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        // UC-04 7a: which courses are at risk, and by how many hours.
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH);
        for (java.util.Map.Entry<com.cramless.model.Course, Integer> e : shortMinutes.entrySet()) {
            double h = e.getValue() / 60.0;
            int n = shortTopics.get(e.getKey());
            warnings.add(AT_RISK + e.getKey().getName() + " is at risk: " + (h == Math.rint(h) ? String.valueOf((int) h) : String.valueOf(h))
                    + " more hours are needed for " + n + (n == 1 ? " topic" : " topics") + " before " + firstDue.get(e.getKey()).format(f) + ".");
        }
        // UC-04 7b: a course whose exams are over is left out.
        for (com.cramless.model.Course c : s.getCourses()) {
            boolean any = !c.getAssessments().isEmpty();
            boolean allPast = any;
            for (com.cramless.model.Assessment a : c.getAssessments()) if (a.getDate() == null || !a.getDate().isBefore(today)) allPast = false;
            if (allPast) warnings.add(LEFT_OUT + c.getName() + " is left out because its exams are over.");
        }
    }

    /** Prefixes that tell the screen which kind of warning it is. */
    public static final String AT_RISK = "AT_RISK:";
    public static final String LEFT_OUT = "LEFT_OUT:";

    /** Number of 30-minute blocks a topic needs: 2, plus one if heavy, plus one if the student is weak at it. */
    int blocksFor(Topic t) {
        int n = 2;
        if (t.getWeightage() >= 3) n++;
        if (t.getWeaknessLevel() > 0.3) n++;
        return n;
    }

    public List<String> getWarnings() { return warnings; }
    public int getBlockMinutes() { return blockMinutes; }
    public PriorityAdvisor getAdvisor() { return advisor; }
}
