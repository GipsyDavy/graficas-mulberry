package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.*;
import org.gipsybuho.util.AppConstants;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * IAView: Versión 100% Sincronizada con AppConstants.java
 */
public class IAView extends VBox {

    private final OllamaService ia = new OllamaService();
    private final ContextoERPService contextoService = new ContextoERPService();

    private final VBox chatBox;
    private final ScrollPane scroll;
    private final TextArea txtInput;
    private final Button btnEnviar;
    private final Label lblEstado;
    private final ComboBox<String> cbModelo;
    private final Button btnInstalarOllama;
    private final CheckBox cbContexto;
    private final ToggleButton btnVoz;
    private final ComboBox<String> cbVozTts;
    private String ultimaRespuestaIA = "";
    private volatile boolean vozWindowsDisponible;

    public IAView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        // Corregido: Nombres de títulos según AppConstants
        Label titulo = new Label(AppConstants.TITLE_IA_ASSISTANT);
        titulo.getStyleClass().add("view-title");
        Label sub = new Label(AppConstants.SUBTITLE_IA_ASSISTANT);
        sub.getStyleClass().add("view-subtitle");

        this.chatBox = new VBox(8);
        this.scroll = buildChat(this.chatBox);

        this.lblEstado = new Label(AppConstants.TEXT_ESTADO_VERIFICANDO);
        this.cbModelo = new ComboBox<>();
        this.btnInstalarOllama = new Button("Instalar Ollama");

        // CORRECCIÓN: 'TEXT_CB_CONTEXTO_ERP' no existe en AppConstants. Usamos literal o una similar.
        this.cbContexto = new CheckBox("Incluir contexto del ERP");
        this.btnVoz = new ToggleButton();
        this.cbVozTts = new ComboBox<>();

        HBox estadoBar = buildEstadoBar();
        this.txtInput = new TextArea();
        this.btnEnviar = new Button(AppConstants.TEXT_BTN_ENVIAR);
        VBox inputArea = buildInputArea();

        getChildren().addAll(titulo, sub, estadoBar, this.scroll, inputArea);
        VBox.setVgrow(this.scroll, Priority.ALWAYS);

        verificarOllama();
    }

    private HBox buildEstadoBar() {
        lblEstado.getStyleClass().add("ia-lbl-verificando");

        // CORRECCIÓN: 'TEXT_PROMPT_MODELO' no existe en AppConstants.
        cbModelo.setPromptText("Seleccionar modelo");
        cbModelo.setOnAction(e -> {
            SoundService.play(SoundService.Sound.CLICK);
            if (cbModelo.getValue() != null) {
                ia.setModeloActual(cbModelo.getValue());
                actualizarEstadoConModelo();
            }
        });

        btnInstalarOllama.getStyleClass().add("ia-btn-enviar");
        btnInstalarOllama.setVisible(false);
        btnInstalarOllama.setManaged(false);
        btnInstalarOllama.setOnAction(e -> abrirInstalador());

        cbContexto.setSelected(true);

        cbVozTts.getItems().setAll(TextToSpeechService.nombresVoces());
        cbVozTts.setValue(TextToSpeechService.getVozSeleccionada());
        cbVozTts.setTooltip(new Tooltip("Seleccionar voz española del asistente"));
        cbVozTts.setOnAction(e -> {
            SoundService.play(SoundService.Sound.CLICK);
            if (cbVozTts.getValue() != null) {
                if (TextToSpeechService.esOpcionInstalacion(cbVozTts.getValue())) {
                    mostrarSugerenciaInstalacionVoces();
                    cbVozTts.setValue(TextToSpeechService.getVozSeleccionada());
                    return;
                }
                TextToSpeechService.setVozSeleccionada(cbVozTts.getValue());
            }
        });

        btnVoz.setSelected("1".equals(DatabaseManager.getConfig("ia_voz_activada")));
        actualizarBotonVoz();
        btnVoz.setDisable(true);
        btnVoz.setOnAction(e -> {
            SoundService.play(SoundService.Sound.CLICK);
            if (btnVoz.isSelected()) {
                if (!vozWindowsDisponible) {
                    btnVoz.setSelected(false);
                    DatabaseManager.setConfig("ia_voz_activada", "0");
                    actualizarBotonVoz();
                    mostrarErrorVozNoDisponible();
                    return;
                }
                DatabaseManager.setConfig("ia_voz_activada", "1");
                actualizarBotonVoz();
                TextToSpeechService.speak("Voz activada");
            } else {
                DatabaseManager.setConfig("ia_voz_activada", "0");
                actualizarBotonVoz();
                TextToSpeechService.stop();
            }
        });
        verificarDisponibilidadVoz();

        Button btnModelos = new Button(AppConstants.TEXT_BTN_GESTION_MODELOS);
        btnModelos.getStyleClass().add("btn-toolbar");
        btnModelos.setOnAction(e -> {
            SoundService.play(SoundService.Sound.WINDOW_OPEN);
            abrirGestionModelos();
        });

        Button btnExportar = new Button(AppConstants.TEXT_BTN_EXPORTAR);
        btnExportar.getStyleClass().add("btn-toolbar");
        btnExportar.setOnAction(e -> exportarChat());

        Button btnLimpiar = new Button(AppConstants.TEXT_BTN_LIMPIAR);
        btnLimpiar.getStyleClass().add("btn-toolbar");
        btnLimpiar.setOnAction(e -> {
            TextToSpeechService.stop();
            chatBox.getChildren().clear();
            ia.limpiarHistorial();
            addMensajeSistema(AppConstants.MSG_CHAT_REINICIADO);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, lblEstado, btnInstalarOllama, spacer, cbContexto, btnVoz, cbVozTts, cbModelo, btnModelos, btnExportar, btnLimpiar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private void actualizarBotonVoz() {
        if (btnVoz.isSelected()) {
            btnVoz.setText("🔊 Voz activada");
            btnVoz.setTooltip(new Tooltip("La respuesta del asistente IA se leerá por voz"));
            btnVoz.getStyleClass().remove("btn-voz-silenciado");
            if (!btnVoz.getStyleClass().contains("btn-voz-activo"))
                btnVoz.getStyleClass().add("btn-voz-activo");
        } else {
            btnVoz.setText("🔇 Voz silenciada");
            btnVoz.setTooltip(new Tooltip("La respuesta del asistente IA no se leerá por voz"));
            btnVoz.getStyleClass().remove("btn-voz-activo");
            if (!btnVoz.getStyleClass().contains("btn-voz-silenciado"))
                btnVoz.getStyleClass().add("btn-voz-silenciado");
        }
    }

    private ScrollPane buildChat(VBox chatBox) {
        chatBox.setPadding(new Insets(15));
        ScrollPane sp = new ScrollPane(chatBox);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        addMensajeSistema(AppConstants.MSG_BIENVENIDA_IA);
        return sp;
    }

    private VBox buildInputArea() {
        txtInput.setPromptText(AppConstants.TEXT_PROMPT_INPUT);
        txtInput.setWrapText(true);
        txtInput.setPrefRowCount(2);
        HBox.setHgrow(txtInput, Priority.ALWAYS);

        txtInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                enviar();
            }
        });

        btnEnviar.getStyleClass().add("ia-btn-enviar");
        btnEnviar.setOnAction(e -> enviar());

        FlowPane chips = new FlowPane(5, 5);
        String[] sugerencias = {
                AppConstants.SUGGESTION_VIEW_MATERIALS,
                AppConstants.SUGGESTION_CREATE_BUDGET,
                AppConstants.SUGGESTION_GENERATE_INVOICE
        };
        for (String s : sugerencias) {
            Button b = new Button(s);
            b.setStyle("-fx-background-radius: 15; -fx-cursor: hand;");
            b.setOnAction(e -> {
                txtInput.setText(s);
                enviar();
            });
            chips.getChildren().add(b);
        }

        return new VBox(5, chips, new HBox(8, txtInput, btnEnviar));
    }

    private void enviar() {
        String prompt = txtInput.getText().trim();
        if (prompt.isEmpty() || btnEnviar.isDisabled()) return;
        boolean incluirContexto = cbContexto.isSelected();

        txtInput.clear();
        btnEnviar.setDisable(true);
        TextToSpeechService.stop();
        SoundService.play(SoundService.Sound.CHAT_SEND);
        addBurbujaUsuario(prompt);

        BurbujaIA burbuja = crearBurbujaIA();
        chatBox.getChildren().add(burbuja.row());
        mostrarSpinner(burbuja.container());

        StringBuilder respuestaFull = new StringBuilder();

        Thread.ofVirtual().start(() -> {
            try {
                if (incluirContexto) {
                    ia.setContextoERP(contextoService.construirContexto());
                } else {
                    ia.setContextoERP(null);
                }

                ia.enviarConsulta(prompt,
                        fragment -> {
                            respuestaFull.append(fragment);
                            burbuja.textFlow().getChildren().clear();
                            burbuja.textFlow().getChildren().add(new Text(respuestaFull.toString()));
                            scrollAbajo();
                        },
                        err -> {
                            quitarSpinner(burbuja.container());
                            // Corregido: Usamos el prefijo existente en AppConstants
                            SoundService.play(SoundService.Sound.ERROR);
                            addMensajeSistema(AppConstants.MSG_ERROR_PREFIX + err);
                            btnEnviar.setDisable(false);
                        },
                        () -> {
                            ultimaRespuestaIA = respuestaFull.toString();
                            quitarSpinner(burbuja.container());
                            btnEnviar.setDisable(false);
                            SoundService.play(SoundService.Sound.CHAT_RECEIVE);
                            if (btnVoz.isSelected() && !ultimaRespuestaIA.isBlank()) {
                                TextToSpeechService.speak(ultimaRespuestaIA);
                            }
                        }
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    btnEnviar.setDisable(false);
                });
            }
        });
    }

    private void verificarDisponibilidadVoz() {
        Thread.ofVirtual().start(() -> {
            boolean disponible = TextToSpeechService.isDisponible();
            Platform.runLater(() -> {
                vozWindowsDisponible = disponible;
                btnVoz.setDisable(false);
                if (!disponible && btnVoz.isSelected()) {
                    btnVoz.setSelected(false);
                    DatabaseManager.setConfig("ia_voz_activada", "0");
                }
                actualizarBotonVoz();
            });
        });
    }

    private void setEstadoClass(String cls) {
        lblEstado.getStyleClass().removeAll("ia-lbl-conectado", "ia-lbl-desconectado", "ia-lbl-verificando");
        lblEstado.getStyleClass().add(cls);
    }

    private void verificarOllama() {
        Thread.ofVirtual().start(() -> {
            if (OllamaManager.isInstalled() && !OllamaManager.isRunning()) {
                OllamaManager.startIfNeeded();
            }
            List<OllamaService.ModelInfo> modelos = ia.getModelosConDetalles();
            boolean ok = !modelos.isEmpty();
            Platform.runLater(() -> {
                btnInstalarOllama.setVisible(false);
                btnInstalarOllama.setManaged(false);
                if (ok) {
                    setEstadoClass("ia-lbl-conectado");
                    cbModelo.getItems().setAll(modelos.stream().map(m -> m.nombre).toList());
                    if (!cbModelo.getItems().isEmpty() && cbModelo.getValue() == null) {
                        cbModelo.getSelectionModel().selectFirst();
                        ia.setModeloActual(cbModelo.getValue());
                    }
                    actualizarEstadoConModelo();
                } else if (OllamaManager.isRunning() || OllamaManager.isInstalled()) {
                    lblEstado.setText("Ollama instalado, sin modelos IA");
                    setEstadoClass("ia-lbl-desconectado");
                    cbModelo.getItems().clear();
                } else {
                    lblEstado.setText(AppConstants.TEXT_ESTADO_DESCONECTADO);
                    setEstadoClass("ia-lbl-desconectado");
                    btnInstalarOllama.setVisible(true);
                    btnInstalarOllama.setManaged(true);
                }
            });
        });
    }

    private void actualizarEstadoConModelo() {
        String modelo = ia.getModeloActual();
        if (modelo == null || modelo.isBlank()) {
            lblEstado.setText(AppConstants.TEXT_ESTADO_CONECTADO);
        } else {
            lblEstado.setText(AppConstants.TEXT_ESTADO_CONECTADO + " — " + modelo);
        }
    }

    private void exportarChat() {
        // 1. Extraer los mensajes actuales
        List<ChatExportService.MensajeChat> mensajes = extraerMensajesChat();

        if (mensajes.isEmpty()) {
            SoundService.play(SoundService.Sound.ERROR);
            addMensajeSistema("No hay mensajes para exportar.");
            return;
        }

        // 2. Configurar el selector de archivos
        FileChooser fc = new FileChooser();
        fc.setTitle(AppConstants.TEXT_TITULO_EXPORTAR);
        fc.setInitialFileName("Historial_Chat_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Documento Word (*.docx)", "*.docx")
        );

        // Obtener la ventana actual de forma segura
        if (getScene() == null || getScene().getWindow() == null) return;
        File file = fc.showSaveDialog(getScene().getWindow());

        if (file != null) {
            // Usamos un hilo virtual para que la UI no se congele mientras se genera el PDF/Word
            Thread.ofVirtual().start(() -> {
                try {
                    ChatExportService service = new ChatExportService();
                    service.exportarChat(mensajes, file);

                    Platform.runLater(() ->
                            {
                                SoundService.play(SoundService.Sound.COMPLETE);
                                addMensajeSistema("✅ Historial guardado correctamente: " + file.getName());
                            }
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        SoundService.play(SoundService.Sound.ERROR);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(AppConstants.TEXT_ERROR_EXPORTAR_TITULO);
                        alert.setHeaderText("Fallo en la exportación");
                        alert.setContentText(e.getMessage());
                        alert.showAndWait();
                    });
                }
            });
        }
    }

    private List<ChatExportService.MensajeChat> extraerMensajesChat() {
        List<ChatExportService.MensajeChat> lista = new ArrayList<>();
        for (Node nodo : chatBox.getChildren()) {
            if (!(nodo instanceof HBox row)) continue;

            // Identificar si es mensaje de usuario (alineado a la derecha)
            if (row.getAlignment() == Pos.CENTER_RIGHT) {
                Node tf = row.getChildren().get(0);
                if (tf instanceof TextFlow) {
                    // CORRECCIÓN: El record usa 'contenido'
                    lista.add(new ChatExportService.MensajeChat("usuario", obtenerTextoTF((TextFlow) tf)));
                }
            } else {
                Node hijo = row.getChildren().get(0);
                if (hijo instanceof VBox vbox) {
                    Node tf = vbox.getChildren().get(0);
                    if (tf instanceof TextFlow) {
                        // CORRECCIÓN: El record usa 'contenido'
                        lista.add(new ChatExportService.MensajeChat("ia", obtenerTextoTF((TextFlow) tf)));
                    }
                } else if (hijo instanceof Label) {
                    lista.add(new ChatExportService.MensajeChat("sistema", ((Label) hijo).getText()));
                }
            }
        }
        return lista;
    }

    private String obtenerTextoTF(TextFlow tf) {
        StringBuilder sb = new StringBuilder();
        tf.getChildren().forEach(n -> { if (n instanceof Text) sb.append(((Text) n).getText()); });
        return sb.toString();
    }

    private void addBurbujaUsuario(String t) {
        Text text = new Text(t); text.setFill(Color.WHITE);
        TextFlow tf = new TextFlow(text);
        tf.setPadding(new Insets(10));
        tf.getStyleClass().add("ia-burbuja-usuario");
        HBox row = new HBox(tf); row.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void addMensajeSistema(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("ia-msg-sistema");
        chatBox.getChildren().add(new HBox(l));
    }

    private record BurbujaIA(HBox row, TextFlow textFlow, VBox container) {}
    private BurbujaIA crearBurbujaIA() {
        TextFlow tf = new TextFlow();
        tf.setPadding(new Insets(10));
        tf.getStyleClass().add("ia-burbuja-ia");
        VBox v = new VBox(tf);
        HBox r = new HBox(v);
        return new BurbujaIA(r, tf, v);
    }

    private void mostrarSpinner(VBox c) {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(20, 20);
        c.getChildren().add(pi);
    }

    private void quitarSpinner(VBox c) {
        c.getChildren().removeIf(n -> n instanceof ProgressIndicator);
    }

    private void scrollAbajo() {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    private void abrirGestionModelos() {
        Stage owner = obtenerStageActual();
        ModelosGestionDialog dialog = new ModelosGestionDialog(owner, ia);
        dialog.showAndWait();
        if (dialog.isHuboCambios()) {
            verificarOllama();
        }
    }

    private void abrirInstalador() {
        SoundService.play(SoundService.Sound.WINDOW_OPEN);
        Stage owner = obtenerStageActual();
        OllamaInstallerDialog dialog = new OllamaInstallerDialog(owner);
        dialog.showAndWait();
        verificarOllama();
    }

    private void mostrarSugerenciaInstalacionVoces() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Voces naturales de Windows");
        alert.setHeaderText("Instala voces naturales es-ES desde Windows");
        alert.setContentText(TextToSpeechService.mensajeInstalacionVocesNaturales());
        if (getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        alert.showAndWait();
    }

    private void mostrarErrorVozNoDisponible() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Voz no disponible");
        alert.setHeaderText("No se ha podido activar la lectura por voz");
        alert.setContentText("Windows no ha devuelto ninguna voz compatible para leer el chat. "
                + "Instala o habilita una voz desde Configuración > Hora e idioma > Voz y reinicia la aplicación.");
        if (getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        alert.showAndWait();
    }

    private Stage obtenerStageActual() {
        Window window = getScene() != null ? getScene().getWindow() : null;
        return window instanceof Stage stage ? stage : null;
    }
}
