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
import javafx.beans.value.ChangeListener;
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
import javafx.scene.text.Text;
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
        "Vampi", "Vampi",
        "David", "David",
        "Gema", "Gema",
        "Sonia", "Sonia",
        "Ana", "Ana"
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
    private Rotate rotBrazoIzq, rotBrazoDer, rotPiernaIzq, rotPiernaDer;
    private Group vampiGrupo, vampiAlaIzq, vampiAlaDer, vampiCabeza;
    private Circle vampiOjoIzq, vampiOjoDer;
    private Group metalCabeza, metalPelo, metalTorso, metalBrazoAlto;
    private Group soniaCabeza, soniaManoIzq, soniaManoDer, soniaMicrofono;
    private Rectangle soniaPantalla;
    private Group davidCabeza, davidTorso, davidRadio, davidLinterna;
    private Circle davidLinternaLuz;
    private Timeline animExtremidades, animPersonaje, animVampiAviso;
    private Node ultimoNodoAyuda;
    private long ultimaAyudaMs;
    private boolean posicionInicializada;
    private final ChangeListener<Bounds> parentBoundsListener = (obs, oldBounds, newBounds) -> ajustarAlContenedor();
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

        String personajeGuardado = valorConfig(KEY_PERSONAJE, "Vampi");
        personajeActual = PERSONAJES.containsKey(personajeGuardado) ? personajeGuardado : "Vampi";
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
            guardarPosicionActual();
        });
        setOnMouseMoved(e -> inclinarHaciaCursor(e.getX()));
        setOnMouseExited(e -> personaje.setRotate(0));

        parentProperty().addListener((obs, oldParent, parent) -> {
            if (oldParent != null) {
                oldParent.layoutBoundsProperty().removeListener(parentBoundsListener);
            }
            if (parent != null) {
                parent.layoutBoundsProperty().addListener(parentBoundsListener);
            }
            restaurarPosicion();
        });
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
        if ("Metalera".equals(personajeActual)) {
            personajeActual = "Gema";
        } else if ("Guardia Civil".equals(personajeActual)) {
            personajeActual = "David";
        }
        this.personajeActual = PERSONAJES.containsKey(personajeActual) ? personajeActual : "Vampi";
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
        guardarPosicionActual();
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
            case "Vampi" -> crearVampi();
            case "David", "Guardia Civil" -> crearGuardiaCivil();
            case "Gema", "Metalera" -> crearMetalera();
            case "Sonia" -> crearSonia();
            case "Ana" -> crearAna();
            default -> crearVampi();
        };
        personaje.getChildren().setAll(avatar);
        iniciarAnimacionesPersonaje();
    }

    private void actualizarTamano() {
        personaje.setMinSize(tamanoActual + 28, tamanoActual + 46);
        personaje.setPrefSize(tamanoActual + 28, tamanoActual + 46);
        personaje.setMaxSize(tamanoActual + 28, tamanoActual + 46);
        actualizarPersonaje();
        Platform.runLater(this::ajustarAlContenedor);
    }

    private Node crearVampi() {
        Polygon alaIzq = polygon("#2A1633", 48, 42, 10, 24, 22, 53, 6, 72, 42, 65);
        Polygon alaDer = polygon("#2A1633", 52, 42, 90, 24, 78, 53, 94, 72, 58, 65);
        Polygon membranaIzq = polygon("#5C2A66", 44, 47, 20, 36, 28, 57, 18, 66, 43, 61);
        Polygon membranaDer = polygon("#5C2A66", 56, 47, 80, 36, 72, 57, 82, 66, 57, 61);
        Ellipse cuerpo = ellipse(50, 62, 20, 28, "#332039");
        Circle cabeza = circle(50, 38, 20, "#3D2545");
        Polygon orejaIzq = polygon("#2A1633", 36, 25, 30, 7, 45, 22);
        Polygon orejaDer = polygon("#2A1633", 64, 25, 70, 7, 55, 22);
        vampiOjoIzq = circle(43, 36, 5, "#F7E6FF");
        vampiOjoDer = circle(57, 36, 5, "#F7E6FF");
        Circle pupilaIzq = circle(43, 37, 2, "#111111");
        Circle pupilaDer = circle(57, 37, 2, "#111111");
        Ellipse hocico = ellipse(50, 47, 8, 5, "#6F4A78");
        Polygon colmilloIzq = polygon("#FFFFFF", 44, 51, 48, 51, 46, 58);
        Polygon colmilloDer = polygon("#FFFFFF", 52, 51, 56, 51, 54, 58);
        Circle sangreIzq = circle(46, 57, 2.2, "#B11226");
        Circle sangreDer = circle(54, 57, 2.0, "#B11226");
        Ellipse gotaSangre = ellipse(54.5, 60, 1.8, 3.2, "#8B0E1A");

        vampiAlaIzq = new Group(alaIzq, membranaIzq);
        vampiAlaDer = new Group(alaDer, membranaDer);
        vampiCabeza = new Group(
            orejaIzq, orejaDer, cabeza, vampiOjoIzq, vampiOjoDer, pupilaIzq, pupilaDer,
            hocico, colmilloIzq, colmilloDer, sangreIzq, sangreDer, gotaSangre
        );
        vampiGrupo = new Group(
            vampiAlaIzq, vampiAlaDer, cuerpo, vampiCabeza
        );
        return escalar(vampiGrupo);
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

        Rectangle corona    = rect(36,  8, 28, 16,  4, "#0D1A0D");
        Polygon   alaIzq    = polygon("#0D1A0D", 36, 24, 22, 22, 24, 29, 38, 30);
        Polygon   alaDer    = polygon("#0D1A0D", 64, 24, 78, 22, 76, 29, 62, 30);
        Rectangle visorFront = rect(40, 24, 20, 5, 2, "#0D1A0D");

        Circle    cabeza    = circle(50, 39, 17, "#E8C8A0");
        Circle    ojoIzq    = circle(44, 37,  3, "#1C1C1C");
        Circle    ojoDer    = circle(56, 37,  3, "#1C1C1C");
        Polygon   bigoteIzq = polygon("#3D1A00", 43, 47, 50, 46, 50, 50, 43, 50);
        Polygon   bigoteDer = polygon("#3D1A00", 50, 46, 57, 47, 57, 50, 50, 50);
        davidCabeza = new Group(cabeza, ojoIzq, ojoDer, bigoteIzq, bigoteDer, alaIzq, alaDer, visorFront, corona);

        Rectangle cuello    = rect(44, 55, 12,  7, 3, "#E8C8A0");
        Rectangle torso     = rect(25, 61, 50, 22, 8, "#2F6B35");
        Rectangle cinturon  = rect(25, 77, 50,  5, 2, "#3D2800");
        Rectangle hebilla   = rect(46, 77,  8,  5, 1, "#B8860B");
        Rectangle funda     = rect(72, 75, 10, 17, 3, "#1A1A1A");
        Rectangle empunadura = rect(70, 73,  9,  5, 2, "#2A2A2A");
        Rectangle cierre    = rect(71, 79,  9,  3, 1, "#3D2800");
        Group pistolaFunda = new Group(funda, empunadura, cierre);
        pistolaFunda.setRotate(-8);
        davidTorso = new Group(cuello, torso, cinturon, hebilla, pistolaFunda);
        davidRadio = new Group(rect(18, 70, 8, 13, 2, "#111111"), rect(20, 66, 4, 5, 1, "#111111"));
        davidLinternaLuz = circle(84, 72, 5, "#F2C94C");
        davidLinternaLuz.setOpacity(0.0);
        davidLinterna = new Group(rect(77, 70, 11, 5, 2, "#222222"), davidLinternaLuz);

        Group g = new Group(
            piernaIzqG, piernaDerG,
            brazoIzqG, brazoDerG, davidTorso, davidRadio, davidLinterna, davidCabeza
        );
        return escalar(g);
    }

    private Node crearMetalera() {
        rotBrazoIzq  = new Rotate(0, 5.5, 31);
        rotBrazoDer  = new Rotate(18, 6, 0);
        rotPiernaIzq = new Rotate(5, 6, 0);
        rotPiernaDer = new Rotate(-5, 6, 0);

        Group piernaIzqG = piernaBotaGroup(31, 83, rotPiernaIzq);
        Group piernaDerG = piernaBotaGroup(57, 83, rotPiernaDer);
        Group brazoBajo = brazoGroup("#111111", "#E8B58F", 70, 63, rotBrazoDer);
        metalBrazoAlto = brazoCuernosGroup();
        metalBrazoAlto.getTransforms().add(rotBrazoIzq);

        Rectangle cuello = rect(45, 53, 10, 8, 3, "#E8B58F");
        Rectangle torso = rect(25, 61, 50, 24, 8, "#101010");
        torso.setStroke(Color.web("#5B5B5B"));
        torso.setStrokeWidth(1.5);
        Polygon rayo = polygon("#F2C94C", 50, 64, 42, 75, 49, 74, 43, 84, 58, 70, 51, 71);
        Text camiseta = new Text(33, 79, "AC/DC");
        camiseta.setFill(Color.web("#FFFFFF"));
        camiseta.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;");
        Rectangle cinturon = rect(27, 84, 46, 5, 1, "#2A2A2A");
        Rectangle hebilla = rect(47, 84, 8, 5, 1, "#C0C0C0");
        metalTorso = new Group(cuello, torso, rayo, camiseta, cinturon, hebilla);

        metalPelo = new Group(
            ellipse(43, 37, 17, 30, "#181014"),
            ellipse(57, 37, 17, 30, "#181014"),
            polygon("#181014", 28, 42, 36, 82, 47, 56),
            polygon("#181014", 72, 42, 64, 82, 53, 56)
        );
        metalCabeza = new Group(
            circle(50, 34, 19, "#E8B58F"),
            ellipse(50, 19, 24, 10, "#181014"),
            circle(43, 33, 3.5, "#111111"),
            circle(57, 33, 3.5, "#111111"),
            rect(43, 45, 14, 3, 2, "#7A1E1E")
        );

        Group g = new Group(
            piernaIzqG, piernaDerG,
            metalTorso, brazoBajo, metalBrazoAlto,
            metalPelo, metalCabeza
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

    private Group brazoCuernosGroup() {
        Rectangle brazo = new Rectangle(0, 0, 11, 31);
        brazo.setArcWidth(4);
        brazo.setArcHeight(4);
        brazo.setFill(Color.web("#111111"));

        Rectangle muneca = new Rectangle(-1, -6, 13, 8);
        muneca.setArcWidth(3);
        muneca.setArcHeight(3);
        muneca.setFill(Color.web("#E8B58F"));
        Circle palma = circle(5.5, -10, 7, "#E8B58F");
        Rectangle indice = rect(0, -29, 4, 17, 2, "#E8B58F");
        Rectangle menique = rect(9, -27, 4, 15, 2, "#E8B58F");
        Rectangle dedosCerrados = rect(3, -16, 8, 7, 3, "#D49B76");
        Rectangle pulgar = rect(-5, -13, 7, 4, 2, "#E8B58F");
        pulgar.setRotate(-28);

        Group g = new Group(brazo, muneca, palma, indice, menique, dedosCerrados, pulgar);
        g.setTranslateX(25);
        g.setTranslateY(29);
        return g;
    }

    private Node crearSonia() {
        Rectangle mesa = rect(14, 76, 72, 18, 4, "#5B3A2E");
        Rectangle pataIzq = rect(21, 92, 8, 12, 2, "#3D251F");
        Rectangle pataDer = rect(71, 92, 8, 12, 2, "#3D251F");
        Rectangle silla = rect(32, 68, 36, 24, 8, "#2C3E50");

        Rectangle torso = rect(35, 56, 30, 23, 9, "#6B2D5E");
        Rectangle cuello = rect(46, 49, 8, 8, 3, "#E8B58F");
        soniaCabeza = new Group(
            ellipse(50, 34, 22, 25, "#5C3324"),
            circle(50, 35, 18, "#E8B58F"),
            ellipse(42, 24, 14, 10, "#5C3324"),
            ellipse(58, 24, 14, 10, "#5C3324"),
            circle(44, 35, 3, "#111111"),
            circle(56, 35, 3, "#111111"),
            rect(44, 46, 12, 3, 2, "#8E4B3A")
        );

        Group cascoIzq = new Group(circle(31, 36, 6, "#1F2937"), rect(27, 31, 5, 11, 2, "#111827"));
        Group cascoDer = new Group(circle(69, 36, 6, "#1F2937"), rect(68, 31, 5, 11, 2, "#111827"));
        Polygon diadema = polygon("#111827", 34, 25, 42, 17, 50, 15, 58, 17, 66, 25, 64, 28, 57, 21, 50, 19, 43, 21, 36, 28);
        soniaMicrofono = new Group(
            rect(66, 42, 5, 3, 2, "#111827"),
            rect(69, 43, 14, 3, 2, "#111827"),
            circle(84, 44, 3, "#2F80ED")
        );
        Group auriculares = new Group(diadema, cascoIzq, cascoDer, soniaMicrofono);

        Rectangle monitorMarco = rect(24, 55, 52, 34, 5, "#111827");
        soniaPantalla = rect(28, 59, 44, 24, 3, "#2F80ED");
        Rectangle soporte = rect(47, 89, 7, 8, 2, "#111827");
        Rectangle base = rect(38, 96, 26, 5, 2, "#111827");
        Text prompt = new Text(34, 75, ">_");
        prompt.setFill(Color.web("#DFF6FF"));
        prompt.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        Group monitor = new Group(monitorMarco, soniaPantalla, prompt, soporte, base);

        Rectangle teclado = rect(31, 84, 38, 7, 2, "#1F2937");
        soniaManoIzq = new Group(rect(29, 80, 13, 5, 3, "#E8B58F"));
        soniaManoDer = new Group(rect(58, 80, 13, 5, 3, "#E8B58F"));

        Group g = new Group(
            pataIzq, pataDer, silla, torso, cuello, soniaCabeza, auriculares,
            mesa, monitor, teclado, soniaManoIzq, soniaManoDer
        );
        return escalar(g);
    }

    private Node crearAna() {
        Rectangle paredSilla = rect(18, 54, 18, 37, 7, "#1F4E79");
        Rectangle asientoSilla = rect(26, 78, 26, 10, 4, "#17446B");
        Rectangle pataSilla = rect(30, 87, 6, 15, 2, "#17324A");

        Rectangle tableroMesa = rect(50, 72, 43, 8, 3, "#5B3A2E");
        Rectangle pataMesa = rect(83, 80, 7, 23, 2, "#3D251F");
        Rectangle cantoMesa = rect(50, 78, 43, 4, 2, "#7A4D3A");

        Rectangle monitorMarco = rect(70, 36, 22, 29, 4, "#111827");
        soniaPantalla = rect(73, 39, 16, 22, 2, "#1D6FB8");
        Rectangle soporte = rect(78, 65, 6, 7, 2, "#111827");
        Text monitorText = new Text(74, 55, "061");
        monitorText.setFill(Color.web("#EAF7FF"));
        monitorText.setStyle("-fx-font-size: 7px; -fx-font-weight: bold;");
        Group monitor = new Group(monitorMarco, soniaPantalla, monitorText, soporte);

        Rectangle torsoBase = rect(31, 55, 27, 25, 8, "#1368A8");
        Rectangle franjaNaranja = rect(47, 55, 9, 25, 3, "#F58220");
        Rectangle mangaNaranja = rect(55, 59, 12, 8, 3, "#F58220");
        Rectangle reflectante = rect(34, 60, 21, 3, 1, "#EAF7FF");
        Text distintivo = new Text(35, 74, "061");
        distintivo.setFill(Color.web("#FFFFFF"));
        distintivo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        Rectangle cuello = rect(48, 48, 8, 8, 3, "#E8B58F");
        Group uniforme = new Group(torsoBase, franjaNaranja, mangaNaranja, reflectante, distintivo);

        soniaCabeza = new Group(
            ellipse(45, 36, 15, 24, "#4E2C20"),
            circle(51, 35, 16, "#E8B58F"),
            ellipse(44, 25, 16, 10, "#4E2C20"),
            polygon("#E8B58F", 63, 35, 71, 38, 63, 41),
            circle(59, 34, 3, "#111111"),
            rect(58, 45, 9, 3, 2, "#8E4B3A")
        );

        Group casco = new Group(circle(44, 35, 6, "#1F2937"), rect(40, 30, 5, 11, 2, "#111827"));
        Polygon diadema = polygon("#111827", 42, 27, 47, 18, 55, 17, 63, 25, 61, 28, 55, 21, 48, 22, 44, 29);
        soniaMicrofono = new Group(
            rect(60, 42, 5, 3, 2, "#111827"),
            rect(64, 43, 12, 3, 2, "#111827"),
            circle(78, 44, 3, "#F58220")
        );
        Group auriculares = new Group(diadema, casco, soniaMicrofono);

        Rectangle brazoIzq = rect(53, 66, 19, 5, 3, "#1368A8");
        Rectangle brazoDer = rect(50, 72, 21, 5, 3, "#F58220");
        Rectangle teclado = rect(63, 78, 22, 5, 2, "#1F2937");
        soniaManoIzq = new Group(rect(68, 72, 10, 4, 2, "#E8B58F"));
        soniaManoDer = new Group(rect(72, 78, 10, 4, 2, "#E8B58F"));

        Group piernas = new Group(
            rect(38, 79, 18, 7, 3, "#1368A8"),
            rect(50, 86, 14, 6, 3, "#1368A8"),
            rect(62, 88, 11, 5, 2, "#111111")
        );

        Group g = new Group(
            pataSilla, paredSilla, asientoSilla, piernas,
            uniforme, cuello, soniaCabeza, auriculares,
            tableroMesa, cantoMesa, pataMesa, monitor,
            brazoIzq, brazoDer, teclado, soniaManoIzq, soniaManoDer
        );
        return escalar(g);
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
        if (vampiAlaIzq != null && vampiAlaDer != null) {
            Timeline saludoVampi = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(vampiAlaIzq.rotateProperty(), -10),
                    new KeyValue(vampiAlaDer.rotateProperty(), 10)),
                new KeyFrame(Duration.millis(120),
                    new KeyValue(vampiAlaIzq.rotateProperty(), 28),
                    new KeyValue(vampiAlaDer.rotateProperty(), -28)),
                new KeyFrame(Duration.millis(240),
                    new KeyValue(vampiAlaIzq.rotateProperty(), -12),
                    new KeyValue(vampiAlaDer.rotateProperty(), 12)),
                new KeyFrame(Duration.millis(360),
                    new KeyValue(vampiAlaIzq.rotateProperty(), 24),
                    new KeyValue(vampiAlaDer.rotateProperty(), -24))
            );
            saludoVampi.play();
        }
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
        if (animVampiAviso   != null) { animVampiAviso.stop();   animVampiAviso   = null; }
        rotBrazoIzq = rotBrazoDer = rotPiernaIzq = rotPiernaDer = null;
        vampiGrupo = vampiAlaIzq = vampiAlaDer = vampiCabeza = null;
        vampiOjoIzq = vampiOjoDer = null;
        metalCabeza = metalPelo = metalTorso = metalBrazoAlto = null;
        soniaCabeza = soniaManoIzq = soniaManoDer = soniaMicrofono = null;
        soniaPantalla = null;
        davidCabeza = davidTorso = davidRadio = davidLinterna = null;
        davidLinternaLuz = null;
    }

    private void iniciarAnimacionesPersonaje() {
        if (vampiGrupo != null) {
            iniciarAnimacionVampi();
            return;
        }
        if (davidCabeza != null) {
            iniciarAnimacionDavid();
            return;
        }
        if (soniaCabeza != null) {
            iniciarAnimacionSonia();
            return;
        }
        if (metalCabeza != null) {
            iniciarAnimacionMetalera();
            return;
        }
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
    }

    private void iniciarAnimacionVampi() {
        animExtremidades = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(vampiAlaIzq.rotateProperty(), -8),
                new KeyValue(vampiAlaDer.rotateProperty(), 8)),
            new KeyFrame(Duration.millis(360),
                new KeyValue(vampiAlaIzq.rotateProperty(), 20),
                new KeyValue(vampiAlaDer.rotateProperty(), -20)),
            new KeyFrame(Duration.millis(720),
                new KeyValue(vampiAlaIzq.rotateProperty(), -8),
                new KeyValue(vampiAlaDer.rotateProperty(), 8))
        );
        animExtremidades.setCycleCount(Animation.INDEFINITE);
        animExtremidades.play();

        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(vampiCabeza.translateYProperty(), 0),
                new KeyValue(vampiCabeza.rotateProperty(), 0),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1),
                new KeyValue(vampiGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(360),
                new KeyValue(vampiCabeza.translateYProperty(), -1.5),
                new KeyValue(vampiCabeza.rotateProperty(), -2),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1)),
            new KeyFrame(Duration.millis(720),
                new KeyValue(vampiCabeza.translateYProperty(), 0),
                new KeyValue(vampiCabeza.rotateProperty(), 2),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1)),
            new KeyFrame(Duration.seconds(4.8),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1)),
            new KeyFrame(Duration.seconds(4.9),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 0.12),
                new KeyValue(vampiOjoDer.scaleYProperty(), 0.12)),
            new KeyFrame(Duration.seconds(5.05),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1)),
            new KeyFrame(Duration.seconds(8.6),
                new KeyValue(vampiGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.seconds(8.95),
                new KeyValue(vampiGrupo.rotateProperty(), 180)),
            new KeyFrame(Duration.seconds(9.3),
                new KeyValue(vampiGrupo.rotateProperty(), 360)),
            new KeyFrame(Duration.seconds(9.45),
                new KeyValue(vampiGrupo.rotateProperty(), 0))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionMetalera() {
        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0),
                new KeyValue(metalTorso.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 18),
                new KeyValue(rotPiernaIzq.angleProperty(), 5),
                new KeyValue(rotPiernaDer.angleProperty(), -5)),
            new KeyFrame(Duration.millis(170),
                new KeyValue(metalCabeza.rotateProperty(), -14),
                new KeyValue(metalCabeza.translateYProperty(), 4),
                new KeyValue(metalPelo.rotateProperty(), 9),
                new KeyValue(metalPelo.translateYProperty(), 6),
                new KeyValue(metalTorso.rotateProperty(), -4),
                new KeyValue(rotBrazoIzq.angleProperty(), -4),
                new KeyValue(rotBrazoDer.angleProperty(), 8),
                new KeyValue(rotPiernaIzq.angleProperty(), -4),
                new KeyValue(rotPiernaDer.angleProperty(), 4)),
            new KeyFrame(Duration.millis(340),
                new KeyValue(metalCabeza.rotateProperty(), 12),
                new KeyValue(metalCabeza.translateYProperty(), -2),
                new KeyValue(metalPelo.rotateProperty(), -11),
                new KeyValue(metalPelo.translateYProperty(), -3),
                new KeyValue(metalTorso.rotateProperty(), 4),
                new KeyValue(rotBrazoIzq.angleProperty(), 4),
                new KeyValue(rotBrazoDer.angleProperty(), 28),
                new KeyValue(rotPiernaIzq.angleProperty(), 8),
                new KeyValue(rotPiernaDer.angleProperty(), -8)),
            new KeyFrame(Duration.millis(520),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0),
                new KeyValue(metalTorso.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 18),
                new KeyValue(rotPiernaIzq.angleProperty(), 5),
                new KeyValue(rotPiernaDer.angleProperty(), -5))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionDavid() {
        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(rotPiernaIzq.angleProperty(), 0),
                new KeyValue(rotPiernaDer.angleProperty(), 0),
                new KeyValue(davidRadio.translateYProperty(), 0),
                new KeyValue(davidLinterna.rotateProperty(), 0),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(550),
                new KeyValue(davidCabeza.rotateProperty(), -8),
                new KeyValue(davidTorso.translateYProperty(), -1),
                new KeyValue(rotBrazoIzq.angleProperty(), -92),
                new KeyValue(rotBrazoDer.angleProperty(), 12),
                new KeyValue(rotPiernaIzq.angleProperty(), 2),
                new KeyValue(rotPiernaDer.angleProperty(), -2),
                new KeyValue(davidRadio.translateYProperty(), -2),
                new KeyValue(davidLinterna.rotateProperty(), -8),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(1100),
                new KeyValue(davidCabeza.rotateProperty(), 8),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), -12),
                new KeyValue(rotBrazoDer.angleProperty(), -58),
                new KeyValue(rotPiernaIzq.angleProperty(), -2),
                new KeyValue(rotPiernaDer.angleProperty(), 2),
                new KeyValue(davidRadio.translateYProperty(), 0),
                new KeyValue(davidLinterna.rotateProperty(), 8),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.65)),
            new KeyFrame(Duration.millis(1650),
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), -1),
                new KeyValue(rotBrazoIzq.angleProperty(), 22),
                new KeyValue(rotBrazoDer.angleProperty(), 4),
                new KeyValue(rotPiernaIzq.angleProperty(), 0),
                new KeyValue(rotPiernaDer.angleProperty(), 0),
                new KeyValue(davidRadio.translateYProperty(), -1),
                new KeyValue(davidLinterna.rotateProperty(), 0),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(2200),
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(rotPiernaIzq.angleProperty(), 0),
                new KeyValue(rotPiernaDer.angleProperty(), 0),
                new KeyValue(davidRadio.translateYProperty(), 0),
                new KeyValue(davidLinterna.rotateProperty(), 0),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionSonia() {
        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(soniaManoIzq.translateYProperty(), 0),
                new KeyValue(soniaManoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED"))),
            new KeyFrame(Duration.millis(280),
                new KeyValue(soniaCabeza.rotateProperty(), -4),
                new KeyValue(soniaCabeza.translateYProperty(), 1),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.5),
                new KeyValue(soniaManoIzq.translateYProperty(), 3),
                new KeyValue(soniaManoDer.translateYProperty(), -1),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#27AE60"))),
            new KeyFrame(Duration.millis(560),
                new KeyValue(soniaCabeza.rotateProperty(), 3),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(soniaManoIzq.translateYProperty(), -1),
                new KeyValue(soniaManoDer.translateYProperty(), 3),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#6B2D5E"))),
            new KeyFrame(Duration.millis(840),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(soniaManoIzq.translateYProperty(), 0),
                new KeyValue(soniaManoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED")))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void actualizarEstadoVisual(String mensaje) {
        body.getStyleClass().removeAll("visual-assistant-alert", "visual-assistant-thinking", "visual-assistant-happy");
        String lower = mensaje.toLowerCase();
        if (lower.contains("error") || lower.contains("aviso") || lower.contains("importante")) {
            body.getStyleClass().add("visual-assistant-alert");
            llamarAtencion();
            animarVampiAviso();
        } else if (lower.contains("consulta") || lower.contains("asistente ia")) {
            body.getStyleClass().add("visual-assistant-thinking");
        } else if (lower.contains("activado") || lower.contains("listo")) {
            body.getStyleClass().add("visual-assistant-happy");
        }
    }

    private void animarVampiAviso() {
        if (vampiOjoIzq == null || vampiOjoDer == null || vampiAlaIzq == null || vampiAlaDer == null) return;
        if (animVampiAviso != null) animVampiAviso.stop();
        animVampiAviso = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(vampiOjoIzq.fillProperty(), Color.web("#F7E6FF")),
                new KeyValue(vampiOjoDer.fillProperty(), Color.web("#F7E6FF")),
                new KeyValue(vampiAlaIzq.rotateProperty(), -8),
                new KeyValue(vampiAlaDer.rotateProperty(), 8)),
            new KeyFrame(Duration.millis(140),
                new KeyValue(vampiOjoIzq.fillProperty(), Color.web("#FF2D55")),
                new KeyValue(vampiOjoDer.fillProperty(), Color.web("#FF2D55")),
                new KeyValue(vampiAlaIzq.rotateProperty(), 32),
                new KeyValue(vampiAlaDer.rotateProperty(), -32)),
            new KeyFrame(Duration.millis(620),
                new KeyValue(vampiOjoIzq.fillProperty(), Color.web("#FF2D55")),
                new KeyValue(vampiOjoDer.fillProperty(), Color.web("#FF2D55"))),
            new KeyFrame(Duration.millis(900),
                new KeyValue(vampiOjoIzq.fillProperty(), Color.web("#F7E6FF")),
                new KeyValue(vampiOjoDer.fillProperty(), Color.web("#F7E6FF")),
                new KeyValue(vampiAlaIzq.rotateProperty(), -8),
                new KeyValue(vampiAlaDer.rotateProperty(), 8))
        );
        animVampiAviso.play();
    }

    private void restaurarPosicion() {
        if (getParent() == null) return;
        Bounds pb = getParent().getLayoutBounds();
        if (pb.getWidth() <= 0 || pb.getHeight() <= 0) {
            Platform.runLater(this::restaurarPosicion);
            return;
        }
        double x = parseDouble(DatabaseManager.getConfig(KEY_POS_X), Double.NaN);
        double y = parseDouble(DatabaseManager.getConfig(KEY_POS_Y), Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y)) {
            colocarAbajoDerecha();
        } else {
            relocateClamped(x, y);
        }
        posicionInicializada = true;
    }

    private void ajustarAlContenedor() {
        if (!posicionInicializada) {
            restaurarPosicion();
            return;
        }
        relocateClamped(getLayoutX(), getLayoutY());
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

    private void guardarPosicionActual() {
        DatabaseManager.setConfig(KEY_POS_X, String.valueOf(getLayoutX()));
        DatabaseManager.setConfig(KEY_POS_Y, String.valueOf(getLayoutY()));
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
