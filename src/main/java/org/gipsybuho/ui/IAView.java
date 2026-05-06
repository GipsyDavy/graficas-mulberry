package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.gipsybuho.model.AccionERP;
import org.gipsybuho.service.AccionDispatcherService;
import org.gipsybuho.service.ChatExportService;
import org.gipsybuho.service.ChatExportService.MensajeChat;
import org.gipsybuho.service.ContextoERPService;
import org.gipsybuho.service.JsonInterceptorService;
import org.gipsybuho.service.OllamaManager;
import org.gipsybuho.service.OllamaService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IAView extends VBox {

    private final OllamaService      ia             = new OllamaService();
    private final ContextoERPService contextoService = new ContextoERPService();
    private VBox chatBox;
    private ScrollPane scroll;
    private TextArea txtInput;
    private Button btnEnviar;
    private Label lblEstado;
    private ComboBox<String> cbModelo;
    private Button btnInstalarOllama;
    private CheckBox cbContexto;

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

        Button btnExportar = new Button("💾 Exportar chat");
        btnExportar.setStyle(
            "-fx-background-color:#2C7A3C; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-padding:5 12; -fx-background-radius:4; -fx-cursor:hand;");
        btnExportar.setOnAction(e -> exportarChat());

        Button btnLimpiar = new Button("🗑 Limpiar chat");
        btnLimpiar.setOnAction(e -> { chatBox.getChildren().clear(); ia.limpiarHistorial(); });

        cbContexto = new CheckBox("📊 Datos ERP");
        cbContexto.setSelected(true);
        cbContexto.setStyle("-fx-font-size:12; -fx-text-fill:#374151;");
        cbContexto.setTooltip(new Tooltip(
            "Incluir datos actuales del ERP (presupuestos, facturas, pedidos…) " +
            "en el contexto del asistente para respuestas más precisas."));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(12, lblEstado, btnInstalarOllama, sp,
            cbContexto, new Label("Modelo:"), cbModelo, btnModelos, btnExportar, btnLimpiar);
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

    private record BurbujaIA(HBox row, TextFlow textFlow) {}

    private BurbujaIA crearBurbujaIA() {
        TextFlow tf = new TextFlow();
        tf.setMaxWidth(600);
        tf.setPadding(new Insets(10, 14, 10, 14));
        tf.setStyle("-fx-background-color:#F0E6EF; -fx-background-radius:16 16 16 4;");
        VBox container = new VBox(tf);
        HBox row = new HBox(container);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));
        return new BurbujaIA(row, tf);
    }

    private void enviar() {
        if (btnEnviar.isDisabled()) return;   // evita doble envío por chips durante streaming
        String texto = txtInput.getText().trim();
        if (texto.isBlank()) return;
        txtInput.clear();
        btnEnviar.setDisable(true);

        addBurbuja(texto, true);

        BurbujaIA burbuja = crearBurbujaIA();
        chatBox.getChildren().add(burbuja.row());
        TextFlow tf = burbuja.textFlow();
        Text cursor = new Text("▊");
        cursor.setFill(Color.web("#6B2D5E"));
        tf.getChildren().add(cursor);
        scrollAbajo();

        StringBuilder respuesta = new StringBuilder();
        boolean inyectarContexto = cbContexto.isSelected();

        Thread.ofVirtual().start(() -> {
            if (inyectarContexto) {
                try {
                    ia.setContextoERP(contextoService.construirContexto());
                } catch (Exception ex) {
                    ia.clearContextoERP();
                }
            } else {
                ia.clearContextoERP();
            }
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
                    SoundService.play(SoundService.Sound.NOTIFICATION);
                    scrollAbajo();
                    procesarRespuestaFinal(respuesta.toString(), tf);
                },
                error -> {
                    tf.getChildren().clear();
                    Text errText = new Text("⚠ " + error + "\n\nAsegúrate de que Ollama esté en ejecución:\n  1. Descarga Ollama desde ollama.com\n  2. Ejecuta: ollama pull llama3.2\n  3. Ollama se inicia automáticamente al ejecutarse");
                    errText.setFill(Color.RED);
                    tf.getChildren().add(errText);
                    SoundService.play(SoundService.Sound.ERROR);
                    btnEnviar.setDisable(false);
                    scrollAbajo();
                },
                modeloUsado -> {
                    if (!cbModelo.getItems().contains(modeloUsado)) cbModelo.getItems().add(modeloUsado);
                    cbModelo.setValue(modeloUsado);
                    lblEstado.setText("🟢 Ollama listo — " + modeloUsado
                        + (ia.isRoutingAutomatico() ? " [auto]" : "")
                        + (inyectarContexto ? " [+ERP]" : ""));
                }
            );
        });
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

    private void exportarChat() {
        List<MensajeChat> mensajes = extraerMensajesChat();
        boolean tieneContenido = mensajes.stream().anyMatch(m -> m.rol().equals("usuario") || m.rol().equals("ia"));
        if (!tieneContenido) {
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setHeaderText(null);
            alerta.setContentText("No hay mensajes en el chat para exportar.");
            alerta.showAndWait();
            return;
        }

        ChoiceDialog<String> dialogo = new ChoiceDialog<>("PDF", "PDF", "Word (.docx)");
        dialogo.setTitle("Exportar chat");
        dialogo.setHeaderText("Selecciona el formato de exportación");
        dialogo.setContentText("Formato:");
        Optional<String> resultado = dialogo.showAndWait();
        if (resultado.isEmpty()) return;

        boolean esPDF = resultado.get().equals("PDF");

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar chat");
        chooser.setInitialFileName("chat-asistente-ia-"
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        if (esPDF) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (*.docx)", "*.docx"));
        }

        Stage owner = getScene() != null ? (Stage) getScene().getWindow() : null;
        File archivo = chooser.showSaveDialog(owner);
        if (archivo == null) return;

        try {
            if (esPDF) {
                ChatExportService.exportarPDF(archivo, mensajes);
            } else {
                ChatExportService.exportarWord(archivo, mensajes);
            }
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText(null);
            ok.setContentText("Chat exportado correctamente:\n" + archivo.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("Error al exportar");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    private List<MensajeChat> extraerMensajesChat() {
        List<MensajeChat> lista = new ArrayList<>();
        for (javafx.scene.Node nodo : chatBox.getChildren()) {
            if (!(nodo instanceof HBox row)) continue;
            if (row.getChildren().isEmpty()) continue;
            javafx.scene.Node hijo = row.getChildren().get(0);

            if (row.getAlignment() == Pos.CENTER_RIGHT && hijo instanceof TextFlow tf) {
                String texto = extraerTextoDeTextFlow(tf);
                if (!texto.isBlank()) lista.add(new MensajeChat("usuario", texto));

            } else if (hijo instanceof VBox vbox && !vbox.getChildren().isEmpty()
                       && vbox.getChildren().get(0) instanceof TextFlow tf) {
                String texto = extraerTextoDeTextFlow(tf);
                if (!texto.isBlank()) lista.add(new MensajeChat("ia", texto));

            } else if (hijo instanceof TextFlow tf) {
                String texto = extraerTextoDeTextFlow(tf);
                if (!texto.isBlank()) lista.add(new MensajeChat("sistema", texto));
            }
        }
        return lista;
    }

    private String extraerTextoDeTextFlow(TextFlow tf) {
        StringBuilder sb = new StringBuilder();
        for (javafx.scene.Node n : tf.getChildren()) {
            if (n instanceof Text t) sb.append(t.getText());
        }
        return sb.toString().trim();
    }

    // ── Interceptor JSON ──────────────────────────────────────────────────────

    private void procesarRespuestaFinal(String respuestaCompleta, TextFlow burbuja) {
        JsonInterceptorService.Resultado resultado =
            JsonInterceptorService.interceptar(respuestaCompleta);

        if (resultado.tieneAccion()) {
            String textoLimpio = resultado.textoLimpio();
            burbuja.getChildren().clear();
            burbuja.getChildren().add(new Text(
                textoLimpio.isBlank() ? "Aquí tienes la acción detectada:" : textoLimpio));
            mostrarPanelConfirmacion(resultado.accion().get());
        } else {
            addSugerenciasContextuales(respuestaCompleta);
        }
    }

    private void addSugerenciasContextuales(String texto) {
        String lower = texto.toLowerCase();

        // Detectar hasta 2 acciones relevantes según el contenido de la respuesta
        record Sugerencia(String etiqueta, String mensaje) {}
        List<Sugerencia> sugerencias = new ArrayList<>();

        if (lower.contains("presupuest"))
            sugerencias.add(new Sugerencia("📋 Crear presupuesto", "Crea un presupuesto con los datos anteriores"));
        if (lower.contains("factur") && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("🧾 Generar factura", "Genera una factura con esos datos"));
        if (lower.contains("pedido") && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("🛒 Crear pedido", "Crea un pedido con esos datos"));
        if (lower.contains("cliente") && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("👤 Crear cliente", "Crea ese cliente en el sistema"));
        if ((lower.contains("stock") || lower.contains("material")) && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("🗃 Ver materiales", "¿Qué materiales están bajo stock?"));
        if ((lower.contains("nómin") || lower.contains("nomina")) && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("💰 Calcular nómina", "Calcula la nómina con esos datos"));
        if ((lower.contains("calendario") || lower.contains("evento") || lower.contains("cita"))
                && sugerencias.size() < 2)
            sugerencias.add(new Sugerencia("📅 Agendar evento", "Agéndalo en el calendario"));

        if (sugerencias.isEmpty()) return;

        Label lbl = new Label("¿Quieres que lo haga ahora?");
        lbl.setStyle("-fx-font-size:11; -fx-text-fill:#6B2D5E; -fx-font-style:italic;");

        FlowPane chips = new FlowPane(8, 4);
        chips.getChildren().add(lbl);
        for (Sugerencia s : sugerencias) {
            Button chip = new Button(s.etiqueta());
            chip.setStyle(
                "-fx-background-color:#F0E6EF; -fx-text-fill:#6B2D5E; -fx-font-size:11; " +
                "-fx-padding:4 12; -fx-border-radius:20; -fx-background-radius:20; -fx-cursor:hand;");
            chip.setOnAction(e -> { txtInput.setText(s.mensaje()); enviar(); });
            chips.getChildren().add(chip);
        }

        HBox row = new HBox(chips);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 8, 4, 8));
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void mostrarPanelConfirmacion(AccionERP accion) {
        ConfirmacionAccionPanel panel = new ConfirmacionAccionPanel(
            accion,
            r -> addMensajeFeedback(r.mensaje(), r.detalle(), r.exito()),
            () -> addMensajeFeedback("Acción cancelada", "El usuario canceló la acción antes de ejecutarla.", false)
        );

        HBox wrapper = new HBox(panel);
        wrapper.setPadding(new Insets(6, 8, 6, 8));
        wrapper.setAlignment(Pos.CENTER_LEFT);
        chatBox.getChildren().add(wrapper);
        scrollAbajo();
    }

    private void addMensajeFeedback(String titulo, String detalle, boolean esExito) {
        String colorTexto = esExito ? "#1A5C2A" : "#7F1D1D";
        String estiloCarta = esExito
            ? "-fx-background-color:#DCFCE7; -fx-background-radius:10; " +
              "-fx-border-color:#86EFAC; -fx-border-radius:10; -fx-border-width:1;"
            : "-fx-background-color:#FEE2E2; -fx-background-radius:10; " +
              "-fx-border-color:#FCA5A5; -fx-border-radius:10; -fx-border-width:1;";

        Label lblTitulo = new Label(titulo);
        lblTitulo.setWrapText(true);
        lblTitulo.setStyle(String.format(
            "-fx-font-weight:bold; -fx-font-size:13; -fx-text-fill:%s;", colorTexto));

        VBox card = new VBox(4, lblTitulo);
        card.setMaxWidth(620);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(estiloCarta);

        if (detalle != null && !detalle.isBlank()) {
            Label lblDetalle = new Label(detalle);
            lblDetalle.setWrapText(true);
            lblDetalle.setStyle(String.format(
                "-fx-font-size:11; -fx-text-fill:%s;", colorTexto));
            card.getChildren().add(lblDetalle);
        }

        HBox row = new HBox(card);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 8, 2, 8));
        chatBox.getChildren().add(row);
        scrollAbajo();
    }

    private void scrollAbajo() {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
