package com.cramless.model;

import java.time.LocalDate;

public class Score {
    private final double marksObtained;
    private final double totalMarks;
    private final LocalDate date;

    public Score(double marksObtained, double totalMarks, LocalDate date) {
        this.marksObtained = marksObtained;
        this.totalMarks = totalMarks;
        this.date = date;
    }

    public double getMarksObtained() { return marksObtained; }
    public double getTotalMarks() { return totalMarks; }
    public LocalDate getDate() { return date; }

    /** Marks obtained as a percentage (0-100). */
    public double getPercentage() {
        return totalMarks <= 0 ? 0 : marksObtained / totalMarks * 100.0;
    }
}
