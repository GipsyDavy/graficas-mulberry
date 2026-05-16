package org.gipsybuho.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
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
    public static final String KEY_INSTALLER_ANIMATIONS = "asistente_visual_instalador_animado";
    private static final String KEY_POS_X = "asistente_visual_x";
    private static final String KEY_POS_Y = "asistente_visual_y";
    private static final double BUBBLE_MIN_WIDTH = 110;
    private static final double BUBBLE_MAX_WIDTH = 360;

    public static final Map<String, String> PERSONAJES = Map.of(
        "Vampi", "Vampi",
        "David", "David",
        "Gema", "Gema",
        "Sonia", "Sonia",
        "Ana", "Ana",
        "Selene", "Selene"
    );

    private final StackPane personaje = new StackPane();
    private final Label texto = new Label();
    private final VBox bubble;
    private final HBox body;
    private final BooleanProperty activo = new SimpleBooleanProperty();
    private final boolean modoEmbebido;
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
    private Group vampiGrupo, vampiMurcielago, vampiDracula, vampiHumo, vampiAlaIzq, vampiAlaDer, vampiCabeza;
    private Group vampiDraculaCabeza, vampiDraculaCapaIzq, vampiDraculaCapaDer;
    private Circle vampiOjoIzq, vampiOjoDer;
    private Group metalCabeza, metalPelo, metalPeloDelante, metalTorso, metalBrazoAlto;
    private Group soniaGrupo, soniaCabeza, soniaBrazoIzq, soniaBrazoDer, soniaManoIzq, soniaManoDer, soniaMicrofono;
    private Group anaBrazoIzq, anaBrazoDer;
    private Rectangle soniaPantalla;
    private Group davidCabeza, davidTorso, davidRadio, davidLinterna;
    private Circle davidLinternaLuz;
    private Group seleneGrupo, seleneCabeza, seleneEdredon, seleneGloboZzz, seleneHamburguesa;
    private Text seleneZPeq, seleneZMed, seleneZGrande;
    private Ellipse seleneNarizBurbuja;
    private Timeline animExtremidades, animPersonaje, animVampiAviso, animSeleneGlobo, animSeleneNariz;
    private boolean usarWindmill = false;
    private Node ultimoNodoAyuda;
    private long ultimaAyudaMs;
    private boolean posicionInicializada;
    private final ChangeListener<Bounds> parentBoundsListener = (obs, oldBounds, newBounds) -> ajustarAlContenedor();
    private final Set<Parent> raicesConAyuda = Collections.newSetFromMap(new WeakHashMap<>());

    public VisualAssistantView() {
        this(false);
    }

    public VisualAssistantView(boolean modoEmbebido) {
        this.modoEmbebido = modoEmbebido;
        getStyleClass().add("visual-assistant");
        setManaged(false);
        setPickOnBounds(false);
        setMinSize(modoEmbebido ? 280 : 340, modoEmbebido ? 122 : 178);
        setPrefSize(modoEmbebido ? 320 : 430, modoEmbebido ? 130 : 178);
        setMaxSize(modoEmbebido ? 360 : 560, modoEmbebido ? 145 : 215);

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
        tamanoActual = modoEmbebido ? 44 : (int) parseDouble(DatabaseManager.getConfig(KEY_TAMANO), 58);
        vozActiva = !modoEmbebido && "1".equals(DatabaseManager.getConfig(KEY_VOZ));
        activo.set(modoEmbebido || !"0".equals(DatabaseManager.getConfig(KEY_ACTIVO)));

        actualizarVisibilidad();
        actualizarPersonaje();
        actualizarTamano();
        iniciarAnimacionReposo();
        iniciarParpadeoSutil();

        if (!modoEmbebido) {
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
        } else {
            body.getStyleClass().add("visual-assistant-embedded");
            setPickOnBounds(false);
        }

        animarSaludo();
        if (modoEmbebido) {
            decir("Preparado para guiar la instalación.");
        } else {
            decir("Estoy listo para ayudarte. Abre un módulo y te iré indicando para qué sirve.");
        }
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
        if (!modoEmbebido) DatabaseManager.setConfig(KEY_ACTIVO, activo ? "1" : "0");
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
        if (!modoEmbebido) DatabaseManager.setConfig(KEY_VOZ, vozActiva ? "1" : "0");
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
        if (!modoEmbebido) DatabaseManager.setConfig(KEY_PERSONAJE, this.personajeActual);
        actualizarPersonaje();
        animarSaludo();
    }

    public int getTamanoActual() {
        return tamanoActual;
    }

    public void setTamanoActual(int tamanoActual) {
        this.tamanoActual = Math.max(44, Math.min(tamanoActual, 88));
        if (!modoEmbebido) DatabaseManager.setConfig(KEY_TAMANO, String.valueOf(this.tamanoActual));
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
            case "Panel principal" ->
                "Panel principal: Tu resumen en tiempo real. Aquí verás avisos importantes, " +
                "eventos del calendario y accesos rápidos a los módulos más usados. " +
                "Es tu punto de partida diario.";
            case "Clientes" ->
                "Clientes: gestiona el directorio completo de clientes de la empresa. " +
                "Crea nuevos registros, edita datos de contacto y consulta el historial de operaciones de cada cliente. " +
                "Todos los presupuestos, facturas y pedidos se vinculan a un cliente de esta lista.";
            case "Presupuestos" ->
                "Presupuestos: prepara ofertas comerciales detalladas para tus clientes. " +
                "Añade líneas de producto o servicio, aplica descuentos y genera el documento PDF para enviar. " +
                "Una vez aceptado por el cliente, puedes convertirlo directamente en pedido o en factura con un solo clic.";
            case "Facturas" ->
                "Facturas: gestiona todos los documentos de facturación emitidos. " +
                "Crea facturas manualmente o genera una automáticamente desde un pedido existente. " +
                "Controla el estado de cobro de cada factura, registra los pagos recibidos y consulta los vencimientos pendientes.";
            case "Albaranes" ->
                "Albaranes: registra y controla las entregas de trabajos realizados. " +
                "Vincula cada albarán a un pedido o factura, indica las unidades entregadas y genera el documento de entrega firmable. " +
                "Útil para llevar un seguimiento preciso de los trabajos completados y pendientes de facturar.";
            case "Pedidos" ->
                "Pedidos: organiza y controla todos los trabajos en curso. " +
                "Registra importes, fechas de entrega previstas y el estado de cada trabajo. " +
                "Gestiona los pagos asociados a cada pedido y consulta el historial de abonos recibidos para hacer un seguimiento del cobro.";
            case "Tarifas" ->
                "Tarifas: Define tus precios por técnica de impresión. " +
                "Configura el precio por unidad, el coste de preparación y la cantidad mínima. " +
                "Para bordado o sublimación, activa el pricing por tramos de tiempo.";
            case "Materiales" ->
                "Materiales: controla el inventario de materias primas y consumibles del taller. " +
                "Registra entradas de stock, consulta el nivel actual de cada material y gestiona los proveedores habituales. " +
                "Registra también los pagos de las compras realizadas para llevar un control de los costes de aprovisionamiento.";
            case "Empleados" ->
                "Empleados: mantiene el registro completo del personal activo de la empresa. " +
                "Gestiona datos personales, de contacto y laborales de cada trabajador. " +
                "Registra altas y bajas, y consulta el historial de cada empleado para tener toda la información centralizada.";
            case "Nóminas" ->
                "Nóminas: calcula y registra los salarios mensuales del personal. " +
                "Introduce los datos de cada período, deducciones, complementos y retenciones para cada empleado. " +
                "Consulta el histórico de nóminas emitidas y el coste total para la empresa en cualquier período.";
            case "Estadísticas" ->
                "Estadísticas: analiza la evolución del negocio con gráficos y métricas detalladas. " +
                "Consulta ventas por período, comparativas entre módulos y los indicadores clave de rendimiento. " +
                "Filtra por fechas para obtener el análisis concreto que necesitas en cada momento.";
            case "Calendario" ->
                "Calendario: organiza eventos, citas y recordatorios de fechas importantes. " +
                "Crea notas con fecha para no olvidar entregas, reuniones con clientes o vencimientos de facturas. " +
                "Los eventos próximos aparecen automáticamente en el panel principal y generan un aviso emergente al iniciar sesión según los días configurados en Preferencias.";
            case "Asistente IA" ->
                "Asistente IA: consulta dudas sobre el ERP, pide resúmenes de actividad o solicita ayuda con las operaciones del negocio. " +
                "Escribe tu pregunta en el campo de texto y pulsa Enviar para obtener una respuesta. " +
                "Activa la voz para escuchar las respuestas en voz alta. Esta función requiere Ollama instalado y un modelo de IA activo.";
            case "Importar Backup" ->
                "Importar Backup: restaura los datos de la aplicación desde una copia de seguridad guardada anteriormente. " +
                "Selecciona el archivo de backup compatible y sigue los pasos del asistente de importación. " +
                "Atención: esta operación reemplaza los datos actuales por los del backup. Úsala únicamente cuando sea necesario recuperar información.";
            case "Exportar / Backup" ->
                "Exportar y Backup: genera copias de seguridad completas de todos los datos de la aplicación. " +
                "Guarda el archivo resultante en un lugar seguro, como una unidad externa o almacenamiento en la nube. " +
                "También permite exportar información en formatos compatibles para análisis externos o para llevar un registro histórico.";
            case "Configuración" ->
                "Configuración: Personaliza la aplicación según las necesidades de tu empresa. " +
                "Ajusta el tema, la tipografía, datos fiscales, IVA por defecto y más. " +
                "Explora las ocho pestañas disponibles para un control total.";
            case "Configuración asistente visual" ->
                "Asistente visual: personaliza el ayudante flotante que te acompaña en todos los módulos. " +
                "Elige entre los personajes disponibles, ajusta el tamaño que ocupa en pantalla y configura si quieres que hable en voz alta. " +
                "El asistente ofrece ayuda contextual según el módulo que estás usando y el botón o campo sobre el que sitúas el ratón.";
            default ->
                "Este módulo agrupa herramientas de gestión del negocio. " +
                "Usa los botones de la barra de herramientas para crear, editar o eliminar registros. " +
                "Selecciona una fila de la tabla para habilitar las acciones disponibles sobre ese elemento.";
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
                ? "Campo de texto: escribe o modifica el dato solicitado en esta pantalla. Los campos marcados con * son obligatorios para poder guardar el registro."
                : "Campo «" + prompt.toLowerCase() + "»: introduce el valor correspondiente a este campo. Si el campo es obligatorio, debes rellenarlo antes de poder guardar.";
        }
        if (node instanceof ComboBoxBase<?>) {
            return "Selector desplegable: haz clic para abrir la lista y elige una de las opciones disponibles. En algunos selectores puedes escribir directamente para filtrar rápidamente las opciones.";
        }
        if (node instanceof TableView<?>) {
            return "Tabla de datos: Haz clic en una fila para seleccionarla y activar las opciones de edición o borrado. Mantén Ctrl para seleccionar varias. Pulsa en la cabecera para ordenar los registros.";
        }
        if (node instanceof ListView<?>) {
            return "Lista de elementos: haz clic en un elemento para seleccionarlo y poder aplicar las acciones disponibles en los botones de esta pantalla. Puedes usar las teclas de flecha para moverte por los elementos de la lista.";
        }
        if (node instanceof TabPane tabPane) {
            Tab tab = tabPane.getSelectionModel().getSelectedItem();
            String titulo = tab != null ? textoLimpio(tab.getText()) : "";
            return titulo.isBlank()
                ? "Pestañas: cambia entre las secciones de esta pantalla pulsando cada pestaña. Cada una agrupa opciones y datos relacionados entre sí."
                : "Pestaña «" + titulo + "»: contiene las opciones y datos de esta sección. Explora las demás pestañas disponibles para acceder a todas las funciones de esta pantalla.";
        }
        if (node instanceof Slider) {
            return "Deslizador: arrastra el control hacia la izquierda o la derecha para ajustar este valor. Puedes ver el resultado del cambio en tiempo real mientras arrastras.";
        }
        if (node instanceof Spinner<?>) {
            return "Selector numérico: usa las flechas arriba y abajo para cambiar el valor de uno en uno, o haz clic en el campo y escribe directamente el número que necesitas. El valor debe estar dentro del rango mínimo y máximo permitido.";
        }
        return null;
    }

    private String ayudaParaBoton(String textoBoton, String tooltip) {
        String base = !textoBoton.isBlank() ? textoBoton : textoLimpio(tooltip);
        String lower = base.toLowerCase();
        if (lower.isBlank()) return "Botón de acción: ejecuta la función asociada en esta pantalla. Asegúrate de tener seleccionado el registro sobre el que quieres actuar.";
        if (lower.contains("tramo")) return "Tramos de tiempo: Gestiona los precios según la duración de producción para la tarifa seleccionada. Añade, edita o elimina tramos indicando minutos (múltiplo de 5) y su precio.";
        if (lower.contains("previsualiz") || lower.contains("preview") || lower.contains("vista previa")) return "Previsualizar: muestra una vista previa del documento antes de generarlo definitivamente. Comprueba que el formato, los datos y el diseño son correctos antes de imprimir o guardar el archivo.";
        if (lower.contains("columna")) return "Columnas: personaliza qué columnas se muestran en esta tabla y en qué orden aparecen. Los cambios se guardan para tu sesión y se mantienen la próxima vez que abras este módulo.";
        if (lower.contains("guardar") || lower.contains("actualizar")) return "Guardar: conserva todos los cambios realizados en el formulario actual. Si hay campos obligatorios sin rellenar, el sistema te avisará antes de guardar para que puedas completarlos.";
        if (lower.contains("nuevo") || lower.contains("nueva") || lower.contains("crear") || lower.contains("añadir") || lower.contains("agregar")) return "Nuevo: abre el formulario para crear un registro en este módulo. Completa los campos obligatorios, marcados con *, y pulsa Aceptar para guardar el nuevo elemento en la base de datos.";
        if (lower.contains("editar") || lower.contains("modificar")) return "Editar: abre el formulario con los datos del registro seleccionado para que puedas modificarlos. Selecciona primero una fila en la tabla y luego pulsa este botón para acceder a la edición.";
        if (lower.contains("eliminar") || lower.contains("borrar")) return "Eliminar: borra de forma permanente el registro o los registros seleccionados. El sistema pedirá confirmación antes de ejecutar el borrado. Esta acción no se puede deshacer, revisa bien la selección.";
        if (lower.contains("buscar") || lower.contains("filtrar")) return "Buscar: filtra los registros mostrados en la tabla según el texto introducido. Escribe en el campo de búsqueda y los resultados se actualizan automáticamente. Deja el campo vacío para volver a ver todos los registros.";
        if (lower.contains("limpiar")) return "Limpiar: restablece los filtros y campos de búsqueda a su estado inicial. Úsalo para volver a ver todos los registros después de haber aplicado un filtro o búsqueda.";
        if (lower.contains("export")) return "Exportar: genera un archivo con los datos de esta pantalla en el formato seleccionado. Elige la ubicación donde quieres guardarlo. Útil para compartir información o crear copias de trabajo externas.";
        if (lower.contains("import")) return "Importar: carga datos desde un archivo externo o desde una copia de seguridad. Sigue el asistente de importación y revisa los datos cargados antes de confirmar para asegurarte de que son correctos.";
        if (lower.contains("imprimir") || lower.contains("pdf")) return "Documento PDF: genera una versión imprimible del registro o listado actual. Puedes previsualizar el documento antes de imprimirlo o guardarlo como archivo PDF en tu equipo.";
        if (lower.contains("enviar")) return "Enviar: procesa y envía el texto o consulta introducida al sistema. La respuesta aparecerá en el área de resultados en unos segundos. Asegúrate de que el contenido es correcto antes de enviar.";
        if (lower.contains("cerrar") || lower.contains("salir") || lower.contains("cancel")) return "Cerrar o cancelar: abandona la acción actual sin guardar los cambios realizados. Volverás al estado anterior sin que se modifique ningún dato en la base de datos.";
        if (lower.contains("aplicar")) return "Aplicar: ejecuta el cambio configurado y lo muestra inmediatamente en la interfaz. Puedes probarlo y volver a ajustar los parámetros si el resultado no es el que buscabas.";
        if (lower.contains("backup") || lower.contains("copia")) return "Backup: genera una copia de seguridad completa de los datos actuales de la aplicación. Guarda el archivo en un lugar seguro, como una unidad externa o un servicio de almacenamiento en la nube.";
        if (lower.contains("restaurar")) return "Restaurar: recupera los datos desde una copia de seguridad anterior. Esta operación reemplaza los datos actuales, úsala únicamente cuando sea necesario recuperar información perdida o dañada.";
        if (lower.contains("config")) return "Configuración: abre las opciones de ajuste de esta función. Aquí puedes cambiar el comportamiento, los parámetros y las preferencias de este elemento de la aplicación.";
        if (lower.contains("activar") || lower.contains("desactivar")) return "Activar / Desactivar: cambia el estado activo de este elemento. Un elemento desactivado no aparecerá disponible en los selectores ni en las operaciones del módulo hasta que lo vuelvas a activar.";
        if (lower.contains("salir") || lower.contains("salida")) return "Salir: cierra la aplicación. Se mostrará una confirmación para evitar cierres accidentales. Asegúrate de haber guardado todos los cambios antes de salir.";
        return "Botón «" + base + "»: ejecuta esta acción dentro del módulo actual. Si está deshabilitado, es posible que necesites seleccionar primero un registro en la tabla.";
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
            case "Selene" -> crearSelene();
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
        vampiMurcielago = new Group(
            vampiAlaIzq, vampiAlaDer, cuerpo, vampiCabeza
        );
        vampiDraculaCapaIzq = new Group(
            polygon("#1B1024", 48, 44, 16, 58, 24, 96, 45, 82),
            polygon("#5C2A66", 46, 49, 25, 61, 31, 86, 45, 75)
        );
        vampiDraculaCapaDer = new Group(
            polygon("#1B1024", 52, 44, 84, 58, 76, 96, 55, 82),
            polygon("#5C2A66", 54, 49, 75, 61, 69, 86, 55, 75)
        );
        Rectangle pantalon = rect(41, 72, 18, 20, 4, "#151018");
        Rectangle piernaIzq = rect(40, 89, 7, 9, 2, "#111111");
        Rectangle piernaDer = rect(53, 89, 7, 9, 2, "#111111");
        Polygon chaleco = polygon("#2A1633", 36, 48, 64, 48, 58, 77, 42, 77);
        Polygon camisa = polygon("#FFFFFF", 45, 48, 55, 48, 52, 73, 48, 73);
        Polygon pajarita = polygon("#B11226", 44, 52, 50, 56, 44, 60, 50, 56, 56, 52, 50, 56, 56, 60);
        Rectangle cuelloAltoIzq = rect(35, 40, 9, 18, 2, "#1B1024");
        cuelloAltoIzq.setRotate(-24);
        Rectangle cuelloAltoDer = rect(56, 40, 9, 18, 2, "#1B1024");
        cuelloAltoDer.setRotate(24);
        vampiDraculaCabeza = new Group(
            circle(50, 31, 16, "#E8B58F"),
            ellipse(50, 20, 18, 9, "#111111"),
            polygon("#111111", 34, 28, 42, 18, 48, 28),
            polygon("#111111", 66, 28, 58, 18, 52, 28),
            circle(44, 31, 2.6, "#111111"),
            circle(56, 31, 2.6, "#111111"),
            rect(45, 39, 10, 2, 1, "#8E4B3A"),
            polygon("#FFFFFF", 45, 42, 48, 42, 46.5, 48),
            polygon("#FFFFFF", 52, 42, 55, 42, 53.5, 48),
            circle(46.5, 47, 1.8, "#B11226"),
            circle(53.5, 47, 1.8, "#B11226"),
            ellipse(53.5, 50.5, 1.4, 2.5, "#8B0E1A")
        );
        vampiDracula = new Group(
            vampiDraculaCapaIzq, vampiDraculaCapaDer,
            cuelloAltoIzq, cuelloAltoDer,
            pantalon, piernaIzq, piernaDer, chaleco, camisa, pajarita,
            vampiDraculaCabeza
        );
        vampiDracula.setOpacity(0.0);

        vampiHumo = new Group(
            circle(34, 52, 8, "#D8D8E6"),
            circle(46, 46, 11, "#F0F0F8"),
            circle(58, 50, 10, "#D8D8E6"),
            circle(66, 61, 8, "#F0F0F8"),
            circle(39, 65, 10, "#C6C6D8"),
            circle(52, 61, 14, "#EFEFF7")
        );
        vampiHumo.setOpacity(0.0);

        vampiGrupo = new Group(vampiMurcielago, vampiDracula, vampiHumo);
        return escalar(vampiGrupo);
    }

    private Node crearGuardiaCivil() {
        rotBrazoIzq  = new Rotate(180, 5.5, 0);
        rotBrazoDer  = new Rotate(0, 5.5, 0);
        rotPiernaIzq = new Rotate(0, 6,   0);
        rotPiernaDer = new Rotate(0, 6,   0);

        Group brazoIzqG = new Group(
            rect(0, 0, 11, 22, 4, "#2F6B35"),
            rect(-3, 20, 17, 5, 3, "#E8C8A0"),
            rect(3, 24, 5, 24, 2, "#222222"),
            rect(2, 22, 7, 5, 2, "#3A3A3A"),
            rect(1, 46, 9, 3, 1, "#111111")
        );
        brazoIzqG.setTranslateX(19.5);
        brazoIzqG.setTranslateY(61);
        brazoIzqG.getTransforms().add(rotBrazoIzq);
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
        davidRadio = new Group(rect(78, 70, 8, 13, 2, "#111111"), rect(80, 66, 4, 5, 1, "#111111"));
        davidLinternaLuz = circle(84, 72, 5, "#F2C94C");
        davidLinternaLuz.setOpacity(0.0);
        davidLinterna = new Group(rect(77, 70, 11, 5, 2, "#222222"), davidLinternaLuz);

        Group g = new Group(
            piernaIzqG, piernaDerG,
            brazoIzqG, davidTorso, davidRadio, davidLinterna, davidCabeza, brazoDerG
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

        Group guitarra = new Group(
            rect(15, 74, 5, 18, 2, "#4A2A1A"),
            rect(16, 60, 3, 18, 1, "#D9C08A"),
            rect(13, 57, 9, 5, 1, "#2A2A2A"),
            ellipse(18, 82, 10, 13, "#7A1E1E"),
            ellipse(12, 84, 7, 8, "#B3262E"),
            ellipse(24, 78, 7, 8, "#B3262E"),
            rect(11, 80, 14, 3, 1, "#C0C0C0"),
            circle(18, 82, 3, "#111111"),
            rect(17, 58, 1, 31, 0, "#F5E6B8"),
            rect(14, 90, 8, 2, 1, "#C0C0C0")
        );
        guitarra.setRotate(-13);

        metalPelo = new Group(
            ellipse(43, 37, 17, 30, "#181014"),
            ellipse(57, 37, 17, 30, "#181014"),
            polygon("#181014", 28, 42, 36, 82, 47, 56),
            polygon("#181014", 72, 42, 64, 82, 53, 56)
        );
        metalPeloDelante = new Group(
            ellipse(50, 36, 9, 28, "#181014"),
            polygon("#181014", 35, 23, 49, 70, 60, 24),
            polygon("#181014", 42, 31, 54, 78, 66, 34)
        );
        metalPeloDelante.setOpacity(0.0);
        metalCabeza = new Group(
            circle(50, 34, 19, "#E8B58F"),
            ellipse(50, 19, 24, 10, "#181014"),
            circle(43, 33, 3.5, "#111111"),
            circle(57, 33, 3.5, "#111111"),
            rect(43, 45, 14, 3, 2, "#7A1E1E")
        );

        Group g = new Group(
            piernaIzqG, piernaDerG,
            guitarra, metalTorso, brazoBajo, metalBrazoAlto,
            metalPelo, metalCabeza, metalPeloDelante
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
        rotBrazoIzq = new Rotate(0, 0, 2.5);
        rotBrazoDer = new Rotate(0, 0, 2.5);

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
        soniaBrazoIzq = new Group(
            polygon("#6B2D5E", 0, 0, 9, 1, 10, 15, 2, 16),
            polygon("#E8B58F", 2, 14, 10, 14, 14, 21, 6, 22),
            ellipse(11, 22, 5, 3, "#E8B58F")
        );
        soniaBrazoIzq.setLayoutX(32);
        soniaBrazoIzq.setLayoutY(62);
        soniaBrazoIzq.getTransforms().add(rotBrazoIzq);
        soniaBrazoDer = new Group(
            polygon("#6B2D5E", -9, 1, 0, 0, -2, 16, -10, 15),
            polygon("#E8B58F", -10, 14, -2, 14, -6, 22, -14, 21),
            ellipse(-11, 22, 5, 3, "#E8B58F")
        );
        soniaBrazoDer.setLayoutX(68);
        soniaBrazoDer.setLayoutY(62);
        soniaBrazoDer.getTransforms().add(rotBrazoDer);
        soniaManoIzq = soniaBrazoIzq;
        soniaManoDer = soniaBrazoDer;

        soniaGrupo = new Group(
            pataIzq, pataDer, silla, torso, cuello, soniaCabeza, auriculares,
            mesa, soniaBrazoIzq, soniaBrazoDer, monitor, teclado
        );
        return escalar(soniaGrupo);
    }

    private Node crearAna() {
        rotBrazoIzq = new Rotate(0, 0, 2.5);
        rotBrazoDer = new Rotate(0, 0, 2.5);

        Rectangle mesa = rect(14, 76, 72, 18, 4, "#5B3A2E");
        Rectangle pataIzq = rect(21, 92, 8, 12, 2, "#3D251F");
        Rectangle pataDer = rect(71, 92, 8, 12, 2, "#3D251F");
        Rectangle silla = rect(32, 68, 36, 24, 8, "#17446B");

        Rectangle torsoBase = rect(35, 56, 30, 23, 9, "#1368A8");
        Rectangle franjaNaranja = rect(49, 56, 8, 23, 3, "#F58220");
        Rectangle reflectante = rect(38, 61, 22, 3, 1, "#EAF7FF");
        Text distintivo = new Text(40, 75, "061");
        distintivo.setFill(Color.web("#FFFFFF"));
        distintivo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        Group uniforme = new Group(torsoBase, franjaNaranja, reflectante, distintivo);
        Rectangle cuello = rect(46, 49, 8, 8, 3, "#E8B58F");

        Group pelo = new Group(
            ellipse(50, 34, 21, 25, "#4E2C20"),
            ellipse(42, 24, 13, 9, "#4E2C20"),
            ellipse(58, 24, 13, 9, "#4E2C20")
        );
        Group cara = new Group(
            circle(50, 35, 18, "#E8B58F"),
            circle(44, 35, 3, "#111111"),
            circle(56, 35, 3, "#111111"),
            rect(44, 46, 12, 3, 2, "#8E4B3A")
        );
        Group gorra = new Group(
            ellipse(50, 22, 18, 8, "#1368A8"),
            rect(37, 18, 26, 9, 4, "#1368A8"),
            polygon("#F58220", 60, 25, 76, 27, 62, 31),
            rect(44, 20, 12, 6, 2, "#F58220")
        );
        Text gorra061 = new Text(44, 25, "061");
        gorra061.setFill(Color.web("#FFFFFF"));
        gorra061.setStyle("-fx-font-size: 7px; -fx-font-weight: bold;");
        gorra.getChildren().add(gorra061);
        soniaCabeza = new Group(pelo, cara, gorra);

        Group cascoIzq = new Group(circle(31, 36, 6, "#1F2937"), rect(27, 31, 5, 11, 2, "#111827"));
        Group cascoDer = new Group(circle(69, 36, 6, "#1F2937"), rect(68, 31, 5, 11, 2, "#111827"));
        Polygon diadema = polygon("#111827", 34, 25, 42, 17, 50, 15, 58, 17, 66, 25, 64, 28, 57, 21, 50, 19, 43, 21, 36, 28);
        soniaMicrofono = new Group(
            rect(66, 42, 5, 3, 2, "#111827"),
            rect(69, 43, 14, 3, 2, "#111827"),
            circle(84, 44, 3, "#F58220")
        );
        Group auriculares = new Group(diadema, cascoIzq, cascoDer, soniaMicrofono);

        Rectangle monitorMarco = rect(24, 55, 52, 34, 5, "#111827");
        soniaPantalla = rect(28, 59, 44, 24, 3, "#1D6FB8");
        Rectangle soporte = rect(47, 89, 7, 8, 2, "#111827");
        Rectangle base = rect(38, 96, 26, 5, 2, "#111827");
        Text monitorText = new Text(39, 75, "061");
        monitorText.setFill(Color.web("#EAF7FF"));
        monitorText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        Group monitor = new Group(monitorMarco, soniaPantalla, monitorText, soporte, base);

        Rectangle teclado = rect(31, 84, 38, 7, 2, "#1F2937");
        anaBrazoIzq = new Group(
            polygon("#1368A8", 0, 0, 9, 1, 10, 15, 2, 16),
            polygon("#E8B58F", 2, 14, 10, 14, 14, 21, 6, 22),
            ellipse(11, 22, 5, 3, "#E8B58F")
        );
        anaBrazoIzq.setLayoutX(32);
        anaBrazoIzq.setLayoutY(62);
        anaBrazoIzq.getTransforms().add(rotBrazoIzq);
        anaBrazoDer = new Group(
            polygon("#F58220", -9, 1, 0, 0, -2, 16, -10, 15),
            polygon("#E8B58F", -10, 14, -2, 14, -6, 22, -14, 21),
            ellipse(-11, 22, 5, 3, "#E8B58F")
        );
        anaBrazoDer.setLayoutX(68);
        anaBrazoDer.setLayoutY(62);
        anaBrazoDer.getTransforms().add(rotBrazoDer);

        soniaGrupo = new Group(
            pataIzq, pataDer, silla, uniforme, cuello, soniaCabeza, auriculares,
            mesa, anaBrazoIzq, anaBrazoDer, monitor, teclado
        );
        return escalar(soniaGrupo);
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

        double anchoTotal = tamanoActual + 58 + ancho;
        setPrefWidth(anchoTotal);
        setMaxWidth(anchoTotal);
        body.setPrefWidth(anchoTotal - 20);
        Platform.runLater(() -> relocateClamped(getLayoutX(), getLayoutY()));
    }

    private double calcularAnchoGlobo(String mensaje) {
        if (mensaje == null || mensaje.isEmpty()) return BUBBLE_MIN_WIDTH;
        Text t = new Text(mensaje);
        t.setFont(texto.getFont());
        double anchoTexto = t.getLayoutBounds().getWidth();
        return Math.max(BUBBLE_MIN_WIDTH, Math.min(anchoTexto + 30, BUBBLE_MAX_WIDTH));
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
        vampiGrupo = vampiMurcielago = vampiDracula = vampiHumo = vampiAlaIzq = vampiAlaDer = vampiCabeza = null;
        vampiDraculaCabeza = vampiDraculaCapaIzq = vampiDraculaCapaDer = null;
        vampiOjoIzq = vampiOjoDer = null;
        metalCabeza = metalPelo = metalPeloDelante = metalTorso = metalBrazoAlto = null;
        soniaGrupo = soniaCabeza = soniaBrazoIzq = soniaBrazoDer = soniaManoIzq = soniaManoDer = soniaMicrofono = null;
        anaBrazoIzq = anaBrazoDer = null;
        soniaPantalla = null;
        davidCabeza = davidTorso = davidRadio = davidLinterna = null;
        davidLinternaLuz = null;
        seleneGrupo = seleneCabeza = seleneEdredon = seleneGloboZzz = seleneHamburguesa = null;
        seleneZPeq = seleneZMed = seleneZGrande = null;
        seleneNarizBurbuja = null;
        if (animSeleneGlobo != null) { animSeleneGlobo.stop(); animSeleneGlobo = null; }
        if (animSeleneNariz != null) { animSeleneNariz.stop(); animSeleneNariz = null; }
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
        if (anaBrazoIzq != null) {
            iniciarAnimacionAna();
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
        if (seleneCabeza != null) {
            iniciarAnimacionSelene();
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
                new KeyValue(vampiAlaDer.rotateProperty(), 8),
                new KeyValue(vampiDraculaCapaIzq.rotateProperty(), -4),
                new KeyValue(vampiDraculaCapaDer.rotateProperty(), 4)),
            new KeyFrame(Duration.millis(360),
                new KeyValue(vampiAlaIzq.rotateProperty(), 20),
                new KeyValue(vampiAlaDer.rotateProperty(), -20),
                new KeyValue(vampiDraculaCapaIzq.rotateProperty(), -13),
                new KeyValue(vampiDraculaCapaDer.rotateProperty(), 13)),
            new KeyFrame(Duration.millis(720),
                new KeyValue(vampiAlaIzq.rotateProperty(), -8),
                new KeyValue(vampiAlaDer.rotateProperty(), 8),
                new KeyValue(vampiDraculaCapaIzq.rotateProperty(), -4),
                new KeyValue(vampiDraculaCapaDer.rotateProperty(), 4))
        );
        animExtremidades.setCycleCount(Animation.INDEFINITE);
        animExtremidades.play();

        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(vampiMurcielago.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiDracula.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.0),
                new KeyValue(vampiHumo.scaleXProperty(), 0.6),
                new KeyValue(vampiHumo.scaleYProperty(), 0.6),
                new KeyValue(vampiCabeza.translateYProperty(), 0),
                new KeyValue(vampiCabeza.rotateProperty(), 0),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 0),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1),
                new KeyValue(vampiGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(360),
                new KeyValue(vampiCabeza.translateYProperty(), -1.5),
                new KeyValue(vampiCabeza.rotateProperty(), -2),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), -1.5),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), -2),
                new KeyValue(vampiOjoIzq.scaleYProperty(), 1),
                new KeyValue(vampiOjoDer.scaleYProperty(), 1)),
            new KeyFrame(Duration.millis(720),
                new KeyValue(vampiCabeza.translateYProperty(), 0),
                new KeyValue(vampiCabeza.rotateProperty(), 2),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 2),
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
                new KeyValue(vampiGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.seconds(9.8),
                new KeyValue(vampiMurcielago.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiDracula.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.0),
                new KeyValue(vampiHumo.scaleXProperty(), 0.6),
                new KeyValue(vampiHumo.scaleYProperty(), 0.6)),
            new KeyFrame(Duration.seconds(10.05),
                new KeyValue(vampiMurcielago.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.95),
                new KeyValue(vampiHumo.scaleXProperty(), 1.25),
                new KeyValue(vampiHumo.scaleYProperty(), 1.25)),
            new KeyFrame(Duration.seconds(10.35),
                new KeyValue(vampiDracula.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.75),
                new KeyValue(vampiHumo.scaleXProperty(), 1.75),
                new KeyValue(vampiHumo.scaleYProperty(), 1.75)),
            new KeyFrame(Duration.seconds(10.7),
                new KeyValue(vampiHumo.opacityProperty(), 0.0),
                new KeyValue(vampiHumo.scaleXProperty(), 2.05),
                new KeyValue(vampiHumo.scaleYProperty(), 2.05),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 0)),
            new KeyFrame(Duration.seconds(11.2),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), -1.5),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), -2)),
            new KeyFrame(Duration.seconds(11.8),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 2)),
            new KeyFrame(Duration.seconds(12.4),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), -1.5),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), -2)),
            new KeyFrame(Duration.seconds(13.0),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 2)),
            new KeyFrame(Duration.seconds(13.9),
                new KeyValue(vampiDracula.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiMurcielago.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.0),
                new KeyValue(vampiHumo.scaleXProperty(), 0.6),
                new KeyValue(vampiHumo.scaleYProperty(), 0.6)),
            new KeyFrame(Duration.seconds(14.15),
                new KeyValue(vampiDracula.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.95),
                new KeyValue(vampiHumo.scaleXProperty(), 1.25),
                new KeyValue(vampiHumo.scaleYProperty(), 1.25)),
            new KeyFrame(Duration.seconds(14.45),
                new KeyValue(vampiMurcielago.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.75),
                new KeyValue(vampiHumo.scaleXProperty(), 1.75),
                new KeyValue(vampiHumo.scaleYProperty(), 1.75)),
            new KeyFrame(Duration.seconds(14.8),
                new KeyValue(vampiMurcielago.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaIzq.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiAlaDer.opacityProperty(), 1.0, Interpolator.DISCRETE),
                new KeyValue(vampiDracula.opacityProperty(), 0.0, Interpolator.DISCRETE),
                new KeyValue(vampiHumo.opacityProperty(), 0.0),
                new KeyValue(vampiHumo.scaleXProperty(), 2.05),
                new KeyValue(vampiHumo.scaleYProperty(), 2.05),
                new KeyValue(vampiGrupo.rotateProperty(), 0),
                new KeyValue(vampiCabeza.translateYProperty(), 0),
                new KeyValue(vampiCabeza.rotateProperty(), 0),
                new KeyValue(vampiDraculaCabeza.translateYProperty(), 0),
                new KeyValue(vampiDraculaCabeza.rotateProperty(), 0))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionMetalera() {
        if (!usarWindmill) {
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
            return;
        }

        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.opacityProperty(), 1.0),
                new KeyValue(metalCabeza.translateXProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0),
                new KeyValue(metalPelo.scaleXProperty(), 1.0),
                new KeyValue(metalPelo.scaleYProperty(), 1.0),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.0),
                new KeyValue(metalPeloDelante.rotateProperty(), 0),
                new KeyValue(metalPeloDelante.translateXProperty(), 0),
                new KeyValue(metalPeloDelante.translateYProperty(), 0),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.0),
                new KeyValue(metalPeloDelante.scaleYProperty(), 1.0),
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
                new KeyValue(rotPiernaDer.angleProperty(), -5)),
            new KeyFrame(Duration.millis(1040),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(1210),
                new KeyValue(metalCabeza.rotateProperty(), -14),
                new KeyValue(metalCabeza.translateYProperty(), 4),
                new KeyValue(metalPelo.rotateProperty(), 9),
                new KeyValue(metalPelo.translateYProperty(), 6)),
            new KeyFrame(Duration.millis(1380),
                new KeyValue(metalCabeza.rotateProperty(), 12),
                new KeyValue(metalCabeza.translateYProperty(), -2),
                new KeyValue(metalPelo.rotateProperty(), -11),
                new KeyValue(metalPelo.translateYProperty(), -3)),
            new KeyFrame(Duration.millis(1560),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(2080),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(2250),
                new KeyValue(metalCabeza.rotateProperty(), -14),
                new KeyValue(metalCabeza.translateYProperty(), 4),
                new KeyValue(metalPelo.rotateProperty(), 9),
                new KeyValue(metalPelo.translateYProperty(), 6)),
            new KeyFrame(Duration.millis(2420),
                new KeyValue(metalCabeza.rotateProperty(), 12),
                new KeyValue(metalCabeza.translateYProperty(), -2),
                new KeyValue(metalPelo.rotateProperty(), -11),
                new KeyValue(metalPelo.translateYProperty(), -3)),
            new KeyFrame(Duration.millis(2600),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(3120),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(3290),
                new KeyValue(metalCabeza.rotateProperty(), -14),
                new KeyValue(metalCabeza.translateYProperty(), 4),
                new KeyValue(metalPelo.rotateProperty(), 9),
                new KeyValue(metalPelo.translateYProperty(), 6)),
            new KeyFrame(Duration.millis(3460),
                new KeyValue(metalCabeza.rotateProperty(), 12),
                new KeyValue(metalCabeza.translateYProperty(), -2),
                new KeyValue(metalPelo.rotateProperty(), -11),
                new KeyValue(metalPelo.translateYProperty(), -3)),
            new KeyFrame(Duration.millis(3640),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(4160),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(4330),
                new KeyValue(metalCabeza.rotateProperty(), -14),
                new KeyValue(metalCabeza.translateYProperty(), 4),
                new KeyValue(metalPelo.rotateProperty(), 9),
                new KeyValue(metalPelo.translateYProperty(), 6)),
            new KeyFrame(Duration.millis(4500),
                new KeyValue(metalCabeza.rotateProperty(), 12),
                new KeyValue(metalCabeza.translateYProperty(), -2),
                new KeyValue(metalPelo.rotateProperty(), -11),
                new KeyValue(metalPelo.translateYProperty(), -3)),
            new KeyFrame(Duration.millis(4680),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.translateXProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0),
                new KeyValue(metalPelo.scaleXProperty(), 1.0),
                new KeyValue(metalPelo.scaleYProperty(), 1.0),
                new KeyValue(metalTorso.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(5200),
                new KeyValue(metalCabeza.rotateProperty(), 18),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateYProperty(), 10),
                new KeyValue(metalPelo.rotateProperty(), 18),
                new KeyValue(metalPelo.translateYProperty(), 9),
                new KeyValue(metalPelo.scaleYProperty(), 0.78),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.0),
                new KeyValue(metalTorso.rotateProperty(), 2)),
            new KeyFrame(Duration.millis(5600),
                new KeyValue(metalCabeza.rotateProperty(), 24),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), -3),
                new KeyValue(metalCabeza.translateYProperty(), 12),
                new KeyValue(metalPelo.rotateProperty(), -70),
                new KeyValue(metalPelo.translateXProperty(), -12),
                new KeyValue(metalPelo.translateYProperty(), 2),
                new KeyValue(metalPelo.scaleXProperty(), 1.18),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.15),
                new KeyValue(metalPeloDelante.rotateProperty(), -70),
                new KeyValue(metalPeloDelante.translateXProperty(), -16),
                new KeyValue(metalPeloDelante.translateYProperty(), 4),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.15),
                new KeyValue(metalPeloDelante.scaleYProperty(), 0.85)),
            new KeyFrame(Duration.millis(6000),
                new KeyValue(metalCabeza.rotateProperty(), 20),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 2),
                new KeyValue(metalCabeza.translateYProperty(), 14),
                new KeyValue(metalPelo.rotateProperty(), 10),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 11),
                new KeyValue(metalPelo.scaleXProperty(), 0.9),
                new KeyValue(metalPeloDelante.opacityProperty(), 1.0),
                new KeyValue(metalPeloDelante.rotateProperty(), 8),
                new KeyValue(metalPeloDelante.translateXProperty(), 1),
                new KeyValue(metalPeloDelante.translateYProperty(), 13),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.55),
                new KeyValue(metalPeloDelante.scaleYProperty(), 1.35)),
            new KeyFrame(Duration.millis(6400),
                new KeyValue(metalCabeza.rotateProperty(), 25),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 3),
                new KeyValue(metalCabeza.translateYProperty(), 12),
                new KeyValue(metalPelo.rotateProperty(), 82),
                new KeyValue(metalPelo.translateXProperty(), 13),
                new KeyValue(metalPelo.translateYProperty(), 2),
                new KeyValue(metalPelo.scaleXProperty(), 1.18),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.2),
                new KeyValue(metalPeloDelante.rotateProperty(), 82),
                new KeyValue(metalPeloDelante.translateXProperty(), 16),
                new KeyValue(metalPeloDelante.translateYProperty(), 4),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.15),
                new KeyValue(metalPeloDelante.scaleYProperty(), 0.85)),
            new KeyFrame(Duration.millis(6800),
                new KeyValue(metalCabeza.rotateProperty(), 18),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 10),
                new KeyValue(metalPelo.rotateProperty(), 180),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), -7),
                new KeyValue(metalPelo.scaleXProperty(), 0.82),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(7200),
                new KeyValue(metalCabeza.rotateProperty(), 24),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), -3),
                new KeyValue(metalCabeza.translateYProperty(), 12),
                new KeyValue(metalPelo.rotateProperty(), 290),
                new KeyValue(metalPelo.translateXProperty(), -12),
                new KeyValue(metalPelo.translateYProperty(), 2),
                new KeyValue(metalPelo.scaleXProperty(), 1.18),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.15),
                new KeyValue(metalPeloDelante.rotateProperty(), -70),
                new KeyValue(metalPeloDelante.translateXProperty(), -16),
                new KeyValue(metalPeloDelante.translateYProperty(), 4)),
            new KeyFrame(Duration.millis(7600),
                new KeyValue(metalCabeza.rotateProperty(), 20),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 2),
                new KeyValue(metalCabeza.translateYProperty(), 14),
                new KeyValue(metalPelo.rotateProperty(), 370),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 11),
                new KeyValue(metalPelo.scaleXProperty(), 0.9),
                new KeyValue(metalPeloDelante.opacityProperty(), 1.0),
                new KeyValue(metalPeloDelante.rotateProperty(), 8),
                new KeyValue(metalPeloDelante.translateXProperty(), 1),
                new KeyValue(metalPeloDelante.translateYProperty(), 13),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.55),
                new KeyValue(metalPeloDelante.scaleYProperty(), 1.35)),
            new KeyFrame(Duration.millis(8000),
                new KeyValue(metalCabeza.rotateProperty(), 25),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 3),
                new KeyValue(metalCabeza.translateYProperty(), 12),
                new KeyValue(metalPelo.rotateProperty(), 442),
                new KeyValue(metalPelo.translateXProperty(), 13),
                new KeyValue(metalPelo.translateYProperty(), 2),
                new KeyValue(metalPelo.scaleXProperty(), 1.18),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.2),
                new KeyValue(metalPeloDelante.rotateProperty(), 82),
                new KeyValue(metalPeloDelante.translateXProperty(), 16),
                new KeyValue(metalPeloDelante.translateYProperty(), 4)),
            new KeyFrame(Duration.millis(8400),
                new KeyValue(metalCabeza.rotateProperty(), 18),
                new KeyValue(metalCabeza.opacityProperty(), 0.0),
                new KeyValue(metalCabeza.translateXProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 10),
                new KeyValue(metalPelo.rotateProperty(), 540),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), -7),
                new KeyValue(metalPelo.scaleXProperty(), 0.82),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(8700),
                new KeyValue(metalCabeza.opacityProperty(), 0.5)),
            new KeyFrame(Duration.millis(9000),
                new KeyValue(metalCabeza.rotateProperty(), 0),
                new KeyValue(metalCabeza.opacityProperty(), 1.0),
                new KeyValue(metalCabeza.translateXProperty(), 0),
                new KeyValue(metalCabeza.translateYProperty(), 0),
                new KeyValue(metalPelo.rotateProperty(), 0),
                new KeyValue(metalPelo.translateXProperty(), 0),
                new KeyValue(metalPelo.translateYProperty(), 0),
                new KeyValue(metalPelo.scaleXProperty(), 1.0),
                new KeyValue(metalPelo.scaleYProperty(), 1.0),
                new KeyValue(metalPeloDelante.opacityProperty(), 0.0),
                new KeyValue(metalPeloDelante.rotateProperty(), 0),
                new KeyValue(metalPeloDelante.translateXProperty(), 0),
                new KeyValue(metalPeloDelante.translateYProperty(), 0),
                new KeyValue(metalPeloDelante.scaleXProperty(), 1.0),
                new KeyValue(metalPeloDelante.scaleYProperty(), 1.0),
                new KeyValue(metalTorso.rotateProperty(), 0))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionDavid() {
        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 180),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(rotPiernaIzq.angleProperty(), 0),
                new KeyValue(rotPiernaDer.angleProperty(), 0),
                new KeyValue(davidRadio.translateYProperty(), 0),
                new KeyValue(davidLinterna.rotateProperty(), 0),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(550),
                new KeyValue(davidCabeza.rotateProperty(), -8),
                new KeyValue(davidTorso.translateYProperty(), -1),
                new KeyValue(rotBrazoIzq.angleProperty(), 166),
                new KeyValue(rotBrazoDer.angleProperty(), -4),
                new KeyValue(rotPiernaIzq.angleProperty(), 2),
                new KeyValue(rotPiernaDer.angleProperty(), -2),
                new KeyValue(davidRadio.translateYProperty(), -2),
                new KeyValue(davidLinterna.rotateProperty(), -8),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(1100),
                new KeyValue(davidCabeza.rotateProperty(), 8),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 194),
                new KeyValue(rotBrazoDer.angleProperty(), 4),
                new KeyValue(rotPiernaIzq.angleProperty(), -2),
                new KeyValue(rotPiernaDer.angleProperty(), 2),
                new KeyValue(davidRadio.translateYProperty(), 0),
                new KeyValue(davidLinterna.rotateProperty(), 8),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.65)),
            new KeyFrame(Duration.millis(1650),
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), -1),
                new KeyValue(rotBrazoIzq.angleProperty(), 172),
                new KeyValue(rotBrazoDer.angleProperty(), -2),
                new KeyValue(rotPiernaIzq.angleProperty(), 0),
                new KeyValue(rotPiernaDer.angleProperty(), 0),
                new KeyValue(davidRadio.translateYProperty(), -1),
                new KeyValue(davidLinterna.rotateProperty(), 0),
                new KeyValue(davidLinternaLuz.opacityProperty(), 0.0)),
            new KeyFrame(Duration.millis(2200),
                new KeyValue(davidCabeza.rotateProperty(), 0),
                new KeyValue(davidTorso.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 180),
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
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaGrupo.translateYProperty(), 0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED"))),
            new KeyFrame(Duration.millis(280),
                new KeyValue(soniaCabeza.rotateProperty(), -4),
                new KeyValue(soniaCabeza.translateYProperty(), 1),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.5),
                new KeyValue(rotBrazoIzq.angleProperty(), 5),
                new KeyValue(rotBrazoDer.angleProperty(), -4),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 3),
                new KeyValue(soniaBrazoDer.translateYProperty(), -1),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#27AE60"))),
            new KeyFrame(Duration.millis(560),
                new KeyValue(soniaCabeza.rotateProperty(), 3),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), -4),
                new KeyValue(rotBrazoDer.angleProperty(), 5),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -1),
                new KeyValue(soniaBrazoDer.translateYProperty(), 3),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#6B2D5E"))),
            new KeyFrame(Duration.millis(840),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED"))),
            new KeyFrame(Duration.seconds(3.8),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2)),
            new KeyFrame(Duration.seconds(4.35),
                new KeyValue(soniaCabeza.rotateProperty(), -2),
                new KeyValue(rotBrazoIzq.angleProperty(), 172),
                new KeyValue(rotBrazoDer.angleProperty(), -172),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -4),
                new KeyValue(soniaBrazoDer.translateYProperty(), -4),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#F58220"))),
            new KeyFrame(Duration.seconds(4.75),
                new KeyValue(soniaCabeza.rotateProperty(), 3),
                new KeyValue(rotBrazoIzq.angleProperty(), 150),
                new KeyValue(rotBrazoDer.angleProperty(), -150),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -6),
                new KeyValue(soniaBrazoDer.translateYProperty(), -6),
                new KeyValue(soniaMicrofono.translateXProperty(), -1.2)),
            new KeyFrame(Duration.seconds(5.15),
                new KeyValue(soniaCabeza.rotateProperty(), -3),
                new KeyValue(rotBrazoIzq.angleProperty(), 176),
                new KeyValue(rotBrazoDer.angleProperty(), -176),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -3),
                new KeyValue(soniaBrazoDer.translateYProperty(), -3),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.2)),
            new KeyFrame(Duration.seconds(5.55),
                new KeyValue(soniaCabeza.rotateProperty(), 4),
                new KeyValue(rotBrazoIzq.angleProperty(), 148),
                new KeyValue(rotBrazoDer.angleProperty(), -148),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -7),
                new KeyValue(soniaBrazoDer.translateYProperty(), -7),
                new KeyValue(soniaMicrofono.translateXProperty(), -1.2)),
            new KeyFrame(Duration.seconds(5.95),
                new KeyValue(soniaCabeza.rotateProperty(), -3),
                new KeyValue(rotBrazoIzq.angleProperty(), 170),
                new KeyValue(rotBrazoDer.angleProperty(), -170),
                new KeyValue(soniaBrazoIzq.translateYProperty(), -4),
                new KeyValue(soniaBrazoDer.translateYProperty(), -4),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.2)),
            new KeyFrame(Duration.seconds(6.35),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaMicrofono.translateXProperty(), 0)),
            new KeyFrame(Duration.seconds(7.0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED"))),
            new KeyFrame(Duration.seconds(8.4),
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaGrupo.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0)),
            new KeyFrame(Duration.seconds(8.75),
                new KeyValue(soniaGrupo.rotateProperty(), 180),
                new KeyValue(soniaGrupo.translateYProperty(), -2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#6B2D5E"))),
            new KeyFrame(Duration.seconds(9.1),
                new KeyValue(soniaGrupo.rotateProperty(), 360),
                new KeyValue(soniaGrupo.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(9.25),
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(soniaBrazoIzq.translateYProperty(), 0),
                new KeyValue(soniaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#2F80ED")))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();
    }

    private void iniciarAnimacionAna() {
        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaGrupo.translateYProperty(), 0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#1D6FB8"))),
            new KeyFrame(Duration.millis(280),
                new KeyValue(soniaCabeza.rotateProperty(), -4),
                new KeyValue(soniaCabeza.translateYProperty(), 1),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.5),
                new KeyValue(rotBrazoIzq.angleProperty(), 5),
                new KeyValue(rotBrazoDer.angleProperty(), -4),
                new KeyValue(anaBrazoIzq.translateYProperty(), 3),
                new KeyValue(anaBrazoDer.translateYProperty(), -1),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#F58220"))),
            new KeyFrame(Duration.millis(560),
                new KeyValue(soniaCabeza.rotateProperty(), 3),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), -5),
                new KeyValue(rotBrazoDer.angleProperty(), 6),
                new KeyValue(anaBrazoIzq.translateYProperty(), -1),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#1368A8"))),
            new KeyFrame(Duration.millis(840),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaCabeza.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#1D6FB8"))),
            new KeyFrame(Duration.seconds(3.8),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2)),
            new KeyFrame(Duration.seconds(4.35),
                new KeyValue(soniaCabeza.rotateProperty(), -2),
                new KeyValue(rotBrazoIzq.angleProperty(), 172),
                new KeyValue(rotBrazoDer.angleProperty(), -172),
                new KeyValue(anaBrazoIzq.translateYProperty(), -4),
                new KeyValue(anaBrazoDer.translateYProperty(), -4),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#F58220"))),
            new KeyFrame(Duration.seconds(4.75),
                new KeyValue(soniaCabeza.rotateProperty(), 3),
                new KeyValue(rotBrazoIzq.angleProperty(), 150),
                new KeyValue(rotBrazoDer.angleProperty(), -150),
                new KeyValue(anaBrazoIzq.translateYProperty(), -6),
                new KeyValue(anaBrazoDer.translateYProperty(), -6),
                new KeyValue(soniaMicrofono.translateXProperty(), -1.2)),
            new KeyFrame(Duration.seconds(5.15),
                new KeyValue(soniaCabeza.rotateProperty(), -3),
                new KeyValue(rotBrazoIzq.angleProperty(), 176),
                new KeyValue(rotBrazoDer.angleProperty(), -176),
                new KeyValue(anaBrazoIzq.translateYProperty(), -3),
                new KeyValue(anaBrazoDer.translateYProperty(), -3),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.2)),
            new KeyFrame(Duration.seconds(5.55),
                new KeyValue(soniaCabeza.rotateProperty(), 4),
                new KeyValue(rotBrazoIzq.angleProperty(), 148),
                new KeyValue(rotBrazoDer.angleProperty(), -148),
                new KeyValue(anaBrazoIzq.translateYProperty(), -7),
                new KeyValue(anaBrazoDer.translateYProperty(), -7),
                new KeyValue(soniaMicrofono.translateXProperty(), -1.2)),
            new KeyFrame(Duration.seconds(5.95),
                new KeyValue(soniaCabeza.rotateProperty(), -3),
                new KeyValue(rotBrazoIzq.angleProperty(), 170),
                new KeyValue(rotBrazoDer.angleProperty(), -170),
                new KeyValue(anaBrazoIzq.translateYProperty(), -4),
                new KeyValue(anaBrazoDer.translateYProperty(), -4),
                new KeyValue(soniaMicrofono.translateXProperty(), 1.2)),
            new KeyFrame(Duration.seconds(6.35),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaMicrofono.translateXProperty(), 0)),
            new KeyFrame(Duration.seconds(7.0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#1D6FB8"))),
            new KeyFrame(Duration.seconds(8.4),
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaGrupo.translateYProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0)),
            new KeyFrame(Duration.seconds(8.75),
                new KeyValue(soniaGrupo.rotateProperty(), 180),
                new KeyValue(soniaGrupo.translateYProperty(), -2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#F58220"))),
            new KeyFrame(Duration.seconds(9.1),
                new KeyValue(soniaGrupo.rotateProperty(), 360),
                new KeyValue(soniaGrupo.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(9.25),
                new KeyValue(soniaGrupo.rotateProperty(), 0),
                new KeyValue(soniaCabeza.rotateProperty(), 0),
                new KeyValue(soniaMicrofono.translateXProperty(), 0),
                new KeyValue(rotBrazoIzq.angleProperty(), 0),
                new KeyValue(rotBrazoDer.angleProperty(), 0),
                new KeyValue(anaBrazoIzq.translateYProperty(), 0),
                new KeyValue(anaBrazoDer.translateYProperty(), 2),
                new KeyValue(soniaPantalla.fillProperty(), Color.web("#1D6FB8")))
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

    private Node crearSelene() {
        Ellipse almohada = ellipse(50, 62, 34, 14, "#D4CCCC");
        Rectangle respaldo = rect(14, 57, 72, 33, 8, "#7A4D3A");
        Rectangle sabana = rect(18, 69, 64, 21, 7, "#F5F0F0");

        Ellipse hombroIzq = ellipse(35, 74, 14, 8, "#E8B58F");
        Ellipse hombroDer = ellipse(65, 74, 14, 8, "#E8B58F");
        Rectangle cuello = rect(44, 62, 12, 12, 4, "#E8B58F");
        Rectangle edredonPrincipal = rect(17, 74, 66, 25, 8, "#B784A7");
        Rectangle pliegue = rect(23, 82, 54, 3, 2, "#8E5A84");
        Ellipse sombraEdredon = ellipse(50, 75, 30, 5, "#8E5A84");
        sombraEdredon.setOpacity(0.25);
        seleneEdredon = new Group(edredonPrincipal, sombraEdredon);

        Ellipse peloFondo = ellipse(50, 43, 24, 27, "#4E2C20");
        Circle cara = circle(50, 47, 21, "#E8B58F");
        Polygon flequillo = polygon("#4E2C20", 28, 39, 36, 26, 44, 34, 50, 28, 56, 34, 64, 26, 72, 39);
        Ellipse mejillaIzq = ellipse(37, 53, 5, 3, "#F0A0A0");
        mejillaIzq.setOpacity(0.45);
        Ellipse mejillaDer = ellipse(63, 53, 5, 3, "#F0A0A0");
        mejillaDer.setOpacity(0.45);
        Rectangle ojoIzq = rect(36, 45, 10, 3, 2, "#3D2000");
        Rectangle ojoDer = rect(54, 45, 10, 3, 2, "#3D2000");
        Ellipse nariz = ellipse(50, 55, 3.5, 2.5, "#D4956A");
        Rectangle boca = rect(46, 62, 8, 3, 2, "#B07060");

        seleneNarizBurbuja = new Ellipse(52, 58, 3.5, 2.7);
        seleneNarizBurbuja.setFill(Color.web("#C8E6F0"));
        seleneNarizBurbuja.setOpacity(0.82);
        seleneNarizBurbuja.setStroke(Color.web("#90BDD0"));
        seleneNarizBurbuja.setStrokeWidth(0.8);
        Ellipse brilloBurbuja = new Ellipse(50.5, 56.8, 1.1, 0.8);
        brilloBurbuja.setFill(Color.web("#FFFFFF"));
        brilloBurbuja.setOpacity(0.7);

        seleneCabeza = new Group(
            peloFondo, cara, flequillo,
            mejillaIzq, mejillaDer,
            ojoIzq, ojoDer, nariz,
            seleneNarizBurbuja, brilloBurbuja,
            boca
        );

        Ellipse fondoOval = ellipse(80, 24, 15, 10, "#EEF4FF");
        fondoOval.setStroke(Color.web("#9BB8D4"));
        fondoOval.setStrokeWidth(1.2);
        Circle cola1 = circle(72, 35, 2.2, "#9BB8D4");
        Circle cola2 = circle(68, 40, 1.8, "#9BB8D4");
        Circle cola3 = circle(65, 45, 1.3, "#9BB8D4");

        seleneZPeq = new Text(73, 23, "z");
        seleneZPeq.setFill(Color.web("#5B7FA6"));
        seleneZPeq.setStyle("-fx-font-size: 7px; -fx-font-weight: bold;");
        seleneZMed = new Text(79, 20, "Z");
        seleneZMed.setFill(Color.web("#4A6A8A"));
        seleneZMed.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        seleneZGrande = new Text(86, 17, "Z");
        seleneZGrande.setFill(Color.web("#2C4F6E"));
        seleneZGrande.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        seleneHamburguesa = new Group(
            ellipse(80, 21, 9, 4.5, "#E9A84A"),
            rect(72, 21, 16, 2, 1, "#F7D27B"),
            rect(72, 23, 16, 2, 1, "#35A853"),
            rect(73, 25, 14, 3, 2, "#6B3A1E"),
            rect(72, 28, 16, 2, 1, "#F2C24D"),
            ellipse(80, 31, 9, 3.5, "#D78A34"),
            circle(76, 18.5, 0.7, "#FFF4C6"),
            circle(80, 17.8, 0.7, "#FFF4C6"),
            circle(84, 18.5, 0.7, "#FFF4C6")
        );
        seleneHamburguesa.setOpacity(0.0);
        seleneGloboZzz = new Group(
            fondoOval, cola1, cola2, cola3,
            seleneZPeq, seleneZMed, seleneZGrande,
            seleneHamburguesa
        );

        seleneGrupo = new Group(
            respaldo, almohada, sabana,
            hombroIzq, hombroDer, cuello,
            seleneEdredon, pliegue,
            seleneCabeza,
            seleneGloboZzz
        );
        return escalar(seleneGrupo);
    }

    private void iniciarAnimacionSelene() {
        animExtremidades = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(seleneEdredon.scaleYProperty(), 1.0),
                new KeyValue(seleneEdredon.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(1.5),
                new KeyValue(seleneEdredon.scaleYProperty(), 1.035),
                new KeyValue(seleneEdredon.translateYProperty(), -0.8)),
            new KeyFrame(Duration.seconds(3.0),
                new KeyValue(seleneEdredon.scaleYProperty(), 1.0),
                new KeyValue(seleneEdredon.translateYProperty(), 0))
        );
        animExtremidades.setCycleCount(Animation.INDEFINITE);
        animExtremidades.play();

        animPersonaje = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(seleneGrupo.rotateProperty(), 0),
                new KeyValue(seleneCabeza.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(seleneCabeza.rotateProperty(), 1.5)),
            new KeyFrame(Duration.millis(400),
                new KeyValue(seleneCabeza.rotateProperty(), -1.0)),
            new KeyFrame(Duration.millis(600),
                new KeyValue(seleneCabeza.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(8000),
                new KeyValue(seleneCabeza.rotateProperty(), 0),
                new KeyValue(seleneEdredon.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(8100),
                new KeyValue(seleneCabeza.rotateProperty(), 3),
                new KeyValue(seleneEdredon.rotateProperty(), 2)),
            new KeyFrame(Duration.millis(8200),
                new KeyValue(seleneCabeza.rotateProperty(), -2),
                new KeyValue(seleneEdredon.rotateProperty(), -1)),
            new KeyFrame(Duration.millis(8350),
                new KeyValue(seleneCabeza.rotateProperty(), 0),
                new KeyValue(seleneEdredon.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(9000),
                new KeyValue(seleneCabeza.rotateProperty(), 0),
                new KeyValue(seleneGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(9600),
                new KeyValue(seleneGrupo.rotateProperty(), 180)),
            new KeyFrame(Duration.millis(10200),
                new KeyValue(seleneGrupo.rotateProperty(), 360)),
            new KeyFrame(Duration.millis(10300),
                new KeyValue(seleneGrupo.rotateProperty(), 0)),
            new KeyFrame(Duration.millis(14000),
                new KeyValue(seleneGrupo.rotateProperty(), 0),
                new KeyValue(seleneCabeza.rotateProperty(), 0))
        );
        animPersonaje.setCycleCount(Animation.INDEFINITE);
        animPersonaje.play();

        animSeleneGlobo = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(seleneGloboZzz.translateYProperty(), 0),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.9),
                new KeyValue(seleneZPeq.opacityProperty(), 0),
                new KeyValue(seleneZMed.opacityProperty(), 0),
                new KeyValue(seleneZGrande.opacityProperty(), 0),
                new KeyValue(seleneHamburguesa.opacityProperty(), 0.0),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 3.5),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 2.7),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), 0)),
            new KeyFrame(Duration.millis(300),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0),
                new KeyValue(seleneZGrande.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(650),
                new KeyValue(seleneZPeq.opacityProperty(), 0.7),
                new KeyValue(seleneZMed.opacityProperty(), 1.0),
                new KeyValue(seleneZGrande.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(1000),
                new KeyValue(seleneZPeq.opacityProperty(), 0.35),
                new KeyValue(seleneZMed.opacityProperty(), 0.75),
                new KeyValue(seleneZGrande.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(1.5),
                new KeyValue(seleneGloboZzz.translateYProperty(), -2),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.95),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.25),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 5.0),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 3.8),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), -0.8)),
            new KeyFrame(Duration.seconds(2.0),
                new KeyValue(seleneZPeq.opacityProperty(), 0.45),
                new KeyValue(seleneZMed.opacityProperty(), 1.0),
                new KeyValue(seleneZGrande.opacityProperty(), 0.4)),
            new KeyFrame(Duration.seconds(2.5),
                new KeyValue(seleneGloboZzz.translateYProperty(), -4),
                new KeyValue(seleneZPeq.opacityProperty(), 0.25),
                new KeyValue(seleneZMed.opacityProperty(), 0.55),
                new KeyValue(seleneZGrande.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(3.0),
                new KeyValue(seleneGloboZzz.translateYProperty(), 0),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.95),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.15),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(3.7),
                new KeyValue(seleneZPeq.opacityProperty(), 0.55),
                new KeyValue(seleneZMed.opacityProperty(), 1.0),
                new KeyValue(seleneZGrande.opacityProperty(), 0.25),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 5.5),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 4.2),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), -1.5)),
            new KeyFrame(Duration.seconds(4.4),
                new KeyValue(seleneGloboZzz.translateYProperty(), -3),
                new KeyValue(seleneZPeq.opacityProperty(), 0.25),
                new KeyValue(seleneZMed.opacityProperty(), 0.55),
                new KeyValue(seleneZGrande.opacityProperty(), 1.0),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 8.0),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 6.0),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), -2.8)),
            new KeyFrame(Duration.seconds(5.0),
                new KeyValue(seleneGloboZzz.translateYProperty(), 0),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.25),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 9.0),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 6.8),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), -3.5)),
            new KeyFrame(Duration.seconds(5.6),
                new KeyValue(seleneZPeq.opacityProperty(), 0.55),
                new KeyValue(seleneZMed.opacityProperty(), 1.0),
                new KeyValue(seleneZGrande.opacityProperty(), 0.35),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 4.2),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 3.2),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), -1.0)),
            new KeyFrame(Duration.seconds(6.0),
                new KeyValue(seleneGloboZzz.translateYProperty(), -4),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.95),
                new KeyValue(seleneZPeq.opacityProperty(), 0.2),
                new KeyValue(seleneZMed.opacityProperty(), 0.55),
                new KeyValue(seleneZGrande.opacityProperty(), 1.0),
                new KeyValue(seleneNarizBurbuja.radiusXProperty(), 3.5),
                new KeyValue(seleneNarizBurbuja.radiusYProperty(), 2.7),
                new KeyValue(seleneNarizBurbuja.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(6.8),
                new KeyValue(seleneGloboZzz.translateYProperty(), 0),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.95),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.25),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0),
                new KeyValue(seleneHamburguesa.opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(7.4),
                new KeyValue(seleneZPeq.opacityProperty(), 0.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.0),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0),
                new KeyValue(seleneHamburguesa.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(10.4),
                new KeyValue(seleneGloboZzz.translateYProperty(), -1),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.95),
                new KeyValue(seleneHamburguesa.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(11.2),
                new KeyValue(seleneHamburguesa.opacityProperty(), 0.0),
                new KeyValue(seleneZPeq.opacityProperty(), 1.0),
                new KeyValue(seleneZMed.opacityProperty(), 0.25),
                new KeyValue(seleneZGrande.opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(12.0),
                new KeyValue(seleneGloboZzz.translateYProperty(), 0),
                new KeyValue(seleneGloboZzz.opacityProperty(), 0.9),
                new KeyValue(seleneZPeq.opacityProperty(), 0),
                new KeyValue(seleneZMed.opacityProperty(), 0),
                new KeyValue(seleneZGrande.opacityProperty(), 0),
                new KeyValue(seleneHamburguesa.opacityProperty(), 0.0))
        );
        animSeleneGlobo.setCycleCount(Animation.INDEFINITE);
        animSeleneGlobo.play();
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
