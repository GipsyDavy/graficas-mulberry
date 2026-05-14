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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.TextToSpeechService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class VisualAssistantView extends StackPane {

    public static final String KEY_ACTIVO = "asistente_visual_activo";
    public static final String KEY_VOZ = "asistente_visual_voz";
    public static final String KEY_PERSONAJE = "asistente_visual_personaje";
    public static final String KEY_TAMANO = "asistente_visual_tamano";
    private static final String KEY_POS_X = "asistente_visual_x";
    private static final String KEY_POS_Y = "asistente_visual_y";
    private static final double BUBBLE_MIN_WIDTH = 170;
    private static final double BUBBLE_MAX_WIDTH = 360;

    public static final Map<String, String> PERSONAJES = Map.of(
        "Búho", "Búho",
        "Robot", "Robot",
        "Guía", "Guía",
        "Técnico", "Técnico",
        "Guardia Civil", "Guardia Civil"
    );

    private final StackPane personaje = new StackPane();
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
    private Rotate rotBrazoIzq, rotBrazoDer, rotPiernaIzq, rotPiernaDer, rotLlave;
    private Circle robLuz;
    private Timeline animExtremidades, animPersonaje;
    private Node ultimoNodoAyuda;
    private long ultimaAyudaMs;
    private final Set<Parent> raicesConAyuda = Collections.newSetFromMap(new WeakHashMap<>());

    public VisualAssistantView() {
        getStyleClass().add("visual-assistant");
        setManaged(false);
        setPickOnBounds(false);
        setMinSize(340, 178);
        setPrefSize(430, 178);
        setMaxSize(560, 215);

        personaje.getStyleClass().add("visual-assistant-character");
        personaje.setMinSize(96, 96);
        personaje.setPrefSize(96, 96);
        personaje.setMaxSize(96, 96);
        texto.getStyleClass().add("visual-assistant-bubble-text");
        texto.setWrapText(true);
        texto.setMinWidth(BUBBLE_MIN_WIDTH - 30);
        texto.setPrefWidth(250);
        texto.setMaxWidth(BUBBLE_MAX_WIDTH - 30);

        this.bubble = new VBox(texto);
        this.bubble.getStyleClass().add("visual-assistant-bubble");
        this.bubble.setMinWidth(BUBBLE_MIN_WIDTH);
        this.bubble.setPrefWidth(280);
        this.bubble.setMaxWidth(BUBBLE_MAX_WIDTH);

        this.body = new HBox(10, personaje, bubble);
        this.body.setAlignment(Pos.BOTTOM_LEFT);
        this.body.getStyleClass().add("visual-assistant-body");
        this.body.setMinSize(320, 156);
        this.body.setPrefSize(410, 156);
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

    public void instalarAyudaAutomatica(Parent root) {
        if (!raicesConAyuda.add(root)) return;
        root.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (!isActivo() || !(e.getTarget() instanceof Node target) || esParteDelAsistente(target)) {
                return;
            }
            Node nodo = buscarNodoAyuda(target);
            if (nodo == null || nodo == ultimoNodoAyuda) return;

            long ahora = System.currentTimeMillis();
            if (ahora - ultimaAyudaMs < 650) return;

            String ayuda = ayudaParaControl(nodo);
            if (ayuda == null || ayuda.isBlank()) return;

            ultimoNodoAyuda = nodo;
            ultimaAyudaMs = ahora;
            decir(ayuda);
        });
    }

    public void decir(String mensaje) {
        ajustarGlobo(mensaje);
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
        colocarAbajoDerecha();
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

    private Node buscarNodoAyuda(Node node) {
        Node actual = node;
        while (actual != null && !(actual instanceof VisualAssistantView)) {
            if (actual instanceof ButtonBase
                || actual instanceof TextInputControl
                || actual instanceof ComboBoxBase<?>
                || actual instanceof TableView<?>
                || actual instanceof ListView<?>
                || actual instanceof TabPane
                || actual instanceof Slider
                || actual instanceof Spinner<?>) {
                return actual;
            }
            actual = actual.getParent();
        }
        return null;
    }

    private String ayudaParaControl(Node node) {
        if (node instanceof ButtonBase button) {
            String textoBoton = textoLimpio(button.getText());
            String tooltip = button.getTooltip() != null ? button.getTooltip().getText() : "";
            return ayudaParaBoton(textoBoton, tooltip);
        }
        if (node instanceof TextInputControl input) {
            String prompt = textoLimpio(input.getPromptText());
            return prompt.isBlank()
                ? "Campo de texto: escribe o modifica el dato solicitado en esta pantalla."
                : "Campo de texto: introduce " + prompt.toLowerCase() + ".";
        }
        if (node instanceof ComboBoxBase<?>) {
            return "Selector: despliega la lista y elige una opción disponible.";
        }
        if (node instanceof TableView<?>) {
            return "Tabla de datos: selecciona registros, revisa columnas y usa los botones de acción para trabajar con ellos.";
        }
        if (node instanceof ListView<?>) {
            return "Lista: selecciona un elemento para consultarlo o aplicar acciones relacionadas.";
        }
        if (node instanceof TabPane tabPane) {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            String titulo = tab != null ? textoLimpio(tab.getText()) : "";
            return titulo.isBlank()
                ? "Pestañas: cambia entre secciones de esta pantalla."
                : "Pestaña " + titulo + ": muestra opciones y datos de esta sección.";
        }
        if (node instanceof Slider) {
            return "Deslizador: arrastra el control para ajustar este valor.";
        }
        if (node instanceof Spinner<?>) {
            return "Selector numérico: usa las flechas o escribe un valor válido.";
        }
        return null;
    }

    private String ayudaParaBoton(String textoBoton, String tooltip) {
        String base = !textoBoton.isBlank() ? textoBoton : textoLimpio(tooltip);
        String lower = base.toLowerCase();
        if (lower.isBlank()) return "Botón de acción: ejecuta la función asociada en esta pantalla.";
        if (lower.contains("guardar") || lower.contains("actualizar")) return "Guardar: conserva los cambios realizados en esta pantalla.";
        if (lower.contains("nuevo") || lower.contains("crear") || lower.contains("añadir") || lower.contains("agregar")) return "Nuevo: crea un registro o añade información al módulo actual.";
        if (lower.contains("editar") || lower.contains("modificar")) return "Editar: permite cambiar los datos del registro seleccionado.";
        if (lower.contains("eliminar") || lower.contains("borrar")) return "Eliminar: borra el registro o los registros seleccionados. Revisa la selección antes de confirmar.";
        if (lower.contains("buscar") || lower.contains("filtrar")) return "Buscar: localiza registros usando el texto o filtros indicados.";
        if (lower.contains("limpiar")) return "Limpiar: vacía filtros, campos o el contenido actual para empezar de nuevo.";
        if (lower.contains("export")) return "Exportar: genera un archivo con la información de esta pantalla.";
        if (lower.contains("import")) return "Importar: carga datos desde un archivo externo o una copia de seguridad.";
        if (lower.contains("imprimir") || lower.contains("pdf")) return "Documento: genera o muestra una versión imprimible o PDF.";
        if (lower.contains("enviar")) return "Enviar: manda el texto o solicitud para que el sistema la procese.";
        if (lower.contains("cerrar") || lower.contains("salir") || lower.contains("cancel")) return "Cerrar o cancelar: abandona esta acción y vuelve al estado anterior.";
        if (lower.contains("config")) return "Configuración: abre opciones para ajustar el comportamiento de esta función.";
        return "Botón " + base + ": ejecuta esta acción dentro del módulo actual.";
    }

    private boolean esParteDelAsistente(Node node) {
        Node actual = node;
        while (actual != null) {
            if (actual == this) return true;
            actual = actual.getParent();
        }
        return false;
    }

    private String textoLimpio(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^\\p{L}\\p{N}\\s/·\\-áéíóúüñÁÉÍÓÚÜÑ]", "")
            .replaceAll("\\s+", " ")
            .trim();
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
        detenerAnimacionPersonaje();
        Node avatar = switch (personajeActual) {
            case "Robot" -> crearRobot();
            case "Guía" -> crearGuia();
            case "Técnico" -> crearTecnico();
            case "Guardia Civil" -> crearGuardiaCivil();
            default -> crearBuho();
        };
        personaje.getChildren().setAll(avatar);
        iniciarAnimacionesPersonaje();
    }

    private void actualizarTamano() {
        personaje.setMinSize(tamanoActual + 28, tamanoActual + 46);
        personaje.setPrefSize(tamanoActual + 28, tamanoActual + 46);
        personaje.setMaxSize(tamanoActual + 28, tamanoActual + 46);
        actualizarPersonaje();
    }

    private Node crearBuho() {
        Group g = new Group();
        Ellipse cuerpo = ellipse(50, 55, 31, 37, "#6B2D5E");
        Polygon alaIzq = polygon("#8F5F86", 22, 44, 5, 68, 28, 75);
        Polygon alaDer = polygon("#8F5F86", 78, 44, 95, 68, 72, 75);
        Polygon orejaIzq = polygon("#4B2142", 29, 23, 38, 7, 47, 29);
        Polygon orejaDer = polygon("#4B2142", 71, 23, 62, 7, 53, 29);
        Circle ojoIzq = circle(39, 46, 12, "#FFFFFF");
        Circle ojoDer = circle(61, 46, 12, "#FFFFFF");
        Circle pupilaIzq = circle(39, 48, 4, "#1C1320");
        Circle pupilaDer = circle(61, 48, 4, "#1C1320");
        Polygon pico = polygon("#F2A93B", 50, 53, 43, 63, 57, 63);
        Ellipse pecho = ellipse(50, 72, 15, 18, "#F4D9EC");
        g.getChildren().addAll(alaIzq, alaDer, orejaIzq, orejaDer, cuerpo, pecho, ojoIzq, ojoDer, pupilaIzq, pupilaDer, pico);
        return escalar(g);
    }

    private Node crearRobot() {
        rotBrazoIzq  = new Rotate(0, 5.5, 0);
        rotBrazoDer  = new Rotate(0, 5.5, 0);
        rotPiernaIzq = new Rotate(0, 6,   0);
        rotPiernaDer = new Rotate(0, 6,   0);
        robLuz = circle(50, 2, 4, "#F2C94C");

        Group brazoIzqG  = brazoGroup("#A9D5FF", "#64748B", 19.5, 63, rotBrazoIzq);
        Group brazoDerG  = brazoGroup("#A9D5FF", "#64748B", 69.5, 63, rotBrazoDer);
        Group piernaIzqG = piernaGroup("#1A2538", 31, 83, rotPiernaIzq);
        Group piernaDerG = piernaGroup("#1A2538", 57, 83, rotPiernaDer);

        Rectangle antena = rect(47, 5, 6, 13, 2, "#64748B");
        Rectangle cuello = rect(43, 57, 14,  7, 3, "#64748B");
        Rectangle cuerpo = rect(25, 63, 50, 22, 7, "#2F80ED");
        Rectangle cabeza = rect(22, 17, 56, 40, 10, "#A9D5FF");
        cabeza.setStroke(Color.web("#2F80ED"));
        cabeza.setStrokeWidth(3);
        Circle orejaIzq = circle(21, 37, 6, "#2F80ED");
        Circle orejaDer = circle(79, 37, 6, "#2F80ED");
        Circle ojoIzq   = circle(40, 35, 5, "#1A2538");
        Circle ojoDer   = circle(60, 35, 5, "#1A2538");
        Rectangle boca  = rect(38, 49, 24, 5, 3, "#1A2538");

        Group g = new Group(
            piernaIzqG, piernaDerG, brazoIzqG, brazoDerG,
            cuello, cuerpo, orejaIzq, orejaDer, cabeza,
            antena, robLuz, ojoIzq, ojoDer, boca
        );
        return escalar(g);
    }

    private Node crearGuia() {
        rotBrazoIzq  = new Rotate(0, 5.5, 0);
        rotBrazoDer  = new Rotate(0, 5.5, 0);
        rotPiernaIzq = new Rotate(0, 6,   0);
        rotPiernaDer = new Rotate(0, 6,   0);

        Group brazoIzqG  = brazoGroup("#27AE60", "#F2C7A5", 20.5, 60, rotBrazoIzq);
        Group brazoDerG  = brazoGroup("#27AE60", "#F2C7A5", 68.5, 60, rotBrazoDer);
        Group piernaIzqG = piernaGroup("#2C3E50", 31, 81, rotPiernaIzq);
        Group piernaDerG = piernaGroup("#2C3E50", 57, 81, rotPiernaDer);

        Ellipse   pelo    = ellipse(50, 20, 24, 12, "#4A2A22");
        Circle    cabeza  = circle(50, 34, 20, "#F2C7A5");
        Circle    ojoIzq  = circle(43, 32, 3.5, "#1F2933");
        Circle    ojoDer  = circle(57, 32, 3.5, "#1F2933");
        Rectangle sonrisa = rect(43, 44, 14, 3, 2, "#B85C5C");
        Rectangle cuello  = rect(45, 53, 10, 8, 3, "#F2C7A5");
        Rectangle torso   = rect(26, 60, 48, 23, 10, "#27AE60");
        Polygon   camisa  = polygon("#FFFFFF", 36, 61, 64, 61, 50, 76);
        Polygon   corbata = polygon("#6B2D5E", 50, 63, 44, 80, 56, 80);

        Group g = new Group(
            brazoIzqG, brazoDerG, cuello, torso, camisa, corbata,
            piernaIzqG, piernaDerG,
            cabeza, pelo, ojoIzq, ojoDer, sonrisa
        );
        return escalar(g);
    }

    private Node crearTecnico() {
        rotBrazoIzq  = new Rotate(0, 5.5, 0);
        rotBrazoDer  = new Rotate(0, 5.5, 0);
        rotPiernaIzq = new Rotate(0, 6,   0);
        rotPiernaDer = new Rotate(0, 6,   0);
        rotLlave     = new Rotate(0, 80, 84);

        Group brazoIzqG  = brazoGroup("#34495E", "#E8B58F", 19.5, 68, rotBrazoIzq);
        Group brazoDerG  = brazoGroup("#34495E", "#E8B58F", 69.5, 68, rotBrazoDer);
        Group piernaIzqG = piernaGroup("#2C3E50", 31, 89, rotPiernaIzq);
        Group piernaDerG = piernaGroup("#2C3E50", 57, 89, rotPiernaDer);

        Rectangle casco  = rect(28, 18, 44, 20, 9, "#F39C12");
        Rectangle visera = rect(24, 34, 52,  7, 3, "#D98200");
        Circle    cabeza = circle(50, 44, 20, "#E8B58F");
        Circle    ojoIzq = circle(43, 42, 3.5, "#17202A");
        Circle    ojoDer = circle(57, 42, 3.5, "#17202A");
        Rectangle boca   = rect(43, 53, 14, 3, 2, "#8E4B3A");
        Rectangle cuello = rect(45, 62, 10, 7, 3, "#E8B58F");
        Rectangle torso  = rect(25, 68, 50, 23, 8, "#34495E");
        Rectangle peto   = rect(36, 68, 28, 23, 5, "#F39C12");
        Polygon   llave  = polygon("#BDC3C7", 71, 82, 85, 69, 90, 74, 78, 87, 87, 93, 82, 98);
        llave.getTransforms().add(rotLlave);

        Group g = new Group(
            brazoIzqG, brazoDerG, cuello, torso, peto,
            piernaIzqG, piernaDerG,
            cabeza, casco, visera, ojoIzq, ojoDer, boca, llave
        );
        return escalar(g);
    }

    private Node crearGuardiaCivil() {
        rotBrazoIzq  = new Rotate(0, 5.5, 0);
        rotBrazoDer  = new Rotate(0, 5.5, 0);
        rotPiernaIzq = new Rotate(0, 6,   0);
        rotPiernaDer = new Rotate(0, 6,   0);

        Group brazoIzqG  = brazoGroup("#2F6B35", "#E8C8A0", 19.5, 61, rotBrazoIzq);
        Group brazoDerG  = brazoGroup("#2F6B35", "#E8C8A0", 69.5, 61, rotBrazoDer);
        Group piernaIzqG = piernaBotaGroup(31, 83, rotPiernaIzq);
        Group piernaDerG = piernaBotaGroup(57, 83, rotPiernaDer);

        // Tricornio
        Rectangle corona    = rect(36,  8, 28, 16,  4, "#0D1A0D");
        Polygon   alaIzq    = polygon("#0D1A0D", 36, 24, 22, 22, 24, 29, 38, 30);
        Polygon   alaDer    = polygon("#0D1A0D", 64, 24, 78, 22, 76, 29, 62, 30);
        Rectangle visorFront = rect(40, 24, 20, 5, 2, "#0D1A0D");

        // Cara
        Circle    cabeza    = circle(50, 39, 17, "#E8C8A0");
        Circle    ojoIzq    = circle(44, 37,  3, "#1C1C1C");
        Circle    ojoDer    = circle(56, 37,  3, "#1C1C1C");
        Polygon   bigoteIzq = polygon("#3D1A00", 43, 47, 50, 46, 50, 50, 43, 50);
        Polygon   bigoteDer = polygon("#3D1A00", 50, 46, 57, 47, 57, 50, 50, 50);

        // Cuerpo
        Rectangle cuello    = rect(44, 55, 12,  7, 3, "#E8C8A0");
        Rectangle torso     = rect(25, 61, 50, 22, 8, "#2F6B35");
        Rectangle cinturon  = rect(25, 77, 50,  5, 2, "#3D2800");
        Rectangle hebilla   = rect(46, 77,  8,  5, 1, "#B8860B");
        Rectangle funda     = rect(72, 75, 10, 17, 3, "#1A1A1A");
        Rectangle empunadura = rect(70, 73,  9,  5, 2, "#2A2A2A");
        Rectangle cierre    = rect(71, 79,  9,  3, 1, "#3D2800");
        Group pistolaFunda = new Group(funda, empunadura, cierre);
        pistolaFunda.setRotate(-8);

        Group g = new Group(
            piernaIzqG, piernaDerG,
            brazoIzqG, brazoDerG,
            cuello, torso, cinturon, hebilla, pistolaFunda,
            cabeza, ojoIzq, ojoDer, bigoteIzq, bigoteDer,
            alaIzq, alaDer, visorFront, corona
        );
        return escalar(g);
    }

    private Node escalar(Group group) {
        double escala = tamanoActual / 78.0;
        group.setScaleX(escala);
        group.setScaleY(escala);
        return group;
    }

    private Group brazoGroup(String colorBrazo, String colorMano, double tx, double ty, Rotate rot) {
        Rectangle brazo = new Rectangle(0, 0, 11, 20);
        brazo.setArcWidth(4); brazo.setArcHeight(4);
        brazo.setFill(Color.web(colorBrazo));
        Rectangle mano = new Rectangle(-3, 20, 17, 5);
        mano.setArcWidth(3); mano.setArcHeight(3);
        mano.setFill(Color.web(colorMano));
        Group g = new Group(brazo, mano);
        g.setTranslateX(tx);
        g.setTranslateY(ty);
        g.getTransforms().add(rot);
        return g;
    }

    private Group piernaBotaGroup(double tx, double ty, Rotate rot) {
        Rectangle pierna = new Rectangle(0, 0, 12, 14);
        pierna.setArcWidth(4); pierna.setArcHeight(4);
        pierna.setFill(Color.web("#347A3C"));
        Rectangle bota = new Rectangle(-2, 14, 16, 8);
        bota.setArcWidth(3); bota.setArcHeight(3);
        bota.setFill(Color.web("#111111"));
        Group g = new Group(pierna, bota);
        g.setTranslateX(tx);
        g.setTranslateY(ty);
        g.getTransforms().add(rot);
        return g;
    }

    private Group piernaGroup(String color, double tx, double ty, Rotate rot) {
        Rectangle pierna = new Rectangle(0, 0, 12, 17);
        pierna.setArcWidth(4); pierna.setArcHeight(4);
        pierna.setFill(Color.web(color));
        Rectangle pie = new Rectangle(-3, 17, 18, 5);
        pie.setArcWidth(3); pie.setArcHeight(3);
        pie.setFill(Color.web(color));
        Group g = new Group(pierna, pie);
        g.setTranslateX(tx);
        g.setTranslateY(ty);
        g.getTransforms().add(rot);
        return g;
    }

    private Circle circle(double x, double y, double r, String color) {
        Circle circle = new Circle(x, y, r);
        circle.setFill(Color.web(color));
        return circle;
    }

    private Ellipse ellipse(double x, double y, double rx, double ry, String color) {
        Ellipse ellipse = new Ellipse(x, y, rx, ry);
        ellipse.setFill(Color.web(color));
        return ellipse;
    }

    private Rectangle rect(double x, double y, double w, double h, double arc, String color) {
        Rectangle rect = new Rectangle(x, y, w, h);
        rect.setArcWidth(arc);
        rect.setArcHeight(arc);
        rect.setFill(Color.web(color));
        return rect;
    }

    private Polygon polygon(String color, double... points) {
        Polygon polygon = new Polygon(points);
        polygon.setFill(Color.web(color));
        return polygon;
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

    private void ajustarGlobo(String mensaje) {
        double ancho = calcularAnchoGlobo(mensaje);
        double anchoTexto = ancho - 30;
        texto.setMinWidth(anchoTexto);
        texto.setPrefWidth(anchoTexto);
        texto.setMaxWidth(anchoTexto);
        bubble.setMinWidth(ancho);
        bubble.setPrefWidth(ancho);
        bubble.setMaxWidth(ancho);

        double anchoTotal = Math.max(320, tamanoActual + 58 + ancho);
        setPrefWidth(anchoTotal);
        setMaxWidth(anchoTotal);
        body.setPrefWidth(anchoTotal - 20);
        Platform.runLater(() -> relocateClamped(getLayoutX(), getLayoutY()));
    }

    private double calcularAnchoGlobo(String mensaje) {
        int longitud = mensaje == null ? 0 : mensaje.length();
        if (longitud <= 28) return BUBBLE_MIN_WIDTH;
        if (longitud <= 70) return 230;
        if (longitud <= 120) return 300;
        return BUBBLE_MAX_WIDTH;
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

    private void detenerAnimacionPersonaje() {
        if (animExtremidades != null) { animExtremidades.stop(); animExtremidades = null; }
        if (animPersonaje    != null) { animPersonaje.stop();    animPersonaje    = null; }
        rotBrazoIzq = rotBrazoDer = rotPiernaIzq = rotPiernaDer = rotLlave = null;
        robLuz = null;
    }

    private void iniciarAnimacionesPersonaje() {
        if (rotBrazoIzq != null) {
            animExtremidades = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(rotBrazoIzq.angleProperty(),  -12),
                    new KeyValue(rotBrazoDer.angleProperty(),   12),
                    new KeyValue(rotPiernaIzq.angleProperty(),   8),
                    new KeyValue(rotPiernaDer.angleProperty(),  -8)),
                new KeyFrame(Duration.millis(750),
                    new KeyValue(rotBrazoIzq.angleProperty(),   12),
                    new KeyValue(rotBrazoDer.angleProperty(),  -12),
                    new KeyValue(rotPiernaIzq.angleProperty(),  -8),
                    new KeyValue(rotPiernaDer.angleProperty(),   8))
            );
            animExtremidades.setAutoReverse(true);
            animExtremidades.setCycleCount(Animation.INDEFINITE);
            animExtremidades.play();
        }
        if (robLuz != null) {
            animPersonaje = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(robLuz.fillProperty(), Color.web("#F2C94C"))),
                new KeyFrame(Duration.millis(500),  new KeyValue(robLuz.fillProperty(), Color.web("#FFFFFF"))),
                new KeyFrame(Duration.seconds(1),   new KeyValue(robLuz.fillProperty(), Color.web("#F2C94C"))),
                new KeyFrame(Duration.seconds(3),   new KeyValue(robLuz.fillProperty(), Color.web("#F2C94C")))
            );
            animPersonaje.setCycleCount(Animation.INDEFINITE);
            animPersonaje.play();
        }
        if (rotLlave != null) {
            animPersonaje = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(rotLlave.angleProperty(), -15)),
                new KeyFrame(Duration.seconds(1), new KeyValue(rotLlave.angleProperty(),  15)),
                new KeyFrame(Duration.seconds(2), new KeyValue(rotLlave.angleProperty(), -15))
            );
            animPersonaje.setCycleCount(Animation.INDEFINITE);
            animPersonaje.play();
        }
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
        if (getParent() == null) return;
        Bounds pb = getParent().getLayoutBounds();
        if (pb.getWidth() <= 0 || pb.getHeight() <= 0) {
            Platform.runLater(this::restaurarPosicion);
            return;
        }
        colocarAbajoDerecha();
    }

    private void colocarAbajoDerecha() {
        if (getParent() == null) {
            relocate(24, 24);
            return;
        }
        Bounds parentBounds = getParent().getLayoutBounds();
        Bounds ownBounds = getLayoutBounds();
        double x = Math.max(8, parentBounds.getWidth() - ownBounds.getWidth() - 24);
        double y = Math.max(8, parentBounds.getHeight() - ownBounds.getHeight() - 24);
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
