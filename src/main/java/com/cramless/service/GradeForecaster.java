package com.cramless.service;

import com.cramless.model.Course;
import com.cramless.util.ValidationException;

/**
 * Projects a final grade. Rule: the assessments that are still to come are assumed to go as well as
 * the average so far. It is an estimate, never a guarantee.
 */
public class GradeForecaster {

    /** Projected final percentage (0-100). Returns 0 when nothing is graded yet; check {@link Course#hasScores()}. */
    public double forecast(Course c) {
        double total = c.getTotalWeight();
        double graded = c.getGradedWeight();
        if (total <= 0 || graded <= 0) return 0;
        double current = c.getCurrentPercentage();
        double earned = current * graded / 100.0;
        double remaining = total - graded;
        double projected = (earned + remaining * current / 100.0) / total * 100.0;
        return Math.max(0, Math.min(100, projected));
    }

    public String toLetter(Course c, double percentage) {
        return c.getGradeScale().toLetter(percentage);
    }

    /**
     * Average the student must score on the remaining assessments to reach the target.
     * The target is a letter ("A-") or a percentage ("85"). The result can be above 100 (not reachable)
     * or below 0 (already secured).
     */
    public double requiredAverage(Course c, String targetGrade) {
        if (targetGrade == null || targetGrade.isBlank()) throw new ValidationException("Please enter a target grade.");
        double minimum = parseTarget(c, targetGrade.trim());
        double total = c.getTotalWeight();
        if (total <= 0) throw new ValidationException("This course has no assessments with weightage yet.");
        double graded = c.getGradedWeight();
        double remaining = total - graded;
        if (remaining <= 0.0001) throw new ValidationException("Every assessment of this course is already graded.");
        double earned = c.getCurrentPercentage() * graded / 100.0;
        double needed = minimum * total / 100.0 - earned;
        return needed / remaining * 100.0;
    }

    private double parseTarget(Course c, String target) {
        try {
            double v = Double.parseDouble(target.replace("%", "").trim());
            if (v < 0 || v > 100) throw new ValidationException("A target percentage must be between 0 and 100.");
            return v;
        } catch (NumberFormatException e) {
            return c.getGradeScale().minimumFor(target);
        }
    }
}
