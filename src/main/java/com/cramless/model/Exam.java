package com.cramless.model;

import java.time.LocalDate;

public class Exam extends Assessment {
    private final String examType; // "mid-term" or "final"

    public Exam(String examType, String name, LocalDate date, double weightage) {
        super(name, date, weightage);
        this.examType = examType;
    }

    public String getExamType() { return examType; }

    @Override public String getKind() { return "Exam"; }
    @Override public String getTypeDetail() { return examType; }
    @Override public int getPointsReward() { return 30; }
}
