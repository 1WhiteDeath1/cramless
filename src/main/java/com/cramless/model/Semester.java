package com.cramless.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Semester {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private final List<Course> courses = new ArrayList<>();

    public Semester(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void addCourse(Course c) { courses.add(c); }

    /** Used when an import replaces a course with the same code (UC-02 6a), or rolls back a failed import. */
    public void removeCourse(Course c) { courses.remove(c); }

    public String getName() { return name; }
    public LocalDate getStartDate() {
        LocalDate d = startDate;
        for (Course c : courses) if (c.getWeek1Date() != null && (d == null || c.getWeek1Date().isBefore(d))) d = c.getWeek1Date();
        return d;
    }

    public LocalDate getEndDate() {
        LocalDate d = endDate;
        for (Course c : courses) {
            for (Assessment a : c.getAssessments()) if (a.getDate() != null && (d == null || a.getDate().isAfter(d))) d = a.getDate();
        }
        return d;
    }
    public void setStartDate(LocalDate d) { this.startDate = d; }
    public void setEndDate(LocalDate d) { this.endDate = d; }
    public List<Course> getCourses() { return courses; }
}
