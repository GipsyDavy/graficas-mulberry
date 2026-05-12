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
import java.util.ArrayList;
import java.util.List;

/**
 * IAView: Versión refactorizada y limpia.
 * Se han eliminado métodos redundantes, importaciones huérfanas y se han centralizado constantes.
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

        Label titulo = new Label(AppConstants.TEXT_IA_TITULO);
        titulo.getStyleClass().add("view-title");
        Label sub = new Label(AppConstants.TEXT_IA_SUBTITULO);
        sub.getStyleClass().add("view-subtitle");

        this.chatBox = new VBox(8);
        this.scroll = buildChat(this.chatBox);
        this.lblEstado = new Label(AppConstants.TEXT_ESTADO_VERIFICANDO);
        this.cbModelo = new ComboBox<>();
        this.btnInstalarOllama = new Button(AppConstants.TEXT_BTN_INSTALAR_OLLAMA);
        this.cbContexto = new CheckBox(AppConstants.TEXT_CB_CONTEXTO_ERP);

        HBox estadoBar = buildEstadoBar();
        this.txtInput = new TextArea();
        this.btnEnviar = new Button(AppConstants.TEXT_BTN_ENVIAR);
        VBox inputArea = buildInputArea();

        getChildren().addAll(titulo, sub, estadoBar, this.scroll, inputArea);
        VBox.setVgrow(this.scroll, Priority.ALWAYS);

        verificarOllama();
    }

    private HBox buildEstadoBar() {
        lblEstado.setStyle("-fx-text-fill: " + AppConstants.COLOR_ORANGE_HEX + "; -fx-font-weight: bold;");

        cbModelo.setPromptText(AppConstants.TEXT_PROMPT_MODELO);
        cbModelo.setOnAction(e -> {
            if (cbModelo.getValue() != null) ia.setModeloActual(cbModelo.getValue());
        });

        btnInstalarOllama.setStyle("-fx-background-color: " + AppConstants.COLOR_MULBERRY_HEX + "; -fx-text-fill: white; -fx-font-weight: bold;");
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

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox bar = new HBox(10, lblEstado, btnInstalarOllama, sp, cbContexto, cbModelo, btnModelos, btnExportar, btnLimpiar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.setStyle(AppConstants.STYLE_ESTADO_BAR);
        return bar;
    }

    private ScrollPane buildChat(VBox chatBox) {
        chatBox.setPadding(new Insets(15));
        ScrollPane sp = new ScrollPane(chatBox);
        sp.setFitToWidth(true);
        sp.setStyle(AppConstants.STYLE_CHAT_SCROLL);
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

        btnEnviar.setStyle("-fx-background-color:" + AppConstants.COLOR_MULBERRY_HEX + "; -fx-text-fill:white;");
        btnEnviar.setOnAction(e -> enviar());

        FlowPane chips = new FlowPane(5, 5);
        for (String s : AppConstants.SUGERENCIAS_IA) {
            Button b = new Button(s);
            b.setStyle(AppConstants.STYLE_CHIP_SUGERENCIA);
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
                    ia.clearContextoERP();
                }

                ia.chatStreaming(prompt,
                        chunk -> Platform.runLater(() -> {
                            respuestaFull.append(chunk);
                            burbuja.textFlow().getChildren().clear();
                            burbuja.textFlow().getChildren().add(new Text(respuestaFull.toString()));
                            scrollAbajo();
                        }),
                        () -> Platform.runLater(() -> {
                            quitarSpinner(burbuja.container());
                            btnEnviar.setDisable(false);
                            SoundService.play(SoundService.Sound.NOTIFICATION);
                        }),
                        err -> Platform.runLater(() -> {
                            quitarSpinner(burbuja.container());
                            addMensajeSistema(AppConstants.MSG_ERROR_PREFIX + err);
                            btnEnviar.setDisable(false);
                        }),
                        modelo -> Platform.runLater(() -> cbModelo.setValue(modelo))
                );
            } catch (Exception e) {
                Platform.runLater(() -> btnEnviar.setDisable(false));
            }
        });
    }

    private void verificarOllama() {
        Thread.ofVirtual().start(() -> {
            boolean ok = ia.verificarConexion();
            if (ok) {
                Platform.runLater(this::configurarOllamaConectado);
            } else {
                Platform.runLater(this::configurarOllamaDesconectado);
            }
        });
    }

    private void configurarOllamaConectado() {
        lblEstado.setText(AppConstants.TEXT_ESTADO_CONECTADO);
        lblEstado.setStyle("-fx-text-fill: " + AppConstants.COLOR_SUCCESS_HEX + "; -fx-font-weight: bold;");
        cbModelo.getItems().setAll(ia.getModelosDisponibles());
    }

    private void configurarOllamaDesconectado() {
        lblEstado.setText(AppConstants.TEXT_ESTADO_DESCONECTADO);
        lblEstado.setStyle("-fx-text-fill: " + AppConstants.COLOR_ERROR_HEX + "; -fx-font-weight: bold;");
        btnInstalarOllama.setVisible(true);
        btnInstalarOllama.setManaged(true);
    }

    private void exportarChat() {
        FileChooser fc = new FileChooser();
        fc.setTitle(AppConstants.TEXT_TITULO_EXPORTAR);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(AppConstants.TEXT_FILTER_PDF, "*.pdf"),
                new FileChooser.ExtensionFilter(AppConstants.TEXT_FILTER_WORD, "*.docx")
        );

        if (getScene() == null) return;
        File file = fc.showSaveDialog(getScene().getWindow());

        if (file != null) {
            try {
                if (file.getName().endsWith(".pdf")) {
                    ChatExportService.exportarPDF(file, extraerMensajesChat());
                } else {
                    ChatExportService.exportarWord(file, extraerMensajesChat());
                }
                SoundService.play(SoundService.Sound.SUCCESS);
            } catch (Exception e) {
                SoundService.play(SoundService.Sound.ERROR);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(AppConstants.TEXT_ERROR_EXPORTAR_TITULO);
                alert.setHeaderText(AppConstants.TEXT_ERROR_EXPORTAR_HEADER);
                alert.setContentText(AppConstants.TEXT_ERROR_EXPORTAR_CONTENT + "\n" + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private List<ChatExportService.MensajeChat> extraerMensajesChat() {
        List<ChatExportService.MensajeChat> lista = new ArrayList<>();
        for (Node nodo : chatBox.getChildren()) {
            if (!(nodo instanceof HBox row) || row.getChildren().isEmpty()) continue;
            Node hijo = row.getChildren().get(0);

            if (row.getAlignment() == Pos.CENTER_RIGHT && hijo instanceof TextFlow tf) {
                lista.add(new ChatExportService.MensajeChat("usuario", extraerTextoDeTextFlow(tf)));
            } else if (hijo instanceof VBox vbox && !vbox.getChildren().isEmpty()
                    && vbox.getChildren().get(0) instanceof TextFlow tf) {
                lista.add(new ChatExportService.MensajeChat("ia", extraerTextoDeTextFlow(tf)));
            } else if (hijo instanceof Label l) {
                lista.add(new ChatExportService.MensajeChat("sistema", l.getText()));
            }
        }
        return lista;
    }

    private String extraerTextoDeTextFlow(TextFlow tf) {
        StringBuilder sb = new StringBuilder();
        for (Node n : tf.getChildren()) {
            if (n instanceof Text t) {
                sb.append(t.getText());
            }
        }
        return sb.toString().trim();
    }

    private void addBurbujaUsuario(String t) {
        Text text = new Text(t); text.setFill(Color.WHITE);
        TextFlow tf = new TextFlow(text);
        tf.setPadding(new Insets(10));
        tf.setStyle("-fx-background-color:" + AppConstants.COLOR_MULBERRY_HEX + "; " + AppConstants.STYLE_BURBUJA_USUARIO);
        HBox row = new HBox(tf); row.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void addMensajeSistema(String t) {
        Label l = new Label(t); l.setStyle(AppConstants.STYLE_MSG_SISTEMA);
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
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://ollama.com/download"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}