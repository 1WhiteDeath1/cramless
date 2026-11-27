package com.cramless.controller;

import com.cramless.model.Course;
import com.cramless.model.GraphPoint;
import com.cramless.model.Leaderboard;
import com.cramless.model.ProgressGraph;
import com.cramless.model.Student;
import com.cramless.model.StudyAvailability;
import com.cramless.model.StudentRegistry;
import com.cramless.model.Topic;
import com.cramless.service.GradeForecaster;
import com.cramless.service.PaceTracker;
import com.cramless.service.ProgressGraphBuilder;
import com.cramless.storage.PersistentStorage;
import com.cramless.util.AppClock;
import com.cramless.util.DateRange;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Session controller for UC-09 (forecast), UC-10 (pace), UC-11 (graphs) and UC-12 (leaderboard). */
public class ProgressController {
    private final Student student;
    private final StudentRegistry registry;
    private final PersistentStorage store;
    private final GradeForecaster forecaster;
    private final PaceTracker paceTracker;
    private final ProgressGraphBuilder graphBuilder;
    private final Leaderboard leaderboard;
    private List<ProgressGraph> graphs = new ArrayList<>();
    private Course selectedCourse;
    private final java.util.Map<Course, Double> current = new java.util.HashMap<>();
    private final java.util.Map<Course, Double> forecast = new java.util.HashMap<>();
    private final java.util.Map<Course, String> letter = new java.util.HashMap<>();
    private final java.util.Map<Course, String> paceStatus = new java.util.HashMap<>();
    private final java.util.Map<Course, Integer> paceDiff = new java.util.HashMap<>();
    private String overall = "";
    private double extraHours;
    private int ownPosition;

    public ProgressController(Student student, StudentRegistry registry, PersistentStorage store, GradeForecaster forecaster,
                              PaceTracker paceTracker, ProgressGraphBuilder graphBuilder, Leaderboard leaderboard) {
        this.student = student;
        this.registry = registry;
        this.store = store;
        this.forecaster = forecaster;
        this.paceTracker = paceTracker;
        this.graphBuilder = graphBuilder;
        this.leaderboard = leaderboard;
    }

    // ------------------------------------------------------------------ UC-09

    /** UC-09 step 1: 1.1 getCourses, then per course 1.2 getCurrentPercentage, 1.3 forecast, 1.4 toLetter. */
    public List<Course> viewGradeForecast() {
        List<Course> courses = student.getCourses();                                    // 1.1
        current.clear(); forecast.clear(); letter.clear();
        for (Course c : courses) {
            if (!c.hasScores()) continue;                                               // 4a: nothing to forecast yet
            current.put(c, c.getCurrentPercentage());                                   // 1.2
            double fp = forecaster.forecast(c);                                         // 1.3
            forecast.put(c, fp);
            letter.put(c, forecaster.toLetter(c, fp));                                  // 1.4
        }
        return courses;
    }

    public boolean hasScores(Course c) { return current.containsKey(c); }
    public double currentPercentage(Course c) { return current.getOrDefault(c, 0.0); }
    public double forecastPercentage(Course c) { return forecast.getOrDefault(c, 0.0); }
    public String forecastLetter(Course c) { return letter.getOrDefault(c, "--"); }

    /** True when the course's weightages do not add up to 100%, so the forecast is only approximate. */
    public boolean isApproximate(Course c) { return Math.abs(c.getTotalWeight() - 100.0) > 0.01; }

    /** Read-only: required average for the saved target, or null when it cannot be worked out. */
    public Double requiredForSavedTarget(Course c) {
        if (c == null || c.getTargetGrade() == null) return null;
        try { return forecaster.requiredAverage(c, c.getTargetGrade()); }
        catch (ValidationException e) { return null; }
    }

    /**
     * UC-09 step 2: 2.1 getCourse, 2.2 requiredAverage. Above 100 means the target cannot be reached (7a).
     * The target is kept on the Course for this session only; post-condition 2 says viewing the forecast changes nothing,
     * so it is not saved.
     */
    public Double enterTargetGrade(String courseName, String targetGrade) {
        Course c = student.getCourse(courseName);                                       // 2.1
        if (c == null) throw new ValidationException("Please choose a course.");
        double req = forecaster.requiredAverage(c, targetGrade);                        // 2.2
        c.setTargetGrade(targetGrade.trim());
        return req;
    }

    // ------------------------------------------------------------------ UC-10

    /** One line per course, for example "Algorithms: Behind (2 topics behind)". */
    /** UC-10 step 1: 1.1 getCourses, per course 1.2 getPaceStatus and 1.3 getTopicsAheadOrBehind, then 1.4 getOverallStatus. */
    public List<String> viewStudyPace() {
        List<String> lines = new ArrayList<>();
        LocalDate today = AppClock.today();
        List<Course> courses = student.getCourses();                                    // 1.1
        paceStatus.clear(); paceDiff.clear();
        for (Course c : courses) {
            String status = paceTracker.getPaceStatus(c, today);                        // 1.2
            int diff = paceTracker.getTopicsAheadOrBehind(c, today);                    // 1.3
            paceStatus.put(c, status);
            paceDiff.put(c, diff);
            lines.add(c.getName() + ": " + status + " (" + describeDiff(diff) + ")");
        }
        overall = paceTracker.getOverallStatus(courses);                                // 1.4
        return lines;
    }

    public String paceStatus(Course c) { return paceStatus.getOrDefault(c, ""); }
    public int aheadOrBehind(Course c) { return paceDiff.getOrDefault(c, 0); }
    public String overallStatus() { return overall; }

    public static String describeDiff(int diff) {
        if (diff == 0) return "right on the syllabus";
        return Math.abs(diff) + (Math.abs(diff) == 1 ? " topic " : " topics ") + (diff > 0 ? "ahead" : "behind");
    }

    /** UC-10 step 2: 2.1 getCourse, 2.2 getOverdueTopics, 2.3 getAvailability, 2.4 extraHoursPerDay. */
    public List<Topic> selectCourse(String courseName) {
        Course c = student.getCourse(courseName);                                       // 2.1
        if (c == null) throw new ValidationException("Please choose a course.");
        selectedCourse = c;
        List<Topic> overdue = c.getOverdueTopics(AppClock.today());                     // 2.2
        List<StudyAvailability> avail = student.getAvailability();                      // 2.3
        extraHours = paceTracker.extraHoursPerDay(c, avail);                            // 2.4
        return overdue;
    }

    public double extraHoursPerDayNeeded() { return extraHours; }

    // ------------------------------------------------------------------ UC-11

    /** UC-11 step 1: 1.1 getCourses. */
    public List<Course> viewProgressGraphs() { return student.getCourses(); }          // 1.1

    /** The default period: from the start of the semester to its end. */
    public DateRange wholeSemester() {
        return new DateRange(student.getSemester().getStartDate(), student.getSemester().getEndDate());
    }

    /**
     * UC-11 step 2: 2.1 buildGradeGraph, 2.2 buildStudyTimeGraph. They are built per chosen course, so that each
     * course is its own labelled line (special requirement 3); the builder loops over the courses it is given.
     */
    public List<ProgressGraph> selectGraphScope(List<Course> courses, DateRange period) {
        if (courses == null || courses.isEmpty()) throw new ValidationException("Choose at least one course.");
        List<ProgressGraph> out = new ArrayList<>();
        for (Course c : courses) {
            List<Course> one = List.of(c);
            out.add(graphBuilder.buildGradeGraph(one, period));                         // 2.1
            out.add(graphBuilder.buildStudyTimeGraph(one, period));                     // 2.2
        }
        graphs = out;
        return out;
    }

    /** Looks up the exact value and date of a point the student clicked. */
    public GraphPoint selectGraphPoint(GraphPoint point) {
        for (ProgressGraph g : graphs) {
            GraphPoint found = g.findPoint(point);                                      // 3.1
            if (found != null) return found;
        }
        throw new ValidationException("That point is not on the graphs any more.");
    }

    // ------------------------------------------------------------------ UC-12

    /** UC-12 step 1: 1.1 getAllStudents, 1.2 refresh, 1.3 getRanking, 1.4 getPositionOf. */
    public List<Student> viewLeaderboard() {
        List<Student> all = registry.getAllStudents();                                  // 1.1
        leaderboard.refresh(all, "everyone");                                           // 1.2
        List<Student> ranking = leaderboard.getRanking();                               // 1.3
        ownPosition = leaderboard.getPositionOf(student);                               // 1.4
        return ranking;
    }

    /** scope: "everyone", "friends" or "classmates". The student is always on their own board. */
    public List<Student> filterLeaderboard(String scope) {
        String s = scope == null ? "everyone" : scope.toLowerCase(Locale.ROOT);
        List<Student> group;
        if (s.equals("friends")) {
            group = new ArrayList<>(student.getFriends());                              // 2.1
            group.add(student);
        } else if (s.equals("classmates")) {
            group = new ArrayList<>(registry.getClassmates(student));                   // 2.2
            group.add(student);
        } else {
            group = registry.getAllStudents();
            s = "everyone";
        }
        leaderboard.refresh(group, s);                                                  // 2.3
        List<Student> ranking = leaderboard.getRanking();                               // 2.4
        ownPosition = leaderboard.getPositionOf(student);                               // 2.5
        return ranking;
    }

    public int getOwnPosition() { return ownPosition; }

    /** Adds a friend by student ID so the "friends" board has someone on it. */
    public void addFriend(String studentId) {
        Student other = studentId == null ? null : registry.findById(studentId);
        if (other == null) throw new ValidationException("No student with that ID is registered.");
        student.addFriend(other);
        store.save(student);
    }

    public Student getStudent() { return student; }
    public Leaderboard getLeaderboard() { return leaderboard; }
}
