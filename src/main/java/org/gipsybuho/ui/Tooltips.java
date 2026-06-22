package org.gipsybuho.ui;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Crea Tooltips con un delay de aparición más corto y una duración visible
 * mayor que el default de JavaFX (1 s de espera, autoocultado a los 5 s
 * aunque el ratón siga encima — demasiado breve para leer el texto).
 */
public final class Tooltips {

    private Tooltips() {}

    public static Tooltip of(String texto) {
        Tooltip tip = of();
        tip.setText(texto);
        return tip;
    }

    /** Para los casos donde el texto se fija después con {@code setText()}. */
    public static Tooltip of() {
        Tooltip tip = new Tooltip();
        tip.setShowDelay(Duration.millis(300));
        // Sin límite de tiempo: permanece mientras el foco/ratón siga en el control,
        // solo se oculta al salir. Con un valor fijo (ej. 20s) se ocultaba sola
        // a destiempo si el usuario seguía mirando, dando sensación de parpadeo.
        tip.setShowDuration(Duration.INDEFINITE);
        return tip;
    }
}
