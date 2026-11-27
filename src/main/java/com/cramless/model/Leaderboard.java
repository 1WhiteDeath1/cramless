package com.cramless.model;

import com.cramless.util.AppClock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ranks students by points, then by streak. */
public class Leaderboard {
    private String scope = "everyone";
    private LocalDateTime lastRefreshed;
    private List<Student> ranked = new ArrayList<>();

    public void refresh(List<Student> students, String scope) {
        this.scope = scope;
        this.ranked = new ArrayList<>(students);
        sortByPointsThenStreak();
        this.lastRefreshed = AppClock.now();
    }

    public List<Student> getRanking() { return ranked; }

    /** 1-based rank of the student, or 0 when not on the board. */
    public int getPositionOf(Student s) {
        int i = ranked.indexOf(s);
        return i < 0 ? 0 : i + 1;
    }

    private void sortByPointsThenStreak() {
        ranked.sort(Comparator.comparingInt(Student::getTotalPoints).reversed()
                .thenComparing(Comparator.comparingInt(Student::getCurrentStreak).reversed())
                .thenComparing(Student::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    public String getScope() { return scope; }
    public LocalDateTime getLastRefreshed() { return lastRefreshed; }
}
