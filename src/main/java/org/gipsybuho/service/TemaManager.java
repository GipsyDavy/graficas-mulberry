package org.gipsybuho.service;

import javafx.scene.Scene;
import org.gipsybuho.db.DatabaseManager;

import java.net.URL;
import java.util.Set;

public class TemaManager {

    public static final String KEY_TEMA      = "ui_tema";
    public static final String KEY_FUENTE    = "ui_fuente";
    public static final String KEY_TAM_FUENTE = "ui_tamano_fuente";
    public static final String KEY_MODO_OSCURO = "ui_modo_oscuro";

    private static final String THEMES_PATH  = "/org/gipsybuho/themes/theme-";
    private static final String TEMA_DEFAULT = "mulberry";
    private static final String CLASE_MODO_OSCURO = "modo-oscuro";
    private static final Set<String> TEMAS_DISPONIBLES = Set.of(
        "mulberry", "azul", "verde", "rojo", "claro"
    );

    // ── Aplicar todo al arrancar ──────────────────────────────────────────────

    public static void aplicarTodo(Scene scene) {
        String tema  = DatabaseManager.getConfig(KEY_TEMA);
        String fuente = DatabaseManager.getConfig(KEY_FUENTE);
        String tamano = DatabaseManager.getConfig(KEY_TAM_FUENTE);
        boolean modoOscuro = Boolean.parseBoolean(DatabaseManager.getConfig(KEY_MODO_OSCURO));

        aplicarTema(scene, normalizarTema(tema));
        aplicarModoOscuro(scene, modoOscuro);
        aplicarFuente(scene, fuente, tamano);
    }

    // ── Tema ──────────────────────────────────────────────────────────────────

    public static void aplicarTema(Scene scene, String temaId) {
        String temaNormalizado = normalizarTema(temaId);
        // Quitar cualquier tema anterior
        scene.getStylesheets().removeIf(s -> s.contains("/themes/theme-"));

        // Añadir el nuevo tema (se añade DESPUÉS de styles.css para que sus
        // variables .root sobreescriban las definidas en styles.css)
        try {
            URL url = TemaManager.class.getResource(THEMES_PATH + temaNormalizado + ".css");
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("TemaManager: no se pudo cargar el tema '" + temaNormalizado + "': " + e.getMessage());
        }

        DatabaseManager.setConfig(KEY_TEMA, temaNormalizado);
    }

    public static void aplicarModoOscuro(Scene scene, boolean activo) {
        scene.getRoot().getStyleClass().remove(CLASE_MODO_OSCURO);
        if (activo) {
            scene.getRoot().getStyleClass().add(CLASE_MODO_OSCURO);
        }
        DatabaseManager.setConfig(KEY_MODO_OSCURO, Boolean.toString(activo));
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
        return normalizarTema(t);
    }

    public static boolean isModoOscuroActivo() {
        return Boolean.parseBoolean(DatabaseManager.getConfig(KEY_MODO_OSCURO));
    }

    private static String normalizarTema(String temaId) {
        return temaId != null && TEMAS_DISPONIBLES.contains(temaId) ? temaId : TEMA_DEFAULT;
    }

    public static String getFuenteActual() {
        return DatabaseManager.getConfig(KEY_FUENTE);
    }

    public static String getTamanoFuenteActual() {
        return DatabaseManager.getConfig(KEY_TAM_FUENTE);
    }
}
