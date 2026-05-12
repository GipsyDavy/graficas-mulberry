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
import org.gipsybuho.service.*;
import org.gipsybuho.util.AppConstants;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
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

        HBox estadoBar = buildEstadoBar();
        this.txtInput = new TextArea();
        this.btnEnviar = new Button(AppConstants.TEXT_BTN_ENVIAR);
        VBox inputArea = buildInputArea();

        getChildren().addAll(titulo, sub, estadoBar, this.scroll, inputArea);
        VBox.setVgrow(this.scroll, Priority.ALWAYS);

        verificarOllama();
    }

    private HBox buildEstadoBar() {
        lblEstado.setStyle("-fx-text-fill: #" + AppConstants.COLOR_DARK_GRAY_HEX + "; -fx-font-weight: bold;");

        // CORRECCIÓN: 'TEXT_PROMPT_MODELO' no existe en AppConstants.
        cbModelo.setPromptText("Seleccionar modelo");
        cbModelo.setOnAction(e -> {
            if (cbModelo.getValue() != null) ia.setModeloActual(cbModelo.getValue());
        });

        btnInstalarOllama.setStyle("-fx-background-color: #" + AppConstants.COLOR_MULBERRY_HEX + "; -fx-text-fill: white;");
        btnInstalarOllama.setVisible(false);
        btnInstalarOllama.setManaged(false);
        btnInstalarOllama.setOnAction(e -> abrirInstalador());

        cbContexto.setSelected(true);

        Button btnModelos = new Button(AppConstants.TEXT_BTN_GESTION_MODELOS);
        btnModelos.setOnAction(e -> abrirGestionModelos());

        Button btnExportar = new Button(AppConstants.TEXT_BTN_EXPORTAR);
        btnExportar.setOnAction(e -> exportarChat());

        Button btnLimpiar = new Button(AppConstants.TEXT_BTN_LIMPIAR);
        btnLimpiar.setOnAction(e -> {
            chatBox.getChildren().clear();
            ia.limpiarHistorial();
            addMensajeSistema(AppConstants.MSG_CHAT_REINICIADO);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, lblEstado, btnInstalarOllama, spacer, cbContexto, cbModelo, btnModelos, btnExportar, btnLimpiar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.setStyle(AppConstants.STYLE_ESTADO_BAR);
        return bar;
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

        btnEnviar.setStyle("-fx-background-color: #" + AppConstants.COLOR_MULBERRY_HEX + "; -fx-text-fill: white;");
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
            b.setOnAction(e -> { txtInput.setText(s); enviar(); });
            chips.getChildren().add(b);
        }

        return new VBox(5, chips, new HBox(8, txtInput, btnEnviar));
    }

    private void enviar() {
        String prompt = txtInput.getText().trim();
        if (prompt.isEmpty() || btnEnviar.isDisabled()) return;

        txtInput.clear();
        btnEnviar.setDisable(true);
        addBurbujaUsuario(prompt);

        BurbujaIA burbuja = crearBurbujaIA();
        chatBox.getChildren().add(burbuja.row());
        mostrarSpinner(burbuja.container());

        StringBuilder respuestaFull = new StringBuilder();

        Thread.ofVirtual().start(() -> {
            try {
                if (cbContexto.isSelected()) {
                    ia.setContextoERP(contextoService.construirContexto());
                } else {
                    ia.setContextoERP(null);
                }

                ia.enviarConsulta(prompt,
                        fragment -> Platform.runLater(() -> {
                            respuestaFull.append(fragment);
                            burbuja.textFlow().getChildren().clear();
                            burbuja.textFlow().getChildren().add(new Text(respuestaFull.toString()));
                            scrollAbajo();
                        }),
                        err -> Platform.runLater(() -> {
                            quitarSpinner(burbuja.container());
                            // Corregido: Usamos el prefijo existente en AppConstants
                            addMensajeSistema(AppConstants.MSG_ERROR_PREFIX + err);
                            btnEnviar.setDisable(false);
                        })
                );

                Platform.runLater(() -> {
                    quitarSpinner(burbuja.container());
                    btnEnviar.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> btnEnviar.setDisable(false));
            }
        });
    }

    private void verificarOllama() {
        Thread.ofVirtual().start(() -> {
            List<OllamaService.ModelInfo> modelos = ia.getModelosConDetalles();
            boolean ok = !modelos.isEmpty();
            Platform.runLater(() -> {
                if (ok) {
                    lblEstado.setText(AppConstants.TEXT_ESTADO_CONECTADO);
                    lblEstado.setStyle("-fx-text-fill: #" + AppConstants.COLOR_SUCCESS_HEX + "; -fx-font-weight: bold;");
                    cbModelo.getItems().setAll(modelos.stream().map(m -> m.nombre).toList());
                } else {
                    lblEstado.setText(AppConstants.TEXT_ESTADO_DESCONECTADO);
                    lblEstado.setStyle("-fx-text-fill: #" + AppConstants.COLOR_ERROR_HEX + "; -fx-font-weight: bold;");
                    btnInstalarOllama.setVisible(true);
                    btnInstalarOllama.setManaged(true);
                }
            });
        });
    }

    private void exportarChat() {
        // 1. Extraer los mensajes actuales
        List<ChatExportService.MensajeChat> mensajes = extraerMensajesChat();

        if (mensajes.isEmpty()) {
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
                            addMensajeSistema("✅ Historial guardado correctamente: " + file.getName())
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
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
        // Usando color Mulberry de AppConstants
        tf.setStyle("-fx-background-color: #" + AppConstants.COLOR_MULBERRY_HEX + "; -fx-background-radius: 15 15 2 15;");
        HBox row = new HBox(tf); row.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void addMensajeSistema(String t) {
        Label l = new Label(t);
        l.setStyle(AppConstants.STYLE_MSG_SISTEMA);
        chatBox.getChildren().add(new HBox(l));
    }

    private record BurbujaIA(HBox row, TextFlow textFlow, VBox container) {}
    private BurbujaIA crearBurbujaIA() {
        TextFlow tf = new TextFlow();
        tf.setPadding(new Insets(10));
        tf.setStyle(AppConstants.STYLE_BURBUJA_IA);
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
        System.out.println(AppConstants.DEBUG_GESTION_MODELOS);
    }

    private void abrirInstalador() {
        System.out.println(AppConstants.DEBUG_REDIR_OLLAMA);
        Thread.ofVirtual().start(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI("https://ollama.com/download"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}