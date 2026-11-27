package com.cramless.ui;

import com.cramless.controller.StudyPlanController;
import com.cramless.model.StudySession;
import com.cramless.model.Topic;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * UC-07 View Daily Study Plan (viewDailyPlan, viewPlanForDate, selectSession) and
 * UC-05 Complete Study Topic (selectTopic, completeTopic), with undo the same day (UC-05 *a).
 */
final class TodayView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private LocalDate shown;
    private boolean weekOpen;
    private boolean doneOpen;

    TodayView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    private StudyPlanController plan() { return ctx.plan; }

    @Override
    public Node build() {
        LocalDate today = AppClock.today();
        if (shown == null) shown = today;
        boolean isToday = shown.equals(today);

        List<StudySession> sessions;
        try {
            sessions = isToday ? plan().viewDailyPlan() : plan().viewPlanForDate(shown);
        } catch (ValidationException e) {
            // UC-07 3b: no plan yet.
            return Ui.page(1000, Ui.pageHead("Today", today.format(Ui.LONG_DAY)),
                    Ui.empty("No study plan yet", "Generate a plan from your courses first. It schedules every topic before its exam.",
                            Ui.primary("Generate study plan", () -> shell.go("plan"))));
        }

        List<StudySession> list = new ArrayList<>();
        List<StudySession> carried = isToday ? plan().getCarriedOver() : List.of();
        list.addAll(carried);                       // 4b: carried over first
        list.addAll(sessions);
        List<StudySession> open = new ArrayList<>();
        List<StudySession> done = new ArrayList<>();
        for (StudySession s : list) (s.isDone() ? done : open).add(s);

        String title = isToday ? "Today" : shown.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
        Node head = Ui.pageHead(title, shown.format(Ui.LONG_DAY), headActions(isToday));
        List<Node> parts = new ArrayList<>(List.of(head));
        if (weekOpen) parts.add(weekStrip(today));

        if (list.isEmpty()) {
            // UC-07 4a / 6a: nothing planned. Present the next upcoming session if there is one.
            StudySession next = plan().getNextUpcomingSession();
            String text = next == null ? "Nothing else is planned in your study plan."
                    : "Next session: " + next.getScheduledDate().format(Ui.LONG_DAY) + " at " + Ui.time(next.getStartTime())
                    + ", " + next.getTopic().getCourse().getName() + ".";
            Button show = next == null ? null : Ui.btn("Show that day", () -> { shown = next.getScheduledDate(); shell.refresh(); });
            parts.add(show == null ? Ui.empty("Nothing planned on this day", text) : Ui.empty("Nothing planned on this day", text, show));
        } else {
            StudySession hero = open.isEmpty() ? null : open.get(0);
            if (hero != null) parts.add(focus(hero, carried.contains(hero), list.size(), done.size(), isToday));
            else parts.add(Ui.alert("ok", isToday ? "All sessions done." : "Every session on this day is done.",
                    isToday ? "Your streak is safe for today." : ""));
            List<StudySession> rest = open.size() > 1 ? open.subList(1, open.size()) : List.of();
            if (!rest.isEmpty()) {
                int mins = 0;
                for (StudySession s : rest) mins += s.getPlannedDuration();
                List<Node> rows = new ArrayList<>();
                for (StudySession s : rest) rows.add(sessionRow(s, carried.contains(s)));
                parts.add(Ui.section(isToday ? "Later today" : "Also on this day",
                        Ui.label(rest.size() + (rest.size() == 1 ? " session, " : " sessions, ") + Ui.minutes(mins), "meta"),
                        Ui.rows(rows)));
            }
            if (!done.isEmpty()) parts.add(completedSection(done, isToday));
        }
        return Ui.page(1000, parts.toArray(new Node[0]));
    }

    private Node[] headActions(boolean isToday) {
        List<Node> a = new ArrayList<>();
        if (!isToday) a.add(Ui.quiet("Back to today", () -> { shown = AppClock.today(); shell.refresh(); }));
        a.add(Ui.iconBtn("left", "Previous day", () -> { shown = shown.minusDays(1); shell.refresh(); }));
        a.add(Ui.iconBtn("right", "Next day", () -> { shown = shown.plusDays(1); shell.refresh(); }));
        a.add(Ui.btn(weekOpen ? "Hide week" : "Week", () -> { weekOpen = !weekOpen; shell.refresh(); }));
        return a.toArray(new Node[0]);
    }

    /** UC-07 data variation: choose another date. */
    private Node weekStrip(LocalDate today) {
        HBox strip = new HBox(6);
        LocalDate monday = shown.with(DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            int n;
            try {
                n = ctx.student.getActivePlan() == null ? 0 : ctx.student.getActivePlan().getSessionsFor(d).size();
            } catch (RuntimeException e) {
                n = 0;
            }
            VBox cell = new VBox(2,
                    Ui.label(d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), "t2"),
                    Ui.label(String.valueOf(d.getDayOfMonth()), "t1", "strong"),
                    Ui.label(n == 0 ? "off" : n + " sessions", "t2"));
            cell.setAlignment(Pos.CENTER);
            cell.getStyleClass().add("day-btn");
            if (d.equals(shown)) cell.getStyleClass().add("on");
            HBox.setHgrow(cell, Priority.ALWAYS);
            cell.setMaxWidth(Double.MAX_VALUE);
            Ui.clickable(cell, () -> { shown = d; shell.refresh(); });
            strip.getChildren().add(cell);
        }
        return strip;
    }

    /** The one thing to do now: the next unfinished session, with today's progress beside it. */
    private Node focus(StudySession s, boolean carried, int total, int doneCount, boolean isToday) {
        Topic t = s.getTopic();
        Label badge = carried ? Ui.badge("Carried over", "bad") : Ui.badge(isToday ? "Up next" : "First", "brand");
        Label title = Ui.wrap(t.getTitle(), "focus-title");
        Label meta = Ui.label(t.getCourse().getName() + ", " + Ui.time(s.getStartTime()) + " for " + Ui.minutes(s.getPlannedDuration())
                + ", priority " + Math.round(s.getPriorityScore()), "meta");
        HBox actions = Ui.row(8);
        if (canComplete(s)) actions.getChildren().add(Ui.btn("Mark complete", () -> openComplete(s), "primary", "lg"));
        actions.getChildren().add(Ui.btn("Details", () -> openDetails(s), "lg"));
        VBox left = new VBox(6, Ui.row(0, badge), title, meta, actions);
        VBox.setMargin(title, new Insets(10, 0, 0, 0));
        VBox.setMargin(actions, new Insets(18, 0, 0, 0));
        left.setPadding(new Insets(28));
        HBox.setHgrow(left, Priority.ALWAYS);

        int planned = plan().getPlannedMinutes(shown);
        int doneMins = plan().getDoneMinutes(shown);
        HBox count = Ui.row(8, Ui.num(String.valueOf(doneCount), 36), Ui.label("of " + total + " sessions done", "meta"));
        count.setAlignment(Pos.BASELINE_LEFT);
        VBox side = new VBox(10, Ui.label(isToday ? "Progress today" : "This day", "meta"), count,
                Ui.label(Ui.minutes(doneMins) + " of " + Ui.minutes(planned) + " studied", "meta"),
                Ui.bar(total, doneCount, "ok"));
        side.getStyleClass().add("side-panel");
        side.setPadding(new Insets(28));
        side.setAlignment(Pos.CENTER_LEFT);
        side.setMinWidth(240);
        side.setPrefWidth(240);
        HBox card = new HBox(left, side);
        card.getStyleClass().add("card");
        Ui.clip(card, 8);
        return card;
    }

    private Node sessionRow(StudySession s, boolean carried) {
        Topic t = s.getTopic();
        String status = s.isDone() ? "Done" : carried ? "Carried over" : "Planned";
        String tone = s.isDone() ? "ok" : carried ? "bad" : "";
        Label time = Ui.label(Ui.time(s.getStartTime()), "time");
        time.setMinWidth(48);
        HBox r = Ui.rowItem(time, Ui.dot(Ui.courseColor(ctx.student, t.getCourse())),
                Ui.twoLine(t.getTitle(), t.getCourse().getName() + ", " + Ui.minutes(s.getPlannedDuration())),
                Ui.label("Priority " + Math.round(s.getPriorityScore()), "meta"),
                Ui.badge(status, tone));
        Ui.clickable(r, () -> openDetails(s));
        return r;
    }

    private Node completedSection(List<StudySession> done, boolean isToday) {
        Button toggle = Ui.disclose("Completed (" + done.size() + ")", doneOpen, () -> { doneOpen = !doneOpen; shell.refresh(); });
        VBox sec = new VBox(toggle);
        sec.getStyleClass().add("sec");
        Ui.clip(sec, 8);
        if (doneOpen) {
            List<Node> rows = new ArrayList<>();
            for (StudySession s : done) {
                Topic t = s.getTopic();
                Label time = Ui.label(Ui.time(s.getStartTime()), "time");
                time.setMinWidth(48);
                Label name = Ui.label(t.getTitle(), "t1", "strike");
                HBox.setHgrow(name, Priority.ALWAYS);
                name.setMaxWidth(Double.MAX_VALUE);
                HBox r = Ui.rowItem(time, name);
                if (isToday && completedToday(t)) {
                    r.getChildren().add(Ui.btn("Undo", () -> undo(t), "quiet", "sm"));
                }
                r.getChildren().add(Ui.badge("Done", "ok"));
                rows.add(r);
            }
            VBox list = Ui.rows(rows);
            list.getStyleClass().add("sunk-strip");
            list.setStyle("-fx-background-color: transparent;");
            sec.getChildren().add(list);
        }
        return sec;
    }

    // ------------------------------------------------------------------ UC-07 step 3: selectSession

    private void openDetails(StudySession s) {
        StudySession ss;
        try {
            // the session is looked up on its own date (carried-over sessions are from earlier days)
            LocalDate keep = shown;
            plan().viewPlanForDate(s.getScheduledDate());
            ss = plan().selectSession(s.getStartTime());
            plan().viewPlanForDate(keep);
        } catch (ValidationException e) {
            Ui.toast(e.getMessage());
            return;
        }
        Topic t = ss.getTopic();
        LocalDate today = AppClock.today();
        VBox body = new VBox(20);
        if (plan().getCarriedOver().contains(ss)) {
            body.getChildren().add(Ui.alert("bad", null, "Carried over from " + ss.getScheduledDate().format(Ui.LONG_DAY) + "."));
        }
        body.getChildren().add(Ui.dl(
                new String[] {"Starts", Ui.time(ss.getStartTime()) + ", " + ss.getScheduledDate().format(Ui.DAY_MONTH)},
                new String[] {"Planned", Ui.minutes(ss.getPlannedDuration())},
                new String[] {"Weightage", Math.round(t.getWeightage()) + "% of the grade"},
                new String[] {"Days left", t.getDueDate() == null ? "No exam date" : Ui.days(Math.max(0, t.getDaysLeft(today))) + " (" + Ui.dayMonth(t.getDueDate()) + ")"},
                new String[] {"Priority", String.valueOf(Math.round(ss.getPriorityScore()))}));
        body.getChildren().add(new VBox(6, Ui.label("Why now", "sec-title"), Ui.wrap(ss.getReason(), "body")));

        Node footer;
        if (t.isCompleted()) {
            footer = completedToday(t) ? Ui.row(8, Ui.badge("Completed today", "ok"), Ui.btn("Undo", () -> undo(t), "quiet"))
                    : Ui.badge("Completed", "ok");
        } else if (canComplete(ss)) {
            footer = Ui.primary("Mark complete", () -> openComplete(ss));
        } else {
            footer = Ui.label("Past sessions are carried over to today.", "meta");
        }
        Ui.drawer(t.getTitle(), t.getCourse().getName() + ", week " + t.getWeekNumber(), body, footer);
    }

    private boolean canComplete(StudySession s) {
        // UC-05 3b: a topic planned for a later day may also be completed now.
        return !s.isDone() && !s.getTopic().isCompleted()
                && (!s.getScheduledDate().isBefore(AppClock.today()) || plan().getCarriedOver().contains(s));
    }

    private static boolean completedToday(Topic t) {
        return t.isCompleted() && t.getProgress().getCompletedAt().toLocalDate().equals(AppClock.today());
    }

    // ------------------------------------------------------------------ UC-05: selectTopic, completeTopic

    private void openComplete(StudySession s) {
        Topic t;
        try {
            t = plan().selectTopic(s.getTopic().getKey());           // UC-05 system operation 1
        } catch (ValidationException e) {
            Ui.toast(e.getMessage());
            return;
        }
        Ui.closeAllOverlays();
        TextField mins = new TextField();
        mins.setPromptText(String.valueOf(s.getPlannedDuration()));
        mins.setPrefWidth(110);
        mins.setMaxWidth(110);
        HBox quick = Ui.row(8, mins, Ui.seg(List.of("30", "45", "60", "90"), null, mins::setText));
        Label err = Ui.errLabel();
        VBox body = new VBox(18,
                Ui.dl(new String[] {"Course", t.getCourse().getName()},
                        new String[] {"Planned", Ui.minutes(s.getPlannedDuration())},
                        new String[] {"Weightage", Math.round(t.getWeightage()) + "%"},
                        new String[] {"Exam", t.getDueDate() == null ? "-" : Ui.date(t.getDueDate())}),
                Ui.field("Time actually spent, in minutes", quick, Ui.label("Leave empty to use the planned time", "hint"), err));
        Button confirm = Ui.primary("Mark complete", null);
        confirm.setDefaultButton(true);
        confirm.setOnAction(e -> {
            Integer spent;
            String txt = mins.getText().trim();
            try {
                spent = txt.isEmpty() ? s.getPlannedDuration() : Integer.parseInt(txt);
            } catch (NumberFormatException ex) {
                Ui.showErr(err, mins, "Enter whole minutes, for example 45.");
                return;
            }
            try {
                int pts = plan().completeTopic(spent);                    // UC-05 system operation 2
                shell.refresh();
                showResult(t, pts);
            } catch (ValidationException ex) {
                Ui.showErr(err, mins, ex.getMessage());
            }
        });
        Ui.dialog("Mark complete", t.getTitle(), 460, body, Ui.btn("Cancel", Ui::closeOverlay), confirm);
    }

    /** UC-05 step 8: points earned, current streak and the next recommended topic. */
    private void showResult(Topic done, int pts) {
        VBox body = new VBox(18);
        body.getChildren().add(Ui.kv(
                Ui.kvCell("Points earned", Ui.num("+" + pts, 24, "num-gold")),
                Ui.kvCell("Current streak", Ui.num(plan().getLastStreak() + (plan().getLastStreak() == 1 ? " day" : " days"), 24, "num-brand"))));
        if (plan().isLastStreakRestarted()) {
            body.getChildren().add(Ui.alert("warn", "Streak restarted.", "You missed a day, so your streak starts again at 1."));
        }
        if (plan().getLastCourseCovered() != null) {
            body.getChildren().add(Ui.alert("ok", plan().getLastCourseCovered() + " is fully covered.", "It is no longer in your plan."));
        }
        Topic next = plan().getLastNextTopic();
        body.getChildren().add(new VBox(6, Ui.label("Next recommended topic", "sec-title"),
                next == null ? Ui.label("You are all caught up.", "body")
                        : Ui.twoLine(next.getTitle(), next.getCourse().getName() + ", week " + next.getWeekNumber())));
        Ui.dialog("Topic complete", done.getTitle(), 460, body,
                Ui.btn("Undo", () -> undo(done), "quiet"), Ui.primary("Done", Ui::closeOverlay));
    }

    /** UC-05 extension *a: undo later the same day. */
    private void undo(Topic t) {
        if (Ui.attempt(() -> plan().undoCompletion(t.getKey()))) {
            shell.refresh();
            Ui.toast("Completion undone. The points were taken back.");
        }
    }
}
