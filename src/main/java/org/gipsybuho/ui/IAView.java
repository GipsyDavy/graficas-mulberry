package org.gipsybuho.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gipsybuho.service.*;
import org.gipsybuho.service.ChatExportService.MensajeChat;
import org.gipsybuho.util.AppConstants;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;



/**
 * FUSIÓN FINAL: IAView.java
 * Estructura de IAView2 (Limpia y Robusta) + Estética de IAView3 (Iconos y UX)
 */
public class IAView extends VBox {

    private final OllamaService ia = new OllamaService();
    private final ContextoERPService contextoService = new ContextoERPService();
    private final Map<String, Stage> modulosAbiertos = new HashMap<>();

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

        // Cabecera estilo IAView3
        Label titulo = new Label("Asistente IA Local");
        titulo.getStyleClass().add("view-title");
        Label sub = new Label("Powered by Ollama — IA 100% local, sin enviar datos a Internet");
        sub.getStyleClass().add("view-subtitle");

        this.chatBox = new VBox(8);
        this.scroll = buildChat(this.chatBox);
        this.lblEstado = new Label("⏳ Verificando conexión...");
        this.cbModelo = new ComboBox<>();
        this.btnInstalarOllama = new Button("⬇  Instalar Ollama");
        this.cbContexto = new CheckBox("📊 Datos ERP");

        HBox estadoBar = buildEstadoBar();
        this.txtInput = new TextArea();
        this.btnEnviar = new Button("Enviar ▶");
        VBox inputArea = buildInputArea();

        getChildren().addAll(titulo, sub, estadoBar, this.scroll, inputArea);
        VBox.setVgrow(this.scroll, Priority.ALWAYS);

        verificarOllama();
    }

    private HBox buildEstadoBar() {
        lblEstado.setStyle("-fx-text-fill: " + AppConstants.COLOR_ORANGE_HEX + "; -fx-font-weight: bold;");

        cbModelo.setPromptText("Modelo");
        cbModelo.setOnAction(e -> {
            if (cbModelo.getValue() != null) ia.setModeloActual(cbModelo.getValue());
        });

        btnInstalarOllama.setStyle("-fx-background-color: " + AppConstants.COLOR_MULBERRY_HEX + "; -fx-text-fill: white; -fx-font-weight: bold;");
        btnInstalarOllama.setVisible(false);
        btnInstalarOllama.setManaged(false);

        cbContexto.setSelected(true);
        
        Button btnModelos = new Button("⚙  Modelos");
        btnModelos.setOnAction(this::abrirGestionModelos);

        Button btnExportar = new Button("💾 Exportar");
        btnExportar.setOnAction(this::exportarChat);

        Button btnLimpiar = new Button("🗑 Limpiar");
        btnLimpiar.setOnAction(e -> {
            chatBox.getChildren().clear();
            ia.limpiarHistorial();
            addMensajeSistema("Chat reiniciado.");
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox bar = new HBox(10, lblEstado, btnInstalarOllama, sp, cbContexto, cbModelo, btnModelos, btnExportar, btnLimpiar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.setStyle("-fx-background-color:#F0F4F8; -fx-background-radius:8;");
        return bar;
    }

    private void exportarChat(ActionEvent actionEvent) {
    }

    private void abrirGestionModelos(ActionEvent actionEvent) {
    }

    private ScrollPane buildChat(VBox chatBox) {
        chatBox.setPadding(new Insets(15));
        ScrollPane sp = new ScrollPane(chatBox);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: white; -fx-border-color:#DDD;");
        addMensajeSistema("¡Hola! Soy el asistente de Gráficas Mulberry. ¿En qué puedo ayudarte hoy?");
        return sp;
    }

    private VBox buildInputArea() {
        txtInput.setPromptText("Escribe aquí...");
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
        btnEnviar.setOnAction(this::enviar);

        // Chips de sugerencia (IAView3)
        FlowPane chips = new FlowPane(5, 5);
        String[] sugerencias = {"Precios de serigrafía", "¿Stock de papel?", "Estado de facturas"};
        for (String s : sugerencias) {
            Button b = new Button(s);
            b.setStyle("-fx-font-size:10; -fx-background-radius:15;");
            b.setOnAction(e -> { txtInput.setText(s); enviar(); });
            chips.getChildren().add(b);
        }

        return new VBox(5, chips, new HBox(8, txtInput, btnEnviar));
    }

    private void enviar(ActionEvent actionEvent) {
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
                        chunk -> Platform.runLater(() -> { // Cambio aquí
                            respuestaFull.append(chunk);
                            burbuja.textFlow().getChildren().clear();
                            burbuja.textFlow().getChildren().add(new Text(respuestaFull.toString()));
                            scrollAbajo();
                        }),
                        () -> Platform.runLater(() -> { // Cambio aquí
                            quitarSpinner(burbuja.container());
                            btnEnviar.setDisable(false);
                            SoundService.play(SoundService.Sound.NOTIFICATION);
                        }),
                        err -> Platform.runLater(() -> { // Cambio aquí
                            quitarSpinner(burbuja.container());
                            addMensajeSistema("Error: " + err);
                            btnEnviar.setDisable(false);
                        }),
                        modelo -> Platform.runLater(() -> cbModelo.setValue(modelo)) // Cambio aquí
                );
            } catch (Exception e) {
                Platform.runLater(() -> btnEnviar.setDisable(false)); // Cambio aquí
            }
        });
    }

    // --- MÉTODOS DE LÓGICA HEREDADOS DE IAView2 ---

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
        lblEstado.setText("🟢 Ollama Conectado");
        lblEstado.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
        cbModelo.getItems().setAll(ia.getModelosDisponibles());
    }

    private void configurarOllamaDesconectado() {
        lblEstado.setText("🔴 Ollama no encontrado");
        lblEstado.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        btnInstalarOllama.setVisible(true);
        btnInstalarOllama.setManaged(true);
    }

    private void exportarChat() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Chat");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Documento Word", "*.docx")
        );

        if (getScene() == null) return;
        File file = fc.showSaveDialog(getScene().getWindow());

        if (file != null) {
            try {
                // CORRECCIÓN: Ahora envolvemos las llamadas en un try-catch
                if (file.getName().endsWith(".pdf")) {
                    ChatExportService.exportarPDF(file, extraerMensajesChat());
                } else {
                    ChatExportService.exportarWord(file, extraerMensajesChat());
                }

                // Opcional: Sonido de éxito
                SoundService.play(SoundService.Sound.SUCCESS);

            } catch (Exception e) {
                // Manejo del error: Informamos al usuario
                e.printStackTrace(); // Para ver el log en consola
                SoundService.play(SoundService.Sound.ERROR);

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error al exportar");
                alert.setHeaderText("No se pudo guardar el archivo");
                alert.setContentText("Asegúrate de que el archivo no esté abierto en otro programa.\n" + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private List<ChatExportService.MensajeChat> extraerMensajesChat() {
        List<ChatExportService.MensajeChat> lista = new ArrayList<>();
        for (Node nodo : chatBox.getChildren()) {
            if (!(nodo instanceof HBox row) || row.getChildren().isEmpty()) continue;

            Node hijo = row.getChildren().get(0);

            // Mensaje de Usuario
            if (row.getAlignment() == Pos.CENTER_RIGHT && hijo instanceof TextFlow tf) {
                // CORRECCIÓN: Usar el campo "texto" para coincidir con el record del servicio
                lista.add(new ChatExportService.MensajeChat("usuario", extraerTextoDeTextFlow(tf)));
            }
            // Mensaje de IA
            else if (hijo instanceof VBox vbox && !vbox.getChildren().isEmpty()
                    && vbox.getChildren().get(0) instanceof TextFlow tf) {
                lista.add(new ChatExportService.MensajeChat("ia", extraerTextoDeTextFlow(tf)));
            }
            // Mensajes de Sistema
            else if (hijo instanceof Label l) {
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
        return sb.toString().trim(); // El .toString() aquí soluciona la regla java:S3063
    }

    // Helpers Visuales
    private void addBurbujaUsuario(String t) {
        Text text = new Text(t); text.setFill(Color.WHITE);
        TextFlow tf = new TextFlow(text);
        tf.setPadding(new Insets(10));
        tf.setStyle("-fx-background-color:" + AppConstants.COLOR_MULBERRY_HEX + "; -fx-background-radius:15 15 2 15;");
        HBox row = new HBox(tf); row.setAlignment(Pos.CENTER_RIGHT);
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void addMensajeSistema(String t) {
        Label l = new Label(t); l.setStyle("-fx-text-fill:#777; -fx-font-style:italic;");
        chatBox.getChildren().add(new HBox(l));
    }

    private record BurbujaIA(HBox row, TextFlow textFlow, VBox container) {}
    private BurbujaIA crearBurbujaIA() {
        TextFlow tf = new TextFlow();
        tf.setPadding(new Insets(10));
        tf.setStyle("-fx-background-color:#F1F1F1; -fx-background-radius:15 15 15 2;");
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
        Platform.runLater(this::setScrollPos);
    }

    private void setScrollPos() {
        scroll.setVvalue(1.0);
    }

    private void abrirGestionModelos() {
        // TODO: Implementar diálogo de descarga y borrado de modelos Ollama
        System.out.println("DEBUG: Solicitada apertura de gestión de modelos.");
    }

    private void abrirInstalador() {
        // Redirige al usuario a la web oficial para solucionar la falta de Ollama
        System.out.println("Redirigiendo a la descarga de Ollama...");
        // getHostServices().showDocument("https://ollama.com/download");
    }
// ... otros métodos como addBurbujaUsuario ...




}