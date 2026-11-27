package com.cramless.storage;

import com.cramless.model.Student;
import java.util.List;

/**
 * Pure Fabrication: keeps all database code out of the domain classes. Only controllers use it.
 * {@code save} accepts a Student, or any object that belongs to one (Course, StudyPlan) and saves that
 * student's data.
 */
public interface PersistentStorage {
    void save(Object obj);

    Student loadStudent(String studentId);

    /** Loads every account, with friendships linked, so leaderboards and classmates work. */
    List<Student> loadAllStudents();
}
