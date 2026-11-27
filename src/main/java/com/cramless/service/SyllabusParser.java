package com.cramless.service;

import java.util.List;

/** Turns pasted syllabus text into raw topic and assessment entries. */
public interface SyllabusParser {

    /** Each entry is {weekNumber, title}. */
    List<String[]> parseOutline(String outline);

    /** Each entry is {type, name, date (yyyy-MM-dd), weightage}. */
    List<String[]> parseAssessments(String text);

    /** Lines of the last parse that could not be understood, with the reason. */
    List<String> getWarnings();
}
