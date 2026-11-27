package com.cramless.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The signed-in frame: a collapsible sidebar (streak, points, navigation, theme, account) and the page area,
 * which also hosts drawers, dialogs and toasts.
 */
final class Shell extends BorderPane {
    interface View { Node build(); }

    private static final Preferences PREFS = Preferences.userRoot().node("cramless");

    private final AppContext ctx;
    private final Runnable onSignOut;
    private final Runnable onToggleTheme;
    private final StackPane host = new StackPane();
    private final Map<String, View> views = new LinkedHashMap<>();
    private String current = "today";
    private boolean collapsed;

    Shell(AppContext ctx, Runnable onSignOut, Runnable onToggleTheme) {
        this.ctx = ctx;
        this.onSignOut = onSignOut;
        this.onToggleTheme = onToggleTheme;
        collapsed = PREFS.getBoolean("sidebarCollapsed", false);
        views.put("today", new TodayView(ctx, this));
        views.put("plan", new PlanView(ctx, this));
        views.put("priority", new PriorityView(ctx, this));
        views.put("courses", new CoursesView(ctx, this));
        views.put("import", new ImportView(ctx, this));
        views.put("forecast", new ForecastView(ctx, this));
        views.put("pace", new PaceView(ctx, this));
        views.put("graphs", new GraphsView(ctx, this));
        views.put("leaderboard", new LeaderboardView(ctx, this));
        Ui.overlayHost = host;
        setCenter(host);
        go("today");
    }

    void go(String key) {
        current = key;
        refresh();
    }

    /** Rebuilds the sidebar and the current page (after a change, or a theme switch). */
    void refresh() {
        Ui.overlayHost = host;
        Ui.closeAllOverlays();
        setLeft(sidebarPane());
        Node page;
        try {
            page = views.get(current).build();
        } catch (RuntimeException e) {
            page = Ui.page(720, Ui.empty("This page could not be shown", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
        host.getChildren().setAll(page);
        if (page instanceof javafx.scene.control.ScrollPane sp) {
            javafx.application.Platform.runLater(() -> sp.setVvalue(0));
        }
        javafx.application.Platform.runLater(host::requestFocus);
    }

    /** Re-renders only the sidebar (points and streak changed). */
    /** Opens the study plan and starts UC-04 (generateStudyPlan, then the availability drawer). */
    void regeneratePlan() {
        go("plan");
        javafx.application.Platform.runLater(((PlanView) views.get("plan"))::openAvailability);
    }

    void refreshSidebar() { setLeft(sidebarPane()); }

    /** The sidebar scrolls on its own when the window is short. */
    private Node sidebarPane() {
        VBox side = sidebar();
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(side);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        // collapsed rail is too narrow for a scrollbar; the wheel still scrolls it
        sp.setVbarPolicy(collapsed ? javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
                                   : javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setMinWidth(side.getMinWidth());
        sp.setPrefWidth(side.getPrefWidth());
        sp.setMaxWidth(side.getMaxWidth());
        sp.getStyleClass().add("sidebar-scroll");
        return sp;
    }

    String current() { return current; }

    private VBox sidebar() {
        boolean open = !collapsed;
        VBox side = new VBox(1);
        side.getStyleClass().add("sidebar");
        double w = open ? 232 : 64;
        side.setMinWidth(w);
        side.setPrefWidth(w);
        side.setMaxWidth(w);
        side.setPadding(new Insets(14, open ? 12 : 10, 12, open ? 12 : 10));

        // logo, wordmark and the collapse button
        Button toggle = Ui.iconBtn("panel", open ? "Collapse sidebar" : "Expand sidebar", () -> {
            collapsed = !collapsed;
            PREFS.putBoolean("sidebarCollapsed", collapsed);
            setLeft(sidebarPane());
        }, "quiet");
        toggle.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32;");
        Node head;
        if (open) {
            HBox brand = Ui.row(10, Ui.logo(30), Ui.wordmark(16));
            head = Ui.row(8, brand, Ui.grow(), toggle);
        } else {
            VBox v = new VBox(10, Ui.logo(30), toggle);
            v.setAlignment(Pos.CENTER);
            head = v;
        }
        VBox headBox = new VBox(head);
        headBox.setPadding(new Insets(0, 2, 12, 2));
        side.getChildren().add(headBox);

        // rewards: streak and points
        String streak = String.valueOf(ctx.student.getCurrentStreak());
        String points = String.format("%,d", ctx.student.getTotalPoints());
        Node streakTile = reward("flame", streak, "day streak", open);
        Node pointsTile = reward("star", points, "points", open);
        Node rewards;
        if (open) {
            HBox h = new HBox(6, streakTile, pointsTile);
            HBox.setHgrow(streakTile, Priority.ALWAYS);
            HBox.setHgrow(pointsTile, Priority.ALWAYS);
            rewards = h;
        } else {
            rewards = new VBox(6, streakTile, pointsTile);
        }
        VBox rw = new VBox(rewards);
        rw.setPadding(new Insets(0, 0, 10, 0));
        side.getChildren().add(rw);

        group(side, "Plan", open);
        side.getChildren().add(nav("today", "today", "Today", open));
        side.getChildren().add(nav("plan", "plan", "Study plan", open));
        side.getChildren().add(nav("priority", "priority", "Study next", open));
        group(side, "Courses", open);
        side.getChildren().add(nav("courses", "courses", "Courses", open));
        group(side, "Progress", open);
        side.getChildren().add(nav("forecast", "forecast", "Grade forecast", open));
        side.getChildren().add(nav("pace", "pace", "Study pace", open));
        side.getChildren().add(nav("graphs", "graphs", "Graphs", open));
        group(side, "Friends", open);
        side.getChildren().add(nav("leaderboard", "leaderboard", "Leaderboard", open));

        side.getChildren().add(Ui.grow());

        Button theme = navButton(Theme.isDark() ? "sunny" : "moon", Theme.isDark() ? "Light theme" : "Dark theme", open);
        theme.setOnAction(e -> onToggleTheme.run());
        side.getChildren().add(theme);

        // account
        String name = ctx.student.getDisplayName();
        StackPane avatar = new StackPane(Ui.label(initials(ctx.student.getFullName()), ""));
        avatar.getStyleClass().add("avatar");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        Button out = Ui.iconBtn("logout", "Sign out", onSignOut, "quiet");
        out.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-pref-width: 32; -fx-pref-height: 32;");
        Node user;
        if (open) {
            VBox who = new VBox(1, Ui.label(name, "t1"), Ui.label(ctx.student.getStudentId(), "t2"));
            HBox.setHgrow(who, Priority.ALWAYS);
            user = Ui.row(10, avatar, who, out);
        } else {
            VBox v = new VBox(8, avatar, out);
            v.setAlignment(Pos.CENTER);
            user = v;
        }
        VBox ub = new VBox(user);
        ub.getStyleClass().add("user-row");
        VBox.setMargin(ub, new Insets(8, 0, 0, 0));
        side.getChildren().add(ub);
        return side;
    }

    private static Node reward(String icon, String value, String label, boolean open) {
        Node ic = Icons.of(icon, 16);
        ic.lookup(".icon").getStyleClass().add("brand");
        Label v = Ui.num(value, open ? 15 : 10);
        VBox box;
        if (open) {
            HBox top = Ui.row(6, ic, v);
            box = new VBox(2, top, Ui.label(label, "t2"));
        } else {
            box = new VBox(4, ic, v);
            box.setAlignment(Pos.CENTER);
            box.getStyleClass().add("compact");
        }
        box.getStyleClass().add("reward-tile");
        box.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(box, new Tooltip(value + " " + label));
        return box;
    }

    private static void group(VBox side, String title, boolean open) {
        if (open) {
            side.getChildren().add(Ui.label(title, "nav-group"));
        } else {
            Region rule = new Region();
            rule.getStyleClass().add("nav-rule");
            VBox.setMargin(rule, new Insets(8, 6, 8, 6));
            side.getChildren().add(rule);
        }
    }

    private Button nav(String key, String icon, String label, boolean open) {
        Button b = navButton(icon, label, open);
        boolean active = key.equals(current) || ("courses".equals(key) && "import".equals(current));
        if (active) b.getStyleClass().add("active");
        b.setOnAction(e -> go(key));
        return b;
    }

    private static Button navButton(String icon, String label, boolean open) {
        Button b = new Button(open ? label : null, Icons.of(icon, 18));
        b.getStyleClass().add("nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAccessibleText(label);
        if (!open) {
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setAlignment(Pos.CENTER);
            b.setStyle("-fx-padding: 0;");
            Tooltip.install(b, new Tooltip(label));
        }
        return b;
    }

    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().replace(".", " ").split("\\s+");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }
}
