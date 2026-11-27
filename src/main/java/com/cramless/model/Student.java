package com.cramless.model;

import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The account holder. Owns the semester, courses, settings, availability, plans, points and friends. */
public class Student {
    private final String studentId;
    private final String fullName;
    private final String displayName;
    private final String email; // optional
    private final String passwordHash;
    private final Semester semester;
    private final StudySettings settings;
    private final List<StudyAvailability> availability = new ArrayList<>();
    private final List<StudyPlan> plans = new ArrayList<>();
    private final List<PointTransaction> transactions = new ArrayList<>();
    private final List<Friendship> friendships = new ArrayList<>();

    public Student(String fullName, String displayName, String studentId, String email, String passwordHash) {
        this.fullName = fullName;
        this.displayName = displayName;
        this.studentId = studentId;
        this.email = email;
        this.passwordHash = passwordHash;
        // Creator: a Student contains its semester and settings, so it creates them.
        LocalDate today = AppClock.today();
        this.semester = new Semester("Current semester", today, today.plusWeeks(16));
        this.settings = new StudySettings();
        for (DayOfWeek d : DayOfWeek.values()) {
            availability.add(new StudyAvailability(d.name(), 2.0));
        }
    }

    // ---- courses ----

    /** Creator: a Student takes Courses, so it creates them. */
    public Course createCourse(String name, String courseCode, LocalDate week1Date) {
        if (name == null || name.isBlank()) throw new ValidationException("Please enter the course name.");
        if (week1Date == null) throw new ValidationException("Please choose the date of week 1.");
        if (getCourse(name.trim()) != null) throw new ValidationException("You already have a course called \"" + name.trim() + "\".");
        Course c = new Course(name.trim(), courseCode == null || courseCode.isBlank() ? null : courseCode.trim(), week1Date);
        c.setOwner(this);
        semester.addCourse(c);
        return c;
    }

    public List<Course> getCourses() { return semester.getCourses(); }

    public Course getCourse(String name) {
        if (name == null) return null;
        for (Course c : semester.getCourses()) if (c.getName().equalsIgnoreCase(name.trim())) return c;
        for (Course c : semester.getCourses()) {
            if (c.getCourseCode() != null && c.getCourseCode().equalsIgnoreCase(name.trim())) return c;
        }
        return null;
    }

    /** Finds a topic by "Course :: Title" or, if unambiguous, by title alone. */
    public Topic findTopic(String titleOrKey) {
        if (titleOrKey == null) return null;
        Topic found = null;
        for (Course c : semester.getCourses()) {
            for (Topic t : c.getTopics()) {
                if (t.getKey().equalsIgnoreCase(titleOrKey.trim())) return t;
                if (t.getTitle().equalsIgnoreCase(titleOrKey.trim()) && found == null) found = t;
            }
        }
        return found;
    }

    /** Topics that are not finished and still have an upcoming assessment that covers them. */
    public List<Topic> getPendingTopics() {
        List<Topic> out = new ArrayList<>();
        for (Course c : semester.getCourses()) {
            for (Topic t : c.getTopics()) {
                if (!t.isCompleted() && t.getDueDate() != null) out.add(t);
            }
        }
        return out;
    }

    // ---- settings and availability ----

    public StudySettings getSettings() { return settings; }

    public List<StudyAvailability> getAvailability() { return availability; }

    public StudyAvailability getAvailabilityFor(DayOfWeek day) {
        for (StudyAvailability a : availability) if (a.getDayOfWeek().equals(day.name())) return a;
        return null;
    }

    /** Sets the same number of hours on every day except the days off (given as day names). */
    public void setAvailability(double availableHours, Collection<String> daysOff) {
        // UC-04 3a: negative or more than 24 hours a day is rejected.
        if (availableHours <= 0 || availableHours > 24) throw new ValidationException("availableHours", "Enter between 0.5 and 24 hours a day.");
        Set<String> off = new HashSet<>();
        if (daysOff != null) for (String d : daysOff) off.add(d.toUpperCase());
        for (StudyAvailability a : availability) {
            double hours = off.contains(a.getDayOfWeek()) ? 0 : availableHours;
            a.setHours(hours, a.getStartTime());
        }
    }

    public void restoreAvailability(String day, double hours, LocalTime start) {
        for (StudyAvailability a : availability) if (a.getDayOfWeek().equals(day)) a.setHours(hours, start);
    }

    // ---- plans ----

    public StudyPlan getActivePlan() {
        return plans.isEmpty() ? null : plans.get(plans.size() - 1);
    }

    public void addPlan(StudyPlan p) {
        p.setOwner(this);
        plans.clear();
        plans.add(p);
    }

    public List<StudyPlan> getPlans() { return plans; }

    // ---- points and streaks ----

    /** Creator: a Student records its PointTransactions, so it creates them. */
    public PointTransaction addPoints(int change, String reason) {
        return addPoints(change, reason, null);
    }

    public PointTransaction addPoints(int change, String reason, String source) {
        PointTransaction t = new PointTransaction(change, reason, AppClock.now(), source);
        transactions.add(t);
        return t;
    }

    public void restoreTransaction(int change, String reason, LocalDateTime when, String source) {
        transactions.add(new PointTransaction(change, reason, when, source));
    }

    public List<PointTransaction> getTransactions() { return transactions; }

    public int getTotalPoints() {
        int total = 0;
        for (PointTransaction t : transactions) total += t.getPointsChange();
        return total;
    }

    /**
     * UC-05 special requirement 2: a streak day is any day with at least one completed topic.
     * Counts consecutive such days ending today (or yesterday, if nothing is done yet today).
     * A completion undone the same day (extension *a) no longer counts.
     */
    public int getCurrentStreak() {
        java.util.Map<LocalDate, Integer> perDay = new java.util.HashMap<>();
        for (PointTransaction t : transactions) {
            int step = REASON_TOPIC.equals(t.getReason()) ? 1 : REASON_UNDO.equals(t.getReason()) ? -1 : 0;
            if (step != 0) perDay.merge(t.getTransactionDateTime().toLocalDate(), step, Integer::sum);
        }
        Set<LocalDate> days = new HashSet<>();
        for (java.util.Map.Entry<LocalDate, Integer> e : perDay.entrySet()) if (e.getValue() > 0) days.add(e.getKey());
        LocalDate d = AppClock.today();
        if (!days.contains(d)) d = d.minusDays(1);
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    /** True when the student had a streak before but missed a day, so the next completion starts at 1 (UC-05 4a). */
    public boolean hasBrokenStreak() {
        if (getCurrentStreak() > 0) return false;
        for (PointTransaction t : transactions) if (REASON_TOPIC.equals(t.getReason())) return true;
        return false;
    }

    public static final String REASON_TOPIC = "topic completed";
    public static final String REASON_UNDO = "completion undone";
    public static final String REASON_SCORE = "score logged";

    // ---- friends ----

    public Friendship addFriend(Student other) {
        if (other == this) throw new ValidationException("You cannot add yourself as a friend.");
        for (Friendship f : friendships) {
            if (f.getFriend() == other) throw new ValidationException(other.getDisplayName() + " is already your friend.");
        }
        Friendship f = new Friendship(other, AppClock.today());
        friendships.add(f);
        return f;
    }

    public void restoreFriendship(Student other, LocalDate added) {
        friendships.add(new Friendship(other, added));
    }

    public List<Friendship> getFriendships() { return friendships; }

    public List<Student> getFriends() {
        List<Student> out = new ArrayList<>();
        for (Friendship f : friendships) out.add(f.getFriend());
        return out;
    }

    // ---- plain getters ----

    public String getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Semester getSemester() { return semester; }

    @Override
    public String toString() { return displayName; }
}
