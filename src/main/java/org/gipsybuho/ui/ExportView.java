package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.SoundService;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportView extends VBox {

    private static final DateTimeFormatter FMT_LOG = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TextArea logArea = new TextArea();

    @FunctionalInterface
    interface ExportarFn {
        void exportar(Path destino) throws Exception;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public ExportView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(16);

        Label titulo = new Label("Exportar y copias de seguridad");
        titulo.getStyleClass().add("view-title");

        Label info = new Label(
            "Exporta toda la base de datos de Gráficas Mulberry — clientes, pedidos, pagos, materiales, " +
            "facturas, presupuestos, empleados, nóminas… — al formato que necesites. " +
            "Útil para trasladar datos a otro ordenador, importarlos en otro programa o " +
            "como copia de seguridad periódica.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:13px;");

        getChildren().addAll(titulo, info, buildInfoDB(), new Separator(), buildCartas(), buildLog());
    }

    // ── Info base de datos ────────────────────────────────────────────────────

    private HBox buildInfoDB() {
        File dbFile = ExportService.getDbFile();
        long bytes  = ExportService.getDbSizeBytes();
        int  regs   = ExportService.contarRegistros();

        String tamano = bytes < 1024        ? bytes + " B"
                      : bytes < 1024 * 1024 ? String.format("%.1f KB", bytes / 1024.0)
                      :                       String.format("%.2f MB", bytes / (1024.0 * 1024.0));

        Label lblTitulo = new Label("💾  Base de datos activa");
        lblTitulo.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");

        Label lblRuta = new Label(dbFile.getAbsolutePath());
        lblRuta.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:11px;");

        Label lblTam = new Label("Tamaño: " + tamano);
        Label lblReg = new Label("Registros principales: " + regs);
        for (Label l : new Label[]{lblTam, lblReg})
            l.setStyle("-fx-font-size:12px;");

        Button btnAbrir = new Button("📂  Abrir carpeta");
        btnAbrir.setStyle("-fx-background-color:-c-primary;-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-padding:5 12;-fx-background-radius:4;-fx-font-size:12px;");
        btnAbrir.setOnAction(e -> abrirCarpetaDB(dbFile));

        VBox texto = new VBox(4, lblTitulo, lblRuta, new HBox(16, lblTam, lblReg));
        HBox.setHgrow(texto, Priority.ALWAYS);

        HBox card = new HBox(16, texto, btnAbrir);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(12, 16, 12, 16));
        return card;
    }

    // ── Cartas de exportación ─────────────────────────────────────────────────

    private GridPane buildCartas() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(50);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc1, cc2);

        grid.add(carta(
            "💾  Copia de seguridad SQLite",
            "Copia exacta y compacta del archivo de base de datos.\n" +
            "Formato nativo SQLite, sin pérdida de datos ni estructura.\n" +
            "Ideal para restaurar en otro ordenador con la misma aplicación.\n" +
            "Formato: .db",
            "#4C9BE8", "db",
            destino -> ExportService.backupSQLite(destino)
        ), 0, 0);

        grid.add(carta(
            "📊  Exportar a CSV (Excel / LibreOffice)",
            "Una hoja de cálculo por cada tabla de datos, todo comprimido en un archivo ZIP.\n" +
            "Los archivos CSV se abren directamente con Excel, LibreOffice Calc o Google Sheets.\n" +
            "Incluye BOM UTF-8 para que los acentos se lean correctamente en Excel español.\n" +
            "Formato: .zip con archivos .csv",
            "#27AE60", "zip",
            destino -> ExportService.exportarZipCSV(destino)
        ), 1, 0);

        grid.add(carta(
            "🗄️  Volcado SQL completo",
            "Script SQL con toda la estructura de tablas y los datos como sentencias INSERT.\n" +
            "Portable a cualquier base de datos SQLite o compatible.\n" +
            "Incluye el orden correcto de tablas respetando las claves foráneas.\n" +
            "Formato: .sql",
            "#8E44AD", "sql",
            destino -> ExportService.exportarSQL(destino)
        ), 0, 1);

        grid.add(carta(
            "{ }  Exportar a JSON",
            "Todas las tablas en formato JSON estructurado con tipos de datos correctos.\n" +
            "Formato universal compatible con cualquier lenguaje de programación.\n" +
            "Ideal para integración con aplicaciones web o APIs externas.\n" +
            "Formato: .json (UTF-8)",
            "#F39C12", "json",
            destino -> ExportService.exportarJSON(destino)
        ), 1, 1);

        return grid;
    }

    private VBox carta(String titulo, String descripcion, String color, String ext, ExportarFn fn) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-border-color:" + color + ";-fx-border-width:0 0 0 5;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

        Label lblDesc = new Label(descripcion);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);

        Button btnExportar = new Button("Exportar ." + ext + "  →");
        btnExportar.setStyle(
            "-fx-background-color:" + color + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-padding:8 20;-fx-background-radius:4;-fx-font-size:12px;");
        btnExportar.setMaxWidth(Double.MAX_VALUE);
        btnExportar.setOnAction(e -> lanzarExportacion(titulo, ext, fn));

        card.getChildren().addAll(lblTitulo, lblDesc, sp, btnExportar);
        return card;
    }

    // ── Log ───────────────────────────────────────────────────────────────────

    private VBox buildLog() {
        Label lblLog = new Label("Registro de exportaciones");
        lblLog.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:-c-text;");

        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setStyle("-fx-background-color:-c-tab-bg;-fx-padding:4 10;-fx-background-radius:4;-fx-text-fill:-c-text;");
        btnLimpiar.setOnAction(e -> logArea.clear());

        HBox header = new HBox(lblLog, new Region(), btnLimpiar);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(7);
        logArea.setStyle("-fx-font-family:monospace;-fx-font-size:12px;");
        logArea.setPromptText("Los resultados de las exportaciones aparecerán aquí…");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox box = new VBox(6, header, logArea);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ── Lógica de exportación ─────────────────────────────────────────────────

    private void lanzarExportacion(String titulo, String ext, ExportarFn fn) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar exportación — " + titulo);
        fc.setInitialFileName("GraficasMulberry_" + ExportService.timestamp() + "." + ext);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(ext.toUpperCase() + " — Gráficas Mulberry", "*." + ext));

        // Intentar usar la carpeta Documentos como directorio inicial
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File f = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (f == null) return;

        Path destino = f.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        log("⏳ Iniciando exportación: " + titulo + "…");

        Thread.ofVirtual().start(() -> {
            try {
                fn.exportar(destino);
                long bytes = destino.toFile().length();
                String tamano = bytes < 1024 * 1024
                    ? String.format("%.1f KB", bytes / 1024.0)
                    : String.format("%.2f MB", bytes / (1024.0 * 1024.0));
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    log("✅ Completado (" + tamano + "): " + destino.getFileName());
                    setDisable(false);
                    mostrarExito(destino);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    log("❌ Error al exportar: " + e.getMessage());
                    setDisable(false);
                    mostrarError(e);
                });
            }
        });
    }

    private void mostrarExito(Path destino) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportación completada");
        alert.setHeaderText("El archivo se ha guardado correctamente");
        alert.setContentText(destino.toString());
        if (getScene() != null) alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        ButtonType btnAbrir = new ButtonType("Abrir carpeta");
        alert.getButtonTypes().add(btnAbrir);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == btnAbrir) abrirCarpetaDB(destino.getParent().toFile());
        });
    }

    private void mostrarError(Exception e) {
        new Alert(Alert.AlertType.ERROR,
            "No se pudo completar la exportación:\n" + e.getMessage(),
            ButtonType.OK).showAndWait();
    }

    private void abrirCarpetaDB(File carpeta) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(carpeta.isDirectory() ? carpeta : carpeta.getParentFile());
            }
        } catch (Exception ex) {
            log("ℹ️ No se pudo abrir la carpeta: " + ex.getMessage());
        }
    }

    private void log(String msg) {
        String hora = LocalDateTime.now().format(FMT_LOG);
        Platform.runLater(() -> {
            logArea.appendText("[" + hora + "] " + msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
