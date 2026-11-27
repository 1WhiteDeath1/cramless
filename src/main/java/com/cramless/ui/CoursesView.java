package com.cramless.ui;

import com.cramless.controller.ChangeItem;
import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** UC-03 Manage Courses and Exams (manageCourses, selectChange, enterChange); entry to UC-02 and UC-06. */
final class CoursesView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private String selected;
    private String confirmRemove;
    private String lastChange;

    CoursesView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        List<Course> courses = ctx.courses.manageCourses();               // system operation 1
        Node head = Ui.pageHead("Courses", courses.size() + (courses.size() == 1 ? " course" : " courses") + " this semester",
                Ui.btn("Log a score", () -> LogScoreDialog.open(ctx, shell, selected)),
                Ui.primary("Add course", () -> shell.go("import")));
        if (courses.isEmpty()) {
            return Ui.page(1100, head, Ui.empty("No courses yet", "Import a syllabus to add a course with its weekly topics and assessments.",
                    Ui.primary("Add course from syllabus", () -> shell.go("import"))));
        }
        Course cur = selected == null ? null : ctx.student.getCourse(selected);
        if (cur == null) {
            cur = courses.get(0);
            selected = cur.getName();
        }
        List<Node> parts = new ArrayList<>(List.of(head));
        if (ctx.courses.isPlanNeedsRegenerating()) {
            // UC-03 step 11: the study plan needs regenerating.
            parts.add(Ui.alert("warn", lastChange == null ? "Your courses changed." : lastChange,
                    "Regenerate your study plan to include the change.", Ui.btn("Regenerate", () -> shell.go("plan"), "sm")));
        }

        VBox nav = new VBox();
        List<Node> navRows = new ArrayList<>();
        for (Course c : courses) {
            HBox r = Ui.rowItem(Ui.dot(Ui.courseColor(ctx.student, c)),
                    Ui.twoLine(c.getName(), c.getCourseCode() == null ? "No code" : c.getCourseCode()));
            if (c == cur) r.getStyleClass().add("current");
            Ui.clickable(r, () -> { selected = c.getName(); confirmRemove = null; shell.refresh(); });
            navRows.add(r);
        }
        nav.getChildren().add(Ui.rows(navRows));
        nav.getStyleClass().add("card");
        Ui.clip(nav, 8);
        nav.setMinWidth(270);
        nav.setPrefWidth(270);
        nav.setMaxWidth(270);
        nav.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox detail = detail(cur);
        HBox.setHgrow(detail, Priority.ALWAYS);
        HBox grid = new HBox(24, nav, detail);
        grid.setAlignment(Pos.TOP_LEFT);
        parts.add(grid);
        return Ui.page(1100, parts.toArray(new Node[0]));
    }

    private VBox detail(Course c) {
        double total = c.getTotalWeight();
        String sub = (c.getCourseCode() == null ? "" : c.getCourseCode() + ", ")
                + (c.getSection() == null ? "" : "section " + c.getSection() + ", ") + "week 1 began " + Ui.date(c.getWeek1Date());
        VBox title = new VBox(4, Ui.label(c.getName(), "h2"), Ui.label(sub, "meta"));
        Label sum = Ui.label(Math.round(total * 10) / 10.0 + "%", "t1", "strong");
        if (Math.abs(total - 100) > 0.01) sum.setStyle("-fx-text-fill: c-coral;");
        Node kv = Ui.kv(
                Ui.kvCell("Topics", Ui.label(String.valueOf(c.getTopics().size()), "t1", "strong")),
                Ui.kvCell("Assessments", Ui.label(String.valueOf(c.getAssessments().size()), "t1", "strong")),
                Ui.kvCell("Weightage entered", sum),
                Ui.kvCell("Target grade", Ui.label(c.getTargetGrade() == null ? "Not set" : c.getTargetGrade(), "t1", "strong")));

        HBox thead = Ui.row(16, grow(Ui.label("Assessment", "")), fixed("Date", 100), fixed("Weight", 60), fixed("Score", 80), fixed("", 88));
        thead.getStyleClass().add("thead");
        List<Node> rows = new ArrayList<>();
        LocalDate today = AppClock.today();
        for (Assessment a : c.getAssessments()) {
            Label score = Ui.label(a.isGraded() ? fmt(a.getScore().getMarksObtained()) + " / " + fmt(a.getScore().getTotalMarks()) : "Not yet", "t2");
            if (a.isGraded()) score.getStyleClass().setAll("label", "t1");
            score.setMinWidth(80);
            score.setPrefWidth(80);
            Button edit = Ui.iconBtn("pencil", "Change " + a.getName(), () -> openChange(c, a), "quiet");
            Button del = Ui.iconBtn("trash", "Remove " + a.getName(), () -> { confirmRemove = a.getName(); shell.refresh(); }, "quiet");
            HBox acts = Ui.row(4, edit, del);
            acts.setMinWidth(88);
            acts.setAlignment(Pos.CENTER_RIGHT);
            HBox r = Ui.rowItem(Ui.twoLine(a.getName(), kindLabel(a) + (a.getDate() != null && a.getDate().isBefore(today) ? "" : ", upcoming")),
                    fixedLabel(Ui.date(a.getDate()), 100), fixedLabel(Math.round(a.getWeightage() * 10) / 10.0 + "%", 60), score, acts);
            rows.add(r);
            if (a.getName().equals(confirmRemove)) rows.add(removeRow(c, a));
        }
        VBox list = rows.isEmpty() ? new VBox(pad(Ui.label("No assessments yet.", "meta"))) : Ui.rows(rows);
        VBox sec = Ui.section("Assessments", Ui.btn("Add assessment", () -> openAdd(c), "quiet", "sm"), thead, list);

        List<Node> topicRows = new ArrayList<>();
        for (var t : c.getTopics()) {
            topicRows.add(Ui.rowItem(fixedLabel("Week " + t.getWeekNumber(), 70), Ui.twoLine(t.getTitle(), null),
                    t.isCompleted() ? Ui.badge("Done", "ok") : Ui.badge("Pending", "")));
        }
        VBox topics = Ui.section("Topics", Ui.label(c.countCompletedTopics() + " of " + c.getTopics().size() + " done", "meta"), Ui.rows(topicRows));
        return new VBox(20, title, kv, sec, topics);
    }

    /** UC-03 5a: removal is confirmed first and warns about the score. */
    private Node removeRow(Course c, Assessment a) {
        String warn = a.isGraded() ? "Its logged score is removed too." : "It has no score yet.";
        HBox r = Ui.rowItem(Ui.twoLine("Remove " + a.getName() + "?", warn + " Your plan will need regenerating."),
                Ui.btn("Keep it", () -> { confirmRemove = null; shell.refresh(); }, "sm"),
                Ui.btn("Remove", () -> {
                    try {
                        ctx.courses.selectChange("remove", new ChangeItem(c.getName(), a.getName(), null));
                        ctx.courses.enterChange(null, null, 0);
                        lastChange = a.getName() + " removed.";
                        confirmRemove = null;
                        shell.refresh();
                    } catch (ValidationException ex) {
                        Ui.toast(ex.getMessage());
                    }
                }, "danger", "sm"));
        r.getStyleClass().add("danger-row");
        return r;
    }

    private void openChange(Course c, Assessment a) {
        Assessment current;
        try {
            current = ctx.courses.selectChange("change", new ChangeItem(c.getName(), a.getName(), null));   // operation 2
        } catch (ValidationException e) {
            Ui.toast(e.getMessage());
            return;
        }
        form(c, "Change assessment", current.getName(), current.getDate(), current.getWeightage(), null, current);
    }

    private void openAdd(Course c) {
        form(c, "Add assessment", "", null, 5, "Quiz", null);
    }

    private void form(Course c, String title, String name, LocalDate date, double weight, String type, Assessment editing) {
        TextField nameF = new TextField(name);
        DatePicker dateF = datePicker(date);
        TextField wF = new TextField(fmt(weight));
        Label nameErr = Ui.errLabel(), dateErr = Ui.errLabel(), wErr = Ui.errLabel();
        double others = 0;
        for (Assessment x : c.getAssessments()) if (x != editing) others += x.getWeightage();
        Label wHint = Ui.label("Others in this course add up to " + Math.round(others * 10) / 10.0 + "%", "hint");
        Label pastWarn = Ui.wrap("", "hint-warn");
        pastWarn.setManaged(false);
        pastWarn.setVisible(false);
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("Quiz", "Assignment", "Mid-term", "Final");
        typeF.setValue(type);
        typeF.setMaxWidth(Double.MAX_VALUE);
        VBox body = new VBox(18, Ui.field("Name", nameF, nameErr));
        if (editing == null) body.getChildren().add(Ui.field("Type", typeF));
        body.getChildren().addAll(Ui.field("Date", dateF, Ui.label("dd/mm/yyyy, or pick from the calendar", "hint"), dateErr, pastWarn),
                Ui.field("Weightage (%)", wF, wHint, wErr));

        boolean[] pastConfirmed = {false};
        Button save = Ui.primary("Save", null);
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            Ui.showErr(nameErr, nameF, null);
            Ui.showErr(dateErr, dateF, null);
            Ui.showErr(wErr, wF, null);
            LocalDate d = readDate(dateF);
            if (d == null) { Ui.showErr(dateErr, dateF, "Enter a valid date as dd/mm/yyyy."); return; }   // 8a
            double w;
            try {
                w = Double.parseDouble(wF.getText().trim().replace("%", ""));
            } catch (NumberFormatException ex) {
                Ui.showErr(wErr, wF, "Enter the weightage as a number, for example 10.");
                return;
            }
            if (d.isBefore(AppClock.today()) && !pastConfirmed[0]) {
                // UC-03 8b: warn about a past date; the student corrects it or confirms it.
                pastWarn.setText("This date has passed. Correct it, or select Save again to keep it.");
                pastWarn.setManaged(true);
                pastWarn.setVisible(true);
                pastConfirmed[0] = true;
                save.setText("Save anyway");
                return;
            }
            try {
                if (editing == null) {
                    ctx.courses.selectChange("add", new ChangeItem(c.getName(), nameF.getText(), typeF.getValue().toLowerCase()));
                }
                ctx.courses.enterChange(nameF.getText(), d, w);                                       // operation 3
                lastChange = nameF.getText().trim() + (editing == null ? " added." : " saved.");
                shell.refresh();
            } catch (ValidationException ex) {
                String m = ex.getMessage();
                if (m.toLowerCase().contains("weightage")) Ui.showErr(wErr, wF, m);
                else if (m.toLowerCase().contains("date")) Ui.showErr(dateErr, dateF, m);
                else Ui.showErr(nameErr, nameF, m);
            }
        });
        Ui.drawer(title, c.getName(), body, Ui.btn("Cancel", Ui::closeOverlay), save);
    }

    static DatePicker datePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d == null ? "" : d.format(Ui.DMY); }
            @Override public LocalDate fromString(String s) {
                try {
                    return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
                } catch (Exception e) {
                    return null;
                }
            }
        });
        dp.setPromptText("dd/mm/yyyy");
        dp.setMaxWidth(Double.MAX_VALUE);
        return dp;
    }

    /** Reads a date picker, including text typed but not yet committed. */
    static LocalDate readDate(DatePicker dp) {
        String typed = dp.getEditor().getText();
        if (typed != null && !typed.isBlank()) {
            LocalDate d = dp.getConverter().fromString(typed);
            if (d != null) return d;
            return null;
        }
        return dp.getValue();
    }

    private static String kindLabel(Assessment a) {
        if (!"Exam".equals(a.getKind())) return a.getKind();
        String d = a.getTypeDetail();
        if ("mid-term".equals(d)) return "Mid-term exam";
        if ("final".equals(d)) return "Final exam";
        return "Exam";
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(Math.round(v * 100) / 100.0);
    }

    private static Node pad(Node n) {
        VBox v = new VBox(n);
        v.setPadding(new Insets(14, 20, 14, 20));
        return v;
    }

    private static Label fixed(String text, double w) {
        Label l = Ui.label(text, "");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    private static Label fixedLabel(String text, double w) {
        Label l = Ui.label(text, "t2");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    private static Label grow(Label l) {
        HBox.setHgrow(l, Priority.ALWAYS);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }
}
