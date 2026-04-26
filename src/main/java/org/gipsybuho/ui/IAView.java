package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.gipsybuho.service.OllamaManager;
import org.gipsybuho.service.OllamaService;

import java.util.List;

public class IAView extends VBox {

    private final OllamaService ia = new OllamaService();
    private VBox chatBox;
    private ScrollPane scroll;
    private TextArea txtInput;
    private Button btnEnviar;
    private Label lblEstado;
    private ComboBox<String> cbModelo;
    private Button btnInstalarOllama;

    public IAView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Asistente IA Local");
        titulo.getStyleClass().add("view-title");

        Label sub = new Label("Powered by Ollama — IA 100% local, sin enviar datos a Internet");
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildEstadoBar(), buildChat(), buildInputArea());
        VBox.setVgrow(scroll, Priority.ALWAYS);

        verificarOllama();
    }

    private HBox buildEstadoBar() {
        lblEstado = new Label("⏳ Verificando conexión con Ollama...");
        cbModelo = new ComboBox<>();
        cbModelo.setPromptText("Modelo IA");
        cbModelo.setOnAction(e -> {
            if (cbModelo.getValue() != null) ia.setModeloActual(cbModelo.getValue());
        });

        btnInstalarOllama = new Button("⬇  Instalar Ollama");
        btnInstalarOllama.setStyle(
            "-fx-background-color: #6B2D5E; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 5 14; -fx-background-radius: 4; -fx-cursor: hand;");
        btnInstalarOllama.setVisible(false);
        btnInstalarOllama.setManaged(false);
        btnInstalarOllama.setOnAction(e -> abrirInstalador());

        Button btnModelos = new Button("⚙  Modelos");
        btnModelos.setStyle(
            "-fx-background-color:#5D4A7A; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-padding:5 12; -fx-background-radius:4; -fx-cursor:hand;");
        btnModelos.setOnAction(e -> abrirGestionModelos());

        Button btnLimpiar = new Button("🗑 Limpiar chat");
        btnLimpiar.setOnAction(e -> chatBox.getChildren().clear());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(12, lblEstado, btnInstalarOllama, sp, new Label("Modelo:"), cbModelo, btnModelos, btnLimpiar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.setStyle("-fx-background-color:#F0F4F8; -fx-border-radius:6; -fx-background-radius:6;");
        return bar;
    }

    private void abrirInstalador() {
        Stage owner = getScene() != null ? (Stage) getScene().getWindow() : null;
        OllamaInstallerDialog dlg = new OllamaInstallerDialog(owner);
        dlg.showAndWait();
        if (dlg.isInstalacionCompleta()) {
            btnInstalarOllama.setVisible(false);
            btnInstalarOllama.setManaged(false);
            lblEstado.setText("⏳ Verificando Ollama...");
            lblEstado.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            verificarOllama();
        }
    }

    private ScrollPane buildChat() {
        chatBox = new VBox(8);
        chatBox.setPadding(new Insets(12));
        chatBox.setFillWidth(true);

        scroll = new ScrollPane(chatBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white; -fx-border-color:#E2E8F0;");
        scroll.setPrefHeight(400);

        // Mensaje de bienvenida
        addMensajeSistema("¡Hola! Soy el asistente IA de Gráficas Mulberry. Puedo ayudarte con presupuestos, precios, materiales, nóminas y mucho más.\n\nEscribe tu pregunta abajo para comenzar.");

        return scroll;
    }

    private VBox buildInputArea() {
        txtInput = new TextArea();
        txtInput.setPromptText("Escribe tu pregunta aquí... (Enter para enviar, Shift+Enter para nueva línea)");
        txtInput.setPrefRowCount(3);
        txtInput.setWrapText(true);
        HBox.setHgrow(txtInput, Priority.ALWAYS);

        txtInput.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                enviar();
            }
        });

        btnEnviar = new Button("Enviar ▶");
        btnEnviar.setStyle("-fx-background-color:#6B2D5E;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 20;");
        btnEnviar.setOnAction(e -> enviar());
        btnEnviar.setMinHeight(80);

        VBox sugerencias = buildSugerencias();

        HBox inputRow = new HBox(8, txtInput, btnEnviar);
        inputRow.setAlignment(Pos.CENTER);

        return new VBox(6, sugerencias, inputRow);
    }

    private VBox buildSugerencias() {
        Label lbl = new Label("Sugerencias rápidas:");
        lbl.setStyle("-fx-font-size:11; -fx-text-fill:#666;");

        FlowPane chips = new FlowPane(6, 4);
        String[] preguntas = {
            "¿Cuánto cuesta imprimir 50 camisetas a 2 colores?",
            "¿Qué materiales están bajo stock?",
            "Explica las deducciones de la nómina",
            "¿Cuál es la diferencia entre DTF y serigrafía?",
            "¿Cómo calcular el precio de un pedido de bordado?"
        };
        for (String p : preguntas) {
            Button chip = new Button(p.length() > 45 ? p.substring(0, 42) + "..." : p);
            chip.setStyle("-fx-background-color:#F0E6EF;-fx-text-fill:#6B2D5E;-fx-font-size:11;-fx-padding:4 10;-fx-border-radius:20;-fx-background-radius:20;");
            chip.setOnAction(e -> { txtInput.setText(p); enviar(); });
            chips.getChildren().add(chip);
        }

        VBox box = new VBox(4, lbl, chips);
        return box;
    }

    private void enviar() {
        String texto = txtInput.getText().trim();
        if (texto.isBlank()) return;
        txtInput.clear();
        btnEnviar.setDisable(true);

        addBurbuja(texto, true);

        // Burbuja IA en construcción
        HBox burbujaIA = crearBurbujaIA();
        chatBox.getChildren().add(burbujaIA);
        TextFlow tf = (TextFlow) ((VBox)burbujaIA.getChildren().get(0)).getChildren().get(0);
        Text cursor = new Text("▊");
        cursor.setFill(Color.web("#6B2D5E"));
        tf.getChildren().add(cursor);
        scrollAbajo();

        StringBuilder respuesta = new StringBuilder();

        ia.chatStreaming(
            texto,
            chunk -> {
                respuesta.append(chunk);
                tf.getChildren().remove(cursor);
                tf.getChildren().clear();
                tf.getChildren().add(new Text(respuesta.toString()));
                tf.getChildren().add(cursor);
                scrollAbajo();
            },
            () -> {
                tf.getChildren().remove(cursor);
                btnEnviar.setDisable(false);
                scrollAbajo();
            },
            error -> {
                tf.getChildren().clear();
                Text errText = new Text("⚠ " + error + "\n\nAsegúrate de que Ollama esté en ejecución:\n  1. Descarga Ollama desde ollama.com\n  2. Ejecuta: ollama pull llama3.2\n  3. Ollama se inicia automáticamente al ejecutarse");
                errText.setFill(Color.RED);
                tf.getChildren().add(errText);
                btnEnviar.setDisable(false);
                scrollAbajo();
            }
        );
    }

    private void addBurbuja(String texto, boolean esUsuario) {
        Text t = new Text(texto);
        t.setFill(esUsuario ? Color.WHITE : Color.web("#1A1A2E"));
        TextFlow tf = new TextFlow(t);
        tf.setMaxWidth(500);
        tf.setPadding(new Insets(10, 14, 10, 14));
        tf.setStyle(esUsuario
            ? "-fx-background-color:#6B2D5E; -fx-background-radius:16 16 4 16;"
            : "-fx-background-color:#F0E6EF; -fx-background-radius:16 16 16 4;");

        HBox row = new HBox(tf);
        row.setAlignment(esUsuario ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private HBox crearBurbujaIA() {
        TextFlow tf = new TextFlow();
        tf.setMaxWidth(600);
        tf.setPadding(new Insets(10, 14, 10, 14));
        tf.setStyle("-fx-background-color:#F0E6EF; -fx-background-radius:16 16 16 4;");

        VBox container = new VBox(tf);
        HBox row = new HBox(container);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));
        return row;
    }

    private void addMensajeSistema(String mensaje) {
        Text t = new Text(mensaje);
        t.setFill(Color.web("#555"));
        TextFlow tf = new TextFlow(t);
        tf.setPadding(new Insets(12));
        tf.setStyle("-fx-background-color:#FAFAFA; -fx-border-color:#E2E8F0; -fx-border-radius:8; -fx-background-radius:8;");

        HBox row = new HBox(tf);
        row.setPadding(new Insets(4));
        chatBox.getChildren().add(row);
    }

    private void abrirGestionModelos() {
        Stage owner = getScene() != null ? (Stage) getScene().getWindow() : null;
        ModelosGestionDialog dlg = new ModelosGestionDialog(owner, ia);
        dlg.showAndWait();
        if (dlg.isHuboCambios()) {
            // Refrescar la lista de modelos en el ComboBox
            Thread.ofVirtual().start(() -> {
                java.util.List<String> modelos = ia.getModelosDisponibles();
                Platform.runLater(() -> {
                    String actual = cbModelo.getValue();
                    cbModelo.getItems().setAll(modelos);
                    if (actual != null && modelos.contains(actual)) {
                        cbModelo.setValue(actual);
                    } else if (!modelos.isEmpty()) {
                        cbModelo.setValue(modelos.get(0));
                        ia.setModeloActual(modelos.get(0));
                    }
                });
            });
        }
    }

    private void verificarOllama() {
        Thread.ofVirtual().start(() -> {
            Platform.runLater(() -> {
                lblEstado.setText("⏳ Iniciando Ollama...");
                lblEstado.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            });

            // Reintentar hasta 12 s para dar tiempo al arranque de OllamaManager
            boolean disponible = false;
            for (int intento = 0; intento < 12 && !disponible; intento++) {
                disponible = ia.isDisponible();
                if (!disponible) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }

            final boolean ok = disponible;
            final List<String> modelos = ok ? ia.getModelosDisponibles() : List.of();

            Platform.runLater(() -> {
                if (ok) {
                    cbModelo.getItems().setAll(modelos);
                    if (!modelos.isEmpty()) {
                        String modelo = modelos.stream()
                            .filter(m -> m.contains("llama") || m.contains("phi") || m.contains("mistral"))
                            .findFirst().orElse(modelos.get(0));
                        cbModelo.setValue(modelo);
                        ia.setModeloActual(modelo);
                        lblEstado.setText("🟢 Ollama listo — " + modelo);
                    } else {
                        lblEstado.setText("🟡 Ollama conectado — Descarga un modelo: ollama pull llama3.2");
                    }
                    lblEstado.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                } else {
                    boolean instalado = OllamaManager.isInstalled();
                    if (instalado) {
                        lblEstado.setText("🟡 Ollama instalado pero no responde — reiniciando...");
                        lblEstado.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
                        Thread.ofVirtual().start(() -> { OllamaManager.startIfNeeded(); verificarOllama(); });
                    } else {
                        lblEstado.setText("🔴 Ollama no instalado");
                        lblEstado.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                        btnInstalarOllama.setVisible(true);
                        btnInstalarOllama.setManaged(true);
                        addMensajeSistema(
                            "⚠  Ollama no está instalado en este equipo.\n\n" +
                            "Haz clic en «⬇ Instalar Ollama» en la barra superior para instalarlo " +
                            "automáticamente. El proceso descargará el instalador oficial y el modelo " +
                            "de IA. Solo necesitas conexión a Internet.");
                    }
                }
            });
        });
    }

    private void scrollAbajo() {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
