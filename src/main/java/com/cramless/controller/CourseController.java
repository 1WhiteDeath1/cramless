package com.cramless.controller;

import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.model.Score;
import com.cramless.model.Student;
import com.cramless.model.StudyPlan;
import com.cramless.service.StudyPlanGenerator;
import com.cramless.service.SyllabusParser;
import com.cramless.storage.PersistentStorage;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Session controller for UC-02 (import syllabus), UC-03 (manage courses and exams) and UC-06 (log a score). */
public class CourseController {
    private final Student student;
    private final SyllabusParser parser;
    private final StudyPlanGenerator generator;
    private final PersistentStorage store;

    private Course selectedCourse;
    private Assessment selectedAssessment;
    private String pendingAction;
    private String pendingType;

    // Results the screens show next to the return values.
    private final List<String> warnings = new ArrayList<>();
    private boolean planNeedsRegenerating;
    private double lastCurrentPercentage;
    private int lastPointsEarned;
    private boolean lastPlanAdjusted;

    public CourseController(Student student, SyllabusParser parser, StudyPlanGenerator generator, PersistentStorage store) {
        this.student = student;
        this.parser = parser;
        this.generator = generator;
        this.store = store;
    }

    // ------------------------------------------------------------------ UC-02

    public List<String> startSyllabusImport() {
        return Arrays.asList("Course name", "Course code", "Date of week 1", "Course outline (week-wise topics)",
                "Assessments (name, date, weightage)");
    }

    public Course importSyllabus(String courseName, String courseCode, LocalDate week1Date,
                                 String outline, String assessments) {
        return importSyllabus(courseName, courseCode, week1Date, outline, assessments, false);
    }

    /**
     * UC-02, following its sequence diagram: 2.1 createCourse, 2.2 parseOutline, 2.3 addTopic (loop),
     * 2.4 parseAssessments, 2.5 addAssessment (loop), 2.6 save. Nothing is saved unless every check passes;
     * a failed check removes the course again, so nothing is recorded (special requirement 2).
     * replaceExisting is the student's answer to extension 6a (same course code already exists).
     */
    public Course importSyllabus(String courseName, String courseCode, LocalDate week1Date,
                                 String outline, String assessments, boolean replaceExisting) {
        warnings.clear();
        // 4b: course name, code and week-1 date are required.
        if (courseName == null || courseName.isBlank()) throw new ValidationException("courseName", "Enter the course name.");
        if (courseCode == null || courseCode.isBlank()) throw new ValidationException("courseCode", "Enter the course code.");
        if (week1Date == null) throw new ValidationException("week1Date", "Enter the date of week 1 as dd/mm/yyyy.");

        // 6a: a course with the same code exists. The student is asked before it is replaced.
        Course existing = student.getCourse(courseCode);
        if (existing != null && !replaceExisting) {
            throw new ValidationException("duplicate", existing.getCourseCode() + " is already in your courses. Replacing it loses its progress and scores.");
        }
        if (existing != null) student.getSemester().removeCourse(existing);
        Course sameName = student.getCourse(courseName);
        if (sameName != null) {
            if (existing != null) student.getSemester().addCourse(existing);
            throw new ValidationException("courseName", "You already have a course called \"" + courseName.trim() + "\".");
        }

        Course c = student.createCourse(courseName, courseCode, week1Date);                        // 2.1
        try {
            List<String[]> lines = parser.parseOutline(outline);                                   // 2.2
            if (!parser.getWarnings().isEmpty()) throw new ValidationException("outline", String.join("\n", parser.getWarnings())); // 4a
            if (lines.isEmpty()) throw new ValidationException("outline", "Add at least one line, for example \"Week 1: Introduction\".");
            for (String[] line : lines) c.addTopic(Integer.parseInt(line[0]), line[1]);             // 2.3

            List<String[]> entries = parser.parseAssessments(assessments);                         // 2.4
            if (!parser.getWarnings().isEmpty()) throw new ValidationException("assessments", String.join("\n", parser.getWarnings())); // 4c, 4d
            for (String[] e : entries) {
                c.addAssessment(e[0], e[1], LocalDate.parse(e[2]), Double.parseDouble(e[3]));     // 2.5
            }
        } catch (RuntimeException ex) {
            // Special requirement 2: a failed import records nothing.
            student.getSemester().removeCourse(c);
            if (existing != null) student.getSemester().addCourse(existing);
            throw ex;
        }
        markPlanOutdated();                                                                        // same as UC-03 3.5-3.6
        store.save(c);                                                                             // 2.6
        return c;
    }

    // ------------------------------------------------------------------ UC-03

    public List<Course> manageCourses() {
        return student.getCourses();
    }

    /** action is "add", "change" or "remove". */
    public Assessment selectChange(String action, ChangeItem item) {
        String a = action == null ? "" : action.toLowerCase(Locale.ROOT);
        if (!a.equals("add") && !a.equals("change") && !a.equals("remove")) {
            throw new ValidationException("Choose add, change or remove.");
        }
        Course c = student.getCourse(item.getCourseName());
        if (c == null) throw new ValidationException("That course no longer exists.");
        selectedCourse = c;
        pendingAction = a;
        selectedAssessment = null;
        pendingType = item.getType();
        if (!a.equals("add")) {
            selectedAssessment = c.getAssessment(item.getName());
            if (selectedAssessment == null) throw new ValidationException("That assessment no longer exists.");
        }
        return selectedAssessment;
    }

    public List<Course> enterChange(String name, LocalDate date, double weightage) {
        if (selectedCourse == null || pendingAction == null) throw new IllegalStateException("Nothing was selected.");
        planNeedsRegenerating = false;
        switch (pendingAction) {
            case "change":
                if (!selectedCourse.validateChange(selectedAssessment, date, weightage)) {               // 3.1
                    throw new ValidationException(selectedCourse.getLastError());
                }
                selectedCourse.updateAssessment(selectedAssessment, name == null ? null : name.trim(), date, weightage); // 3.2
                break;
            case "add":
                if (!selectedCourse.validateChange(date, weightage)) throw new ValidationException(selectedCourse.getLastError()); // 3.1
                selectedCourse.addAssessment(pendingType == null ? name : pendingType, name == null ? null : name.trim(), date, weightage); // 3.3
                break;
            default: // remove
                selectedCourse.removeAssessment(selectedAssessment);                                     // 3.4
                break;
        }
        markPlanOutdated();                                                                              // 3.5, 3.6
        store.save(selectedCourse);                                                                      // 3.7
        selectedAssessment = null;
        pendingAction = null;
        return student.getCourses();
    }

    // ------------------------------------------------------------------ UC-06

    public List<String> startScoreLogging() {
        return Arrays.asList("Course", "Assessment", "Marks obtained", "Total marks", "Date");
    }

    /** UC-06, following its sequence diagram (2.1 to 2.9). Points are only given the first time (9a: overwrite). */
    public Score logScore(String courseName, String assessmentName, double marksObtained, double totalMarks, LocalDate date) {
        Course c = student.getCourse(courseName);                                                  // 2.1
        if (c == null) throw new ValidationException("Please choose a course.");
        Assessment a = c.getAssessment(assessmentName);                                            // 2.2
        if (a == null) throw new ValidationException("Please choose an assessment.");
        boolean firstTime = !a.isGraded();

        Score sc = a.recordScore(marksObtained, totalMarks, date);                                  // 2.3
        lastCurrentPercentage = c.getCurrentPercentage();                                           // 2.4
        lastPointsEarned = 0;
        if (firstTime) {
            lastPointsEarned = a.getPointsReward();                                                 // 2.5
            student.addPoints(lastPointsEarned, Student.REASON_SCORE, c.getName() + " :: " + a.getName()); // 2.6
        }
        lastPlanAdjusted = false;
        StudyPlan p = student.getActivePlan();                                                      // 2.7
        if (p != null) {
            try {
                generator.rebalance(p, student);                                                    // 2.8
                lastPlanAdjusted = true;
            } catch (ValidationException e) {
                p.markOutdated();
            }
        }
        store.save(student);                                                                        // 2.9
        return sc;
    }

    // ------------------------------------------------------------------ helpers

    private void markPlanOutdated() {
        StudyPlan p = student.getActivePlan();
        if (p != null) {
            p.markOutdated();
            planNeedsRegenerating = true;
        }
    }

    public List<String> getWarnings() { return warnings; }
    public boolean isPlanNeedsRegenerating() { return planNeedsRegenerating || hasOutdatedPlan(); }
    private boolean hasOutdatedPlan() { return student.getActivePlan() != null && student.getActivePlan().isOutdated(); }
    public double getLastCurrentPercentage() { return lastCurrentPercentage; }
    public int getLastPointsEarned() { return lastPointsEarned; }
    public boolean isLastPlanAdjusted() { return lastPlanAdjusted; }
    public Student getStudent() { return student; }
}
