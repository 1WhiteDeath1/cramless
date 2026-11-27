package com.cramless.model;

import com.cramless.util.ValidationException;
import java.time.LocalDate;

/** A graded item of a course (quiz, assignment or exam). Abstract: each kind has its own point reward. */
public abstract class Assessment {
    protected String name;
    protected LocalDate date;
    protected double weightage;
    protected String weightageUnit = "%";
    private Score score; // 0..1

    protected Assessment(String name, LocalDate date, double weightage) {
        this.name = name;
        this.date = date;
        this.weightage = weightage;
    }

    /** Creator: an Assessment records its own Score. */
    public Score recordScore(double marksObtained, double totalMarks, LocalDate scoreDate) {
        LocalDate today = com.cramless.util.AppClock.today();
        java.time.format.DateTimeFormatter day = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH);
        if (date != null && date.isAfter(today))
            throw new ValidationException(name + " is on " + date.format(day) + ". You can log its score once it has taken place.");
        if (scoreDate == null) scoreDate = today;
        if (scoreDate.isAfter(today)) throw new ValidationException("The score date cannot be in the future.");
        if (date != null && scoreDate.isBefore(date))
            throw new ValidationException("The score date cannot be before " + name + " (" + date.format(day) + ").");
        if (totalMarks <= 0) throw new ValidationException("Total marks must be greater than zero.");
        if (marksObtained < 0) throw new ValidationException("Marks obtained cannot be negative.");
        if (marksObtained > totalMarks) throw new ValidationException("Marks obtained cannot be more than the total marks.");
        this.score = new Score(marksObtained, totalMarks, scoreDate == null ? date : scoreDate);
        return score;
    }

    /** Used when loading from the database. */
    public void restoreScore(double marks, double total, LocalDate scoreDate) {
        this.score = new Score(marks, total, scoreDate);
    }

    public Score getScore() { return score; }
    public boolean isGraded() { return score != null; }

    public void update(String newName, LocalDate newDate, double newWeightage) {
        this.name = newName;
        this.date = newDate;
        this.weightage = newWeightage;
    }

    public String getName() { return name; }
    public LocalDate getDate() { return date; }
    public double getWeightage() { return weightage; }
    public String getWeightageUnit() { return weightageUnit; }

    /** "Quiz", "Assignment" or "Exam". */
    public abstract String getKind();

    /** Detail used when saving: the exam type for exams, otherwise the kind itself. */
    public String getTypeDetail() { return getKind().toLowerCase(); }

    public abstract int getPointsReward();

    @Override
    public String toString() { return name; }
}
