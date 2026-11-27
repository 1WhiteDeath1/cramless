package com.cramless.util;

import java.time.LocalDate;

/** The "period" chosen on the progress-graph screen. */
public final class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    public DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public LocalDate getStart() { return start; }
    public LocalDate getEnd() { return end; }

    public boolean contains(LocalDate d) {
        return d != null && (start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end));
    }

    public static DateRange unbounded() {
        return new DateRange(null, null);
    }

    @Override
    public String toString() {
        return (start == null ? "…" : start) + " to " + (end == null ? "…" : end);
    }
}
