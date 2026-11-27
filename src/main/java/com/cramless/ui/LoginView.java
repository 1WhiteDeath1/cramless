package com.cramless.ui;

import com.cramless.model.Student;
import com.cramless.util.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Sign in, and UC-01 Register Student Account (startRegistration, register). */
final class LoginView {
    private final AppContext ctx;
    private final Consumer<Student> onSignedIn;
    private final Runnable onToggleTheme;
    private boolean registerMode;
    private VBox formHolder;

    LoginView(AppContext ctx, Consumer<Student> onSignedIn, Runnable onToggleTheme) {
        this.ctx = ctx;
        this.onSignedIn = onSignedIn;
        this.onToggleTheme = onToggleTheme;
    }

    Node build() {
        HBox root = new HBox(brandPanel(), formSide());
        root.getStyleClass().add("app-bg");
        return root;
    }

    private Node brandPanel() {
        VBox p = new VBox(32);
        p.getStyleClass().add("brand-panel");
        p.setPadding(new Insets(48));
        p.setMinWidth(440);
        p.setPrefWidth(440);
        p.setMaxWidth(440);
        p.getChildren().add(Ui.row(12, Ui.logo(44), Ui.wordmark(24)));
        p.getChildren().add(new VBox(8,
                Ui.wrap("Your syllabus, turned into a semester plan", "h2"),
                Ui.wrap("Study a little each day instead of cramming the night before.", "sub")));
        VBox feats = new VBox();
        String[][] f = {
                {"courses", "Import your syllabus", "Weekly topics and exams become a plan"},
                {"priority", "Know what to study next", "Ranked by due date, weightage and scores"},
                {"star", "Keep a streak with friends", "Points for every topic you finish"}};
        boolean first = true;
        for (String[] r : f) {
            StackPane ic = new StackPane(Icons.of(r[0], 16));
            ic.getStyleClass().add("feat-icon");
            ic.setMinSize(32, 32);
            ic.setMaxSize(32, 32);
            HBox row = new HBox(14, ic, Ui.twoLine(r[1], r[2]));
            row.getStyleClass().add("feat-row");
            if (first) row.getStyleClass().add("first");
            first = false;
            feats.getChildren().add(row);
        }
        p.getChildren().add(feats);
        return p;
    }

    private Node formSide() {
        formHolder = new VBox();
        formHolder.setMaxWidth(520);
        renderForm();
        StackPane side = new StackPane(formHolder);
        side.setPadding(new Insets(48));
        HBox.setHgrow(side, Priority.ALWAYS);
        Button theme = Ui.iconBtn(Theme.isDark() ? "sunny" : "moon", Theme.isDark() ? "Switch to light theme" : "Switch to dark theme", onToggleTheme);
        StackPane.setAlignment(theme, Pos.TOP_RIGHT);
        StackPane.setMargin(theme, new Insets(-24, -24, 0, 0));
        side.getChildren().add(theme);
        return side;
    }

    private void renderForm() {
        Node tabs = Ui.seg(List.of("Sign in", "Create account"), registerMode ? "Create account" : "Sign in", o -> {
            registerMode = o.equals("Create account");
            renderForm();
        });
        VBox body = registerMode ? registerForm() : signInForm();
        VBox sec = Ui.section(registerMode ? "New account" : "Welcome back", tabs, body);
        sec.lookup(".sec-head").setStyle("-fx-min-height: 56; -fx-pref-height: 56;");
        ((Label) sec.lookup(".sec-title")).setStyle("-fx-font-size: 15px; -fx-text-fill: c-ink;");
        formHolder.getChildren().setAll(sec);
    }

    private VBox signInForm() {
        TextField id = new TextField();
        id.setPromptText("25i-0644 or i250644@nu.edu.pk");
        PasswordField pw = new PasswordField();
        Label err = Ui.errLabel();
        Button go = Ui.btn("Sign in", null, "primary", "lg");
        go.setDefaultButton(true);
        go.setMaxWidth(Double.MAX_VALUE);
        go.setOnAction(e -> {
            try {
                onSignedIn.accept(ctx.registration.login(id.getText().trim(), pw.getText()));
            } catch (ValidationException ex) {
                Ui.showErr(err, pw, ex.getMessage());
            }
        });
        VBox v = new VBox(16, Ui.field("Student ID or university email", id), Ui.field("Password", pw, err), go);
        v.setPadding(new Insets(24));
        return v;
    }

    private VBox registerForm() {
        ctx.registration.startRegistration(); // UC-01 system operation 1: the details to request
        TextField full = new TextField();
        TextField display = new TextField();
        TextField sid = new TextField();
        sid.setPromptText("25i-0644");
        TextField email = new TextField();
        email.setPromptText("i250644@nu.edu.pk (optional)");
        PasswordField pw = new PasswordField();
        PasswordField pw2 = new PasswordField();

        Map<String, Label> errs = new HashMap<>();
        Map<String, Control> inputs = new HashMap<>();
        String[] keys = {"fullName", "displayName", "studentId", "email", "password", "confirmPassword"};
        Control[] ctrls = {full, display, sid, email, pw, pw2};
        for (int i = 0; i < keys.length; i++) {
            errs.put(keys[i], Ui.errLabel());
            inputs.put(keys[i], ctrls[i]);
        }

        Label dnHint = Ui.label("Shown on the leaderboard, 3 to 20 characters", "hint");
        Label rule1 = Ui.label("8 or more characters", "hint");
        Label rule2 = Ui.label("A letter and a digit", "hint");
        pw.textProperty().addListener((o, a, t) -> {
            rule1.setText((t.length() >= 8 ? "✓ " : "") + "8 or more characters");
            rule2.setText((t.matches(".*[A-Za-z].*") && t.matches(".*\\d.*") ? "✓ " : "") + "A letter and a digit");
        });

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(16);
        g.add(Ui.field("Full name", full, errs.get("fullName")), 0, 0);
        g.add(Ui.field("Display name", display, dnHint, errs.get("displayName")), 1, 0);
        g.add(Ui.field("Student ID", sid, Ui.label("Roll number, or your university email", "hint"), errs.get("studentId")), 0, 1);
        g.add(Ui.field("University email", email, errs.get("email")), 1, 1);
        g.add(Ui.field("Password", pw, Ui.row(16, rule1, rule2), errs.get("password")), 0, 2);
        g.add(Ui.field("Confirm password", pw2, errs.get("confirmPassword")), 1, 2);
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        g.getColumnConstraints().addAll(c, c);

        VBox taken = new VBox();
        Button create = Ui.btn("Create account", null, "primary", "lg");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setDefaultButton(true);
        create.setOnAction(e -> {
            for (String k : keys) Ui.showErr(errs.get(k), inputs.get(k), null);
            taken.getChildren().clear();
            try {
                Student s = ctx.registration.register(full.getText(), display.getText(), sid.getText(),
                        email.getText(), pw.getText(), pw2.getText());
                onSignedIn.accept(s);
                Ui.toast("Account created. You start with 0 points and a 0-day streak.");
            } catch (ValidationException ex) {
                String f = ex.getField();
                boolean alreadyRegistered = ("studentId".equals(f) || "email".equals(f)) && ex.getMessage().contains("already registered");
                if (alreadyRegistered) {
                    // UC-01 6a: offer to identify them to the existing account instead.
                    taken.getChildren().add(Ui.alert("bad", ex.getMessage(), "Sign in to that account instead?",
                            Ui.btn("Sign in", () -> { registerMode = false; renderForm(); }, "sm")));
                    if (inputs.get(f) != null) inputs.get(f).getStyleClass().add("invalid");
                } else if (f != null && errs.containsKey(f)) {
                    Ui.showErr(errs.get(f), inputs.get(f), ex.getMessage());
                } else {
                    taken.getChildren().add(Ui.alert("bad", null, ex.getMessage()));
                }
            }
        });
        VBox v = new VBox(16, g, taken, create);
        v.setPadding(new Insets(24));
        return v;
    }
}
