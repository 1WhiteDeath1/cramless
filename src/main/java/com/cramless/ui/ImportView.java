package com.cramless.ui;

import com.cramless.model.Course;
import com.cramless.util.ValidationException;
import java.io.File;
import java.nio.file.Files;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** UC-02 Import Course Syllabus (startSyllabusImport, importSyllabus), as four short steps. */
final class ImportView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;

    private int step;
    private String name = "", code = "";
    private LocalDate week1;
    private String outline = "";
    private final List<String[]> rows = new ArrayList<>();   // name, type, dd/mm/yyyy, weight
    private String error;            // a message from importSyllabus, shown on the step it belongs to
    private String errorField;
    private boolean confirmReplace;
    private Course imported;

    ImportView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
        reset();
    }

    private void reset() {
        step = 0;
        name = "";
        code = "";
        week1 = null;
        outline = "";
        rows.clear();
        rows.add(new String[] {"", "Quiz", "", ""});
        error = null;
        errorField = null;
        confirmReplace = false;
        imported = null;
    }

    @Override
    public Node build() {
        if (step == 0 && imported == null) ctx.courses.startSyllabusImport();      // system operation 1
        Label crumb = Ui.label("Courses", "link");
        crumb.setOnMouseClicked(e -> { reset(); shell.go("courses"); });
        VBox headText = new VBox(4, crumb, Ui.label("Add course", "h1"), Ui.label("From its syllabus. Nothing is saved until you import.", "sub"));
        HBox head = new HBox(headText);
        head.getStyleClass().add("page-head");

        if (imported != null) {
            Course c = imported;
            VBox done = new VBox(10, Ui.num(String.valueOf(c.getTopics().size()), 30, "num-ok"), Ui.label("topics added", "meta"),
                    Ui.label(c.getName() + " imported", "h3"),
                    Ui.label(c.getAssessments().size() + (c.getAssessments().size() == 1 ? " assessment" : " assessments") + ". Regenerate your plan to fit it in.", "meta"),
                    Ui.row(8, Ui.btn("View course", () -> { reset(); shell.go("courses"); }),
                            Ui.primary("Regenerate plan", () -> { reset(); shell.regeneratePlan(); })));
            done.setAlignment(Pos.CENTER);
            ((HBox) done.getChildren().get(4)).setAlignment(Pos.CENTER);
            done.setPadding(new Insets(40));
            return Ui.page(820, head, Ui.card(done));
        }
        return Ui.page(820, head, steps(), wizard());
    }

    private Node steps() {
        String[] labels = {"Course", "Outline", "Assessments", "Review"};
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < labels.length; i++) {
            String state = i == step ? "current" : i < step ? "done" : "";
            StackPane n = new StackPane(Ui.label(i < step ? "✓" : String.valueOf(i + 1), ""));
            n.getStyleClass().add("step-num");
            if (!state.isEmpty()) n.getStyleClass().add(state);
            Label l = Ui.label(labels[i], "step-label");
            if (!state.isEmpty()) l.getStyleClass().add(state);
            HBox s = Ui.row(8, n, l);
            int target = i;
            if (i < step) Ui.clickable(s, () -> { step = target; shell.refresh(); });
            h.getChildren().add(s);
            if (i < labels.length - 1) {
                Region line = new Region();
                line.getStyleClass().add("step-line");
                HBox.setHgrow(line, Priority.ALWAYS);
                h.getChildren().add(line);
            }
        }
        return h;
    }

    private Node wizard() {
        String[] titles = {"Course details", "Weekly outline", "Assessments", "Review and import"};
        VBox body;
        Button next;
        switch (step) {
            case 0 -> body = courseStep();
            case 1 -> body = outlineStep();
            case 2 -> body = assessmentStep();
            default -> body = reviewStep();
        }
        body.setPadding(new Insets(28));
        HBox foot = new HBox(8);
        foot.getStyleClass().add("panel-foot");
        foot.setStyle("-fx-padding: 14 28 14 28; -fx-background-radius: 0 0 8 8;");
        if (step > 0) foot.getChildren().add(Ui.btn("Back", () -> { saveRows(); step--; error = null; confirmReplace = false; shell.refresh(); }));
        foot.getChildren().add(Ui.grow());
        if (step < 3) {
            next = Ui.primary("Continue", this::advance);
            next.setDefaultButton(true);
            foot.getChildren().add(next);
        } else if (confirmReplace) {
            foot.getChildren().addAll(Ui.btn("Cancel", () -> { confirmReplace = false; shell.refresh(); }),
                    Ui.primary("Replace course", () -> doImport(true)));
        } else {
            foot.getChildren().add(Ui.primary("Import course", () -> doImport(false)));
        }
        HBox headStrip = new HBox(Ui.label(titles[step], "h3"));
        headStrip.getStyleClass().add("panel-head");
        headStrip.setStyle("-fx-padding: 16 28 16 28; -fx-background-radius: 8 8 0 0;");
        return Ui.card(headStrip, body, foot);
    }

    // ------------------------------------------------------------------ steps

    private TextField nameF, codeF;
    private DatePicker weekF;
    private TextArea outlineF;

    private VBox courseStep() {
        nameF = new TextField(name);
        codeF = new TextField(code);
        codeF.setPromptText("CS3004");
        weekF = CoursesView.datePicker(week1);
        Label nameErr = Ui.errLabel(), codeErr = Ui.errLabel(), weekErr = Ui.errLabel();
        if ("courseName".equals(errorField)) Ui.showErr(nameErr, nameF, error);
        if ("courseCode".equals(errorField)) Ui.showErr(codeErr, codeF, error);
        if ("week1Date".equals(errorField)) Ui.showErr(weekErr, weekF, error);
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(16);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        g.getColumnConstraints().addAll(c, c);
        g.add(Ui.field("Course code", codeF, codeErr), 0, 0);
        g.add(Ui.field("Week 1 starts on", weekF, Ui.label("dd/mm/yyyy", "hint"), weekErr), 1, 0);
        VBox v = new VBox(18, Ui.field("Course name", nameF, nameErr), g);
        return v;
    }

    private VBox outlineStep() {
        outlineF = new TextArea(outline);
        outlineF.setPromptText("Week 1: Introduction");
        outlineF.setPrefRowCount(12);
        outlineF.setWrapText(true);
        Label err = Ui.errLabel();
        if ("outline".equals(errorField)) Ui.showErr(err, outlineF, error);
        Label count = Ui.label(countWeeks(outline) + " weeks found, one topic each", "hint");
        outlineF.textProperty().addListener((o, a, t) -> {
            count.setText(countWeeks(t) + " weeks found, one topic each");
            Ui.showErr(err, outlineF, null);                  // the old error no longer describes this text
            if ("outline".equals(errorField)) { errorField = null; error = null; }
        });
        Button load = Ui.btn("Load .txt file", () -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
            File f = fc.showOpenDialog(outlineF.getScene().getWindow());
            if (f != null) {
                try {
                    outlineF.setText(Files.readString(f.toPath()));
                } catch (Exception ex) {
                    Ui.showErr(err, outlineF, "That file could not be read.");
                }
            }
        }, "quiet", "sm");
        return new VBox(8, Ui.row(8, Ui.label("Weekly outline, one week per line", "label-sm"), Ui.grow(), load), outlineF, count, err);
    }

    private static int countWeeks(String text) {
        int n = 0;
        for (String l : text.split("\\R")) if (l.trim().matches("(?i)^(weeks?|wk|w)?\\s*\\d{1,2}\\s*[:.\\-–)]\\s*\\S.*")) n++;
        return n;
    }

    private final List<TextField[]> rowFields = new ArrayList<>();
    private final List<ComboBox<String>> rowTypes = new ArrayList<>();

    private VBox assessmentStep() {
        rowFields.clear();
        rowTypes.clear();
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        double[] pct = {38, 20, 26, 16};
        for (double p : pct) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            g.getColumnConstraints().add(c);
        }
        String[] heads = {"Name", "Type", "Date (dd/mm/yyyy)", "Weight %"};
        for (int i = 0; i < heads.length; i++) g.add(Ui.label(heads[i], "hint"), i, 0);
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            TextField n = new TextField(row[0]);
            ComboBox<String> t = new ComboBox<>();
            t.getItems().addAll("Quiz", "Assignment", "Mid-term", "Final");
            t.setValue(row[1]);
            t.setMaxWidth(Double.MAX_VALUE);
            TextField d = new TextField(row[2]);
            TextField w = new TextField(row[3]);
            n.setAccessibleText("Row " + (r + 1) + " name");
            d.setAccessibleText("Row " + (r + 1) + " date");
            w.setAccessibleText("Row " + (r + 1) + " weightage");
            g.add(n, 0, r + 1);
            g.add(t, 1, r + 1);
            g.add(d, 2, r + 1);
            g.add(w, 3, r + 1);
            rowFields.add(new TextField[] {n, d, w});
            rowTypes.add(t);
        }
        Label totalLbl = Ui.label("", "");
        HBox totalBox = new HBox();
        Runnable recalc = () -> {
            double sum = 0;
            for (TextField[] f : rowFields) {
                try { sum += Double.parseDouble(f[2].getText().trim().replace("%", "")); } catch (NumberFormatException ignored) { /* not a number yet */ }
            }
            boolean ok = Math.abs(sum - 100) < 0.01;
            totalBox.getChildren().setAll(Ui.alert(ok ? "ok" : "bad", ok ? "Weightages add up to 100%." : "Weightages add up to " + CoursesView.fmt(sum) + "%.",
                    ok ? "" : "They must add up to 100%."));
            HBox.setHgrow(totalBox.getChildren().get(0), Priority.ALWAYS);
        };
        for (TextField[] f : rowFields) f[2].textProperty().addListener((o, a, b) -> recalc.run());
        recalc.run();
        Label err = Ui.errLabel();
        if ("assessments".equals(errorField)) Ui.showErr(err, null, error);
        Button add = Ui.btn("Add row", () -> { saveRows(); rows.add(new String[] {"", "Quiz", "", ""}); shell.refresh(); }, "quiet", "sm");
        totalLbl.setVisible(false);
        totalLbl.setManaged(false);
        return new VBox(10, g, Ui.row(0, add), totalBox, err);
    }

    private VBox reviewStep() {
        double sum = 0;
        for (String[] r : rows) try { sum += Double.parseDouble(r[3]); } catch (NumberFormatException ignored) { /* checked on import */ }
        VBox v = new VBox(18, Ui.dl(
                new String[] {"Course", name + " (" + code + ")"},
                new String[] {"Week 1", week1 == null ? "-" : week1.format(Ui.DMY)},
                new String[] {"Topics", countWeeks(outline) + " weeks"},
                new String[] {"Assessments", rows.size() + ", total " + CoursesView.fmt(sum) + "%"}));
        if (confirmReplace) {
            // UC-02 6a: same course code already exists.
            v.getChildren().add(Ui.alert("warn", error, "Replace it with this syllabus?"));
        } else if (error != null) {
            v.getChildren().add(Ui.alert("bad", null, error));
        }
        return v;
    }

    // ------------------------------------------------------------------ navigation and import

    private void saveRows() {
        if (step == 0 && nameF != null) {
            name = nameF.getText().trim();
            code = codeF.getText().trim();
            week1 = CoursesView.readDate(weekF);
        } else if (step == 1 && outlineF != null) {
            outline = outlineF.getText();
        } else if (step == 2) {
            rows.clear();
            for (int i = 0; i < rowFields.size(); i++) {
                TextField[] f = rowFields.get(i);
                if (f[0].getText().isBlank() && f[1].getText().isBlank() && f[2].getText().isBlank()) continue;
                rows.add(new String[] {f[0].getText().trim(), rowTypes.get(i).getValue(), f[1].getText().trim(), f[2].getText().trim().replace("%", "")});
            }
            if (rows.isEmpty()) rows.add(new String[] {"", "Quiz", "", ""});
        }
    }

    private void advance() {
        saveRows();
        error = null;
        errorField = null;
        // the same checks importSyllabus makes (UC-02 step 4), shown on the step they belong to
        if (step == 0) {
            if (name.isEmpty()) { fail("courseName", "Enter the course name."); return; }
            if (code.isEmpty()) { fail("courseCode", "Enter the course code."); return; }
            if (week1 == null) { fail("week1Date", "Enter the date of week 1 as dd/mm/yyyy."); return; }
        } else if (step == 1) {
            List<String> bad = new ArrayList<>();
            int i = 0;
            for (String l : outline.split("\\R")) {
                i++;
                if (!l.isBlank() && !l.trim().matches("(?i)^(weeks?|wk|w)?\\s*\\d{1,2}\\s*[:.\\-–)]\\s*\\S.*")) bad.add("Line " + i + ": \"" + l.trim() + "\"");
            }
            if (outline.isBlank()) { fail("outline", "Add at least one line, for example \"Week 1: Introduction\"."); return; }
            if (!bad.isEmpty()) { fail("outline", "These lines need a week number and a topic:\n" + String.join("\n", bad)); return; }
        } else if (step == 2) {
            double sum = 0;
            for (String[] r : rows) {
                if (r[0].isEmpty()) { fail("assessments", "Every assessment needs a name."); return; }
                if (parseDmy(r[2]) == null) { fail("assessments", r[0] + " has an invalid date. Use dd/mm/yyyy."); return; }
                try { sum += Double.parseDouble(r[3]); } catch (NumberFormatException e) { fail("assessments", r[0] + " has an invalid weightage."); return; }
            }
            if (Math.abs(sum - 100) > 0.01) { fail("assessments", "Weightages add up to " + CoursesView.fmt(sum) + "%. They must add up to 100%."); return; }
        }
        step++;
        shell.refresh();
    }

    private void fail(String field, String msg) {
        errorField = field;
        error = msg;
        shell.refresh();
    }

    private static LocalDate parseDmy(String s) {
        try {
            return LocalDate.parse(s.trim(), java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
        } catch (Exception e) {
            return null;
        }
    }

    private void doImport(boolean replace) {
        StringBuilder a = new StringBuilder();
        for (String[] r : rows) a.append(r[0]).append(" | ").append(r[2]).append(" | ").append(r[3]).append(" | ").append(r[1]).append('\n');
        try {
            imported = ctx.courses.importSyllabus(name, code, week1, outline, a.toString(), replace);   // system operation 2
            error = null;
            confirmReplace = false;
            shell.refresh();
        } catch (ValidationException e) {
            error = e.getMessage();
            errorField = e.getField();
            if ("duplicate".equals(errorField)) {
                confirmReplace = true;
            } else if ("courseName".equals(errorField) || "courseCode".equals(errorField) || "week1Date".equals(errorField)) {
                step = 0;
            } else if ("outline".equals(errorField)) {
                step = 1;
            } else if ("assessments".equals(errorField)) {
                step = 2;
            }
            shell.refresh();
        }
    }
}
