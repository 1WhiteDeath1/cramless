package com.cramless.ui;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/** Line icons drawn from Lucide (ISC licence, lucide.dev), stored as SVG path data on a 24x24 grid. */
final class Icons {
    private static final Map<String, String> P = new HashMap<>();
    static {
        P.put("moon", "M0 0 M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401");
        P.put("sunny", "M8.0 12.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0 M0 0 M12 2v2 M0 0 M12 20v2 M0 0 m4.93 4.93 1.41 1.41 M0 0 m17.66 17.66 1.41 1.41 M0 0 M2 12h2 M0 0 M20 12h2 M0 0 m6.34 17.66-1.41 1.41 M0 0 m19.07 4.93-1.41 1.41");
        P.put("today", "M8.0 12.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0 M0 0 M12 2v2 M0 0 M12 20v2 M0 0 m4.93 4.93 1.41 1.41 M0 0 m17.66 17.66 1.41 1.41 M0 0 M2 12h2 M0 0 M20 12h2 M0 0 m6.34 17.66-1.41 1.41 M0 0 m19.07 4.93-1.41 1.41");
        P.put("plan", "M0 0 M8 2v3 M0 0 M16 2v3 M5.0 3.0h14.0a2.0 2.0 0 0 1 2.0 2.0v14.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-14.0a2.0 2.0 0 0 1 2.0 -2.0Z M0 0 M3 9h18 M0 0 M8 13h.01 M0 0 M12 13h.01 M0 0 M16 13h.01 M0 0 M8 17h.01 M0 0 M12 17h.01 M0 0 M16 17h.01");
        P.put("priority", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0 M6.0 12.0a6.0 6.0 0 1 0 12.0 0a6.0 6.0 0 1 0 -12.0 0 M10.0 12.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0");
        P.put("courses", "M0 0 M12 5v16 M0 0 M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z");
        P.put("forecast", "M0 0 M16 7h6v6 M0 0 m22 7-8.5 8.5-5-5L2 17");
        P.put("pace", "M0 0 m12 14 4-4 M0 0 M3.34 19a10 10 0 1 1 17.32 0");
        P.put("graphs", "M0 0 M3 3v16a2 2 0 0 0 2 2h16 M0 0 m19 9-5 5-4-4-3 3");
        P.put("leaderboard", "M0 0 M10 14.66V17a1 1 0 0 1-1 1 2 2 0 0 0-2 2v2 M0 0 M14 14.66V17a1 1 0 0 0 1 1 2 2 0 0 1 2 2v2 M0 0 M17.916 10H19.5A2.5 2.5 0 0 0 22 7.5V5a1 1 0 0 0-1-1h-3 M0 0 M4 22h16 M0 0 M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z M0 0 M6.084 10H4.5A2.5 2.5 0 0 1 2 7.5V5a1 1 0 0 1 1-1h3");
        P.put("flame", "M0 0 M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4");
        P.put("star", "M0 0 M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z");
        P.put("logout", "M0 0 m16 17 5-5-5-5 M0 0 M21 12H9 M0 0 M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4");
        P.put("plus", "M0 0 M5 12h14 M0 0 M12 5v14");
        P.put("check", "M0 0 M20 6 9 17l-5-5");
        P.put("checkCircle", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0 M0 0 m16 9-5.5 5.5L8 12");
        P.put("circle", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0");
        P.put("clock", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0 M0 0 M12 6v6l4 2");
        P.put("left", "M0 0 m15 18-6-6 6-6");
        P.put("right", "M0 0 m9 18 6-6-6-6");
        P.put("upload", "M0 0 M12 3v12 M0 0 m17 8-5-5-5 5 M0 0 M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4");
        P.put("pencil", "M0 0 M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z M0 0 m15 5 4 4");
        P.put("trash", "M0 0 M10 11v6 M0 0 M14 11v6 M0 0 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M0 0 M3 6h18 M0 0 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2");
        P.put("userPlus", "M0 0 M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M5.0 7.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0 M19.0 8.0L19.0 14.0 M22.0 11.0L16.0 11.0");
        P.put("alert", "M0 0 m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3 M0 0 M12 9v4 M0 0 M12 17h.01");
        P.put("sparkles", "M0 0 M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z M0 0 M20 2v4 M0 0 M22 4h-4 M2.0 20.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0");
        P.put("cap", "M0 0 M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z M0 0 M22 10v6 M0 0 M6 12.5V16a6 3 0 0 0 12 0v-3.5");
        P.put("timer", "M10.0 2.0L14.0 2.0 M12.0 14.0L15.0 11.0 M4.0 14.0a8.0 8.0 0 1 0 16.0 0a8.0 8.0 0 1 0 -16.0 0");
        P.put("crown", "M0 0 M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 .798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 .798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z M0 0 M5 21h14");
        P.put("bulb", "M0 0 M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5 M0 0 M9 18h6 M0 0 M10 22h4");
        P.put("file", "M0 0 M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z M0 0 M14 2v5a1 1 0 0 0 1 1h5 M0 0 M10 9H8 M0 0 M16 13H8 M0 0 M16 17H8");
        P.put("calCheck", "M0 0 M8 2v3 M0 0 M16 2v3 M5.0 3.0h14.0a2.0 2.0 0 0 1 2.0 2.0v14.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-14.0a2.0 2.0 0 0 1 2.0 -2.0Z M0 0 M3 9h18 M0 0 m9 15 2 2 4-4");
        P.put("zap", "M0 0 M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z");
        P.put("hourglass", "M0 0 M5 22h14 M0 0 M5 2h14 M0 0 M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22 M0 0 M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2");
        P.put("list", "M0 0 M13 5h8 M0 0 M13 12h8 M0 0 M13 19h8 M0 0 m3 17 2 2 4-4 M0 0 m3 7 2 2 4-4");
        P.put("dot", "M11.0 12.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0 M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0");
        P.put("arrow", "M0 0 M5 12h14 M0 0 m12 5 7 7-7 7");
        P.put("x", "M0 0 M18 6 6 18 M0 0 m6 6 12 12");
        P.put("refresh", "M0 0 M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8 M0 0 M21 3v5h-5 M0 0 M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16 M0 0 M8 16H3v5");
        P.put("users", "M0 0 M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M0 0 M16 3.128a4 4 0 0 1 0 7.744 M0 0 M22 21v-2a4 4 0 0 0-3-3.87 M5.0 7.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0");
        P.put("user", "M0 0 M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2 M8.0 7.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0");
        P.put("award", "M0 0 m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526 M6.0 8.0a6.0 6.0 0 1 0 12.0 0a6.0 6.0 0 1 0 -12.0 0");
        P.put("book", "M0 0 M12 5v16 M0 0 m16 12 2 2 4-4 M0 0 M22 6V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2h4.001A2 2 0 0022 17v-1.344");
        P.put("eye", "M0 0 M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0 M9.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0");
        P.put("lock", "M5.0 11.0h14.0a2.0 2.0 0 0 1 2.0 2.0v7.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-7.0a2.0 2.0 0 0 1 2.0 -2.0Z M0 0 M7 11V7a5 5 0 0 1 10 0v4");
        P.put("mail", "M0 0 m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7 M4.0 4.0h16.0a2.0 2.0 0 0 1 2.0 2.0v12.0a2.0 2.0 0 0 1 -2.0 2.0h-16.0a2.0 2.0 0 0 1 -2.0 -2.0v-12.0a2.0 2.0 0 0 1 2.0 -2.0Z");
        P.put("idcard", "M0 0 M13 19a4 4 0 00-8 0 M0 0 M16 10h2 M0 0 M16 14h2 M6.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0 M4.0 5.0h16.0a2.0 2.0 0 0 1 2.0 2.0v10.0a2.0 2.0 0 0 1 -2.0 2.0h-16.0a2.0 2.0 0 0 1 -2.0 -2.0v-10.0a2.0 2.0 0 0 1 2.0 -2.0Z");
        P.put("flag", "M0 0 M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528");
        P.put("panel", "M5.0 3.0h14.0a2.0 2.0 0 0 1 2.0 2.0v14.0a2.0 2.0 0 0 1 -2.0 2.0h-14.0a2.0 2.0 0 0 1 -2.0 -2.0v-14.0a2.0 2.0 0 0 1 2.0 -2.0Z M0 0 M9 3v18");
        P.put("chevDown", "M0 0 m6 9 6 6 6-6");
        P.put("chevUp", "M0 0 m18 15-6-6-6 6");
        P.put("target", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0 M6.0 12.0a6.0 6.0 0 1 0 12.0 0a6.0 6.0 0 1 0 -12.0 0 M10.0 12.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0");
    }

    private Icons() {}

    static String hex(Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    /** An icon of the given size; its stroke colour comes from the "icon" style class unless a paint is given. */
    static Node of(String name, double size) {
        return of(name, size, null);
    }

    static Node of(String name, double size, Paint paint) {
        SVGPath p = new SVGPath();
        // the bare move-tos at the corners pin every icon to the same 24x24 frame, so they all centre alike
        p.setContent("M0 0 " + P.getOrDefault(name, P.get("circle")) + " M24 24");
        p.setFill(Color.TRANSPARENT);
        p.setStrokeWidth(1.8);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.getStyleClass().add("icon");
        // an inline style, because the stylesheet's .icon colour would otherwise win over a colour set in code
        if (paint instanceof Color c) p.setStyle("-fx-stroke: " + hex(c) + ";");
        p.getTransforms().add(new Scale(size / 24.0, size / 24.0, 0, 0));
        StackPane box = new StackPane(new Group(p));
        box.getStyleClass().add("icon-box");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }
}
