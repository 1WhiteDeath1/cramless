package com.cramless.ui;

import com.cramless.model.Student;
import com.cramless.util.ValidationException;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * UC-12 View Leaderboard (viewLeaderboard, filterLeaderboard). Only display names and points are shown;
 * ties are broken by the longer current streak. Friends are added by student ID (extension 6a).
 */
final class LeaderboardView implements Shell.View {
    private final AppContext ctx;
    private final Shell shell;
    private String scope = "Everyone";

    LeaderboardView(AppContext ctx, Shell shell) {
        this.ctx = ctx;
        this.shell = shell;
    }

    @Override
    public Node build() {
        List<Student> ranking = scope.equals("Everyone")
                ? ctx.progress.viewLeaderboard()                                     // system operation 1
                : ctx.progress.filterLeaderboard(scope.toLowerCase());             // system operation 2
        Node head = Ui.pageHead("Leaderboard", "Only display names and points are shown",
                Ui.primary("Add friend", this::openAddFriend));
        Node seg = Ui.seg(List.of("Everyone", "Classmates", "Friends"), scope, o -> { scope = o; shell.refresh(); });
        int pos = ctx.progress.getOwnPosition();
        HBox bar = Ui.row(12, seg, Ui.grow(), Ui.label("Your position: " + pos + " of " + ranking.size(), "meta"));

        List<Node> parts = new ArrayList<>(List.of(head, bar));
        boolean noFriends = scope.equals("Friends") && ctx.student.getFriends().isEmpty();
        if (noFriends) {
            // UC-12 6b
            parts.add(Ui.empty("No friends added yet", "Add classmates by student ID to compare points. They see only your display name and points.",
                    Ui.primary("Add friend", this::openAddFriend)));
            return Ui.page(820, parts.toArray(new Node[0]));
        }
        List<Node> rows = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            Student s = ranking.get(i);
            boolean me = s == ctx.student;
            Label rank = Ui.num(String.valueOf(i + 1), 14, i < 3 ? "num-gold" : "num-muted");
            rank.setMinWidth(36);
            StackPane av = new StackPane(Ui.label(s.getDisplayName().substring(0, 1).toUpperCase(), ""));
            av.getStyleClass().add("avatar");
            if (!me) av.getStyleClass().add("other");
            av.setMinSize(32, 32);
            av.setMaxSize(32, 32);
            boolean tie = i > 0 && ranking.get(i - 1).getTotalPoints() == s.getTotalPoints();
            VBox name = Ui.twoLine(s.getDisplayName() + (me ? " (you)" : ""), tie ? "Same points, ranked by the longer streak" : null);
            Label pts = Ui.num(String.format("%,d", s.getTotalPoints()), 15);
            HBox r = Ui.rowItem(rank, av, name, pts, Ui.label("points", "t2"));
            if (me) r.getStyleClass().add("current");
            rows.add(r);
        }
        parts.add(Ui.section(scope.equals("Everyone") ? "All students" : scope, Ui.label(ranking.size() + " ranked", "meta"), Ui.rows(rows)));
        return Ui.page(820, parts.toArray(new Node[0]));
    }

    /** UC-12 6a: add a friend by student ID; the check says why when it fails. */
    private void openAddFriend() {
        TextField id = new TextField();
        id.setPromptText("25i-0731");
        Label err = Ui.errLabel();
        VBox body = new VBox(16, Ui.field("Friend's student ID", id, Ui.label("They will see only your display name and points", "hint"), err));
        Button add = Ui.primary("Add friend", null);
        add.setDefaultButton(true);
        add.setOnAction(e -> {
            try {
                ctx.progress.addFriend(id.getText().trim());
                scope = "Friends";
                shell.refresh();
                Ui.toast("Friend added.");
            } catch (ValidationException ex) {
                Ui.showErr(err, id, ex.getMessage());
            }
        });
        Ui.dialog("Add friend", null, 440, body, Ui.btn("Cancel", Ui::closeOverlay), add);
    }
}
