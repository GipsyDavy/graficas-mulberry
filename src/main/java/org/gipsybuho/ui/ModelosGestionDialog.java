package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gipsybuho.service.OllamaService;

import java.util.List;

public class ModelosGestionDialog extends Stage {

    private static final String[][] MODELOS_POPULARES = {
        {"llama3.2",    "~2 GB — Rápido y equilibrado (recomendado)"},
        {"llama3.1",    "~4.7 GB — Más preciso que llama3.2"},
        {"mistral",     "~4.1 GB — Muy eficiente en español"},
        {"phi4",        "~9.1 GB — Microsoft, excelente razonamiento"},
        {"phi3.5",      "~2.2 GB — Microsoft, ligero y rápido"},
        {"qwen2.5",     "~4.7 GB — Multilingüe, muy bueno en español"},
        {"gemma3",      "~3.3 GB — Google, eficiente"},
        {"deepseek-r1", "~4.7 GB — Razonamiento avanzado"},
    };

    private final OllamaService ia;
    private VBox modelosInstalados;
    private TextField txtModelo;
    private ProgressBar progressBar;
    private Label lblProgreso;
    private Button btnDescargar;
    private boolean huboCambios = false;

    public ModelosGestionDialog(Stage owner, OllamaService ia) {
        this.ia = ia;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Gestionar modelos de IA — Gráficas Mulberry");
        setResizable(true);
        setMinWidth(580);
        setMinHeight(480);

        ScrollPane scroll = new ScrollPane(buildUI());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");

        Scene scene = new Scene(scroll, 640, 640);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        setScene(scene);
        cargarModelosInstalados();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private VBox buildUI() {
        Label titulo = new Label("🤖  Gestionar modelos de IA");
        titulo.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        Label desc = new Label(
            "Visualiza los modelos instalados, elimínalos para liberar espacio, " +
            "o descarga nuevos modelos de Ollama.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#666; -fx-font-size:12px;");

        VBox root = new VBox(14,
            titulo, desc,
            new Separator(),
            buildSeccionInstalados(),
            new Separator(),
            buildSeccionDescargar(),
            new Separator(),
            buildBotonesBottom()
        );
        root.setPadding(new Insets(24));
        return root;
    }

    private VBox buildSeccionInstalados() {
        Label lblTitulo = new Label("Modelos instalados");
        lblTitulo.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        Button btnRefrescar = new Button("↻ Actualizar lista");
        btnRefrescar.setStyle("-fx-font-size:11px; -fx-padding:3 10; -fx-background-radius:4;");
        btnRefrescar.setOnAction(e -> cargarModelosInstalados());

        HBox header = new HBox(8, lblTitulo, new Region(), btnRefrescar);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        modelosInstalados = new VBox(6);
        modelosInstalados.setPadding(new Insets(10));
        modelosInstalados.setStyle(
            "-fx-background-color:#F8F8F8; -fx-border-color:#DDD; " +
            "-fx-border-radius:6; -fx-background-radius:6;");
        modelosInstalados.setMinHeight(80);

        return new VBox(8, header, modelosInstalados);
    }

    private VBox buildSeccionDescargar() {
        Label lblTitulo = new Label("Descargar nuevo modelo");
        lblTitulo.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        Label lblPop = new Label("Modelos populares — haz clic para seleccionar:");
        lblPop.setStyle("-fx-font-size:11px; -fx-text-fill:#777;");

        FlowPane chips = new FlowPane(8, 6);
        for (String[] m : MODELOS_POPULARES) {
            String nombre = m[0];
            Button chip = new Button(nombre);
            chip.setTooltip(new Tooltip(m[1]));
            chip.setStyle(
                "-fx-background-color:#F0E6EF; -fx-text-fill:#6B2D5E; " +
                "-fx-font-size:11px; -fx-padding:4 12; -fx-background-radius:20; -fx-cursor:hand;");
            chip.setOnAction(e -> txtModelo.setText(nombre));
            chips.getChildren().add(chip);
        }

        Label lblCustom = new Label("O escribe el nombre del modelo:");
        lblCustom.setStyle("-fx-font-size:11px; -fx-text-fill:#555;");

        txtModelo = new TextField();
        txtModelo.setPromptText("ej. llama3.2, mistral, phi4, gemma3...");
        HBox.setHgrow(txtModelo, Priority.ALWAYS);

        btnDescargar = new Button("⬇  Descargar");
        btnDescargar.setStyle(
            "-fx-background-color:#6B2D5E; -fx-text-fill:white; " +
            "-fx-font-weight:bold; -fx-padding:8 20; -fx-background-radius:4;");
        btnDescargar.setDisable(true);
        btnDescargar.setOnAction(e -> iniciarDescarga());
        txtModelo.textProperty().addListener((obs, o, n) ->
            btnDescargar.setDisable(n.trim().isEmpty() || btnDescargar.isDisabled() && n.equals(o)));

        txtModelo.textProperty().addListener((obs, o, n) ->
            btnDescargar.setDisable(n.trim().isEmpty()));

        HBox inputRow = new HBox(8, txtModelo, btnDescargar);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(14);
        progressBar.setStyle("-fx-accent:#6B2D5E;");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        lblProgreso = new Label();
        lblProgreso.setWrapText(true);
        lblProgreso.setStyle("-fx-font-size:11px; -fx-text-fill:#6B2D5E;");
        lblProgreso.setVisible(false);
        lblProgreso.setManaged(false);

        return new VBox(8, lblTitulo, lblPop, chips, lblCustom, inputRow, progressBar, lblProgreso);
    }

    private HBox buildBotonesBottom() {
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-padding:8 24; -fx-background-radius:4;");
        btnCerrar.setOnAction(e -> close());
        HBox box = new HBox(btnCerrar);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ── Modelos instalados ────────────────────────────────────────────────────

    private void cargarModelosInstalados() {
        modelosInstalados.getChildren().clear();
        Label cargando = new Label("⏳ Cargando modelos...");
        cargando.setStyle("-fx-text-fill:#999; -fx-font-size:12px;");
        modelosInstalados.getChildren().add(cargando);

        Thread.ofVirtual().start(() -> {
            List<OllamaService.ModelInfo> modelos = ia.getModelosConDetalles();
            Platform.runLater(() -> {
                modelosInstalados.getChildren().clear();
                if (modelos.isEmpty()) {
                    Label vacio = new Label("No hay modelos instalados. Descarga uno en la sección inferior.");
                    vacio.setStyle("-fx-text-fill:#999; -fx-font-size:12px; -fx-font-style:italic;");
                    modelosInstalados.getChildren().add(vacio);
                } else {
                    for (OllamaService.ModelInfo m : modelos) {
                        modelosInstalados.getChildren().add(buildFilaModelo(m));
                    }
                }
            });
        });
    }

    private HBox buildFilaModelo(OllamaService.ModelInfo modelo) {
        Label lblNombre = new Label(modelo.nombre);
        lblNombre.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        Label lblTam = new Label(modelo.tamano());
        lblTam.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        VBox info = new VBox(2, lblNombre, lblTam);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnEliminar = new Button("🗑  Eliminar");
        btnEliminar.setStyle(
            "-fx-background-color:#E74C3C; -fx-text-fill:white; " +
            "-fx-font-size:11px; -fx-padding:6 14; -fx-background-radius:4; -fx-cursor:hand;");
        btnEliminar.setOnAction(e -> confirmarEliminar(modelo.nombre));

        HBox fila = new HBox(12, info, btnEliminar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(10, 12, 10, 12));
        fila.setStyle(
            "-fx-background-color:white; -fx-border-color:#E8E8E8; " +
            "-fx-border-radius:4; -fx-background-radius:4;");
        return fila;
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    private void confirmarEliminar(String nombre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar modelo");
        confirm.setHeaderText("¿Eliminar el modelo «" + nombre + "»?");
        confirm.setContentText(
            "Se liberará el espacio en disco ocupado por el modelo.\n" +
            "Podrás volver a descargarlo en cualquier momento.");
        if (getScene() != null) confirm.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) ejecutarEliminar(nombre);
        });
    }

    private void ejecutarEliminar(String nombre) {
        Thread.ofVirtual().start(() -> {
            try {
                ia.eliminarModelo(nombre);
                huboCambios = true;
                Platform.runLater(() -> {
                    cargarModelosInstalados();
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Modelo «" + nombre + "» eliminado correctamente.", ButtonType.OK);
                    if (getScene() != null) ok.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    ok.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR,
                        "No se pudo eliminar el modelo:\n" + e.getMessage(), ButtonType.OK);
                    if (getScene() != null) err.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    err.showAndWait();
                });
            }
        });
    }

    // ── Descargar ─────────────────────────────────────────────────────────────

    private static boolean modeloNombreValido(String nombre) {
        return nombre.matches("[a-zA-Z0-9][a-zA-Z0-9._:\\-]*");
    }

    private void iniciarDescarga() {
        String nombre = txtModelo.getText().trim();
        if (nombre.isBlank()) return;
        if (!modeloNombreValido(nombre)) {
            lblProgreso.setText("❌ Nombre de modelo no válido. Solo se permiten letras, números, puntos, guiones y ':'");
            mostrarProgreso(true);
            return;
        }

        btnDescargar.setDisable(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        mostrarProgreso(true);
        lblProgreso.setText("Conectando con Ollama...");

        ia.pullModeloStreaming(
            nombre,
            estado -> lblProgreso.setText("Estado: " + estado),
            prog -> {
                double pct  = prog[0];
                double comp = prog[1];
                double tot  = prog[2];
                progressBar.setProgress(pct);
                lblProgreso.setText(String.format(
                    "Descargando: %.0f / %.0f MB  (%.0f%%)",
                    comp / 1_048_576, tot / 1_048_576, pct * 100));
            },
            () -> {
                progressBar.setProgress(1.0);
                lblProgreso.setText("✅ Modelo «" + nombre + "» descargado correctamente.");
                btnDescargar.setDisable(false);
                txtModelo.clear();
                huboCambios = true;
                cargarModelosInstalados();
            },
            error -> {
                progressBar.setProgress(0);
                lblProgreso.setText("❌ Error: " + error);
                btnDescargar.setDisable(false);
            }
        );
    }

    private void mostrarProgreso(boolean visible) {
        progressBar.setVisible(visible);
        progressBar.setManaged(visible);
        lblProgreso.setVisible(visible);
        lblProgreso.setManaged(visible);
    }

    public boolean isHuboCambios() { return huboCambios; }
}
