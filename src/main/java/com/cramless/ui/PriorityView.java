package com.cramless.ui;

import com.cramless.model.Assessment;
import com.cramless.model.Course;
import com.cramless.model.StudySettings;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** UC-08 View Priority Suggestion (requestNextTopic, viewFullRanking). */
final class PriorityView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private boolean whyOpen;
    private boolean fullOpen;

    PriorityView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        Node head = Ui.pageHead("Study next", "Ranked by weightage, time left before the exam and your scores",
                Ui.quiet("Adjust weighting", this::openWeighting));
        Topic top;
        try {
            top = ctx.plan.requestNextTopic();                           // system operation 1
        } catch (ValidationException e) {
            // UC-08 3b: no pending topics remain.
            return Ui.page(1000, head, Ui.empty("You are all caught up", "No unfinished topic has an exam coming up. Review a weak area or take the evening off."));
        }
        LocalDate today = AppClock.today();
        List<Node> parts = new ArrayList<>(List.of(head));
        if (!anyScores()) {
            parts.add(Ui.alert("", "No scores logged yet.", "Topics are ranked by weightage and time left only."));   // 4a
        }
        parts.add(focus(top, today));

        List<Topic> ranked = ctx.plan.viewFullRanking();
        if (ranked.size() > 1) {
            List<Topic> shown = fullOpen ? ranked.subList(1, ranked.size()) : ranked.subList(1, Math.min(5, ranked.size()));
            List<Node> rows = new ArrayList<>();
            for (int i = 0; i < shown.size(); i++) {
                Topic t = shown.get(i);
                int rank = i + 2;
                Topic prev = ranked.get(rank - 2);
                boolean tie = Math.abs(ctx.plan.scoreOf(prev) - ctx.plan.scoreOf(t)) < 0.005;
                rows.add(rankRow(rank, t, tie, today));
            }
            String btn = fullOpen ? "Show top 5" : "Full ranking (" + ranked.size() + ")";
            parts.add(Ui.section(fullOpen ? "Full ranking" : "Next in line",
                    Ui.btn(btn, () -> { fullOpen = !fullOpen; shell.refresh(); }, "quiet", "sm"),
                    header(), Ui.rows(rows)));
        }
        String finished = finishedCourses();
        if (finished != null) parts.add(Ui.label(finished + " left out because its exams are over.", "hint"));   // 4c
        return Ui.page(1000, parts.toArray(new Node[0]));
    }

    private Node focus(Topic top, LocalDate today) {
        String reason = ctx.plan.explain(top);
        Label title = Ui.wrap(top.getTitle(), "focus-title");
        VBox left = new VBox(6, Ui.row(0, Ui.badge("Top priority", "brand")), title,
                Ui.label(top.getCourse().getName() + ", week " + top.getWeekNumber(), "meta"),
                Ui.wrap(reason + ".", "body"));
        VBox.setMargin(title, new Insets(10, 0, 0, 0));
        HBox actions = Ui.row(8, Ui.btn("Go to today", () -> shell.go("today"), "primary", "lg"),
                Ui.btn(whyOpen ? "Hide reasons" : "Why first", () -> { whyOpen = !whyOpen; shell.refresh(); }, "lg"));
        VBox.setMargin(actions, new Insets(18, 0, 0, 0));
        left.getChildren().add(actions);
        left.setPadding(new Insets(28));
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox score = Ui.row(8, Ui.num(String.valueOf(Math.round(ctx.plan.scoreOf(top))), 40, "num-brand"), Ui.label("out of 100", "meta"));
        score.setAlignment(Pos.BASELINE_LEFT);
        VBox side = new VBox(10, Ui.label("Priority score", "meta"), score);
        side.getStyleClass().add("side-panel");
        side.setAlignment(Pos.CENTER_LEFT);
        side.setPadding(new Insets(28));
        side.setMinWidth(240);
        side.setPrefWidth(240);
        HBox main = new HBox(left, side);
        VBox card = Ui.card(main);
        if (whyOpen) card.getChildren().add(factors(top, today));
        return card;
    }

    /** The three inputs of the priority score, each from the Topic (Information Expert). */
    private Node factors(Topic t, LocalDate today) {
        int days = t.getDaysLeft(today);
        double w = t.getWeightage();
        double weak = t.getWeaknessLevel();
        String note = t.getLatestScoreNote();
        HBox h = new HBox();
        h.getStyleClass().add("sunk-strip");
        h.getChildren().addAll(
                factor("Weightage", Math.round(w * 10) / 10.0 + "% of grade", (int) Math.round(Math.min(1, w / 5) * 10), "gold", true),
                factor("Time left", Ui.days(Math.max(0, days)), (int) Math.round(10 / (1 + days / 7.0)), "brand", false),
                factor("Weak area", note == null ? "No scores yet" : note, (int) Math.round(weak * 10), "bad", false));
        return h;
    }

    private static Node factor(String k, String v, int cells, String tone, boolean first) {
        VBox box = new VBox(8, Ui.label(k, "meta"), Ui.label(v, "t1", "strong"), Ui.bar(10, cells, tone));
        box.setPadding(new Insets(18, 28, 18, 28));
        box.getStyleClass().add("kv-cell");
        if (first) box.getStyleClass().add("first");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Node header() {
        HBox h = Ui.row(16, fixed("Rank", 36), grow(Ui.label("Topic", "")), fixed("Exam", 120), fixed("Weightage", 80), fixed("Score", 56));
        h.getStyleClass().add("thead");
        return h;
    }

    private Node rankRow(int rank, Topic t, boolean tie, LocalDate today) {
        VBox mid = Ui.twoLine(t.getTitle(), t.getCourse().getName());
        if (tie) mid.getChildren().add(Ui.label("Same score as the one above, nearer exam first", "hint-warn"));   // 4b
        Label r = Ui.num(String.valueOf(rank), 14, "num-muted");
        r.setMinWidth(36);
        Label exam = Ui.label(t.getDueDate() == null ? "-" : Ui.dayMonth(t.getDueDate()) + ", " + Ui.days(Math.max(0, t.getDaysLeft(today))), "meta");
        exam.setMinWidth(120);
        exam.setPrefWidth(120);
        Label wt = Ui.label(Math.round(t.getWeightage() * 10) / 10.0 + "%", "meta");
        wt.setMinWidth(80);
        wt.setPrefWidth(80);
        Label sc = Ui.label(String.valueOf(Math.round(ctx.plan.scoreOf(t))), "t1", "strong");
        sc.setMinWidth(56);
        sc.setPrefWidth(56);
        sc.setAlignment(Pos.CENTER_RIGHT);
        return Ui.rowItem(r, Ui.dot(Ui.courseColor(ctx.student, t.getCourse())), mid, exam, wt, sc);
    }

    private static Label fixed(String text, double w) {
        Label l = Ui.label(text, "");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    private static Label grow(Label l) {
        HBox.setHgrow(l, Priority.ALWAYS);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private boolean anyScores() {
        for (Course c : ctx.student.getCourses()) if (c.hasScores()) return true;
        return false;
    }

    private String finishedCourses() {
        LocalDate today = AppClock.today();
        List<String> names = new ArrayList<>();
        for (Course c : ctx.student.getCourses()) {
            if (c.getAssessments().isEmpty()) continue;
            boolean over = true;
            for (Assessment a : c.getAssessments()) if (a.getDate() == null || !a.getDate().isBefore(today)) over = false;
            if (over) names.add(c.getName());
        }
        return names.isEmpty() ? null : String.join(", ", names) + (names.size() == 1 ? " is" : " are");
    }

    /** UC-08 special requirement 4: the weighting between weightage, urgency and weakness can be adjusted. */
    private void openWeighting() {
        StudySettings s = ctx.student.getSettings();
        Slider w = slider(s.getWeightageFactor());
        Slider u = slider(s.getUrgencyFactor());
        Slider k = slider(s.getWeaknessFactor());
        VBox body = new VBox(20,
                Ui.wrap("How much each reason counts when topics are ranked. They are balanced to add up to 100%.", "body"),
                Ui.field("Weightage of the topic", w),
                Ui.field("Time left before the exam", u),
                Ui.field("Weak area from your scores", k));
        Ui.drawer("Adjust weighting", "Smart Priority Advisor", body, Ui.btn("Cancel", Ui::closeOverlay),
                Ui.primary("Save", () -> {
                    if (Ui.attempt(() -> ctx.plan.updateSettings(w.getValue(), u.getValue(), k.getValue()))) {
                        shell.refresh();
                        Ui.toast("Weighting saved. Regenerate your plan to apply it.");
                    }
                }));
    }

    private static Slider slider(double v) {
        Slider s = new Slider(0, 1, v);
        s.setShowTickMarks(false);
        s.setMajorTickUnit(0.25);
        return s;
    }
}
