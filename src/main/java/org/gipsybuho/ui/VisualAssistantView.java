package org.gipsybuho.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.TextToSpeechService;

import java.util.List;
import java.util.Map;

public class VisualAssistantView extends StackPane {

    public static final String KEY_ACTIVO = "asistente_visual_activo";
    public static final String KEY_VOZ = "asistente_visual_voz";
    public static final String KEY_PERSONAJE = "asistente_visual_personaje";
    public static final String KEY_TAMANO = "asistente_visual_tamano";
    private static final String KEY_POS_X = "asistente_visual_x";
    private static final String KEY_POS_Y = "asistente_visual_y";

    public static final Map<String, String> PERSONAJES = Map.of(
        "Búho", "🦉",
        "Robot", "🤖",
        "Guía", "🧑‍💼",
        "Técnico", "👩‍🔧"
    );

    private final Label personaje = new Label();
    private final Label texto = new Label();
    private final VBox bubble;
    private final HBox body;
    private final BooleanProperty activo = new SimpleBooleanProperty();
    private boolean vozActiva;
    private String personajeActual;
    private int tamanoActual;
    private ScaleTransition hablarAnimacion;
    private Timeline escrituraAnimacion;
    private Timeline pulsoVozAnimacion;
    private PauseTransition pararPulsoVoz;
    private FadeTransition parpadeoAnimacion;
    private double dragOffsetX;
    private double dragOffsetY;

    public VisualAssistantView() {
        getStyleClass().add("visual-assistant");
        setManaged(false);
        setPickOnBounds(false);

        personaje.getStyleClass().add("visual-assistant-character");
        texto.getStyleClass().add("visual-assistant-bubble-text");
        texto.setWrapText(true);
        texto.setMaxWidth(260);

        this.bubble = new VBox(texto);
        this.bubble.getStyleClass().add("visual-assistant-bubble");
        this.bubble.setMaxWidth(300);

        this.body = new HBox(10, personaje, bubble);
        this.body.setAlignment(Pos.BOTTOM_LEFT);
        this.body.getStyleClass().add("visual-assistant-body");
        getChildren().add(this.body);

        personajeActual = valorConfig(KEY_PERSONAJE, "Búho");
        tamanoActual = (int) parseDouble(DatabaseManager.getConfig(KEY_TAMANO), 58);
        vozActiva = "1".equals(DatabaseManager.getConfig(KEY_VOZ));
        activo.set(!"0".equals(DatabaseManager.getConfig(KEY_ACTIVO)));

        actualizarVisibilidad();
        actualizarPersonaje();
        actualizarTamano();
        iniciarAnimacionReposo();
        iniciarParpadeoSutil();

        personaje.setOnMousePressed(e -> {
            dragOffsetX = e.getX();
            dragOffsetY = e.getY();
            body.getStyleClass().add("visual-assistant-dragging");
            body.setScaleX(1.04);
            body.setScaleY(1.04);
        });
        personaje.setOnMouseDragged(e -> {
            relocateClamped(getLayoutX() + e.getX() - dragOffsetX, getLayoutY() + e.getY() - dragOffsetY);
            DatabaseManager.setConfig(KEY_POS_X, String.valueOf((int) getLayoutX()));
            DatabaseManager.setConfig(KEY_POS_Y, String.valueOf((int) getLayoutY()));
        });
        personaje.setOnMouseReleased(e -> {
            body.getStyleClass().remove("visual-assistant-dragging");
            body.setScaleX(1.0);
            body.setScaleY(1.0);
        });
        setOnMouseMoved(e -> inclinarHaciaCursor(e.getX()));
        setOnMouseExited(e -> personaje.setRotate(0));

        parentProperty().addListener((obs, oldParent, parent) -> restaurarPosicion());
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) restaurarPosicion();
        });

        animarSaludo();
        decir("Estoy listo para ayudarte. Abre un módulo y te iré indicando para qué sirve.");
    }

    public static List<String> nombresPersonajes() {
        return PERSONAJES.keySet().stream().sorted().toList();
    }

    public void decirModulo(String modulo) {
        decir(ayudaPara(modulo));
    }

    public void decir(String mensaje) {
        escribirTexto(mensaje);
        actualizarEstadoVisual(mensaje);
        animarGlobo();
        if (isActivo() && vozActiva) {
            TextToSpeechService.speak(mensaje);
            animarPulsoVoz(mensaje);
        } else {
            animarHabla();
        }
    }

    public boolean isActivo() {
        return activo.get();
    }

    public BooleanProperty activoProperty() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo.set(activo);
        DatabaseManager.setConfig(KEY_ACTIVO, activo ? "1" : "0");
        actualizarVisibilidadAnimada(activo);
        if (activo) {
            decir("Asistente visual activado. Abre un módulo y te explicaré su función.");
        } else {
            TextToSpeechService.stop();
            detenerPulsoVoz();
        }
    }

    public boolean isVozActiva() {
        return vozActiva;
    }

    public void setVozActiva(boolean vozActiva) {
        this.vozActiva = vozActiva;
        DatabaseManager.setConfig(KEY_VOZ, vozActiva ? "1" : "0");
        if (!vozActiva) TextToSpeechService.stop();
    }

    public String getPersonajeActual() {
        return personajeActual;
    }

    public void setPersonajeActual(String personajeActual) {
        this.personajeActual = PERSONAJES.containsKey(personajeActual) ? personajeActual : "Búho";
        DatabaseManager.setConfig(KEY_PERSONAJE, this.personajeActual);
        actualizarPersonaje();
        animarSaludo();
    }

    public int getTamanoActual() {
        return tamanoActual;
    }

    public void setTamanoActual(int tamanoActual) {
        this.tamanoActual = Math.max(44, Math.min(tamanoActual, 88));
        DatabaseManager.setConfig(KEY_TAMANO, String.valueOf(this.tamanoActual));
        actualizarTamano();
    }

    public void restablecerPosicion() {
        DatabaseManager.setConfig(KEY_POS_X, "24");
        DatabaseManager.setConfig(KEY_POS_Y, "24");
        relocateClamped(24, 24);
        llamarAtencion();
    }

    public void llamarAtencion() {
        Timeline shake = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(body.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(70), new KeyValue(body.translateXProperty(), -6)),
            new KeyFrame(Duration.millis(140), new KeyValue(body.translateXProperty(), 6)),
            new KeyFrame(Duration.millis(210), new KeyValue(body.translateXProperty(), -4)),
            new KeyFrame(Duration.millis(280), new KeyValue(body.translateXProperty(), 4)),
            new KeyFrame(Duration.millis(350), new KeyValue(body.translateXProperty(), 0))
        );
        shake.play();
    }

    private String ayudaPara(String modulo) {
        String limpio = modulo.replaceAll("^[^\\p{L}\\p{N}]+", "").trim();
        return switch (limpio) {
            case "Panel principal" -> "Panel principal: aquí ves el resumen general, avisos y accesos rápidos de la gestión.";
            case "Clientes" -> "Clientes: registra, busca, edita y elimina clientes. Desde aquí se alimentan presupuestos, facturas y pedidos.";
            case "Presupuestos" -> "Presupuestos: crea ofertas para clientes, añade líneas y prepara documentos para enviar.";
            case "Facturas" -> "Facturas: gestiona facturación, estados de cobro y documentos emitidos.";
            case "Albaranes" -> "Albaranes: controla entregas y documentos vinculados a pedidos o facturas.";
            case "Pedidos" -> "Pedidos: organiza trabajos, entregas previstas, importes y pagos asociados.";
            case "Tarifas" -> "Tarifas: define precios por técnica, mínimos y costes de preparación.";
            case "Materiales" -> "Materiales: controla stock, consumos, proveedores y pagos de compras.";
            case "Empleados" -> "Empleados: gestiona datos del personal, altas, bajas y contacto.";
            case "Nóminas" -> "Nóminas: calcula y revisa salarios, deducciones y costes de empresa.";
            case "Estadísticas" -> "Estadísticas: consulta gráficos y métricas para entender la evolución del negocio.";
            case "Calendario" -> "Calendario: crea notas y recordatorios para fechas importantes.";
            case "Asistente IA" -> "Asistente IA: escribe consultas, pide ayuda sobre el ERP y activa la voz si quieres escuchar las respuestas.";
            case "Importar Backup" -> "Importar Backup: restaura datos desde una copia de seguridad compatible.";
            case "Exportar / Backup" -> "Exportar y Backup: genera copias de seguridad y exporta información de la aplicación.";
            case "Configuración" -> "Configuración: ajusta empresa, temas, audio, música, calendario y preferencias generales.";
            case "Configuración asistente visual" -> "Configuración asistente visual: elige personaje, tamaño, voz y posición del ayudante flotante.";
            default -> "Este módulo agrupa herramientas de gestión. Usa los botones principales y las tablas para trabajar con los registros.";
        };
    }

    private void actualizarVisibilidad() {
        setVisible(isActivo());
        setManaged(false);
        setOpacity(isActivo() ? 1 : 0);
    }

    private void actualizarVisibilidadAnimada(boolean mostrar) {
        if (mostrar) {
            setVisible(true);
            setOpacity(0);
            setTranslateY(12);
            FadeTransition fade = new FadeTransition(Duration.millis(220), this);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition subir = new TranslateTransition(Duration.millis(220), this);
            subir.setFromY(12);
            subir.setToY(0);
            fade.play();
            subir.play();
            animarSaludo();
        } else {
            FadeTransition fade = new FadeTransition(Duration.millis(180), this);
            fade.setFromValue(getOpacity());
            fade.setToValue(0);
            fade.setOnFinished(e -> setVisible(false));
            fade.play();
        }
    }

    private void actualizarPersonaje() {
        personaje.setText(PERSONAJES.getOrDefault(personajeActual, "🦉"));
    }

    private void actualizarTamano() {
        personaje.setStyle("-fx-font-size: " + tamanoActual + "px;");
    }

    private void iniciarAnimacionReposo() {
        TranslateTransition subir = new TranslateTransition(Duration.seconds(1.6), personaje);
        subir.setFromY(0);
        subir.setToY(-5);
        subir.setAutoReverse(true);
        subir.setCycleCount(Animation.INDEFINITE);
        subir.play();
    }

    private void iniciarParpadeoSutil() {
        parpadeoAnimacion = new FadeTransition(Duration.millis(110), personaje);
        parpadeoAnimacion.setFromValue(1.0);
        parpadeoAnimacion.setToValue(0.82);
        parpadeoAnimacion.setAutoReverse(true);
        parpadeoAnimacion.setCycleCount(2);

        Timeline ciclo = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
            if (isActivo()) parpadeoAnimacion.playFromStart();
        }));
        ciclo.setCycleCount(Animation.INDEFINITE);
        ciclo.play();
    }

    private void escribirTexto(String mensaje) {
        if (escrituraAnimacion != null) escrituraAnimacion.stop();
        texto.setText("");
        int longitud = mensaje.length();
        escrituraAnimacion = new Timeline();
        int paso = Math.max(1, longitud / 90);
        for (int i = 0; i <= longitud; i += paso) {
            int end = Math.min(i, longitud);
            escrituraAnimacion.getKeyFrames().add(new KeyFrame(
                Duration.millis(end * 9L),
                e -> texto.setText(mensaje.substring(0, end))
            ));
        }
        escrituraAnimacion.getKeyFrames().add(new KeyFrame(
            Duration.millis(longitud * 9L + 15),
            e -> texto.setText(mensaje)
        ));
        escrituraAnimacion.play();
    }

    private void animarGlobo() {
        bubble.setScaleX(0.96);
        bubble.setScaleY(0.96);
        ScaleTransition pop = new ScaleTransition(Duration.millis(180), bubble);
        pop.setToX(1.0);
        pop.setToY(1.0);
        pop.play();
    }

    private void animarSaludo() {
        RotateTransition saludo = new RotateTransition(Duration.millis(360), personaje);
        saludo.setFromAngle(-8);
        saludo.setToAngle(8);
        saludo.setAutoReverse(true);
        saludo.setCycleCount(2);
        saludo.setOnFinished(e -> personaje.setRotate(0));
        saludo.play();
    }

    private void animarHabla() {
        if (!isActivo()) return;
        if (hablarAnimacion != null) hablarAnimacion.stop();
        hablarAnimacion = new ScaleTransition(Duration.millis(180), personaje);
        hablarAnimacion.setFromX(1.0);
        hablarAnimacion.setFromY(1.0);
        hablarAnimacion.setToX(1.08);
        hablarAnimacion.setToY(1.08);
        hablarAnimacion.setAutoReverse(true);
        hablarAnimacion.setCycleCount(4);
        hablarAnimacion.play();
    }

    private void animarPulsoVoz(String mensaje) {
        detenerPulsoVoz();
        pulsoVozAnimacion = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(personaje.scaleXProperty(), 1.0), new KeyValue(personaje.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(260), new KeyValue(personaje.scaleXProperty(), 1.06), new KeyValue(personaje.scaleYProperty(), 1.06)),
            new KeyFrame(Duration.millis(520), new KeyValue(personaje.scaleXProperty(), 1.0), new KeyValue(personaje.scaleYProperty(), 1.0))
        );
        pulsoVozAnimacion.setCycleCount(Animation.INDEFINITE);
        pulsoVozAnimacion.play();

        pararPulsoVoz = new PauseTransition(Duration.millis(Math.min(9000, Math.max(1800, mensaje.length() * 55L))));
        pararPulsoVoz.setOnFinished(e -> detenerPulsoVoz());
        pararPulsoVoz.play();
    }

    private void detenerPulsoVoz() {
        if (pulsoVozAnimacion != null) pulsoVozAnimacion.stop();
        if (pararPulsoVoz != null) pararPulsoVoz.stop();
        personaje.setScaleX(1.0);
        personaje.setScaleY(1.0);
    }

    private void inclinarHaciaCursor(double cursorX) {
        double centro = Math.max(1, getLayoutBounds().getWidth() / 2);
        double ratio = Math.max(-1, Math.min(1, (cursorX - centro) / centro));
        personaje.setRotate(ratio * 5);
    }

    private void actualizarEstadoVisual(String mensaje) {
        body.getStyleClass().removeAll("visual-assistant-alert", "visual-assistant-thinking", "visual-assistant-happy");
        String lower = mensaje.toLowerCase();
        if (lower.contains("error") || lower.contains("aviso") || lower.contains("importante")) {
            body.getStyleClass().add("visual-assistant-alert");
            llamarAtencion();
        } else if (lower.contains("consulta") || lower.contains("asistente ia")) {
            body.getStyleClass().add("visual-assistant-thinking");
        } else if (lower.contains("activado") || lower.contains("listo")) {
            body.getStyleClass().add("visual-assistant-happy");
        }
    }

    private void restaurarPosicion() {
        double x = parseDouble(DatabaseManager.getConfig(KEY_POS_X), 24);
        double y = parseDouble(DatabaseManager.getConfig(KEY_POS_Y), 24);
        relocateClamped(x, y);
    }

    private void relocateClamped(double x, double y) {
        if (getParent() == null) {
            relocate(x, y);
            return;
        }
        Bounds parentBounds = getParent().getLayoutBounds();
        Bounds ownBounds = getLayoutBounds();
        double maxX = Math.max(0, parentBounds.getWidth() - ownBounds.getWidth() - 12);
        double maxY = Math.max(0, parentBounds.getHeight() - ownBounds.getHeight() - 12);
        relocate(Math.max(8, Math.min(x, maxX)), Math.max(8, Math.min(y, maxY)));
    }

    private String valorConfig(String clave, String defecto) {
        String valor = DatabaseManager.getConfig(clave);
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    private double parseDouble(String valor, double defecto) {
        try {
            return valor == null || valor.isBlank() ? defecto : Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return defecto;
        }
    }
}
