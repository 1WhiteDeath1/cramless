package com.cramless.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Single source of "now" so the app (and the tests) agree on today's date. */
public final class AppClock {
    private static Clock clock = Clock.systemDefaultZone();

    private AppClock() {}

    public static LocalDate today() {
        return LocalDate.now(clock);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** For tests: freeze time at the given moment. */
    public static void setFixed(LocalDateTime moment) {
        ZoneId zone = ZoneId.systemDefault();
        clock = Clock.fixed(moment.atZone(zone).toInstant(), zone);
    }

    public static void reset() {
        clock = Clock.systemDefaultZone();
    }
}
