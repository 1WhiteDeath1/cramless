package com.cramless.model;

import java.time.LocalDate;

public class Assignment extends Assessment {
    public Assignment(String name, LocalDate date, double weightage) {
        super(name, date, weightage);
    }

    @Override public String getKind() { return "Assignment"; }
    @Override public int getPointsReward() { return 15; }
}
