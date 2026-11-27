package com.cramless.ui;

import com.cramless.model.Course;
import com.cramless.model.Student;
import com.cramless.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The CramLess design system in code: page template, outlined sections with gray header strips,
 * list rows, buttons, badges, alerts, drawers, dialogs and toasts. Every screen is built from these,
 * so all screens share one look.
 */
final class Ui {
    static final String CSS = Ui.class.getResource("/cramless.css").toExternalForm();
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
    static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Where drawers, dialogs and toasts are shown (the area right of the sidebar, or the whole window). */
    static StackPane overlayHost;
    private static final List<Node> openOverlays = new ArrayList<>();

    private Ui() {}

    // ------------------------------------------------------------------ text

    static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        return l;
    }

    static Label wrap(String text, String... styles) {
        Label l = label(text, styles);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    /** A number in the pixel font, the one bold element of the design. */
    static Label num(String text, double size, String... styles) {
        Label l = label(text, "num");
        l.getStyleClass().addAll(styles);
        l.setStyle("-fx-font-size: " + size + "px;");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    static String date(LocalDate d) { return d == null ? "-" : d.format(DATE); }
    static String dayMonth(LocalDate d) { return d == null ? "-" : d.format(DAY_MONTH); }
    static String time(LocalTime t) { return t == null ? "-" : t.format(TIME); }
    static String pct(double v) { return String.format(Locale.ROOT, "%.1f%%", v); }
    static String hours(double h) { return (h == Math.rint(h) ? String.valueOf((int) h) : String.format(Locale.ROOT, "%.1f", h)) + " h"; }

    static String minutes(int mins) {
        if (mins < 60) return mins + " min";
        return mins / 60 + " h" + (mins % 60 == 0 ? "" : " " + mins % 60 + " min");
    }

    static String days(int n) { return n + (n == 1 ? " day" : " days"); }

    static String courseColor(Student s, Course c) {
        String[] colors = Theme.courseColors();
        if (s == null || c == null) return colors[0];
        int i = s.getCourses().indexOf(c);
        return colors[Math.max(0, i) % colors.length];
    }

    // ------------------------------------------------------------------ layout

    static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    static HBox row(double gap, Node... children) {
        HBox h = new HBox(gap, children);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static VBox col(double gap, Node... children) {
        return new VBox(gap, children);
    }

    /** The page template: a centred column of the given width inside a scroll pane. */
    static ScrollPane page(double maxWidth, Node... children) {
        VBox column = new VBox(24, children);
        column.setMaxWidth(maxWidth);
        column.setPadding(new Insets(40, 48, 72, 48));
        StackPane centre = new StackPane(column);
        centre.setAlignment(Pos.TOP_CENTER);
        ScrollPane sp = new ScrollPane(centre);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    /** Page title with one line of context, and the page's actions on the right. */
    static HBox pageHead(String title, String subtitle, Node... actions) {
        VBox text = new VBox(6, label(title, "h1"));
        if (subtitle != null) text.getChildren().add(label(subtitle, "sub"));
        HBox h = new HBox(16, text, grow());
        h.getChildren().addAll(actions);
        h.setAlignment(Pos.BOTTOM_LEFT);
        h.getStyleClass().add("page-head");
        return h;
    }

    /** An outlined panel with a gray header strip. */
    static VBox section(String title, Node trailing, Node... body) {
        HBox head = new HBox(12, label(title, "sec-title"), grow());
        if (trailing != null) head.getChildren().add(trailing);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("sec-head");
        VBox v = new VBox(head);
        v.getChildren().addAll(body);
        v.getStyleClass().add("sec");
        clip(v, 8);
        return v;
    }

    static VBox card(Node... children) {
        VBox v = new VBox(children);
        v.getStyleClass().add("card");
        clip(v, 8);
        return v;
    }

    /** Clips a region to its rounded corners so row hovers and strips stay inside. */
    static void clip(Region r, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(r.widthProperty());
        clip.heightProperty().bind(r.heightProperty());
        r.setClip(clip);
    }

    /** List rows separated by lines. */
    static VBox rows(List<? extends Node> rows) {
        VBox v = new VBox();
        boolean first = true;
        for (Node n : rows) {
            if (!first) n.getStyleClass().add("divided");
            first = false;
            v.getChildren().add(n);
        }
        return v;
    }

    static HBox rowItem(Node... children) {
        HBox h = new HBox(16, children);
        h.getStyleClass().add("row-item");
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static void clickable(Node row, Runnable action) {
        row.getStyleClass().add("clickable");
        row.setFocusTraversable(true);
        row.setOnMouseClicked(e -> action.run());
        row.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) action.run();
        });
    }

    /** Title and subtitle stacked, the usual middle of a row. */
    static VBox twoLine(String title, String sub) {
        VBox v = new VBox(2, label(title, "t1"));
        if (sub != null) v.getChildren().add(label(sub, "t2"));
        v.setMinWidth(0);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    /** A strip of figures, each with a small label above it. */
    static HBox kv(Node[]... cells) {
        HBox h = new HBox();
        h.getStyleClass().add("card");
        clip(h, 8);
        boolean first = true;
        for (Node[] c : cells) {
            VBox cell = new VBox(8, c);
            cell.getStyleClass().add("kv-cell");
            if (first) cell.getStyleClass().add("first");
            first = false;
            HBox.setHgrow(cell, Priority.ALWAYS);
            cell.setMaxWidth(Double.MAX_VALUE);
            h.getChildren().add(cell);
        }
        return h;
    }

    static Node[] kvCell(String key, Node value) {
        return new Node[] {label(key, "meta"), value};
    }

    /** Label and value pairs on lines, like a definition list. */
    static VBox dl(String[]... pairs) {
        VBox v = new VBox();
        boolean first = true;
        for (String[] p : pairs) {
            Label value = wrap(p[1], "t1");
            value.setAlignment(Pos.CENTER_RIGHT);
            value.setStyle("-fx-text-alignment: right;");
            HBox r = new HBox(16, label(p[0], "meta"), grow(), value);
            r.setAlignment(Pos.CENTER_LEFT);
            r.getStyleClass().add("dl-row");
            if (first) r.getStyleClass().add("first");
            first = false;
            v.getChildren().add(r);
        }
        return v;
    }

    // ------------------------------------------------------------------ small parts

    static Label badge(String text, String tone) {
        Label l = label(text, "badge");
        if (tone != null && !tone.isEmpty()) l.getStyleClass().add(tone);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    static Region dot(String color) {
        Region r = new Region();
        r.setMinSize(8, 8);
        r.setMaxSize(8, 8);
        r.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
        return r;
    }

    /** A segmented pixel bar: n cells, the first `on` of them filled. */
    static HBox bar(int cells, int on, String tone) {
        HBox h = new HBox(2);
        for (int i = 0; i < cells; i++) {
            Region r = new Region();
            r.getStyleClass().add("bar-cell");
            if (i < on) r.getStyleClass().add("on-" + tone);
            HBox.setHgrow(r, Priority.ALWAYS);
            r.setMaxWidth(Double.MAX_VALUE);
            h.getChildren().add(r);
        }
        return h;
    }

    /** As {@link #bar} but with each filled cell in its own colour. */
    static HBox colourBar(int cells, int on, String color) {
        HBox h = bar(cells, 0, "ok");
        for (int i = 0; i < on && i < cells; i++) h.getChildren().get(i).setStyle("-fx-background-color: " + color + ";");
        return h;
    }

    /** Segmented control; returns the box, calls onChange with the chosen option. */
    static HBox seg(List<String> options, String selected, Consumer<String> onChange) {
        HBox box = new HBox();
        box.getStyleClass().add("seg");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Region.USE_PREF_SIZE);
        for (String o : options) {
            Button b = new Button(o);
            b.getStyleClass().add("seg-btn");
            if (o.equals(selected)) b.getStyleClass().add("on");
            b.setOnAction(e -> {
                for (Node n : box.getChildren()) n.getStyleClass().remove("on");
                b.getStyleClass().add("on");
                onChange.accept(o);
            });
            box.getChildren().add(b);
        }
        return box;
    }

    static ImageView logo(double width) {
        ImageView iv = new ImageView(new Image(Ui.class.getResource("/images/logo-144.png").toExternalForm()));
        iv.setFitWidth(width);
        iv.setPreserveRatio(true);
        iv.setSmooth(false);
        return iv;
    }

    static HBox wordmark(double size) {
        Label a = label("CRAM", "wordmark");
        Label b = label("LESS", "wordmark-accent");
        a.setStyle("-fx-font-size: " + size + "px;");
        b.setStyle("-fx-font-size: " + size + "px;");
        return new HBox(0, a, b);
    }

    // ------------------------------------------------------------------ buttons

    static Button btn(String text, Runnable action, String... styles) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(styles);
        b.setMinWidth(Region.USE_PREF_SIZE);
        if (action != null) b.setOnAction(e -> action.run());
        return b;
    }

    static Button primary(String text, Runnable action) { return btn(text, action, "primary"); }
    static Button quiet(String text, Runnable action) { return btn(text, action, "quiet"); }

    static Button iconBtn(String icon, String tip, Runnable action, String... styles) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, 18));
        b.getStyleClass().add("ib");
        b.getStyleClass().addAll(styles);
        b.setAccessibleText(tip);
        Tooltip.install(b, new Tooltip(tip));
        if (action != null) b.setOnAction(e -> action.run());
        return b;
    }

    static Button disclose(String text, boolean open, Runnable action) {
        Button b = new Button(text, Icons.of(open ? "chevDown" : "right", 14));
        b.getStyleClass().add("disclose");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ------------------------------------------------------------------ forms

    static Label errLabel() {
        Label l = wrap("", "err");
        l.setManaged(false);
        l.setVisible(false);
        return l;
    }

    static void showErr(Label err, Control input, String msg) {
        boolean show = msg != null && !msg.isBlank();
        err.setText(show ? msg : "");
        err.setManaged(show);
        err.setVisible(show);
        if (input != null) {
            input.getStyleClass().remove("invalid");
            if (show) input.getStyleClass().add("invalid");
        }
    }

    static VBox field(String label, Node input, Node... below) {
        VBox v = new VBox(6, label(label, "label-sm"), input);
        v.getChildren().addAll(below);
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    // ------------------------------------------------------------------ messages

    /** An alert strip with an optional bold lead-in. tone: "", "warn", "bad" or "ok". */
    static HBox alert(String tone, String bold, String text, Node... actions) {
        String icon = "bad".equals(tone) || "warn".equals(tone) ? "alert" : "ok".equals(tone) ? "checkCircle" : "dot";
        Node ic = Icons.of(icon, 18);
        if ("bad".equals(tone)) ic.lookup(".icon").getStyleClass().add("bad");
        if ("ok".equals(tone)) ic.lookup(".icon").getStyleClass().add("ok");
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
        if (bold != null) {
            javafx.scene.text.Text tb = new javafx.scene.text.Text(bold + " ");
            tb.getStyleClass().addAll("alert-text", "alert-bold");
            flow.getChildren().add(tb);
        }
        javafx.scene.text.Text tt = new javafx.scene.text.Text(text == null ? "" : text);
        tt.getStyleClass().add("alert-text");
        flow.getChildren().add(tt);
        HBox.setHgrow(flow, Priority.ALWAYS);
        flow.setMaxWidth(Double.MAX_VALUE);
        HBox h = new HBox(12, ic, flow);
        h.getChildren().addAll(actions);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("alert");
        if (tone != null && !tone.isEmpty()) h.getStyleClass().add(tone);
        return h;
    }

    static VBox empty(String title, String text, Node... actions) {
        VBox v = new VBox(6, label(title, "t1", "strong"), wrap(text, "meta"));
        v.setAlignment(Pos.CENTER);
        ((Label) v.getChildren().get(1)).setStyle("-fx-text-alignment: center;");
        if (actions.length > 0) {
            HBox a = new HBox(8, actions);
            a.setAlignment(Pos.CENTER);
            a.setPadding(new Insets(10, 0, 0, 0));
            v.getChildren().add(a);
        }
        v.getStyleClass().add("empty");
        return v;
    }

    // ------------------------------------------------------------------ overlays

    /** A panel sliding in from the right with a gray header and footer. Returns a closer. */
    static Runnable drawer(String title, String sub, Node body, Node... footer) {
        VBox panel = new VBox();
        panel.getStyleClass().add("drawer");
        panel.setPrefWidth(420);
        panel.setMaxWidth(420);
        panel.getChildren().add(panelHead(title, sub));
        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        if (body instanceof Region r) r.setPadding(new Insets(24));
        VBox.setVgrow(sp, Priority.ALWAYS);
        panel.getChildren().add(sp);
        if (footer.length > 0) panel.getChildren().add(panelFoot(footer));
        StackPane layer = new StackPane(shade(), panel);
        StackPane.setAlignment(panel, Pos.CENTER_RIGHT);
        return open(layer, panel);
    }

    /** A centred dialog card with a gray header and footer. Returns a closer. */
    static Runnable dialog(String title, String sub, double width, Node body, Node... footer) {
        VBox card = new VBox();
        card.getStyleClass().add("dialog-card");
        card.setMaxWidth(width);
        card.setPrefWidth(width);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getChildren().add(panelHead(title, sub));
        if (body instanceof Region r) r.setPadding(new Insets(24));
        card.getChildren().add(body);
        if (footer.length > 0) card.getChildren().add(panelFoot(footer));
        clip(card, 8);
        StackPane layer = new StackPane(shade(), card);
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(24));
        return open(layer, card);
    }

    private static HBox panelHead(String title, String sub) {
        VBox t = new VBox(4, wrap(title, "h3"));
        if (sub != null) t.getChildren().add(wrap(sub, "meta"));
        HBox.setHgrow(t, Priority.ALWAYS);
        Button close = iconBtn("x", "Close", Ui::closeOverlay, "quiet");
        HBox h = new HBox(12, t, close);
        h.setAlignment(Pos.TOP_LEFT);
        h.getStyleClass().add("panel-head");
        return h;
    }

    private static HBox panelFoot(Node... footer) {
        HBox f = new HBox(8, grow());
        f.getChildren().addAll(footer);
        f.setAlignment(Pos.CENTER_RIGHT);
        f.getStyleClass().add("panel-foot");
        return f;
    }

    private static Region shade() {
        Region r = new Region();
        r.getStyleClass().add("shade");
        r.setOnMouseClicked(e -> closeOverlay());
        return r;
    }

    private static Runnable open(StackPane layer, Node focusTarget) {
        layer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) closeOverlay();
        });
        overlayHost.getChildren().add(layer);
        openOverlays.add(layer);
        focusTarget.requestFocus();
        return () -> {
            overlayHost.getChildren().remove(layer);
            openOverlays.remove(layer);
        };
    }

    /** Closes the top drawer or dialog. */
    static void closeOverlay() {
        if (openOverlays.isEmpty()) return;
        Node top = openOverlays.remove(openOverlays.size() - 1);
        overlayHost.getChildren().remove(top);
    }

    static void closeAllOverlays() {
        for (Node n : openOverlays) overlayHost.getChildren().remove(n);
        openOverlays.clear();
    }

    private static Node currentToast;

    /** A short message at the bottom, with an optional action such as Undo. */
    static void toast(String message, String actionText, Runnable action) {
        if (overlayHost == null) return;
        if (currentToast != null) overlayHost.getChildren().remove(currentToast);
        Label l = label(message);
        HBox t = new HBox(16, l);
        t.setAlignment(Pos.CENTER_LEFT);
        t.getStyleClass().add("toast");
        t.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (actionText != null) {
            Button b = btn(actionText, null);
            b.setOnAction(e -> {
                overlayHost.getChildren().remove(t);
                action.run();
            });
            t.getChildren().add(b);
        } else {
            t.setPadding(new Insets(10, 16, 10, 16));
        }
        StackPane.setAlignment(t, Pos.BOTTOM_CENTER);
        StackPane.setMargin(t, new Insets(0, 0, 28, 0));
        overlayHost.getChildren().add(t);
        currentToast = t;
        PauseTransition p = new PauseTransition(Duration.seconds(actionText == null ? 4 : 8));
        p.setOnFinished(e -> overlayHost.getChildren().remove(t));
        p.play();
    }

    static void toast(String message) { toast(message, null, null); }

    /** Runs an action; a ValidationException is shown as a toast instead of crashing. */
    static boolean attempt(Runnable action) {
        try {
            action.run();
            return true;
        } catch (ValidationException e) {
            toast(e.getMessage());
            return false;
        } catch (RuntimeException e) {
            toast("Something went wrong: " + e.getMessage());
            return false;
        }
    }
}
