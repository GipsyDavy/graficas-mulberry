package org.gipsybuho.service;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Notificaciones tipo toast en esquina inferior derecha de la ventana.
 * Uso: ToastService.show(window, "Mensaje", ToastService.Type.SUCCESS);
 */
public final class ToastService {

    public enum Type { SUCCESS, WARNING, ERROR, INFO }

    private static final int MAX_STACK = 4;
    private static final VBox stack = new VBox(6);
    private static Popup popup;
    private static Consumer<String> articleNavigator;

    private ToastService() {}

    /** Registra el navegador que abre artículos de ayuda desde los toasts de error con enlace. */
    public static void setArticleNavigator(Consumer<String> nav) { articleNavigator = nav; }

    public static void show(Window owner, String mensaje, Type type) {
        Platform.runLater(() -> mostrar(owner, mensaje, type, null));
    }

    public static void success(Window owner, String msg) { show(owner, msg, Type.SUCCESS); }
    public static void warning(Window owner, String msg) { show(owner, msg, Type.WARNING); }
    public static void error(Window owner, String msg)   { show(owner, msg, Type.ERROR);   }
    public static void info(Window owner, String msg)    { show(owner, msg, Type.INFO);    }

    /** Muestra un toast de error con enlace "Ver solución →" que abre el artículo de ayuda indicado. */
    public static void error(Window owner, String msg, String articleId) {
        Platform.runLater(() -> mostrar(owner, msg, Type.ERROR, articleId));
    }

    private static void mostrar(Window owner, String mensaje, Type type, String articleId) {
        if (popup == null) {
            stack.getStyleClass().add("toast-stack");
            stack.setAlignment(Pos.BOTTOM_RIGHT);
            stack.setPickOnBounds(false);
            popup = new Popup();
            popup.getContent().add(stack);
            popup.setAutoFix(false);
        }

        if (stack.getChildren().size() >= MAX_STACK) {
            stack.getChildren().remove(0);
        }

        Node inner = buildNode(mensaje, type, articleId);
        StackPane card = new StackPane(inner);
        card.setOpacity(0);
        stack.getChildren().add(card);

        posicionar(owner);
        if (!popup.isShowing()) popup.show(owner);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), card);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), card);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            stack.getChildren().remove(card);
            if (stack.getChildren().isEmpty()) popup.hide();
        });

        SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
        seq.play();

        card.setOnMouseClicked(e -> {
            seq.stop();
            stack.getChildren().remove(card);
            if (stack.getChildren().isEmpty()) popup.hide();
        });

        owner.xProperty().addListener((o, ov, nv) -> posicionar(owner));
        owner.yProperty().addListener((o, ov, nv) -> posicionar(owner));
        owner.widthProperty().addListener((o, ov, nv) -> posicionar(owner));
        owner.heightProperty().addListener((o, ov, nv) -> posicionar(owner));
    }

    private static Node buildNode(String mensaje, Type type, String articleId) {
        Label lbl = new Label(mensaje);
        lbl.setWrapText(true);

        if (articleId == null || articleNavigator == null) {
            lbl.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
            lbl.setMaxWidth(300);
            return lbl;
        }

        lbl.setMaxWidth(276);
        Hyperlink link = new Hyperlink("Ver solución →");
        link.getStyleClass().add("toast-link");
        link.setOnAction(e -> articleNavigator.accept(articleId));

        VBox box = new VBox(4, lbl, link);
        box.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
        box.setMaxWidth(300);
        return box;
    }

    private static void posicionar(Window owner) {
        if (popup != null && owner != null) {
            popup.setX(owner.getX() + owner.getWidth() - 330);
            popup.setY(owner.getY() + owner.getHeight() - 200);
        }
    }
}
