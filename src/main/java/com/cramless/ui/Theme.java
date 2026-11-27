package com.cramless.ui;

import java.util.prefs.Preferences;
import javafx.scene.Scene;

/**
 * Light and dark themes. The colour tokens live in /themes/light.css and /themes/dark.css; this class
 * remembers the choice and holds the few colours that are painted from code (course lines and dots).
 */
final class Theme {
    private static final Preferences PREFS = Preferences.userRoot().node("cramless");
    private static boolean dark;

    /** Course colours, told apart by lightness too; every course is also always labelled by name. */
    private static final String[] LIGHT_COURSES = {"#B84D27", "#7B4F9E", "#1F7A8C", "#4F7A2E", "#A97400", "#8C7B70", "#C25B7A", "#3F6CA8"};
    private static final String[] DARK_COURSES = {"#EE8A5E", "#B48BD6", "#5FB7C6", "#9CC46E", "#F2BE52", "#C7B8AD", "#E58AA6", "#82A9E0"};

    static {
        try {
            dark = PREFS.getBoolean("dark", false);
        } catch (Exception e) {
            dark = false;
        }
    }

    private Theme() {}

    static boolean isDark() { return dark; }

    static String[] courseColors() { return dark ? DARK_COURSES : LIGHT_COURSES; }

    static String ink() { return dark ? "#F2F0EE" : "#1F1D1B"; }
    static String ink3() { return dark ? "#9A9590" : "#73706B"; }
    static String line() { return dark ? "#363432" : "#D3D3CF"; }
    static String surface() { return dark ? "#1B1A19" : "#FFFFFF"; }

    static void apply(Scene scene) {
        if (scene == null) return;
        String tokens = Theme.class.getResource(dark ? "/themes/dark.css" : "/themes/light.css").toExternalForm();
        scene.getStylesheets().setAll(tokens, Ui.CSS);
    }

    /** Switches theme, remembers the choice and restyles the scene. */
    static void toggle(Scene scene) {
        dark = !dark;
        try {
            PREFS.putBoolean("dark", dark);
        } catch (Exception ignored) {
            // the choice is just not remembered next time
        }
        apply(scene);
    }
}
