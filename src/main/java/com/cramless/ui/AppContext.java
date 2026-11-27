package com.cramless.ui;

import com.cramless.controller.CourseController;
import com.cramless.controller.ProgressController;
import com.cramless.controller.RegistrationController;
import com.cramless.controller.StudyPlanController;
import com.cramless.model.Leaderboard;
import com.cramless.model.Student;
import com.cramless.model.StudentRegistry;
import com.cramless.service.GradeForecaster;
import com.cramless.service.LineFormatSyllabusParser;
import com.cramless.service.PaceTracker;
import com.cramless.service.PriorityAdvisor;
import com.cramless.service.ProgressGraphBuilder;
import com.cramless.service.StudyPlanGenerator;
import com.cramless.storage.PersistentStorage;

/** Wires the domain, services, storage and controllers together for the signed-in student. */
final class AppContext {
    final PersistentStorage store;
    final StudentRegistry registry = new StudentRegistry();
    final RegistrationController registration;

    Student student;
    CourseController courses;
    StudyPlanController plan;
    ProgressController progress;

    AppContext(PersistentStorage store) {
        this.store = store;
        for (Student s : store.loadAllStudents()) registry.addExisting(s);
        this.registration = new RegistrationController(registry, store);
    }

    /** Creates the three session controllers for the student who just signed in. */
    void signIn(Student s) {
        this.student = s;
        PriorityAdvisor advisor = new PriorityAdvisor();
        StudyPlanGenerator generator = new StudyPlanGenerator(advisor);
        this.courses = new CourseController(s, new LineFormatSyllabusParser(), generator, store);
        this.plan = new StudyPlanController(s, generator, advisor, store);
        this.progress = new ProgressController(s, registry, store, new GradeForecaster(), new PaceTracker(),
                new ProgressGraphBuilder(), new Leaderboard());
    }

    void signOut() {
        student = null;
        courses = null;
        plan = null;
        progress = null;
    }
}
