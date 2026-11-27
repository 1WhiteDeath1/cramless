package com.cramless;

import static org.junit.jupiter.api.Assertions.*;

import com.cramless.controller.ChangeItem;
import com.cramless.controller.CourseController;
import com.cramless.controller.ProgressController;
import com.cramless.controller.RegistrationController;
import com.cramless.controller.StudyPlanController;
import com.cramless.model.*;
import com.cramless.service.*;
import com.cramless.storage.SqlitePersistentStorage;
import com.cramless.util.AppClock;
import com.cramless.util.DateRange;
import com.cramless.util.ValidationException;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the twelve use cases through the controllers (no UI) against a real SQLite file. */
class FlowTest {
    private File dbFile;
    private SqlitePersistentStorage store;
    private StudentRegistry registry;
    private Student student;
    private CourseController courses;
    private StudyPlanController plan;
    private ProgressController progress;

    private static final String OUTLINE = "Week 1: Introduction\nWeek 2: Linked Lists\nWeek 3: Stacks and Queues\n"
            + "Week 4: Trees\nWeek 5: Graphs";
    private static final String ASSESSMENTS = "Quiz 1, 2026-12-04, 5%\nMid-term, 2026-12-20, 25%\nFinal Exam, 2027-01-20, 40%\n"
            + "Assignment 1, 2026-12-10, 10%\nProject, 2027-01-10, 20%";

    @BeforeEach
    void setUp() throws Exception {
        AppClock.setFixed(LocalDateTime.of(2026, 11, 26, 9, 0));
        dbFile = File.createTempFile("cramless", ".db");
        store = new SqlitePersistentStorage(dbFile.getAbsolutePath());
        registry = new StudentRegistry();
        student = new RegistrationController(registry, store)
                .register("Test Student", "tester", "i250001", "t@x.com", "secret12", "secret12");
        attach();
    }

    private void attach() {
        PriorityAdvisor advisor = new PriorityAdvisor();
        StudyPlanGenerator gen = new StudyPlanGenerator(advisor);
        courses = new CourseController(student, new LineFormatSyllabusParser(), gen, store);
        plan = new StudyPlanController(student, gen, advisor, store);
        progress = new ProgressController(student, registry, store, new GradeForecaster(), new PaceTracker(),
                new ProgressGraphBuilder(), new Leaderboard());
    }

    @AfterEach
    void tearDown() {
        store.close();
        dbFile.delete();
        AppClock.reset();
    }

    /** Logs a score on the assessment's own date (scores for assessments still to come are refused). */
    private Score logAt(String course, String assessment, double marks, double total) {
        LocalDate d = student.getCourse(course).getAssessment(assessment).getDate();
        AppClock.setFixed(d.atTime(18, 0));
        try {
            return courses.logScore(course, assessment, marks, total, d);
        } finally {
            AppClock.setFixed(LocalDateTime.of(2026, 11, 26, 9, 0));
        }
    }

    private Course importAlgorithms() {
        return courses.importSyllabus("Algorithms", "CS2001", LocalDate.of(2026, 11, 23), OUTLINE, ASSESSMENTS);
    }

    @Test
    void uc01_registration_validatesAndRejectsDuplicates() {
        RegistrationController reg = new RegistrationController(registry, store);
        assertEquals(6, reg.startRegistration().size());
        assertEquals(0, student.getTotalPoints());
        assertEquals(0, student.getCurrentStreak());
        assertThrows(ValidationException.class, () -> reg.register("A", "b", "i250001", "", "secret12", "secret12")); // taken
        assertThrows(ValidationException.class, () -> reg.register("A", "b2", "i250002", "", "short", "short"));
        assertThrows(ValidationException.class, () -> reg.register("A", "b2", "i250002", "", "secret12", "different"));
        assertSame(student, reg.login("i250001", "secret12"));
        assertThrows(ValidationException.class, () -> reg.login("i250001", "wrong"));
    }

    @Test
    void uc02_importSyllabus_createsTopicsAndTypedAssessments() {
        Course c = importAlgorithms();
        assertEquals(5, c.getTopics().size());
        assertEquals(5, c.getAssessments().size());
        assertTrue(c.getAssessment("Quiz 1") instanceof Quiz);
        assertTrue(c.getAssessment("Mid-term") instanceof Exam);
        assertTrue(c.getAssessment("Assignment 1") instanceof Assignment);
        assertEquals(100.0, c.getTotalWeight(), 1e-9);
        assertThrows(ValidationException.class,
                () -> courses.importSyllabus("Algorithms", "", LocalDate.now(), OUTLINE, ASSESSMENTS)); // 4b: code missing
        assertThrows(ValidationException.class,
                () -> courses.importSyllabus("Empty", "CS9", LocalDate.now(), "   ", ASSESSMENTS));
        // 6a: same course code asks first; replacing swaps the course
        ValidationException dup = assertThrows(ValidationException.class,
                () -> courses.importSyllabus("Algorithms II", "CS2001", LocalDate.of(2026, 11, 23), OUTLINE, ASSESSMENTS));
        assertEquals("duplicate", dup.getField());
        Course replaced = courses.importSyllabus("Algorithms II", "CS2001", LocalDate.of(2026, 11, 23), OUTLINE, ASSESSMENTS, true);
        assertEquals(1, student.getCourses().size());
        assertSame(replaced, student.getCourse("Algorithms II"));
    }

    @Test
    void uc02_outlineLineWithoutWeekIsListed() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> courses.importSyllabus("DB", "CS2005", LocalDate.of(2026, 11, 23), "Week 1: Intro\nNormalization", ASSESSMENTS));
        assertEquals("outline", e.getField());
        assertTrue(e.getMessage().contains("Normalization"));
        assertTrue(student.getCourses().isEmpty(), "nothing is recorded unless every check passes");
    }

    @Test
    void uc02_weightagesMustTotal100() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> courses.importSyllabus("DB", "CS2005", LocalDate.of(2026, 11, 23), OUTLINE, "Quiz 1, 05/12/2026, 5%"));
        assertEquals("assessments", e.getField());
        assertTrue(e.getMessage().contains("5"));
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    void uc02_badAssessmentDateNamesTheAssessment() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> courses.importSyllabus("DB", "CS2005", LocalDate.of(2026, 11, 23), OUTLINE, "Quiz 1, notadate, 5%\nFinal, 2026-12-05, 95%"));
        assertEquals("assessments", e.getField());
        assertTrue(e.getMessage().contains("Quiz 1"));
        assertTrue(student.getCourses().isEmpty());
    }

    @Test
    void uc03_manageCourses_addChangeRemove_andMarksPlanOutdated() {
        Course c = importAlgorithms();
        plan.generateStudyPlan();
        plan.enterAvailability(2, List.of("SUNDAY"));
        assertFalse(student.getActivePlan().isOutdated());

        assertEquals(1, courses.manageCourses().size());
        courses.selectChange("remove", new ChangeItem("Algorithms", "Project", null));
        courses.enterChange(null, null, 0);
        courses.selectChange("change", new ChangeItem("Algorithms", "Quiz 1", null));
        courses.enterChange("Quiz 1", LocalDate.of(2026, 12, 6), 8);
        assertEquals(8, c.getAssessment("Quiz 1").getWeightage(), 1e-9);
        assertTrue(student.getActivePlan().isOutdated());
        assertTrue(courses.isPlanNeedsRegenerating());

        courses.selectChange("add", new ChangeItem("Algorithms", null, "quiz"));
        courses.enterChange("Quiz 2", LocalDate.of(2026, 12, 15), 5);
        assertTrue(c.getAssessment("Quiz 2") instanceof Quiz);

        // would push the total over 100%
        courses.selectChange("add", new ChangeItem("Algorithms", null, "assignment"));
        assertThrows(ValidationException.class, () -> courses.enterChange("Big", LocalDate.of(2026, 12, 30), 50));

        courses.selectChange("remove", new ChangeItem("Algorithms", "Quiz 2", null));
        courses.enterChange(null, null, 0);
        assertNull(c.getAssessment("Quiz 2"));
    }

    @Test
    void uc04_generatePlan_respectsAvailabilityDaysOffAndExamDates() {
        importAlgorithms();
        plan.generateStudyPlan();
        StudyPlan p = plan.enterAvailability(2, List.of("SUNDAY"));
        assertFalse(p.getSessions().isEmpty());
        for (StudySession s : p.getSessions()) {
            assertNotEquals("SUNDAY", s.getScheduledDate().getDayOfWeek().name());
            assertTrue(p.getTotalPlannedTime(s.getScheduledDate()) <= 120, "never above availability");
            assertTrue(s.getScheduledDate().isBefore(s.getTopic().getDueDate()), "planned before its exam");
            assertFalse(s.getScheduledDate().isBefore(AppClock.today()));
        }
        assertSame(p, student.getActivePlan());
    }

    @Test
    void uc04_noHoursAnywhere_isRejected() {
        importAlgorithms();
        assertThrows(ValidationException.class, () -> plan.enterAvailability(0, List.of()));
    }

    @Test
    void uc05_completeTopic_awardsPointsStreakAndReplans() {
        importAlgorithms();
        plan.enterAvailability(2, List.of());
        Topic t = plan.selectTopic("Algorithms :: Linked Lists");
        int before = student.getActivePlan().getSessions().size();
        int pts = plan.completeTopic(45);
        assertTrue(pts >= 10 && pts <= 20);
        assertEquals(pts, student.getTotalPoints());
        assertEquals(1, plan.getLastStreak());
        assertTrue(t.isCompleted());
        assertNotNull(plan.getLastNextTopic());
        assertNotSame(t, plan.getLastNextTopic());
        for (StudySession s : student.getActivePlan().getSessions()) {
            if (s.getTopic() == t) assertTrue(s.isDone(), "no planned sessions left for a finished topic");
        }
        assertTrue(student.getActivePlan().getSessions().size() < before);
        assertThrows(ValidationException.class, () -> t.complete(10)); // already done
        plan.selectTopic("Algorithms :: Linked Lists");
        assertThrows(ValidationException.class, () -> plan.completeTopic(10)); // 3a: nothing awarded twice
        assertEquals(pts, student.getTotalPoints());

        // *a: undo the same day takes the points back and the topic is pending again
        plan.undoCompletion("Algorithms :: Linked Lists");
        assertFalse(t.isCompleted());
        assertEquals(0, student.getTotalPoints());
        assertEquals(0, student.getCurrentStreak());
    }

    @Test
    void uc05_streakCountsCompletionDaysAndRestarts() {
        importAlgorithms();
        plan.enterAvailability(2, List.of());
        plan.selectTopic("Algorithms :: Introduction");
        plan.completeTopic(30);
        AppClock.setFixed(LocalDateTime.of(2026, 11, 27, 9, 0));
        plan.selectTopic("Algorithms :: Linked Lists");
        plan.completeTopic(30);
        assertEquals(2, plan.getLastStreak());
        // a logged score alone does not extend the streak
        AppClock.setFixed(LocalDateTime.of(2026, 11, 30, 9, 0));
        plan.selectTopic("Algorithms :: Trees");
        plan.completeTopic(30);
        assertEquals(1, plan.getLastStreak());
        assertTrue(plan.isLastStreakRestarted()); // 4a
    }

    @Test
    void uc06_logScore_updatesPercentagePointsAndPlan() {
        Course c = importAlgorithms();
        plan.enterAvailability(2, List.of());
        Score sc = logAt("Algorithms", "Quiz 1", 2, 5);
        assertEquals(40.0, sc.getPercentage(), 1e-9);
        assertEquals(40.0, courses.getLastCurrentPercentage(), 1e-9);
        assertEquals(10, courses.getLastPointsEarned());
        assertTrue(courses.isLastPlanAdjusted());
        assertEquals(10, student.getTotalPoints());
        // logging the same quiz again corrects the score but gives no second reward
        logAt("Algorithms", "Quiz 1", 3, 5);
        assertEquals(10, student.getTotalPoints());
        assertThrows(ValidationException.class, () -> courses.logScore("Algorithms", "Quiz 1", 9, 5, LocalDate.now()));
        assertThrows(ValidationException.class, () -> courses.logScore("Nope", "Quiz 1", 1, 5, LocalDate.now()));
        assertTrue(c.getAssessment("Quiz 1").isGraded());
        // an assessment that has not happened yet cannot be scored
        assertThrows(ValidationException.class, () -> courses.logScore("Algorithms", "Final Exam", 30, 40, AppClock.today()));
        assertFalse(c.getAssessment("Final Exam").isGraded());
    }

    @Test
    void uc07_dailyPlan_datesAndSessionDetail() {
        importAlgorithms();
        plan.enterAvailability(2, List.of());
        List<StudySession> today = plan.viewDailyPlan();
        assertFalse(today.isEmpty());
        assertEquals(plan.getPlannedMinutes(AppClock.today()), today.size() * 30);
        StudySession s = plan.selectSession(today.get(0).getStartTime());
        assertNotNull(s.getReason());
        assertTrue(s.getTopic().getWeightage() > 0);
        assertTrue(s.getTopic().getDaysLeft(AppClock.today()) >= 0);
        assertTrue(plan.viewPlanForDate(LocalDate.of(2030, 1, 1)).isEmpty());
    }

    @Test
    void uc07_noPlanYet_isExplained() {
        assertThrows(ValidationException.class, () -> plan.viewDailyPlan());
    }

    @Test
    void uc08_priority_ranksByWeightageUrgencyAndWeakness() {
        Course c = importAlgorithms();
        List<Topic> ranked = plan.viewFullRanking();
        assertEquals(5, ranked.size());
        assertSame(ranked.get(0), plan.requestNextTopic());
        for (int i = 1; i < ranked.size(); i++) {
            assertTrue(plan.scoreOf(ranked.get(i - 1)) >= plan.scoreOf(ranked.get(i)));
        }
        assertTrue(plan.explain(ranked.get(0)).contains("No scores logged yet"));
        // a bad quiz makes the topics it covers weaker and the explanation mentions it
        logAt("Algorithms", "Quiz 1", 1, 5);
        Topic week1 = c.getTopics().get(0);
        assertTrue(week1.getWeaknessLevel() > 0.7);
        assertTrue(plan.explain(week1).contains("Quiz 1 scored 20%"));
    }

    @Test
    void uc08_allCaughtUp() {
        assertThrows(ValidationException.class, () -> plan.requestNextTopic());
    }

    @Test
    void uc09_forecastAndRequiredAverage() {
        importAlgorithms();
        courses.selectChange("remove", new ChangeItem("Algorithms", "Project", null));
        courses.enterChange(null, null, 0);
        progress.viewGradeForecast();                                   // UC-09 system operation 1
        assertFalse(progress.hasScores(student.getCourse("Algorithms")));
        logAt("Algorithms", "Quiz 1", 4, 5); // 80% of 5
        logAt("Algorithms", "Assignment 1", 8, 10); // 80% of 10
        Course c = student.getCourse("Algorithms");
        progress.viewGradeForecast();
        assertEquals(80.0, progress.currentPercentage(c), 1e-9);
        assertEquals(80.0, progress.forecastPercentage(c), 1e-9);
        assertEquals("B+", progress.forecastLetter(c)); // 80% is B+ on the default scale
        assertTrue(progress.isApproximate(c)); // weights total 80, not 100

        // earned 12 of a total weight of 80 (15% graded). To reach 90% of 80 = 72, remaining 65 must give 60.
        double req = progress.enterTargetGrade("Algorithms", "A+");
        assertEquals(60.0 / 65.0 * 100.0, req, 1e-9);
        assertEquals("A+", c.getTargetGrade());
        assertTrue(progress.enterTargetGrade("Algorithms", "100") > 100); // not reachable
        assertThrows(ValidationException.class, () -> progress.enterTargetGrade("Algorithms", "Z"));
    }

    @Test
    void uc10_pace_behindThenCaughtUp() {
        AppClock.setFixed(LocalDateTime.of(2026, 12, 8, 9, 0)); // weeks 1-2 are over
        Course c = importAlgorithms();
        progress.viewStudyPace();                                       // UC-10 system operation 1
        assertEquals("Behind", progress.paceStatus(c));
        assertEquals(-2, progress.aheadOrBehind(c));
        assertEquals("Behind", progress.overallStatus());
        assertEquals(2, progress.selectCourse("Algorithms").size());
        assertTrue(progress.extraHoursPerDayNeeded() > 0);
        assertTrue(progress.viewStudyPace().get(0).contains("2 topics behind"));

        plan.selectTopic("Algorithms :: Introduction");
        plan.completeTopic(30);
        plan.selectTopic("Algorithms :: Linked Lists");
        plan.completeTopic(30);
        progress.viewStudyPace();
        assertEquals("On track", progress.paceStatus(c));
        plan.selectTopic("Algorithms :: Stacks and Queues");
        plan.completeTopic(30);
        progress.viewStudyPace();
        assertEquals("Ahead", progress.paceStatus(c));
    }

    @Test
    void uc11_graphs_gradeAndStudyTime() {
        Course c = importAlgorithms();
        logAt("Algorithms", "Quiz 1", 4, 5);
        plan.selectTopic("Algorithms :: Introduction");
        plan.completeTopic(90);
        List<ProgressGraph> graphs = progress.selectGraphScope(List.of(c), progress.wholeSemester());
        assertEquals(2, graphs.size());
        assertEquals(1, graphs.get(0).getPoints().size());
        assertEquals(80.0, graphs.get(0).getPoints().get(0).getValue(), 1e-9);
        assertEquals(1.5, graphs.get(1).getPoints().get(0).getValue(), 1e-9);
        GraphPoint p = graphs.get(1).getPoints().get(0);
        assertEquals(p, progress.selectGraphPoint(new GraphPoint(p.getDate(), p.getValue())));
        assertThrows(ValidationException.class, () -> progress.selectGraphPoint(new GraphPoint(LocalDate.of(1999, 1, 1), 1)));
        // a period without data gives empty graphs
        List<ProgressGraph> none = progress.selectGraphScope(List.of(c),
                new DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1)));
        assertTrue(none.get(0).getPoints().isEmpty());
        assertThrows(ValidationException.class, () -> progress.selectGraphScope(List.of(), null));
    }

    @Test
    void uc12_leaderboard_everyoneFriendsClassmates() {
        importAlgorithms();
        RegistrationController reg = new RegistrationController(registry, store);
        Student other = reg.register("Other Person", "other", "i250002", "", "secret12", "secret12");
        Student third = reg.register("Third Person", "third", "i250003", "", "secret12", "secret12");
        other.createCourse("Algorithms", "CS2001", LocalDate.of(2026, 11, 23)); // same course code = classmate
        other.addPoints(50, "test");
        third.addPoints(5, "test");
        student.addPoints(20, "test");

        List<Student> all = progress.viewLeaderboard();
        assertEquals(List.of(other, student, third), all);
        assertEquals(2, progress.getOwnPosition());

        assertEquals(List.of(other, student), progress.filterLeaderboard("classmates"));
        assertEquals(List.of(student), progress.filterLeaderboard("friends"));
        progress.addFriend("i250003");
        assertEquals(List.of(student, third), progress.filterLeaderboard("friends"));
        assertThrows(ValidationException.class, () -> progress.addFriend("nobody"));
    }

    @Test
    void storage_everythingSurvivesReloadFromTheDatabase() {
        Course c = importAlgorithms();
        plan.enterAvailability(3, List.of("SATURDAY", "SUNDAY"));
        plan.selectTopic("Algorithms :: Introduction");
        plan.completeTopic(40);
        logAt("Algorithms", "Quiz 1", 3, 5);
        progress.enterTargetGrade("Algorithms", "B+");
        new RegistrationController(registry, store).register("Friend", "friend", "i250009", "", "secret12", "secret12");
        progress.addFriend("i250009");

        List<Student> loaded = store.loadAllStudents();
        assertEquals(2, loaded.size());
        Student s = loaded.stream().filter(x -> x.getStudentId().equals("i250001")).findFirst().orElseThrow();
        assertEquals(student.getTotalPoints(), s.getTotalPoints());
        assertEquals(student.getCurrentStreak(), s.getCurrentStreak());
        assertEquals(student.getFullName(), s.getFullName());
        assertEquals(1, s.getFriends().size());
        assertEquals(0, s.getAvailabilityFor(java.time.DayOfWeek.SUNDAY) == null ? -1 : (int) s.getAvailabilityFor(java.time.DayOfWeek.SUNDAY).getAvailableHours());
        assertEquals(3.0, s.getAvailabilityFor(java.time.DayOfWeek.MONDAY).getAvailableHours(), 1e-9);

        Course lc = s.getCourse("Algorithms");
        assertEquals(5, lc.getTopics().size());
        assertEquals(1, lc.countCompletedTopics());
        assertEquals(5, lc.getAssessments().size());
        assertEquals(60.0, lc.getAssessment("Quiz 1").getScore().getPercentage(), 1e-9);
        assertEquals("B+", lc.getTargetGrade());
        assertTrue(lc.getAssessment("Mid-term") instanceof Exam);
        assertEquals(c.getCurrentPercentage(), lc.getCurrentPercentage(), 1e-9);

        StudyPlan lp = s.getActivePlan();
        assertNotNull(lp);
        assertEquals(student.getActivePlan().getSessions().size(), lp.getSessions().size());
        assertEquals(student.getActivePlan().getStatus(), lp.getStatus());

        // the registry can sign the reloaded account in
        StudentRegistry fresh = new StudentRegistry();
        loaded.forEach(fresh::addExisting);
        assertNotNull(fresh.authenticate("t@x.com", "secret12"));
        assertNull(fresh.authenticate("t@x.com", "nope"));
    }

    @Test
    void parser_handlesCommonFormats() {
        LineFormatSyllabusParser p = new LineFormatSyllabusParser();
        List<String[]> outline = p.parseOutline("Week 1: Intro\n2. Arrays\n3 - Sorting\nHashing\n");
        assertEquals("1", outline.get(0)[0]);
        assertEquals("Arrays", outline.get(1)[1]);
        assertEquals("Sorting", outline.get(2)[1]);
        assertEquals(3, outline.size()); // UC-02 4a: a line without a week number is not read as a topic
        assertTrue(p.getWarnings().get(0).contains("Hashing"));
        List<String[]> a = p.parseAssessments("Quiz 1 | 5 Dec 2026 | 5\nMidterm; 20/12/2026; 25%\nProject, 2027-01-05, 10 marks");
        assertEquals(3, a.size());
        assertEquals("quiz", a.get(0)[0]);
        assertEquals("2026-12-05", a.get(0)[2]);
        assertEquals("mid-term", a.get(1)[0]);
        assertEquals("assignment", a.get(2)[0]);
        assertEquals("10.0", a.get(2)[3]);
    }

    @Test
    void settings_changeRankingAndMarkPlanOutdated() {
        importAlgorithms();
        plan.enterAvailability(2, List.of());
        plan.updateSettings(1, 0, 0);
        assertTrue(student.getActivePlan().isOutdated());
        assertEquals(1.0, student.getSettings().getWeightageFactor(), 1e-9);
        assertThrows(ValidationException.class, () -> plan.updateSettings(0, 0, 0));
    }

    // ------------------------------------------------------------------ conformance with the D5 sequence diagrams

    @Test
    void uc01_displayNameIsCheckedWhenTheStudentIsCreated() {
        RegistrationController reg = new RegistrationController(registry, store);
        ValidationException e = assertThrows(ValidationException.class,
                () -> reg.register("A Person", "ab", "i250005", "", "secret12", "secret12"));
        assertEquals("displayName", e.getField());
        assertEquals(1, registry.getAllStudents().size()); // nothing was created
    }

    @Test
    void uc09_targetGradeIsNotSaved() {
        importAlgorithms();
        logAt("Algorithms", "Quiz 1", 4, 5);
        progress.viewGradeForecast();
        progress.enterTargetGrade("Algorithms", "A");
        // post-condition 2: viewing the forecast changes nothing in the database
        Student reloaded = store.loadStudent(student.getStudentId());
        assertNull(reloaded.getCourse("Algorithms").getTargetGrade());
    }

    @Test
    void uc07_selectSessionReturnsWhatTheDiagramAsksFor() {
        importAlgorithms();
        plan.generateStudyPlan();
        plan.enterAvailability(3, List.of());
        List<StudySession> today = plan.viewDailyPlan();
        assertFalse(today.isEmpty());
        StudySession ss = plan.selectSession(today.get(0).getStartTime());
        assertEquals(ss.getReason(), plan.getSelectedReason());
        assertEquals(ss.getTopic().getWeightage(), plan.getSelectedWeightage(), 1e-9);
        assertEquals(ss.getTopic().getDaysLeft(AppClock.today()), plan.getSelectedDaysLeft());
        assertEquals(today.size() * 30, plan.getPlannedMinutes(AppClock.today()));
    }

    @Test
    void uc12_positionComesWithTheRanking() {
        progress.viewLeaderboard();
        assertEquals(1, progress.getOwnPosition());
    }
}
