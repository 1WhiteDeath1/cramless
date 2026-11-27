package com.cramless.model;

import com.cramless.util.ValidationException;
import java.util.ArrayList;
import java.util.List;

/** Maps percentages to letter grades. The default scale is an absolute-grading scale. */
public class GradeScale {
    private final String name;
    private final boolean isDefault;
    private final List<GradeBand> bands = new ArrayList<>(); // highest first

    public GradeScale(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }

    public static GradeScale defaultScale() {
        GradeScale g = new GradeScale("Default (absolute)", true);
        g.bands.add(new GradeBand("A+", 90, 4.00));
        g.bands.add(new GradeBand("A", 86, 4.00));
        g.bands.add(new GradeBand("A-", 82, 3.67));
        g.bands.add(new GradeBand("B+", 78, 3.33));
        g.bands.add(new GradeBand("B", 74, 3.00));
        g.bands.add(new GradeBand("B-", 70, 2.67));
        g.bands.add(new GradeBand("C+", 66, 2.33));
        g.bands.add(new GradeBand("C", 62, 2.00));
        g.bands.add(new GradeBand("C-", 58, 1.67));
        g.bands.add(new GradeBand("D+", 54, 1.33));
        g.bands.add(new GradeBand("D", 50, 1.00));
        g.bands.add(new GradeBand("F", 0, 0.00));
        return g;
    }

    public String toLetter(double percentage) {
        for (GradeBand b : bands) {
            if (percentage >= b.getMinimumPercentage()) return b.getLetterGrade();
        }
        return bands.get(bands.size() - 1).getLetterGrade();
    }

    public double gpaFor(double percentage) {
        for (GradeBand b : bands) {
            if (percentage >= b.getMinimumPercentage()) return b.getGpaPoints();
        }
        return 0;
    }

    public double minimumFor(String letter) {
        String wanted = letter == null ? "" : letter.trim();
        for (GradeBand b : bands) {
            if (b.getLetterGrade().equalsIgnoreCase(wanted)) return b.getMinimumPercentage();
        }
        throw new ValidationException("\"" + letter + "\" is not a grade on this scale.");
    }

    public String getName() { return name; }
    public boolean isDefault() { return isDefault; }
    public List<GradeBand> getBands() { return bands; }
}
