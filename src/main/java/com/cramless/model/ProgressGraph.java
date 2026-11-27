package com.cramless.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProgressGraph {
    private final String title;
    private final String valueLabel;
    private final List<GraphPoint> points = new ArrayList<>();

    public ProgressGraph(String title, String valueLabel) {
        this.title = title;
        this.valueLabel = valueLabel;
    }

    /** Creator: a graph contains its points, so it creates them. */
    public void addPoint(LocalDate date, double value) {
        points.add(new GraphPoint(date, value));
    }

    /** Returns the stored point that matches the one the student picked, or null. */
    public GraphPoint findPoint(GraphPoint wanted) {
        for (GraphPoint p : points) if (p.equals(wanted)) return p;
        return null;
    }

    public String getTitle() { return title; }
    public String getValueLabel() { return valueLabel; }
    public List<GraphPoint> getPoints() { return points; }
}
