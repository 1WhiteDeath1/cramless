package com.cramless.ui;

import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.model.Score;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** UC-06 Log Quiz or Test Score (startScoreLogging, logScore), as a dialog over the current page. */
final class LogScoreDialog {
    private LogScoreDialog() {}

    static void open(AppContext ctx, Shell shell, String presetCourse) {
        ctx.courses.startScoreLogging();                                       // system operation 1
        List<Course> courses = ctx.student.getCourses();
        if (courses.isEmpty()) {
            Ui.toast("Add a course before logging a score.");
            return;
        }
        ComboBox<String> courseF = new ComboBox<>();
        for (Course c : courses) courseF.getItems().add(c.getName());
        courseF.setValue(presetCourse != null && ctx.student.getCourse(presetCourse) != null ? presetCourse : courses.get(0).getName());
        courseF.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> assessF = new ComboBox<>();
        assessF.setMaxWidth(Double.MAX_VALUE);
        Label noneMsg = Ui.wrap("", "hint-warn");
        VBox oldBox = new VBox();

        boolean[] percent = {false};
        TextField marks = new TextField();
        TextField total = new TextField();
        TextField pctF = new TextField();
        marks.setPromptText("Marks obtained");
        total.setPromptText("Total");
        pctF.setPromptText("Percentage");
        Label of = Ui.label("of", "meta");
        HBox marksRow = Ui.row(10, marks, of, total);
        javafx.scene.layout.HBox.setHgrow(marks, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(total, javafx.scene.layout.Priority.ALWAYS);
        VBox scoreHolder = new VBox(marksRow);
        Label scoreErr = Ui.errLabel();
        Node seg = Ui.seg(List.of("Marks", "Percentage"), "Marks", o -> {
            percent[0] = o.equals("Percentage");
            scoreHolder.getChildren().setAll(percent[0] ? pctF : marksRow);
        });
        DatePicker dateF = CoursesView.datePicker(AppClock.today());
        Label dateErr = Ui.errLabel();
        Button save = Ui.primary("Save score", null);
        save.setDefaultButton(true);

        Runnable refill = () -> {
            Course c = ctx.student.getCourse(courseF.getValue());
            assessF.getItems().clear();
            List<Assessment> taken = new ArrayList<>();
            for (Assessment a : c.getAssessments()) {
                if (a.getDate() == null || !a.getDate().isAfter(AppClock.today())) taken.add(a);   // pre-condition: it has taken place
            }
            for (Assessment a : taken) assessF.getItems().add(a.getName());
            boolean none = taken.isEmpty();
            noneMsg.setText(none ? "No assessment of this course has taken place yet. Add a missing one in Courses." : "");
            noneMsg.setManaged(none);
            noneMsg.setVisible(none);
            if (!none) assessF.setValue(taken.get(taken.size() - 1).getName());
            save.setDisable(none);
        };
        Runnable onAssess = () -> {
            oldBox.getChildren().clear();
            Course c = ctx.student.getCourse(courseF.getValue());
            Assessment a = assessF.getValue() == null ? null : c.getAssessment(assessF.getValue());
            if (a == null) return;
            if (a.getDate() != null) dateF.setValue(a.getDate());
            if (a.isGraded()) {
                // UC-06 9a: a score already exists; saving overwrites it without awarding points again.
                Score s = a.getScore();
                oldBox.getChildren().add(Ui.alert("warn", "Already logged: " + CoursesView.fmt(s.getMarksObtained()) + " / " + CoursesView.fmt(s.getTotalMarks()) + ".",
                        "Saving replaces it. No extra points are added."));
                total.setText(CoursesView.fmt(s.getTotalMarks()));
                save.setText("Replace score");
            } else {
                save.setText("Save score");
            }
        };
        courseF.setOnAction(e -> { refill.run(); onAssess.run(); });
        assessF.setOnAction(e -> onAssess.run());
        refill.run();
        onAssess.run();

        Button toCourses = Ui.btn("Add it in Courses", () -> { Ui.closeOverlay(); shell.go("courses"); }, "quiet", "sm");
        VBox body = new VBox(18,
                Ui.field("Course", courseF),
                Ui.field("Assessment", assessF, Ui.row(8, Ui.label("Only assessments that have taken place", "hint"), Ui.grow(), toCourses), noneMsg),
                oldBox,
                new VBox(6, Ui.row(8, Ui.label("Score", "label-sm"), Ui.grow(), seg), scoreHolder, scoreErr),
                Ui.field("Date", dateF, dateErr));

        save.setOnAction(e -> {
            Ui.showErr(scoreErr, null, null);
            Ui.showErr(dateErr, dateF, null);
            double m, t;
            try {
                if (percent[0]) {
                    m = Double.parseDouble(pctF.getText().trim().replace("%", ""));
                    t = 100;
                } else {
                    m = Double.parseDouble(marks.getText().trim());
                    t = Double.parseDouble(total.getText().trim());
                }
            } catch (NumberFormatException ex) {
                Ui.showErr(scoreErr, null, "Enter the marks as numbers, up to 2 decimal places.");   // 4a
                return;
            }
            if (m < 0 || m > t || t <= 0) {
                Ui.showErr(scoreErr, null, percent[0] ? "A percentage is between 0 and 100." : "Marks cannot be negative or more than the total.");
                return;
            }
            LocalDate d = CoursesView.readDate(dateF);
            if (d == null) { Ui.showErr(dateErr, dateF, "Enter a valid date as dd/mm/yyyy."); return; }
            try {
                Score sc = ctx.courses.logScore(courseF.getValue(), assessF.getValue(), m, t, d);   // system operation 2
                Ui.closeOverlay();
                shell.refresh();
                result(ctx, assessF.getValue(), courseF.getValue(), sc);
            } catch (ValidationException ex) {
                Ui.showErr(scoreErr, null, ex.getMessage());
            }
        });
        Ui.dialog("Log a score", "Updates your forecast, ranking and graphs", 480, body, Ui.btn("Cancel", Ui::closeOverlay), save);
    }

    /** UC-06 step 10: the saved score, the updated course percentage and a note that the plan was adjusted. */
    private static void result(AppContext ctx, String assessment, String course, Score sc) {
        int pts = ctx.courses.getLastPointsEarned();
        VBox body = new VBox(16, Ui.kv(
                Ui.kvCell("Score", Ui.num(Math.round(sc.getPercentage() * 10) / 10.0 + "%", 22)),
                Ui.kvCell("Course now", Ui.num(Math.round(ctx.courses.getLastCurrentPercentage() * 10) / 10.0 + "%", 22, "num-brand")),
                Ui.kvCell("Points", Ui.num(pts == 0 ? "0" : "+" + pts, 22, "num-gold"))));
        if (pts == 0) body.getChildren().add(Ui.label("Points for this assessment were already given.", "hint"));
        body.getChildren().add(ctx.courses.isLastPlanAdjusted()
                ? Ui.alert("ok", "Plan adjusted.", "Weak topics from before this assessment get more time.")
                : Ui.alert("", null, "No study plan to adjust yet."));
        Ui.dialog(assessment + " saved", course, 460, body, Ui.primary("Done", Ui::closeOverlay));
    }

}
