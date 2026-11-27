package com.cramless.ui;

import com.cramless.model.Student;
import com.cramless.storage.SqlitePersistentStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** CramLess: plan the semester, skip the panic. Run with {@code mvn javafx:run}. */
public class MainApp extends Application {
    private SqlitePersistentStorage store;
    private AppContext ctx;
    private Scene scene;
    private final StackPane root = new StackPane();
    private Shell shell;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();
        root.getStyleClass().add("app-bg");
        root.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new Image(MainApp.class.getResource("/images/icon-" + size + ".png").toExternalForm()));
        }
        store = new SqlitePersistentStorage(databasePath());
        ctx = new AppContext(store);

        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1360, area.getWidth() - 40);
        double height = Math.min(860, area.getHeight() - 70);
        scene = new Scene(root, width, height);
        Theme.apply(scene);
        stage.setTitle("CramLess");
        stage.setScene(scene);
        stage.setMinWidth(Math.min(1000, width));
        stage.setMinHeight(Math.min(640, height));
        showLogin();
        stage.show();
        stage.setX(area.getMinX() + (area.getWidth() - stage.getWidth()) / 2);
        stage.setY(Math.max(area.getMinY(), area.getMinY() + (area.getHeight() - stage.getHeight()) / 2));
    }

    /** Lexend for the interface, Silkscreen (pixel) for the wordmark and numbers. Both SIL Open Font Licence. */
    private static void loadFonts() {
        for (String f : new String[] {"Lexend-Regular", "Lexend-Medium", "Lexend-SemiBold", "Lexend-Bold", "Silkscreen-Regular", "Silkscreen-Bold"}) {
            try (var in = MainApp.class.getResourceAsStream("/fonts/" + f + ".ttf")) {
                if (in != null) Font.loadFont(in, 13);
            } catch (Exception ignored) {
                // a missing font falls back to the system font
            }
        }
    }

    /** The database lives in ~/.cramless/cramless.db unless -Dcramless.db=path is given. */
    private static String databasePath() throws Exception {
        String custom = System.getProperty("cramless.db");
        if (custom != null && !custom.isBlank()) return custom;
        Path dir = Paths.get(System.getProperty("user.home"), ".cramless");
        Files.createDirectories(dir);
        return dir.resolve("cramless.db").toString();
    }

    void toggleTheme() {
        Theme.toggle(scene);
        if (shell != null && ctx.student != null) shell.refresh();
        else showLogin();
    }

    void showLogin() {
        ctx.signOut();
        shell = null;
        Ui.overlayHost = root;
        Ui.closeAllOverlays();
        root.getChildren().setAll(new LoginView(ctx, this::signedIn, this::toggleTheme).build());
    }

    private void signedIn(Student s) {
        ctx.signIn(s);
        Ui.closeAllOverlays();
        shell = new Shell(ctx, this::showLogin, this::toggleTheme);
        root.getChildren().setAll(shell);
    }

    @Override
    public void stop() {
        if (store != null) store.close();
    }
}
