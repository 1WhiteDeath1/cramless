package com.cramless.ui;

import com.cramless.model.StudyAvailability;
import com.cramless.model.StudyPlan;
import com.cramless.model.StudySession;
import com.cramless.model.Topic;
import com.cramless.service.StudyPlanGenerator;
import com.cramless.util.AppClock;
import com.cramless.util.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** UC-04 Generate Semester Study Plan (generateStudyPlan, enterAvailability), shown as a calendar or a list. */
final class PlanView implements Shell.View {
    private static final String[] DAYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};

    private final AppContext ctx;
    private final Shell shell;
    private LocalDate weekStart;
    private boolean listView;
    private boolean moreOpen;
    private String openBlock;
    private String justGenerated;

    PlanView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        LocalDate today = AppClock.today();
        if (weekStart == null) weekStart = today.with(DayOfWeek.MONDAY);
        StudyPlan p = ctx.student.getActivePlan();
        if (p == null) {
            return Ui.page(1000, Ui.pageHead("Study plan", "Plan every topic before its exam"),
                    Ui.empty("No study plan yet", "Tell CramLess how many hours you can study each day. It splits them into 30-minute blocks across all your courses.",
                            Ui.primary("Generate study plan", this::openAvailability)));
        }

        LocalDate weekEnd = weekStart.plusDays(6);
        int weekCount = 0, weekMins = 0;
        for (int i = 0; i < 7; i++) {
            for (StudySession s : p.getSessionsFor(weekStart.plusDays(i))) {
                weekCount++;
                weekMins += s.getPlannedDuration();
            }
        }
        String sub = weekStart.format(Ui.DAY_MONTH) + " to " + weekEnd.format(Ui.DAY_MONTH) + ", "
                + weekCount + " sessions, " + Ui.minutes(weekMins);
        Node head = Ui.pageHead("Study plan", sub,
                Ui.iconBtn("left", "Previous week", () -> { weekStart = weekStart.minusWeeks(1); shell.refresh(); }),
                Ui.iconBtn("right", "Next week", () -> { weekStart = weekStart.plusWeeks(1); shell.refresh(); }),
                Ui.seg(List.of("Calendar", "List"), listView ? "List" : "Calendar", o -> { listView = o.equals("List"); shell.refresh(); }),
                Ui.primary("Edit availability", this::openAvailability));

        List<Node> parts = new ArrayList<>(List.of(head));
        if (justGenerated != null) {
            parts.add(Ui.alert("ok", "Plan generated.", justGenerated));
            justGenerated = null;
        }
        if (p.isOutdated()) {
            parts.add(Ui.alert("warn", "Your courses changed since this plan was made.", "Regenerate it to include the changes.",
                    Ui.btn("Regenerate", this::openAvailability, "sm")));
        }
        Node warn = warnings();
        if (warn != null) parts.add(warn);
        parts.add(overview(p, today));
        parts.add(listView ? list(p) : calendar(p, today));
        return Ui.page(1160, parts.toArray(new Node[0]));
    }

    /** UC-04 7a / 7b from the last generation: at-risk courses and courses left out. */
    private Node warnings() {
        List<String> risk = new ArrayList<>(), left = new ArrayList<>();
        for (String w : ctx.plan.getLastWarnings()) {
            if (w.startsWith(StudyPlanGenerator.AT_RISK)) risk.add(w.substring(StudyPlanGenerator.AT_RISK.length()));
            else if (w.startsWith(StudyPlanGenerator.LEFT_OUT)) left.add(w.substring(StudyPlanGenerator.LEFT_OUT.length()));
        }
        if (risk.isEmpty() && left.isEmpty()) return null;
        List<String> all = new ArrayList<>(risk);
        all.addAll(left);
        String first = all.get(0);
        String rest = String.join("\n", all.subList(1, all.size()));
        Button more = all.size() > 1 ? Ui.btn(moreOpen ? "Less" : (all.size() - 1) + " more", () -> { moreOpen = !moreOpen; shell.refresh(); }, "quiet", "sm") : null;
        Button add = Ui.btn("Add hours", this::openAvailability, "sm");
        String body = moreOpen && !rest.isEmpty() ? "Add study hours, then generate again.\n" + rest : "Add study hours, then generate again.";
        return more == null ? Ui.alert(risk.isEmpty() ? "warn" : "bad", first, body, add)
                : Ui.alert(risk.isEmpty() ? "warn" : "bad", first, body, more, add);
    }

    /** UC-04 step 10: the semester overview. */
    private Node overview(StudyPlan p, LocalDate today) {
        int left = 0;
        Set<String> courses = new HashSet<>();
        for (StudySession s : p.getSessions()) {
            if (!s.isDone() && !s.getScheduledDate().isBefore(today)) {
                left++;
                courses.add(s.getTopic().getCourse().getName());
            }
        }
        double weekly = 0;
        for (StudyAvailability a : ctx.student.getAvailability()) weekly += a.getAvailableHours();
        LocalDate last = p.getLastDate();
        return Ui.kv(
                Ui.kvCell("Sessions left", Ui.num(String.valueOf(left), 22)),
                Ui.kvCell("Hours per week", Ui.row(6, Ui.num(Ui.hours(weekly).replace(" h", ""), 22), Ui.label("hours", "meta"))),
                Ui.kvCell("Courses planned", Ui.num(String.valueOf(courses.size()), 22)),
                Ui.kvCell("Plan runs until", Ui.label(last == null ? "-" : Ui.date(last), "t1", "strong")));
    }

    private Node calendar(StudyPlan p, LocalDate today) {
        HBox cal = new HBox();
        cal.getStyleClass().add("cal");
        Ui.clip(cal, 8);
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            List<StudySession> list = p.getSessionsFor(d);
            int mins = 0;
            for (StudySession s : list) mins += s.getPlannedDuration();
            HBox headRow = Ui.row(6, Ui.label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), "meta"),
                    Ui.label(String.valueOf(d.getDayOfMonth()), "day-num"), Ui.grow(), Ui.label(mins == 0 ? "" : Ui.hours(mins / 60.0), "meta"));
            headRow.getStyleClass().add("cal-head");
            if (d.equals(today)) headRow.getStyleClass().add("today");
            VBox blocks = new VBox(6);
            blocks.setPadding(new Insets(8));
            if (list.isEmpty()) {
                Label off = Ui.label(isDayOff(d) ? "Day off" : "Free", "meta");
                off.setMaxWidth(Double.MAX_VALUE);
                off.setAlignment(Pos.CENTER);
                off.setPadding(new Insets(20, 0, 20, 0));
                blocks.getChildren().add(off);
            }
            for (StudySession s : list) blocks.getChildren().add(block(s));
            VBox col = new VBox(headRow, blocks);
            col.getStyleClass().add("cal-col");
            if (i == 0) col.getStyleClass().add("first");
            col.setMinHeight(440);
            col.setMinWidth(0);
            col.setPrefWidth(0);
            HBox.setHgrow(col, Priority.ALWAYS);
            cal.getChildren().add(col);
        }
        return cal;
    }

    private Node block(StudySession s) {
        Topic t = s.getTopic();
        String key = s.getScheduledDate() + " " + s.getStartTime();
        boolean open = key.equals(openBlock);
        String color = Ui.courseColor(ctx.student, t.getCourse());
        VBox b = new VBox(2, Ui.label(Ui.time(s.getStartTime()) + ", " + Ui.minutes(s.getPlannedDuration()), "t2"),
                Ui.wrap(t.getTitle(), "t1"));
        ((Label) b.getChildren().get(1)).setStyle("-fx-font-size: 12.5px;");
        if (s.isDone()) b.getChildren().add(Ui.badge("Done", "ok"));
        if (open) {
            Label more = Ui.wrap(t.getCourse().getName() + ". Priority " + Math.round(s.getPriorityScore()) + ". " + s.getReason(), "t2");
            VBox.setMargin(more, new Insets(6, 0, 0, 0));
            b.getChildren().add(more);
        }
        b.getStyleClass().add("blk");
        b.setStyle("-fx-border-color: " + Theme.line() + " " + Theme.line() + " " + Theme.line() + " " + color + "; -fx-border-width: 1 1 1 3;");
        Ui.clickable(b, () -> { openBlock = open ? null : key; shell.refresh(); });
        return b;
    }

    private Node list(StudyPlan p) {
        VBox v = new VBox(16);
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            List<StudySession> list = p.getSessionsFor(d);
            List<Node> rows = new ArrayList<>();
            int mins = 0;
            for (StudySession s : list) {
                mins += s.getPlannedDuration();
                Topic t = s.getTopic();
                Label time = Ui.label(Ui.time(s.getStartTime()), "time");
                time.setMinWidth(48);
                rows.add(Ui.rowItem(time, Ui.dot(Ui.courseColor(ctx.student, t.getCourse())),
                        Ui.twoLine(t.getTitle(), t.getCourse().getName()),
                        Ui.label("Priority " + Math.round(s.getPriorityScore()), "meta"),
                        Ui.label(Ui.minutes(s.getPlannedDuration()), "meta")));
            }
            Node body = rows.isEmpty() ? padded(Ui.label(isDayOff(d) ? "Day off" : "Nothing planned", "meta")) : Ui.rows(rows);
            v.getChildren().add(Ui.section(d.format(Ui.LONG_DAY), Ui.label(mins == 0 ? "" : Ui.minutes(mins), "meta"), body));
        }
        return v;
    }

    private static Node padded(Node n) {
        VBox v = new VBox(n);
        v.setPadding(new Insets(12, 20, 12, 20));
        return v;
    }

    private boolean isDayOff(LocalDate d) {
        StudyAvailability a = ctx.student.getAvailabilityFor(d.getDayOfWeek());
        return a == null || a.isDayOff();
    }

    // ------------------------------------------------------------------ UC-04: generateStudyPlan, enterAvailability

    void openAvailability() {
        List<StudyAvailability> avail;
        try {
            avail = ctx.plan.generateStudyPlan();                 // system operation 1: last saved availability
        } catch (ValidationException e) {
            // UC-04 5a: no courses or topics yet.
            Ui.dialog("Nothing to plan yet", null, 440,
                    Ui.wrap(e.getMessage(), "body"),
                    Ui.btn("Close", Ui::closeOverlay), Ui.primary("Add course", () -> { Ui.closeOverlay(); shell.go("import"); }));
            return;
        }
        double hours = 0;
        for (StudyAvailability a : avail) if (!a.isDayOff()) { hours = a.getAvailableHours(); break; }
        TextField hoursField = new TextField(hours == 0 ? "3" : Ui.hours(hours).replace(" h", ""));
        hoursField.setMaxWidth(120);
        Label hoursErr = Ui.errLabel();
        List<CheckBox> offBoxes = new ArrayList<>();
        VBox days = new VBox();
        for (int i = 0; i < 7; i++) {
            String day = DAYS[i];
            boolean off = false;
            for (StudyAvailability a : avail) if (a.getDayOfWeek().equals(day)) off = a.isDayOff();
            CheckBox cb = new CheckBox("Day off");
            cb.setSelected(off);
            cb.setUserData(day);
            offBoxes.add(cb);
            HBox r = Ui.row(12, Ui.label(DayOfWeek.valueOf(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH), "t1"), Ui.grow(), cb);
            r.getStyleClass().add("dl-row");
            if (i == 0) r.getStyleClass().add("first");
            days.getChildren().add(r);
        }
        VBox confirmBox = new VBox();
        VBox body = new VBox(20,
                Ui.field("Hours you can study on a study day", hoursField, Ui.label("Split into 30-minute blocks", "hint"), hoursErr),
                new VBox(6, Ui.label("Days off", "label-sm"), days),
                confirmBox);

        Button cancel = Ui.btn("Cancel", Ui::closeOverlay);
        Button gen = Ui.primary(ctx.student.getActivePlan() == null ? "Generate plan" : "Regenerate plan", null);
        gen.setOnAction(e -> {
            Ui.showErr(hoursErr, hoursField, null);
            double h;
            try {
                h = Double.parseDouble(hoursField.getText().trim());
            } catch (NumberFormatException ex) {
                Ui.showErr(hoursErr, hoursField, "Enter a number of hours, for example 3 or 2.5.");
                return;
            }
            if (h <= 0 || h > 24) {                                // UC-04 3a
                Ui.showErr(hoursErr, hoursField, "Enter between 0.5 and 24 hours a day.");
                return;
            }
            List<String> off = new ArrayList<>();
            for (CheckBox cb : offBoxes) if (cb.isSelected()) off.add((String) cb.getUserData());
            if (off.size() == 7) {
                Ui.showErr(hoursErr, hoursField, "Leave at least one study day.");
                return;
            }
            if (ctx.student.getActivePlan() != null && confirmBox.getChildren().isEmpty()) {
                // UC-04 8a: ask before replacing the existing plan.
                confirmBox.getChildren().add(Ui.alert("warn", "Replace your current plan?", "Completed topics, points and scores are kept."));
                gen.setText("Replace plan");
                return;
            }
            final double hh = h;
            if (Ui.attempt(() -> {
                StudyPlan p = ctx.plan.enterAvailability(hh, off);     // system operation 2
                justGenerated = p.getSessions().size() + " sessions until " + (p.getLastDate() == null ? "-" : Ui.date(p.getLastDate())) + ".";
            })) {
                weekStart = AppClock.today().with(DayOfWeek.MONDAY);
                shell.refresh();
            }
        });
        Ui.drawer("Weekly availability", "Last saved values are filled in", body, cancel, gen);
    }
}
