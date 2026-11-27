package com.cramless.model;

import java.time.LocalDate;

public class Quiz extends Assessment {
    public Quiz(String name, LocalDate date, double weightage) {
        super(name, date, weightage);
    }

    @Override public String getKind() { return "Quiz"; }
    @Override public int getPointsReward() { return 10; }
}
