package com.cramless.ui;

import com.cramless.controller.ProgressController;
import com.cramless.model.Course;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** UC-10 Track Study Pace (viewStudyPace, selectCourse). Status in words as well as colour. */
final class PaceView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private String open;

    PaceView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        LocalDate today = AppClock.today();
        Node head = Ui.pageHead("Study pace", "Topics finished compared with the topics due by today");
        if (ctx.student.getActivePlan() == null) {
            // UC-10 3b
            return Ui.page(1000, head, Ui.empty("No study plan yet", "Generate a plan first, then your pace is measured against it.",
                    Ui.primary("Generate study plan", () -> shell.go("plan"))));
        }
        ctx.progress.viewStudyPace();                                    // system operation 1
        List<Course> courses = ctx.student.getCourses();
        int behind = 0, ahead = 0, onTrack = 0, due = 0;
        for (Course c : courses) {
            String st = ctx.progress.paceStatus(c);
            due += c.countDueTopics(today);
            if (st.startsWith("Behind")) behind++;
            else if (st.startsWith("Ahead")) ahead++;
            else onTrack++;
        }
        String overall = ctx.progress.overallStatus();
        List<Node> parts = new ArrayList<>(List.of(head));
        parts.add(Ui.kv(
                Ui.kvCell("Overall", Ui.label(overall, "t1", "strong")),
                Ui.kvCell("Behind", Ui.num(String.valueOf(behind), 26, behind > 0 ? "num-bad" : "")),
                Ui.kvCell("On track", Ui.num(String.valueOf(onTrack), 26)),
                Ui.kvCell("Ahead", Ui.num(String.valueOf(ahead), 26, "num-ok"))));
        if (due == 0) parts.add(Ui.alert("ok", "On track.", "Nothing is due yet, you are just getting started."));      // 4a
        else if (behind == 0) parts.add(Ui.alert("ok", "Every course is on track or ahead.", "There is nothing to catch up on."));  // 5a

        List<Node> rows = new ArrayList<>();
        for (Course c : courses) {
            rows.add(row(c, today));
            if (c.getName().equals(open)) rows.add(expanded(c));
        }
        parts.add(Ui.section("By course", Ui.label("Behind means more than 10% of the due topics are unfinished", "meta"), Ui.rows(rows)));
        return Ui.page(1000, parts.toArray(new Node[0]));
    }

    private Node row(Course c, LocalDate today) {
        int total = Math.max(1, c.getTopics().size());
        int done = c.countCompletedTopics();
        int dueN = c.countDueTopics(today);
        String status = ctx.progress.paceStatus(c);
        int diff = ctx.progress.aheadOrBehind(c);
        boolean isBehind = status.startsWith("Behind");
        boolean isAhead = status.startsWith("Ahead");
        HBox bar = Ui.bar(total, done, isBehind ? "bad" : "ok");
        if (!isBehind) {
            String color = Ui.courseColor(ctx.student, c);
            for (int i = 0; i < done && i < total; i++) bar.getChildren().get(i).setStyle("-fx-background-color: " + color + ";");
        }
        if (dueN > 0 && dueN <= total) {
            // the line marks where the student should be today
            Region cell = (Region) bar.getChildren().get(dueN - 1);
            cell.setStyle((cell.getStyle() == null ? "" : cell.getStyle()) + "-fx-border-color: transparent " + Theme.ink() + " transparent transparent; -fx-border-width: 0 2 0 0;");
        }
        bar.setMinWidth(220);
        bar.setPrefWidth(220);
        bar.setMaxWidth(220);
        VBox barBox = new VBox(bar);
        barBox.setPadding(new Insets(0, 8, 0, 0));
        String words = isBehind ? Math.abs(diff) + " behind" : isAhead ? diff + " ahead" : "On track";
        Label badge = Ui.badge(words, isBehind ? "bad" : isAhead ? "ok" : "");
        badge.setMinWidth(90);
        HBox r = Ui.rowItem(Ui.dot(Ui.courseColor(ctx.student, c)),
                Ui.twoLine(c.getName(), done + " of " + c.getTopics().size() + " topics done, " + dueN + " due by today"),
                barBox, badge, Icons.of(c.getName().equals(open) ? "chevUp" : "chevDown", 16));
        Ui.clickable(r, () -> { open = c.getName().equals(open) ? null : c.getName(); shell.refresh(); });
        return r;
    }

    private Node expanded(Course c) {
        VBox v = new VBox(12);
        v.setPadding(new Insets(16, 20, 18, 44));
        v.getStyleClass().add("sunk-strip");
        List<Topic> overdue;
        try {
            overdue = ctx.progress.selectCourse(c.getName());                // system operation 2
        } catch (ValidationException e) {
            v.getChildren().add(Ui.label(e.getMessage(), "meta"));
            return v;
        }
        if (overdue.isEmpty()) {
            v.getChildren().add(Ui.label("No overdue topics in this course. " + ProgressController.describeDiff(ctx.progress.aheadOrBehind(c)) + ".", "body"));
            return v;
        }
        List<String[]> pairs = new ArrayList<>();
        for (Topic t : overdue) pairs.add(new String[] {"Week " + t.getWeekNumber(), t.getTitle()});
        v.getChildren().add(new VBox(6, Ui.label("Overdue topics", "sec-title"), Ui.dl(pairs.toArray(new String[0][]))));
        double extra = ctx.progress.extraHoursPerDayNeeded();
        v.getChildren().add(Ui.row(12,
                Ui.wrap("About " + (extra < 1 ? Ui.minutes((int) Math.max(5, Math.round(extra * 60 / 5.0) * 5)) : Ui.hours(Math.round(extra * 2) / 2.0)) + " extra per study day until the next assessment closes the gap.", "body"),
                Ui.grow(), Ui.primary("Regenerate plan", shell::regeneratePlan)));
        return v;
    }
}
