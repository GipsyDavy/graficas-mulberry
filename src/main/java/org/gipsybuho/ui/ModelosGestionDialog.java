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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelosGestionDialog extends Stage {

    private record ModeloInfo(
        String tamano,
        int ramGB,
        String descripcion,
        String serigrafia,
        boolean recomendado
    ) {}

    private static final Map<String, ModeloInfo> CATALOGO = new LinkedHashMap<>();
    static {
        CATALOGO.put("llama4", new ModeloInfo(
            "~5.6 GB", 12,
            "Meta Llama 4 Scout. Modelo multimodal que comprende texto e imágenes. " +
            "Muy capaz en razonamiento y análisis de documentos complejos.",
            "Útil si necesitas analizar imágenes de diseños o bocetos adjuntos. " +
            "Requiere un equipo potente para responder con fluidez.",
            false));

        CATALOGO.put("llama3.2", new ModeloInfo(
            "~2.0 GB", 8,
            "Meta Llama 3.2. Modelo rápido y equilibrado, ideal para tareas del día a día. " +
            "Bajo consumo de recursos y buena comprensión del español.",
            "⭐ Recomendado como primer modelo para el sector. Ágil para gestionar " +
            "pedidos, facturas y albaranes sin ralentizar el equipo.",
            true));

        CATALOGO.put("llama3.1", new ModeloInfo(
            "~4.7 GB", 10,
            "Meta Llama 3.1. Más preciso que la versión 3.2. Mayor capacidad de análisis " +
            "y razonamiento en consultas elaboradas.",
            "Bueno para presupuestos complejos, cálculo de costes y consultas " +
            "detalladas sobre materiales y técnicas.",
            false));

        CATALOGO.put("mistral", new ModeloInfo(
            "~4.1 GB", 8,
            "Mistral 7B. Desarrollado en Europa, muy eficiente en español y otros idiomas. " +
            "Excelente relación calidad-tamaño, rápido y preciso.",
            "⭐ Ideal para redactar albaranes, facturas y comunicaciones con clientes. " +
            "Responde con naturalidad en español, perfecto para el trato comercial.",
            true));

        CATALOGO.put("phi4", new ModeloInfo(
            "~9.1 GB", 16,
            "Microsoft Phi-4. Especializado en razonamiento matemático y lógico avanzado. " +
            "Destaca en cálculos precisos y análisis estructurado.",
            "Excelente para calcular costes de producción, márgenes de beneficio y " +
            "optimizar tarifas. Requiere al menos 16 GB de RAM.",
            false));

        CATALOGO.put("phi3.5", new ModeloInfo(
            "~2.2 GB", 6,
            "Microsoft Phi-3.5. Versión muy ligera, diseñada para equipos con recursos " +
            "limitados. Rápido en consultas cortas y directas.",
            "Perfecto si el equipo tiene poca RAM. Útil para consultas rápidas del ERP " +
            "y navegación entre módulos.",
            false));

        CATALOGO.put("qwen3", new ModeloInfo(
            "~5.2 GB", 10,
            "Qwen 3 (Alibaba). Último modelo de la familia Qwen. Razonamiento avanzado, " +
            "multilingüe y muy bueno en español. Excelente para tareas analíticas.",
            "⭐ Muy bueno para análisis de materiales, control de stock y gestión " +
            "de datos complejos del taller. Destaca en razonamiento estructurado.",
            true));

        CATALOGO.put("qwen2.5", new ModeloInfo(
            "~4.7 GB", 8,
            "Qwen 2.5 (Alibaba). Multilingüe avanzado con muy buen rendimiento en español. " +
            "Equilibrado entre velocidad y precisión.",
            "Bueno para comunicación con clientes, generación de documentos y " +
            "consultas en castellano.",
            false));

        CATALOGO.put("gemma3", new ModeloInfo(
            "~3.3 GB", 8,
            "Google Gemma 3. Modelo eficiente de Google con buen equilibrio entre " +
            "velocidad y calidad. Compacto y versátil.",
            "Útil para consultas generales del ERP y gestión básica de módulos. " +
            "Buena opción intermedia.",
            false));

        CATALOGO.put("deepseek-r1", new ModeloInfo(
            "~4.7 GB", 10,
            "DeepSeek R1. Especializado en razonamiento lógico paso a paso. " +
            "Muy preciso en análisis y desglose de problemas complejos.",
            "Excelente para optimización de costes, análisis de rentabilidad por técnica " +
            "y detección de márgenes en presupuestos.",
            false));
    }

    private final OllamaService ia;
    private final long ramSistemaGB = detectarRamGB();
    private VBox modelosInstalados;
    private TextField txtModelo;
    private ProgressBar progressBar;
    private Label lblProgreso;
    private Button btnDescargar;
    private VBox descPanel;
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

        Scene scene = new Scene(scroll, 660, 700);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        setScene(scene);
        cargarModelosInstalados();
    }

    // ── RAM del sistema ───────────────────────────────────────────────────────

    private static long detectarRamGB() {
        try {
            var osBean = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return osBean.getTotalMemorySize() / (1024L * 1024 * 1024);
        } catch (Exception e) {
            return 0;
        }
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

        Label lblPop = new Label("Haz clic en un modelo para ver su descripción y compatibilidad con tu equipo:");
        lblPop.setStyle("-fx-font-size:11px; -fx-text-fill:#777;");

        FlowPane chips = new FlowPane(8, 6);
        for (String nombre : CATALOGO.keySet()) {
            Button chip = new Button(nombre);
            chip.setStyle(
                "-fx-background-color:#F0E6EF; -fx-text-fill:#6B2D5E; " +
                "-fx-font-size:11px; -fx-padding:4 12; -fx-background-radius:20; -fx-cursor:hand;");
            chip.setOnAction(e -> {
                txtModelo.setText(nombre);
                actualizarDescripcion(nombre);
            });
            chips.getChildren().add(chip);
        }

        // Panel de descripción del modelo seleccionado
        descPanel = new VBox(8);
        descPanel.setVisible(false);
        descPanel.setManaged(false);
        descPanel.setPadding(new Insets(14));
        descPanel.setStyle(
            "-fx-background-color:#F9F3F8; -fx-border-color:#C9A0BC; " +
            "-fx-border-radius:8; -fx-background-radius:8;");

        Label lblCustom = new Label("O escribe el nombre del modelo:");
        lblCustom.setStyle("-fx-font-size:11px; -fx-text-fill:#555;");

        txtModelo = new TextField();
        txtModelo.setPromptText("ej. llama3.2, mistral, phi4, qwen3...");
        HBox.setHgrow(txtModelo, Priority.ALWAYS);

        btnDescargar = new Button("⬇  Descargar");
        btnDescargar.setStyle(
            "-fx-background-color:#6B2D5E; -fx-text-fill:white; " +
            "-fx-font-weight:bold; -fx-padding:8 20; -fx-background-radius:4;");
        btnDescargar.setDisable(true);
        btnDescargar.setOnAction(e -> iniciarDescarga());

        txtModelo.textProperty().addListener((obs, o, n) -> {
            btnDescargar.setDisable(n.trim().isEmpty());
            String match = CATALOGO.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(n.trim()) || k.startsWith(n.trim().toLowerCase()))
                .findFirst().orElse(null);
            if (match != null) actualizarDescripcion(match);
            else ocultarDescripcion();
        });

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

        return new VBox(8, lblTitulo, lblPop, chips, descPanel, lblCustom, inputRow, progressBar, lblProgreso);
    }

    private HBox buildBotonesBottom() {
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-padding:8 24; -fx-background-radius:4;");
        btnCerrar.setOnAction(e -> close());
        HBox box = new HBox(btnCerrar);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ── Panel de descripción del modelo ──────────────────────────────────────

    private void actualizarDescripcion(String nombre) {
        ModeloInfo info = CATALOGO.get(nombre);
        if (info == null) { ocultarDescripcion(); return; }

        descPanel.getChildren().clear();

        // Nombre + tamaño en disco
        Label lblNombre = new Label(nombre + "   " + info.tamano());
        lblNombre.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#4A1A3E;");

        // Requisito de RAM y estado de compatibilidad
        String ramIcono;
        String ramColor;
        if (ramSistemaGB == 0) {
            ramIcono = "ℹ"; ramColor = "#888";
        } else if (ramSistemaGB >= info.ramGB()) {
            ramIcono = "✔"; ramColor = "#1E8449";
        } else if (ramSistemaGB >= info.ramGB() - 2) {
            ramIcono = "⚠"; ramColor = "#D35400";
        } else {
            ramIcono = "✗"; ramColor = "#C0392B";
        }
        Label lblRam = new Label(ramIcono + "  RAM mínima recomendada: " + info.ramGB() + " GB");
        lblRam.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + ramColor + ";");

        String textoEquipo = ramSistemaGB > 0
            ? "Tu equipo tiene ~" + ramSistemaGB + " GB de RAM"
            : "RAM del equipo no detectada";
        Label lblEquipo = new Label(textoEquipo);
        lblEquipo.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");

        HBox ramRow = new HBox(18, lblRam, lblEquipo);
        ramRow.setAlignment(Pos.CENTER_LEFT);

        // Descripción general
        Label lblDesc = new Label(info.descripcion());
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size:12px; -fx-text-fill:#333;");

        Separator sep = new Separator();

        // Nota específica para el sector serigrafía
        Label lblSeriTitulo = new Label("🖨  Uso en serigrafia / artes gráficas:");
        lblSeriTitulo.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6B2D5E;");
        Label lblSeri = new Label(info.serigrafia());
        lblSeri.setWrapText(true);
        lblSeri.setStyle("-fx-font-size:12px; -fx-text-fill:#5A2050;");

        // Recomendación final basada en RAM del equipo
        Label lblRecom = construirEtiquetaRecomendacion(info);

        descPanel.getChildren().addAll(lblNombre, ramRow, lblDesc, sep, lblSeriTitulo, lblSeri);
        if (lblRecom != null) descPanel.getChildren().add(lblRecom);

        descPanel.setVisible(true);
        descPanel.setManaged(true);
    }

    private Label construirEtiquetaRecomendacion(ModeloInfo info) {
        if (ramSistemaGB == 0) return null;

        String texto;
        String estilo;

        if (ramSistemaGB < info.ramGB() - 2) {
            texto = "✗  No recomendado: tu equipo tiene " + ramSistemaGB + " GB de RAM y este modelo " +
                    "necesita " + info.ramGB() + " GB. Podría no arrancar o ir muy lento. " +
                    "Elige un modelo que requiera " + ramSistemaGB + " GB o menos.";
            estilo = "-fx-font-size:11px; -fx-text-fill:#C0392B; -fx-font-style:italic;";
        } else if (ramSistemaGB < info.ramGB()) {
            texto = "⚠  Podría funcionar con " + ramSistemaGB + " GB de RAM, pero con el rendimiento " +
                    "justo. Cierra otras aplicaciones antes de usarlo.";
            estilo = "-fx-font-size:11px; -fx-text-fill:#D35400; -fx-font-style:italic;";
        } else if (info.recomendado()) {
            texto = "✅  Compatible y recomendado para tu equipo (" + ramSistemaGB + " GB RAM) " +
                    "y el sector de la serigrafía.";
            estilo = "-fx-font-size:11px; -fx-text-fill:#1E8449; -fx-font-weight:bold;";
        } else {
            texto = "✅  Compatible con tu equipo (" + ramSistemaGB + " GB RAM).";
            estilo = "-fx-font-size:11px; -fx-text-fill:#27AE60;";
        }

        Label lbl = new Label(texto);
        lbl.setWrapText(true);
        lbl.setStyle(estilo);
        return lbl;
    }

    private void ocultarDescripcion() {
        descPanel.setVisible(false);
        descPanel.setManaged(false);
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
