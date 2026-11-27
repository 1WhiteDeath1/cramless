package com.cramless.ui;

import com.cramless.model.Course;
import com.cramless.model.GraphPoint;
import com.cramless.model.ProgressGraph;
import com.cramless.util.AppClock;
import com.cramless.util.DateRange;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 * UC-11 View Progress Graphs (viewProgressGraphs, selectGraphScope, selectGraphPoint): a grade-over-time graph
 * and a study-time graph with one line per course, labelled axes, a legend, and a distinct marker shape and
 * line dash per course so courses can be told apart without colour.
 */
final class GraphsView implements Shell.View {
    private static final String[] SHAPES = {
            "M0 -5.5 A5.5 5.5 0 1 1 0 5.5 A5.5 5.5 0 1 1 0 -5.5 Z", "M-5 -5 H5 V5 H-5 Z", "M0 -6 L6 4.5 H-6 Z",
            "M0 -6.5 L6.5 0 L0 6.5 L-6.5 0 Z", "M-2 -6 H2 V-2 H6 V2 H2 V6 H-2 V2 H-6 V-2 H-2 Z"};
    private static final double[][] DASH = {{}, {7, 4}, {2, 4}, {10, 3, 2, 3}, {4, 2}};
    private static final double W = 820, H = 300, L = 56, R = 140, T = 12, B = 44;

    private final AppContext ctx;
    private final Shell shell;
    private String period = "Semester";
    private Set<String> chosen;
    private String pointText;

    GraphsView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    private record Series(Course course, int index, ProgressGraph graph) {}

    @Override
    public Node build() {
        List<Course> courses = ctx.progress.viewProgressGraphs();            // system operation 1
        Node head = Ui.pageHead("Progress graphs", "How your grades and study time change over time",
                Ui.seg(List.of("7 days", "30 days", "Semester"), period, o -> { period = o; pointText = null; shell.refresh(); }));
        if (courses.isEmpty()) {
            return Ui.page(1000, head, Ui.empty("No courses yet", "Add a course, log scores and complete topics to see graphs.",
                    Ui.primary("Add course", () -> shell.go("import"))));
        }
        if (chosen == null) {                                                // default: all courses
            chosen = new LinkedHashSet<>();
            for (Course c : courses) chosen.add(c.getName());
        }
        FlowPane chips = new FlowPane(6, 6);
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            boolean on = chosen.contains(c.getName());
            Button b = new Button(c.getName(), marker(i, Ui.courseColor(ctx.student, c), 0.9));
            b.getStyleClass().addAll("btn", "sm");
            if (!on) b.setStyle("-fx-text-fill: c-ink3; -fx-border-style: segments(4, 3);");
            else b.setStyle("-fx-border-color: c-ink2;");
            b.setOnAction(e -> {
                if (!chosen.remove(c.getName())) chosen.add(c.getName());
                pointText = null;
                shell.refresh();
            });
            chips.getChildren().add(b);
        }
        VBox chooser = Ui.section("Courses", Ui.label(chosen.size() + " of " + courses.size() + " shown", "meta"), padded(chips));

        List<Course> sel = new ArrayList<>();
        for (Course c : courses) if (chosen.contains(c.getName())) sel.add(c);
        if (sel.isEmpty()) {
            return Ui.page(1000, head, chooser, Ui.empty("No course selected", "Select at least one course above."));
        }
        DateRange range = range();
        List<ProgressGraph> graphs;
        try {
            graphs = ctx.progress.selectGraphScope(sel, range);              // system operation 2
        } catch (ValidationException e) {
            return Ui.page(1000, head, chooser, Ui.empty("Nothing to show", e.getMessage()));
        }
        List<Series> grade = new ArrayList<>(), study = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < sel.size(); i++) {
            Course c = sel.get(i);
            int idx = courses.indexOf(c);
            grade.add(new Series(c, idx, graphs.get(2 * i)));
            study.add(new Series(c, idx, graphs.get(2 * i + 1)));
            if (!graphs.get(2 * i).getPoints().isEmpty() || !graphs.get(2 * i + 1).getPoints().isEmpty()) any = true;
        }
        List<Node> parts = new ArrayList<>(List.of(head, chooser));
        if (!any) {
            // UC-11 6a
            parts.add(Ui.empty("No scores or study time in this period", "Log scores or complete topics, or choose a longer period.",
                    Ui.btn("Log a score", () -> LogScoreDialog.open(ctx, shell, null)), Ui.primary("Go to today", () -> shell.go("today"))));
            return Ui.page(1000, parts.toArray(new Node[0]));
        }
        if (pointText != null) parts.add(Ui.alert("", "Selected point:", pointText, Ui.btn("Clear", () -> { pointText = null; shell.refresh(); }, "quiet", "sm")));
        double maxH = 1;
        for (Series s : study) for (GraphPoint p : s.graph().getPoints()) maxH = Math.max(maxH, p.getValue());
        parts.add(Ui.section("Grade over time", Ui.label("Score of each assessment, in %", "meta"),
                chart(grade, range, 100, "Score (%)", "%"), legend(grade)));
        parts.add(Ui.section("Study time", Ui.label("Hours studied per day", "meta"),
                chart(study, range, niceHours(maxH), "Hours studied", " h"), legend(study)));
        return Ui.page(1000, parts.toArray(new Node[0]));
    }

    /** Axis top whose quarter steps are round numbers (0.5, 1, 1.5, 2, 3 ... h). */
    private static double niceHours(double max) {
        for (double top : new double[]{2, 4, 6, 8, 12, 16, 20, 24}) if (max <= top) return top;
        return Math.ceil(max / 4) * 4;
    }

    private DateRange range() {
        LocalDate today = AppClock.today();
        if (period.equals("7 days")) return new DateRange(today.minusDays(6), today);
        if (period.equals("30 days")) return new DateRange(today.minusDays(29), today);
        DateRange sem = ctx.progress.wholeSemester();
        return new DateRange(sem.getStart(), sem.getEnd().isAfter(today) ? today : sem.getEnd());
    }

    private Node chart(List<Series> series, DateRange range, double yMax, String yTitle, String unit) {
        Pane p = new Pane();
        p.setMinSize(W, H);
        p.setPrefSize(W, H);
        p.setMaxSize(W, H);
        double pw = W - L - R, ph = H - T - B;
        LocalDate start = range.getStart(), end = range.getEnd();
        long span = Math.max(1, ChronoUnit.DAYS.between(start, end));

        for (int i = 0; i <= 4; i++) {
            double v = yMax * i / 4.0;
            double y = T + ph - ph * i / 4.0;
            Line g = new Line(L, y, L + pw, y);
            g.getStyleClass().add("grid-line");
            Text t = new Text(fmt(v));
            t.getStyleClass().add("axis-text");
            t.setX(L - 10 - t.getLayoutBounds().getWidth());
            t.setY(y + 4);
            p.getChildren().addAll(g, t);
        }
        int ticks = (int) Math.min(6, span);
        for (int i = 0; i <= ticks; i++) {
            LocalDate d = start.plusDays(Math.round(span * (double) i / ticks));
            double x = L + pw * ChronoUnit.DAYS.between(start, d) / span;
            Text t = new Text(d.format(Ui.DAY_MONTH));
            t.getStyleClass().add("axis-text");
            t.setX(x - t.getLayoutBounds().getWidth() / 2);
            t.setY(T + ph + 18);
            p.getChildren().add(t);
        }
        Text xt = new Text("Date");
        xt.getStyleClass().add("axis-title");
        xt.setX(L + pw / 2 - 12);
        xt.setY(H - 6);
        Text yt = new Text(yTitle);
        yt.getStyleClass().add("axis-title");
        yt.setRotate(-90);
        yt.setX(-yt.getLayoutBounds().getWidth() / 2 + 10);
        yt.setY(T + ph / 2);
        p.getChildren().addAll(xt, yt);

        for (Series s : series) {
            String color = Ui.courseColor(ctx.student, s.course());
            List<GraphPoint> pts = s.graph().getPoints();
            Polyline line = new Polyline();
            line.setStroke(Color.web(color));
            line.setStrokeWidth(2);
            for (double d : DASH[s.index() % DASH.length]) line.getStrokeDashArray().add(d);
            double lastX = 0, lastY = 0;
            List<Node> marks = new ArrayList<>();
            for (GraphPoint gp : pts) {
                double x = L + pw * Math.max(0, ChronoUnit.DAYS.between(start, gp.getDate())) / span;
                double y = T + ph - ph * Math.min(1, gp.getValue() / yMax);
                line.getPoints().addAll(x, y);
                SVGPath m = marker(s.index(), color, 1);
                m.setLayoutX(x);
                m.setLayoutY(y);
                m.getStyleClass().add("point-hit");
                String tip = s.course().getName() + ": " + fmt(gp.getValue()) + unit + " on " + Ui.date(gp.getDate());
                Tooltip.install(m, new Tooltip(tip));
                m.setOnMouseClicked(e -> {
                    try {
                        GraphPoint found = ctx.progress.selectGraphPoint(gp);   // system operation 3
                        pointText = s.course().getName() + ", " + fmt(found.getValue()) + unit + " on " + Ui.date(found.getDate()) + ".";
                    } catch (ValidationException ex) {
                        pointText = ex.getMessage();
                    }
                    shell.refresh();
                });
                marks.add(m);
                lastX = x;
                lastY = y;
            }
            if (pts.size() > 1) p.getChildren().add(line);    // 6b: a single score is a single point
            p.getChildren().addAll(marks);
            if (!pts.isEmpty()) {
                Text label = new Text(shortName(s.course().getName()));
                label.setFill(Color.web(color));
                label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");
                label.setX(lastX + 10);
                label.setY(lastY + 4);
                p.getChildren().add(label);
            }
        }
        VBox box = new VBox(p);
        box.setPadding(new Insets(20, 24, 8, 24));
        return box;
    }

    private Node legend(List<Series> series) {
        FlowPane f = new FlowPane(16, 8);
        f.setPadding(new Insets(0, 24, 18, 24));
        for (Series s : series) {
            int n = s.graph().getPoints().size();
            HBox item = Ui.row(6, marker(s.index(), Ui.courseColor(ctx.student, s.course()), 0.9),
                    Ui.label(s.course().getName() + (n == 0 ? " (no data)" : ""), "t2"));
            f.getChildren().add(item);
        }
        return f;
    }

    private static SVGPath marker(int index, String color, double scale) {
        SVGPath m = new SVGPath();
        m.setContent(SHAPES[index % SHAPES.length]);
        m.setFill(Color.web(color));
        m.setStroke(Color.web(Theme.surface()));
        m.setStrokeWidth(1.5);
        m.setScaleX(scale);
        m.setScaleY(scale);
        return m;
    }

    private static Node padded(Node n) {
        VBox v = new VBox(n);
        v.setPadding(new Insets(14, 20, 14, 20));
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    private static String shortName(String name) {
        String[] w = name.split("\\s+");
        if (w.length == 1) return name;
        StringBuilder sb = new StringBuilder();
        for (String s : w) if (!s.isEmpty() && Character.isUpperCase(s.charAt(0))) sb.append(s.charAt(0));
        return sb.length() >= 2 ? sb.toString() : w[0];
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
