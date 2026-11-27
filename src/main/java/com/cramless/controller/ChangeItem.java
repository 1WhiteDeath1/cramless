package com.cramless.controller;

/** What the student picked on the Manage Courses screen: a course and, for change/remove, one assessment of it. */
public class ChangeItem {
    private final String courseName;
    private final String name;  // assessment name (null when adding)
    private final String type;  // quiz / assignment / mid-term / final (used when adding)

    public ChangeItem(String courseName, String name, String type) {
        this.courseName = courseName;
        this.name = name;
        this.type = type;
    }

    public String getCourseName() { return courseName; }
    public String getName() { return name; }
    public String getType() { return type; }
}
