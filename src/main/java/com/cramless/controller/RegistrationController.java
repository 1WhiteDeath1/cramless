package com.cramless.controller;

import com.cramless.model.Student;
import com.cramless.model.StudentRegistry;
import com.cramless.storage.PersistentStorage;
import com.cramless.util.ValidationException;
import java.util.Arrays;
import java.util.List;

/** Session controller for UC-01 (and sign-in). Receives the system events and only delegates. */
public class RegistrationController {
    private final StudentRegistry registry;
    private final PersistentStorage store;

    public RegistrationController(StudentRegistry registry, PersistentStorage store) {
        this.registry = registry;
        this.store = store;
    }

    /** UC-01 step 1: the details the student has to give. */
    public List<String> startRegistration() {
        return Arrays.asList("Full name", "Display name", "Student ID or university email", "Email (optional)", "Password", "Confirm password");
    }

    /** UC-01 step 2: validate, check for duplicates, create the account and save it. */
    public Student register(String fullName, String displayName, String studentId, String email,
                            String password, String confirmPassword) {
        // UC-01 sequence diagram: 2.1 validateDetails, 2.2 isTaken, 2.3 createStudent, 2.4 save.
        if (!registry.validateDetails(studentId, email, password, confirmPassword)) {                  // 2.1
            throw new ValidationException(registry.getLastField(), registry.getLastError());
        }
        if (registry.isTaken(studentId, email, displayName)) {                                         // 2.2
            throw new ValidationException(registry.getLastField(), registry.getLastError());
        }
        Student s = registry.createStudent(fullName, displayName, studentId, email, password);       // 2.3
        store.save(s);                                                                                 // 2.4
        return s;
    }

    /** Signs in with a student ID or email. */
    public Student login(String idOrEmail, String password) {
        Student s = registry.authenticate(idOrEmail, password);
        if (s == null) throw new ValidationException("Wrong student ID or password.");
        return s;
    }

    public StudentRegistry getRegistry() { return registry; }
}
