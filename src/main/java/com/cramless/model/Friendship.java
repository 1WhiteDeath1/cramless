package com.cramless.model;

import java.time.LocalDate;

public class Friendship {
    private final Student friend;
    private final LocalDate dateAdded;

    public Friendship(Student friend, LocalDate dateAdded) {
        this.friend = friend;
        this.dateAdded = dateAdded;
    }

    public Student getFriend() { return friend; }
    public LocalDate getDateAdded() { return dateAdded; }
}
