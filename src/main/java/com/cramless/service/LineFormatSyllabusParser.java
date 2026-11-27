package com.cramless.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one entry per line.
 * <pre>
 * Outline:      Week 1: Introduction          (also "1. Introduction", "1 - Introduction")
 * Assessments:  Quiz 1, 2026-12-05, 5%       (also separated by | or ;)
 * </pre>
 * Dates may be yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy or "5 Dec 2026".
 */
public class LineFormatSyllabusParser implements SyllabusParser {
    private static final Pattern OUTLINE_LINE = Pattern.compile(
            "^\\s*(?:weeks?|wk|w)?\\s*(\\d{1,2})\\s*[:.\\-\\u2013)]\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String[] DATE_PATTERNS = {
            "d/M/yyyy", "yyyy-MM-dd", "d-M-yyyy", "d MMM yyyy", "d MMMM yyyy", "MMM d yyyy", "MMMM d yyyy"
    };

    private final String linePattern = "one entry per line";
    private final List<String> warnings = new ArrayList<>();

    @Override
    public List<String[]> parseOutline(String outline) {
        warnings.removeIf(w -> w.startsWith("Outline"));
        List<String[]> out = new ArrayList<>();
        if (outline == null) return out;
        int lineNo = 0;
        for (String raw : outline.split("\\R")) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher m = OUTLINE_LINE.matcher(line);
            if (m.matches() && !m.group(2).isBlank()) {
                out.add(new String[]{m.group(1), m.group(2)});
            } else {
                // UC-02 4a: a line without a week number or a topic is listed back to the student.
                warnings.add("Outline line " + lineNo + " needs a week number and a topic: \"" + line + "\"");
            }
        }
        return out;
    }

    @Override
    public List<String[]> parseAssessments(String text) {
        warnings.removeIf(w -> w.startsWith("Assessment"));
        List<String[]> out = new ArrayList<>();
        if (text == null) return out;
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s*[|;\\t]\\s*");
            if (parts.length < 3) parts = line.split("\\s*,\\s*");
            if (parts.length < 3) {
                warnings.add("Assessment \"" + line + "\" needs a name, a date and a weightage.");
                continue;
            }
            String name = parts[0].trim();
            LocalDate date = parseDate(parts[1].trim());
            Double weight = parseWeight(parts[2]);
            // UC-02 4c: name the assessment whose date (or weightage) cannot be read.
            if (name.isEmpty()) { warnings.add("Assessment \"" + line + "\" has no name."); continue; }
            if (date == null) { warnings.add("Assessment \"" + name + "\" has an invalid date. Use dd/mm/yyyy."); continue; }
            if (weight == null) { warnings.add("Assessment \"" + name + "\" has an invalid weightage."); continue; }
            boolean twice = false;
            for (String[] e : out) twice |= e[1].equalsIgnoreCase(name);
            if (twice) { warnings.add("Assessment \"" + name + "\" is listed twice."); continue; }
            String type = parts.length > 3 ? normaliseType(parts[3]) : inferType(name);
            out.add(new String[]{type, name, date.toString(), String.valueOf(weight)});
        }
        // UC-02 4d: the weightages must add up to 100%.
        if (warnings.stream().noneMatch(w -> w.startsWith("Assessment"))) {
            double total = 0;
            for (String[] e : out) total += Double.parseDouble(e[3]);
            if (out.isEmpty() || Math.abs(total - 100.0) > 0.01) {
                warnings.add("Assessment weightages add up to " + Math.round(total * 10) / 10.0 + "%. They must add up to 100%.");
            }
        }
        return out;
    }

    /** UC-06 data variation: quiz, assignment, mid-term, final. */
    static String normaliseType(String t) {
        String n = t.trim().toLowerCase(Locale.ROOT);
        if (n.startsWith("quiz")) return "quiz";
        if (n.startsWith("mid")) return "mid-term";
        if (n.startsWith("final")) return "final";
        if (n.startsWith("assign")) return "assignment";
        return inferType(t);
    }

    static String inferType(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("quiz")) return "quiz";
        if (n.contains("mid")) return "mid-term";
        if (n.contains("final")) return "final";
        if (n.contains("exam") || n.contains("test") || n.contains("sessional")) return "exam";
        return "assignment";
    }

    static LocalDate parseDate(String s) {
        for (String p : DATE_PATTERNS) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(p, Locale.ENGLISH));
            } catch (DateTimeParseException ignored) {
                // try the next pattern
            }
        }
        return null;
    }

    static Double parseWeight(String s) {
        String t = s.replace("%", "").replaceAll("(?i)marks?|pts?|points?", "").trim();
        try {
            double v = Double.parseDouble(t);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> getWarnings() { return warnings; }

    public String getLinePattern() { return linePattern; }
}
