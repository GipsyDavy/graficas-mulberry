package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gipsybuho.service.SoundService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Gestiona la apertura de módulos ERP en ventanas emergentes independientes.
 * Cada módulo puede tener como máximo una ventana emergente abierta a la vez.
 * Detecta cambios sin guardar mediante escucha de eventos de teclado en campos
 * de texto y resetea el flag cuando el usuario pulsa botones de guardado.
 */
public class ModuloWindowManager {

    private static final Map<String, Stage>    ventanas    = new LinkedHashMap<>();
    private static final Map<String, boolean[]> dirtyFlags  = new LinkedHashMap<>();

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Abre el módulo indicado en una ventana emergente independiente.
     * Si ya hay una ventana abierta para ese módulo, la trae al frente.
     *
     * @param titulo      Texto del botón del menú lateral (incluye icono).
     * @param factory     Proveedor que crea la vista (llamado solo la primera vez).
     * @param cssSheets   Hojas de estilo a aplicar (de la escena principal).
     */
    public static void abrirEnVentana(String titulo, Supplier<Parent> factory,
                                      List<String> cssSheets) {
        Stage existente = ventanas.get(titulo);
        if (existente != null && existente.isShowing()) {
            SoundService.play(SoundService.Sound.NOTIFICATION);
            existente.toFront();
            return;
        }

        Parent vista = factory.get();
        Scene scene = new Scene(vista, 1150, 750);
        if (cssSheets != null && !cssSheets.isEmpty()) {
            scene.getStylesheets().addAll(cssSheets);
        }

        // Intentar cargar icono de la aplicación
        Stage stage = new Stage();
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(
                ModuloWindowManager.class.getResourceAsStream("/org/gipsybuho/img/logo.jpg"))));
        } catch (Exception ignored) {}

        stage.setTitle("Gráficas Mulberry — " + limpiarIcono(titulo));
        stage.initModality(Modality.NONE);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Rastreo de cambios sin guardar
        boolean[] dirty = {false};
        dirtyFlags.put(titulo, dirty);
        instalarRastreoCambios(scene, dirty);

        // Interceptar el cierre de la ventana
        stage.setOnCloseRequest(e -> {
            e.consume();
            gestionarCierre(stage, titulo, dirty);
        });

        ventanas.put(titulo, stage);
        stage.show();
        SoundService.play(SoundService.Sound.WINDOW_OPEN);
    }

    public static boolean estaAbierto(String titulo) {
        Stage s = ventanas.get(titulo);
        return s != null && s.isShowing();
    }

    public static void traerAlFrente(String titulo) {
        Stage s = ventanas.get(titulo);
        if (s != null && s.isShowing()) s.toFront();
    }

    /** Cierra todas las ventanas emergentes (útil al salir de la aplicación). */
    public static void cerrarTodas() {
        new ArrayList<>(ventanas.values()).forEach(Stage::close);
        ventanas.clear();
        dirtyFlags.clear();
    }

    // ── Rastreo de cambios ────────────────────────────────────────────────────

    private static void instalarRastreoCambios(Scene scene, boolean[] dirty) {
        // Marcar dirty cuando el usuario escribe en cualquier campo de texto
        scene.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (e.getTarget() instanceof TextInputControl) {
                dirty[0] = true;
            }
        });

        // Limpiar dirty cuando se pulsa un botón de guardado / creación / confirmación
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getTarget() instanceof Button btn) {
                String t = btn.getText().toLowerCase();
                if (t.contains("guard") || t.contains("crear") || t.contains("nuevo")
                        || t.contains("actualiz") || t.contains("añad")
                        || t.contains("ok") || t.contains("accept")) {
                    Platform.runLater(() -> dirty[0] = false);
                }
            }
        });
    }

    // ── Diálogo de cierre ─────────────────────────────────────────────────────

    private static void gestionarCierre(Stage stage, String titulo, boolean[] dirty) {
        if (!dirty[0]) {
            // No hay cambios detectados → cerrar directamente
            cerrarVentana(stage, titulo);
            return;
        }

        // Hay cambios sin guardar → preguntar al usuario
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("«" + limpiarIcono(titulo) + "» tiene cambios sin guardar");
        alert.setContentText(
            "Has editado uno o más campos que podrían no haberse guardado todavía.\n\n" +
            "¿Qué deseas hacer?"
        );

        ButtonType btnCerrar   = new ButtonType("Cerrar sin guardar");
        ButtonType btnCancelar = new ButtonType("Volver al módulo", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCerrar, btnCancelar);

        // Aplicar CSS de la ventana al diálogo
        if (stage.getScene() != null) {
            alert.getDialogPane().getStylesheets()
                 .addAll(stage.getScene().getStylesheets());
        }

        // Estilizar el botón destructivo en rojo
        alert.setOnShown(ev -> {
            Button btnC = (Button) alert.getDialogPane().lookupButton(btnCerrar);
            if (btnC != null) {
                btnC.setStyle("-fx-background-color:#C0392B; -fx-text-fill:white; -fx-font-weight:bold;");
            }
        });

        alert.showAndWait().ifPresent(resp -> {
            if (resp == btnCerrar) {
                cerrarVentana(stage, titulo);
            }
        });
    }

    private static void cerrarVentana(Stage stage, String titulo) {
        ventanas.remove(titulo);
        dirtyFlags.remove(titulo);
        stage.close();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Elimina emojis e iconos del texto del botón para usarlo como título de ventana. */
    static String limpiarIcono(String texto) {
        if (texto == null) return "";
        // Eliminar emoji/símbolos Unicode que no sean letras, números, espacios o puntuación básica
        return texto.replaceAll("[^\\p{L}\\p{N}\\s/·\\-áéíóúüñÁÉÍÓÚÜÑ]", "").trim();
    }
}
