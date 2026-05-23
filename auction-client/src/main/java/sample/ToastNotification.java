package sample;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Toast thông báo nhỏ hiện góc trên-phải, tự động ẩn sau 3 giây.
 */
public class ToastNotification {

    public enum ToastType {
        SUCCESS("#27ae60", "✅"),
        ERROR  ("#e74c3c", "❌"),
        WARNING("#f39c12", "⚠️"),
        INFO   ("#2980b9", "ℹ️");

        final String color, icon;
        ToastType(String color, String icon){ this.color = color; this.icon = icon; }
    }

    /**
     * Hiện toast trên cửa sổ chỉ định.
     * @param owner   Cửa sổ cha
     * @param message Nội dung
     * @param type    Loại (SUCCESS / ERROR / WARNING / INFO)
     */
    public static void show(Window owner, String message, ToastType type) {
        Popup popup = new Popup();
        popup.setAutoHide(false);

        // ── Container ──────────────────────────────────────────────────
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 20, 14, 16));
        box.setMinWidth(300);
        box.setMaxWidth(420);
        box.setStyle(
                "-fx-background-color: " + type.color + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 14, 0, 0, 5);"
        );

        Label icon = new Label(type.icon);
        icon.setStyle("-fx-font-size: 16;");

        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        box.getChildren().addAll(icon, lbl);
        popup.getContent().add(box);

        // ── Vị trí: góc trên-phải cửa sổ ─────────────────────────────
        double x = owner.getX() + owner.getWidth()  - 440;
        double y = owner.getY() + 70;
        popup.show(owner, x, y);

        // ── Fade in ────────────────────────────────────────────────────
        box.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), box);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        // ── Tự ẩn sau 3 giây ──────────────────────────────────────────
        PauseTransition hold = new PauseTransition(Duration.seconds(3));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), box);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    // ── Convenience methods ────────────────────────────────────────────
    public static void success(Window owner, String msg){ show(owner, msg, ToastType.SUCCESS); }
    public static void error  (Window owner, String msg){ show(owner, msg, ToastType.ERROR);   }
    public static void warning(Window owner, String msg){ show(owner, msg, ToastType.WARNING); }
    public static void info   (Window owner, String msg){ show(owner, msg, ToastType.INFO);    }
}
