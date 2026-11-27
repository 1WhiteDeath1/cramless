package com.cramless.model;

public class GradeBand {
    private final String letterGrade;
    private final double minimumPercentage;
    private final double gpaPoints;

    public GradeBand(String letterGrade, double minimumPercentage, double gpaPoints) {
        this.letterGrade = letterGrade;
        this.minimumPercentage = minimumPercentage;
        this.gpaPoints = gpaPoints;
    }

    public String getLetterGrade() { return letterGrade; }
    public double getMinimumPercentage() { return minimumPercentage; }
    public double getGpaPoints() { return gpaPoints; }
}
