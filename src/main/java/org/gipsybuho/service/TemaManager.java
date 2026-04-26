package org.gipsybuho.service;

import javafx.scene.Scene;
import org.gipsybuho.db.DatabaseManager;

import java.net.URL;

public class TemaManager {

    public static final String KEY_TEMA      = "ui_tema";
    public static final String KEY_FUENTE    = "ui_fuente";
    public static final String KEY_TAM_FUENTE = "ui_tamano_fuente";

    private static final String THEMES_PATH  = "/org/gipsybuho/themes/theme-";
    private static final String TEMA_DEFAULT = "mulberry";

    // ── Aplicar todo al arrancar ──────────────────────────────────────────────

    public static void aplicarTodo(Scene scene) {
        String tema  = DatabaseManager.getConfig(KEY_TEMA);
        String fuente = DatabaseManager.getConfig(KEY_FUENTE);
        String tamano = DatabaseManager.getConfig(KEY_TAM_FUENTE);

        aplicarTema(scene, tema.isBlank() ? TEMA_DEFAULT : tema);
        aplicarFuente(scene, fuente, tamano);
    }

    // ── Tema ──────────────────────────────────────────────────────────────────

    public static void aplicarTema(Scene scene, String temaId) {
        // Quitar cualquier tema anterior
        scene.getStylesheets().removeIf(s -> s.contains("/themes/theme-"));

        // Añadir el nuevo tema (se añade DESPUÉS de styles.css para que sus
        // variables .root sobreescriban las definidas en styles.css)
        try {
            URL url = TemaManager.class.getResource(THEMES_PATH + temaId + ".css");
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("TemaManager: no se pudo cargar el tema '" + temaId + "': " + e.getMessage());
        }

        DatabaseManager.setConfig(KEY_TEMA, temaId);
    }

    // ── Tipografía ────────────────────────────────────────────────────────────

    public static void aplicarFuente(Scene scene, String familia, String tamano) {
        StringBuilder style = new StringBuilder();

        if (familia != null && !familia.isBlank()) {
            style.append("-fx-font-family: '").append(familia).append("';");
        }
        if (tamano != null && !tamano.isBlank()) {
            style.append("-fx-font-size: ").append(tamano).append("px;");
        }

        scene.getRoot().setStyle(style.toString());

        if (familia != null) DatabaseManager.setConfig(KEY_FUENTE,     familia);
        if (tamano  != null) DatabaseManager.setConfig(KEY_TAM_FUENTE, tamano);
    }

    // ── Getters para la UI ────────────────────────────────────────────────────

    public static String getTemaActual() {
        String t = DatabaseManager.getConfig(KEY_TEMA);
        return t.isBlank() ? TEMA_DEFAULT : t;
    }

    public static String getFuenteActual() {
        return DatabaseManager.getConfig(KEY_FUENTE);
    }

    public static String getTamanoFuenteActual() {
        return DatabaseManager.getConfig(KEY_TAM_FUENTE);
    }
}
