package com.cramless.util;

/**
 * Raised when something the student entered is not acceptable; the message is shown to them.
 * {@link #getField()} names the input the problem belongs to, so the screen can show it next to that field.
 */
public class ValidationException extends RuntimeException {
    private final String field;

    public ValidationException(String message) {
        this(null, message);
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The input this message is about ("studentId", "displayName", ...), or null for a general message. */
    public String getField() { return field; }
}
