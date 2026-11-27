package com.cramless.ui;

import com.cramless.model.Course;
import com.cramless.model.GradeBand;
import com.cramless.util.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** UC-09 View Grade Forecast (viewGradeForecast, enterTargetGrade). Always labelled an estimate. */
final class ForecastView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private String open;
    private final Map<String, Double> required = new HashMap<>();

    ForecastView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        List<Course> courses = ctx.progress.viewGradeForecast();          // system operation 1
        Node head = Ui.pageHead("Grade forecast", "An estimate: remaining assessments are assumed to go as well as your average so far",
                Ui.btn("Log a score", () -> LogScoreDialog.open(ctx, shell, open)));
        if (courses.isEmpty()) {
            return Ui.page(1000, head, Ui.empty("No courses yet", "Import a syllabus first, then log scores to see a forecast.",
                    Ui.primary("Add course", () -> shell.go("import"))));
        }
        int withScores = 0, onTarget = 0;
        double gpa = 0;
        for (Course c : courses) {
            if (!ctx.progress.hasScores(c)) continue;
            withScores++;
            double f = ctx.progress.forecastPercentage(c);
            gpa += c.getGradeScale().gpaFor(f);
            Double min = targetMinimum(c, c.getTargetGrade());
            if (min != null && f >= min) onTarget++;
        }
        Node kv = Ui.kv(
                Ui.kvCell("Estimated GPA", Ui.num(withScores == 0 ? "--" : String.format("%.2f", gpa / withScores), 26, "num-brand")),
                Ui.kvCell("Forecast at or above target", Ui.row(8, Ui.num(String.valueOf(onTarget), 26), Ui.label("of " + withScores, "meta"))),
                Ui.kvCell("Courses without scores", Ui.num(String.valueOf(courses.size() - withScores), 26)));
        List<Node> rows = new ArrayList<>();
        for (Course c : courses) {
            rows.add(row(c));
            if (c.getName().equals(open)) rows.add(expanded(c));
        }
        return Ui.page(1000, head, kv, Ui.section("By course", Ui.label("Select a course to set a target grade", "meta"), Ui.rows(rows)));
    }

    private Node row(Course c) {
        boolean has = ctx.progress.hasScores(c);
        String sub;
        String tag;
        String tone;
        if (!has) {
            sub = "No scores logged yet";                                    // 4a
            tag = "No scores";
            tone = "";
        } else {
            sub = "Now " + Ui.pct(ctx.progress.currentPercentage(c)) + ", forecast " + Ui.pct(ctx.progress.forecastPercentage(c));
            boolean approx = ctx.progress.isApproximate(c);                  // 4b
            tag = approx ? "Approximate" : "Estimate";
            tone = approx ? "warn" : "";
        }
        Label letter = Ui.num(has ? ctx.progress.forecastLetter(c) : "--", 20, has ? "" : "num-muted");
        letter.setMinWidth(48);
        letter.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        HBox r = Ui.rowItem(Ui.dot(Ui.courseColor(ctx.student, c)), Ui.twoLine(c.getName(), sub), Ui.badge(tag, tone), letter,
                Icons.of(c.getName().equals(open) ? "chevUp" : "chevDown", 16));
        Ui.clickable(r, () -> { open = c.getName().equals(open) ? null : c.getName(); shell.refresh(); });
        return r;
    }

    private Node expanded(Course c) {
        VBox v = new VBox(16);
        v.setPadding(new Insets(18, 20, 20, 44));
        v.getStyleClass().add("sunk-strip");
        if (!ctx.progress.hasScores(c)) {
            v.getChildren().add(Ui.row(12, Ui.wrap("Log a score to get a forecast for this course.", "body"), Ui.grow(),
                    Ui.btn("Log a score", () -> LogScoreDialog.open(ctx, shell, c.getName()))));
            return v;
        }
        double cur = ctx.progress.currentPercentage(c);
        double fc = ctx.progress.forecastPercentage(c);
        v.getChildren().add(Ui.kv(
                Ui.kvCell("Current percentage", Ui.label(Ui.pct(cur), "t1", "strong")),
                Ui.kvCell("Forecast percentage", Ui.label(Ui.pct(fc), "t1", "strong")),
                Ui.kvCell("Forecast grade", Ui.label(ctx.progress.forecastLetter(c), "t1", "strong")),
                Ui.kvCell("Graded so far", Ui.label(Math.round(c.getGradedWeight()) + "% of the grade", "t1", "strong"))));
        if (ctx.progress.isApproximate(c)) {
            v.getChildren().add(Ui.alert("warn", "Approximate.", "This course's weightages add up to " + Math.round(c.getTotalWeight()) + "%, not 100%."));
        }
        ComboBox<String> target = new ComboBox<>();
        for (GradeBand b : c.getGradeScale().getBands()) {
            if (b.getMinimumPercentage() > 0) target.getItems().add(b.getLetterGrade());
        }
        target.setEditable(true);
        target.setValue(c.getTargetGrade());
        target.setPromptText("A- or 85");
        target.setPrefWidth(140);
        VBox result = new VBox();
        Double req = required.containsKey(c.getName()) ? required.get(c.getName()) : ctx.progress.requiredForSavedTarget(c);
        if (req != null && c.getTargetGrade() != null) result.getChildren().add(requiredAlert(c.getTargetGrade(), req, c));
        target.setOnAction(e -> {
            String g = target.getValue();
            if (g == null) return;
            try {
                double r = ctx.progress.enterTargetGrade(c.getName(), g);     // system operation 2
                required.put(c.getName(), r);
                shell.refresh();
            } catch (ValidationException ex) {
                Ui.toast(ex.getMessage());
            }
        });
        v.getChildren().addAll(Ui.row(12, Ui.label("Target grade", "label-sm"), target,
                Ui.label(minLabel(c, target.getValue()), "meta")), result);
        return v;
    }

    /** UC-09 data variation: the target is a letter grade or a percentage. */
    private static Double targetMinimum(Course c, String target) {
        if (target == null || target.isBlank()) return null;
        try {
            return Double.parseDouble(target.replace("%", "").trim());
        } catch (NumberFormatException e) {
            try {
                return c.getGradeScale().minimumFor(target.trim());
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }

    private static String minLabel(Course c, String target) {
        Double m = targetMinimum(c, target);
        return m == null ? "Choose a letter, or type a percentage" : "needs " + Math.round(m) + "% overall";
    }

    private static Node requiredAlert(String grade, double req, Course c) {
        if (grade.trim().matches("\\d+(\\.\\d+)?")) grade = grade.trim() + "%";
        double remaining = 100 - c.getGradedWeight();
        if (req <= 0) return Ui.alert("ok", grade + " is already secured.", "Even with zero on the remaining assessments.");
        if (req > 100) {
            // UC-09 7a: the target cannot be reached.
            return Ui.alert("bad", grade + " cannot be reached.", "You would need " + Ui.pct(req) + " on the remaining " + Math.round(remaining) + "% of the grade.");
        }
        return Ui.alert(req > 90 ? "warn" : "ok", "Average " + Ui.pct(req) + " needed", "on the remaining " + Math.round(remaining) + "% of the grade to reach " + grade + ".");
    }
}
