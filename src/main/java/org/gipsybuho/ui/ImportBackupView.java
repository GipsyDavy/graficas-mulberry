package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportBackupService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImportBackupView extends VBox {

    private static final DateTimeFormatter FMT_LOG = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final TextArea logArea = new TextArea();

    @FunctionalInterface
    interface ImportarFn {
        void importar(Path origen) throws Exception;
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public ImportBackupView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(16);

        Label titulo = new Label("Importar Backup");
        titulo.getStyleClass().add("view-title");

        Label aviso = new Label(
            "⚠  ATENCIÓN: Esta operación reemplaza TODOS los datos actuales con los del archivo de backup. " +
            "Se recomienda realizar una copia de seguridad previa desde Exportar / Backup antes de continuar. " +
            "La operación no se puede deshacer.");
        aviso.setWrapText(true);
        aviso.setStyle(
            "-fx-text-fill:#c0392b;-fx-font-size:13px;-fx-font-weight:bold;" +
            "-fx-background-color:#fdf2f2;-fx-padding:10 14;-fx-background-radius:6;");

        Label info = new Label(
            "Restaura la base de datos de Gráficas Mulberry desde una copia de seguridad. " +
            "Se admiten los cuatro formatos generados por la función Exportar / Backup: " +
            "SQLite (.db), CSV comprimido (.zip), volcado SQL (.sql) y JSON (.json).");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:13px;");

        getChildren().addAll(titulo, aviso, info, buildInfoDB(), new Separator(), buildCartas(), buildLog());
    }

    // ── Info base de datos ────────────────────────────────────────────────────

    private HBox buildInfoDB() {
        File dbFile = ExportService.getDbFile();
        long bytes  = ExportService.getDbSizeBytes();
        int  regs   = ExportService.contarRegistros();

        String tamano = bytes < 1024        ? bytes + " B"
                      : bytes < 1024 * 1024 ? String.format("%.1f KB", bytes / 1024.0)
                      :                       String.format("%.2f MB", bytes / (1024.0 * 1024.0));

        Label lblTitulo = new Label("💾  Base de datos actual");
        lblTitulo.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");

        Label lblRuta = new Label(dbFile.getAbsolutePath());
        lblRuta.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:11px;");

        Label lblTam = new Label("Tamaño: " + tamano);
        Label lblReg = new Label("Registros actuales: " + regs);
        for (Label l : new Label[]{lblTam, lblReg})
            l.setStyle("-fx-font-size:12px;");

        VBox texto = new VBox(4, lblTitulo, lblRuta, new HBox(16, lblTam, lblReg));
        HBox.setHgrow(texto, Priority.ALWAYS);

        HBox card = new HBox(16, texto);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(12, 16, 12, 16));
        return card;
    }

    // ── Cartas de importación ─────────────────────────────────────────────────

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
            "💾  Restaurar desde SQLite",
            "Restaura la base de datos completa desde un archivo .db generado por la función de backup.\n" +
            "Copia exacta del archivo de base de datos, sin pérdida de datos ni estructura.\n" +
            "Si la restauración falla, se recupera automáticamente la base de datos anterior.\n" +
            "Formato: .db",
            "#4C9BE8", "db",
            ImportBackupService::restaurarSQLite
        ), 0, 0);

        grid.add(carta(
            "📊  Restaurar desde CSV comprimido",
            "Restaura todos los datos desde un archivo ZIP con ficheros CSV generado por la función de backup.\n" +
            "Un CSV por cada tabla, con el separador de punto y coma del formato de exportación.\n" +
            "Los datos existentes se reemplazan tabla por tabla dentro de una única transacción.\n" +
            "Formato: .zip con archivos .csv",
            "#27AE60", "zip",
            ImportBackupService::restaurarZipCSV
        ), 1, 0);

        grid.add(carta(
            "🗄️  Restaurar desde volcado SQL",
            "Ejecuta el script SQL generado por la función de backup para restaurar toda la base de datos.\n" +
            "Contiene sentencias DROP TABLE, CREATE TABLE e INSERT con todos los datos.\n" +
            "Respeta el orden de tablas y las claves foráneas del esquema original.\n" +
            "Formato: .sql",
            "#8E44AD", "sql",
            ImportBackupService::restaurarSQL
        ), 0, 1);

        grid.add(carta(
            "{ }  Restaurar desde JSON",
            "Restaura todos los datos desde el archivo JSON estructurado generado por la función de backup.\n" +
            "Contiene todas las tablas con los tipos de datos correctos (enteros, decimales, texto, nulos).\n" +
            "Los datos existentes se reemplazan tabla por tabla dentro de una única transacción.\n" +
            "Formato: .json (UTF-8)",
            "#F39C12", "json",
            ImportBackupService::restaurarJSON
        ), 1, 1);

        return grid;
    }

    private VBox carta(String titulo, String descripcion, String color, String ext, ImportarFn fn) {
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

        Button btnImportar = new Button("Importar ." + ext + "  ←");
        btnImportar.setStyle(
            "-fx-background-color:" + color + ";-fx-text-fill:white;" +
            "-fx-font-weight:bold;-fx-padding:8 20;-fx-background-radius:4;-fx-font-size:12px;");
        btnImportar.setMaxWidth(Double.MAX_VALUE);
        btnImportar.setOnAction(e -> lanzarImportacion(titulo, ext, fn));

        card.getChildren().addAll(lblTitulo, lblDesc, sp, btnImportar);
        return card;
    }

    // ── Log ───────────────────────────────────────────────────────────────────

    private VBox buildLog() {
        Label lblLog = new Label("Registro de importaciones");
        lblLog.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");

        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setStyle("-fx-background-color:-c-tab-bg;-fx-padding:4 10;-fx-background-radius:4;");
        btnLimpiar.setOnAction(e -> logArea.clear());

        HBox header = new HBox(lblLog, new Region(), btnLimpiar);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(7);
        logArea.setStyle("-fx-font-family:monospace;-fx-font-size:12px;");
        logArea.setPromptText("Los resultados de las restauraciones aparecerán aquí…");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox box = new VBox(6, header, logArea);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ── Lógica de importación ─────────────────────────────────────────────────

    private void lanzarImportacion(String titulo, String ext, ImportarFn fn) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar restauración de backup");
        confirm.setHeaderText("¿Restaurar backup desde formato ." + ext + "?");
        confirm.setContentText(
            "Esta operación reemplazará TODOS los datos actuales con los del archivo de backup.\n\n" +
            "Esta acción no se puede deshacer. ¿Deseas continuar?");
        if (getScene() != null) confirm.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar backup — " + titulo);
        String desc = switch (ext) {
            case "db"   -> "Base de datos SQLite";
            case "zip"  -> "CSV comprimido (ZIP)";
            case "sql"  -> "Volcado SQL";
            case "json" -> "JSON";
            default     -> ext.toUpperCase();
        };
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(desc + " — Gráficas Mulberry", "*." + ext),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (f == null) return;

        Path origen = f.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        log("⏳ Iniciando restauración: " + titulo + "…");
        log("   Archivo: " + f.getName());

        Thread.ofVirtual().start(() -> {
            try {
                fn.importar(origen);
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    log("✅ Restauración completada correctamente.");
                    setDisable(false);
                    mostrarExito(f.getName());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    log("❌ Error al restaurar: " + e.getMessage());
                    setDisable(false);
                    mostrarError(e);
                });
            }
        });
    }

    private void mostrarExito(String nombreArchivo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Restauración completada");
        alert.setHeaderText("El backup se ha restaurado correctamente");
        alert.setContentText(
            "Archivo restaurado: " + nombreArchivo + "\n\n" +
            "Vuelve al Panel principal para verificar que los datos se han cargado correctamente.");
        if (getScene() != null) alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        alert.showAndWait();
    }

    private void mostrarError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error al restaurar backup");
        alert.setHeaderText("No se pudo completar la restauración");
        alert.setContentText("Error: " + e.getMessage());
        if (getScene() != null) alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        alert.showAndWait();
    }

    private void log(String msg) {
        String hora = LocalDateTime.now().format(FMT_LOG);
        Platform.runLater(() -> {
            logArea.appendText("[" + hora + "] " + msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
