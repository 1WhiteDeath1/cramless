package com.cramless.model;

import java.time.LocalDate;
import java.util.Objects;

public class GraphPoint {
    private final LocalDate date;
    private final double value;

    public GraphPoint(LocalDate date, double value) {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate() { return date; }
    public double getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GraphPoint)) return false;
        GraphPoint p = (GraphPoint) o;
        return date.equals(p.date) && Double.compare(value, p.value) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(date, value); }
}
