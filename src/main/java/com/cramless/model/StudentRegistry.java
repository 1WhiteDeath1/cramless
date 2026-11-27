package com.cramless.model;

import com.cramless.util.PasswordHasher;
import com.cramless.util.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Knows every registered Student: validates new accounts, creates them and finds classmates. */
public class StudentRegistry {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_\\-]{3,20}$");
    private String lastField;

    private final List<Student> students = new ArrayList<>();
    private String lastError = "";

    /** Checks the sign-up details. When it returns false, {@link #getLastError()} says why. */
    public boolean validateDetails(String studentId, String email, String password, String confirmPassword) {
        // UC-01 4a: student ID is the roll number or the university email address.
        String id = studentId == null ? "" : studentId.trim();
        if (id.isEmpty()) return fail("studentId", "Enter your student ID or university email.");
        if (!ID.matcher(id).matches() && !EMAIL.matcher(id).matches()) {
            return fail("studentId", "Use your roll number (for example 25i-0644) or your university email.");
        }
        if (email != null && !email.isBlank() && !EMAIL.matcher(email.trim()).matches()) {
            return fail("email", "That email address does not look right.");
        }
        // UC-01 4b, special requirement 4: at least 8 characters with a letter and a digit.
        if (password == null || password.length() < 8) return fail("password", "Use at least 8 characters.");
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            return fail("password", "Use at least one letter and one digit.");
        }
        if (!password.equals(confirmPassword)) return fail("confirmPassword", "The two passwords do not match.");
        lastError = "";
        lastField = null;
        return true;
    }

    private boolean validateName(String fullName, String displayName) {
        if (fullName == null || fullName.isBlank()) return fail("fullName", "Enter your full name.");
        // UC-01 special requirement 3: 3 to 20 characters.
        String d = displayName == null ? "" : displayName.trim();
        if (d.length() < 3 || d.length() > 20) return fail("displayName", "Display name must be 3 to 20 characters.");
        lastError = "";
        lastField = null;
        return true;
    }

    public boolean isTaken(String studentId, String email, String displayName) {
        for (Student s : students) {
            if (s.getStudentId().equalsIgnoreCase(studentId.trim())) return takenMsg("studentId", "That student ID is already registered.");
            if (email != null && !email.isBlank() && s.getEmail() != null
                    && s.getEmail().equalsIgnoreCase(email.trim())) return takenMsg("email", "That email is already registered.");
            if (s.getDisplayName().equalsIgnoreCase(displayName.trim())) return takenMsg("displayName", "That display name is taken. Try another.");
        }
        return false;
    }

    private boolean takenMsg(String field, String msg) { lastField = field; lastError = msg; return true; }
    private boolean fail(String msg) { return fail(null, msg); }
    private boolean fail(String field, String msg) { lastField = field; lastError = msg; return false; }

    /** The input the last problem was about, or null. */
    public String getLastField() { return lastField; }

    /** Creator: the registry contains Students, so it creates them. */
    /** UC-01 2.3. As the creator of Student it also checks the name rules (special requirement 3). */
    public Student createStudent(String fullName, String displayName, String studentId, String email, String password) {
        if (!validateName(fullName, displayName)) throw new ValidationException(lastField, lastError);
        String mail = email == null || email.isBlank() ? null : email.trim();
        Student s = new Student(fullName.trim(), displayName.trim(), studentId.trim(), mail, PasswordHasher.hash(password));
        students.add(s);
        return s;
    }

    /** Adds a student loaded from the database. */
    public void addExisting(Student s) { students.add(s); }

    /** Finds the account for a student ID or email whose password matches, or null. */
    public Student authenticate(String idOrEmail, String password) {
        if (idOrEmail == null || password == null) return null;
        for (Student s : students) {
            boolean idMatch = s.getStudentId().equalsIgnoreCase(idOrEmail.trim())
                    || (s.getEmail() != null && s.getEmail().equalsIgnoreCase(idOrEmail.trim()));
            if (idMatch && PasswordHasher.matches(password, s.getPasswordHash())) return s;
        }
        return null;
    }

    public Student findById(String studentId) {
        for (Student s : students) if (s.getStudentId().equalsIgnoreCase(studentId.trim())) return s;
        return null;
    }

    public List<Student> getAllStudents() { return new ArrayList<>(students); }

    /** Other students who take at least one course with the same code (or, without a code, the same name). */
    public List<Student> getClassmates(Student s) {
        List<Student> out = new ArrayList<>();
        for (Student other : students) {
            if (other == s) continue;
            if (sharesCourse(s, other)) out.add(other);
        }
        return out;
    }

    private boolean sharesCourse(Student a, Student b) {
        for (Course ca : a.getCourses()) {
            for (Course cb : b.getCourses()) {
                String ka = ca.getCourseCode() != null ? ca.getCourseCode() : ca.getName();
                String kb = cb.getCourseCode() != null ? cb.getCourseCode() : cb.getName();
                if (ka.equalsIgnoreCase(kb)) return true;
            }
        }
        return false;
    }

    public String getLastError() { return lastError; }
}
