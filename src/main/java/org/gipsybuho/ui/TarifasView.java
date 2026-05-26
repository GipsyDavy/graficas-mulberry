package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.dao.TarifaTramoDAO;
import org.gipsybuho.model.Tarifa;
import org.gipsybuho.model.TarifaTramo;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TarifasView extends VBox {

    private static final String[] TECNICAS = {"Serigrafía", "DTF", "Bordado", "Vinilo", "Sublimación", "Gran Formato", "Offset", "Otros"};
    private final TarifaDAO dao = new TarifaDAO();
    private final ObservableList<Tarifa> datos = FXCollections.observableArrayList();
    private final TableView<Tarifa> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("tecnica", "Técnica");
        COLUMNAS_BASE.put("nombre", "Nombre");
        COLUMNAS_BASE.put("descripcion", "Descripción");
        COLUMNAS_BASE.put("precio_unit", "Precio/ud.");
        COLUMNAS_BASE.put("precio_setup", "Setup");
        COLUMNAS_BASE.put("minimo_unidades", "Mín. uds.");
        COLUMNAS_BASE.put("activa", "Activa");
        COLUMNAS_BASE.put("usa_tiempo", "Tramos");
        COLUMNAS_BASE.put("updated_at", "Actualizado");
    }
    private final DynamicColumnRuntime<Tarifa> dynamicColumns =
        new DynamicColumnRuntime<>("tarifas", "Tarifas", COLUMNAS_BASE, tabla, datos, Tarifa::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();

    public TarifasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Tarifas");
        titulo.getStyleClass().add("view-title");
        Label sub = new Label("Precios por técnica de impresión");
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn("+ Nueva tarifa",  "#4C9BE8", this::nueva);
        Button btnEditar   = btn("✏ Editar",         "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",        "#E74C3C", this::borrar);
        Button btnTramos   = btn("⏱ Tramos",        "#1A7A4A", this::verTramos);
        Button btnImportar   = btn("📥 Importar",      "#27AE60", this::importar);
        Button btnExportar   = btn("📤 Exportar",      "#8E44AD", this::exportar);
        Button btnPreview    = btn("👁 Previsualizar", "#6B2D5E", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas",       "#34495E", dynamicColumns::configure);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnBorrar, btnTramos, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Tarifa> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Tarifa, String> colTecnica = new TableColumn<>("Técnica");
        colTecnica.setCellValueFactory(new PropertyValueFactory<>("tecnica"));
        colTecnica.setPrefWidth(120);
        colTecnica.setUserData("tecnica");

        TableColumn<Tarifa, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(220);
        colNombre.setUserData("nombre");

        TableColumn<Tarifa, Double> colPrecio = new TableColumn<>("Precio/ud.");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        colPrecio.setUserData("precio_unit");
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Double> colSetup = new TableColumn<>("Setup");
        colSetup.setCellValueFactory(new PropertyValueFactory<>("precioSetup"));
        colSetup.setUserData("precio_setup");
        colSetup.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Integer> colMin = new TableColumn<>("Mín. uds.");
        colMin.setCellValueFactory(new PropertyValueFactory<>("minimoUnidades"));
        colMin.setUserData("minimo_unidades");

        TableColumn<Tarifa, String> colTramos = new TableColumn<>("Tramos");
        colTramos.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().isUsaTiempo() ? "⏱ Sí" : "—"));
        colTramos.setPrefWidth(90);
        colTramos.setUserData("usa_tiempo");

        tabla.getColumns().addAll(colTecnica, colNombre, colPrecio, colSetup, colMin, colTramos);
        tabla.setPlaceholder(Icons.emptyState("No hay tarifas registradas todavía"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); dynamicColumns.apply(); } catch (Exception e) { mostrarError(e); }
    }

    private void nueva() {
        dialogo(new Tarifa()).ifPresent(t -> {
            try { dao.save(t); dynamicColumns.saveFormFields(t, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una tarifa para editar."); return; }
        dialogo(sel).ifPresent(t -> {
            try { dao.save(t); dynamicColumns.saveFormFields(t, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        List<Tarifa> seleccionadas = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionadas.isEmpty()) { alerta("Selecciona una o varias tarifas para borrar."); return; }
        String mensaje = seleccionadas.size() == 1
            ? "¿Eliminar la tarifa \"" + seleccionadas.get(0).getNombre() + "\"?"
            : "¿Eliminar " + seleccionadas.size() + " tarifas seleccionadas?";
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje, ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Tarifa tarifa : seleccionadas) dao.delete(tarifa.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Tarifa> dialogo(Tarifa t) {
        Dialog<Tarifa> dlg = new Dialog<>();
        dlg.setTitle(t.getId() == 0 ? "Nueva tarifa" : "Editar tarifa");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        ComboBox<String> fTecnica = new ComboBox<>(FXCollections.observableArrayList(TECNICAS));
        fTecnica.setValue(t.getTecnica() != null ? t.getTecnica() : TECNICAS[0]);
        TextField fNombre      = tf(t.getNombre());
        TextField fDescripcion = tf(t.getDescripcion());
        TextField fPrecioUnit  = tf(t.getPrecioUnit() > 0 ? String.valueOf(t.getPrecioUnit()) : "");
        TextField fPrecioSetup = tf(t.getPrecioSetup() > 0 ? String.valueOf(t.getPrecioSetup()) : "0");
        TextField fMinimo      = tf(t.getMinimoUnidades() > 0 ? String.valueOf(t.getMinimoUnidades()) : "1");
        CheckBox chkUsaTiempo = new CheckBox("Tarifa con pricing basado en tiempo");
        chkUsaTiempo.setSelected(t.isUsaTiempo());
        Label lblInfo = new Label(
            "→ Gestiona los tramos desde el botón ⏱ de la pantalla principal.");
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        grid.addRow(0, lbl("Técnica *"), fTecnica);
        grid.addRow(1, lbl("Nombre *"), fNombre);
        grid.addRow(2, lbl("Descripción"), fDescripcion);
        grid.addRow(3, lbl("Precio/ud. (€) *"), fPrecioUnit);
        grid.addRow(4, lbl("Setup (€)"), fPrecioSetup);
        grid.addRow(5, lbl("Mínimo uds."), fMinimo);
        grid.add(chkUsaTiempo, 0, 6, 2, 1);
        grid.add(lblInfo, 0, 7, 2, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(grid, 8, t, dialogExtraFields);

        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                t.setTecnica(fTecnica.getValue());
                t.setNombre(fNombre.getText().trim());
                t.setDescripcion(fDescripcion.getText().trim());
                t.setPrecioUnit(parseDouble(fPrecioUnit.getText()));
                t.setPrecioSetup(parseDouble(fPrecioSetup.getText()));
                t.setMinimoUnidades(parseInt(fMinimo.getText(), 1));
                t.setActiva(true);
                t.setUsaTiempo(chkUsaTiempo.isSelected());
                return t;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    private void verTramos() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una tarifa."); return; }
        if (!sel.isUsaTiempo()) {
            alerta("Esta tarifa no usa pricing basado en tiempo."); return;
        }
        abrirVentanaTramos(sel);
    }

    private void abrirVentanaTramos(Tarifa tarifa) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Tramos de tiempo — " + tarifa.getNombre());
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        ObservableList<TarifaTramo> tramos = FXCollections.observableArrayList();
        TableView<TarifaTramo> tablaTramos = new TableView<>(tramos);

        TableColumn<TarifaTramo, Integer> colTiempo = new TableColumn<>("Tiempo (min)");
        colTiempo.setCellValueFactory(new PropertyValueFactory<>("tiempoMinutos"));
        colTiempo.setPrefWidth(120);

        TableColumn<TarifaTramo, Double> colPrecio = new TableColumn<>("Precio (€)");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioTiempo"));
        colPrecio.setPrefWidth(120);
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tablaTramos.getColumns().addAll(colTiempo, colPrecio);
        tablaTramos.setPlaceholder(new Label("No hay tramos registrados"));

        Runnable recargar = () -> {
            try { tramos.setAll(new TarifaTramoDAO().findByTarifaId(tarifa.getId())); } catch (Exception e) { mostrarError(e); }
        };
        recargar.run();

        Button btnAdd = btn("+ Añadir", "#4C9BE8", () -> {
            dialogoTramo(tarifa, new TarifaTramo(tarifa.getId(), 0, 0)).ifPresent(tramo -> {
                try { new TarifaTramoDAO().save(tramo); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });
        Button btnEdit = btn("✏ Editar", "#F39C12", () -> {
            TarifaTramo sel = tablaTramos.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta("Selecciona un tramo para editar."); return; }
            dialogoTramo(tarifa, sel).ifPresent(tramo -> {
                try { new TarifaTramoDAO().save(tramo); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });
        Button btnDel = btn("🗑 Borrar", "#E74C3C", () -> {
            TarifaTramo sel = tablaTramos.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta("Selecciona un tramo para borrar."); return; }
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar este tramo?", ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                try { new TarifaTramoDAO().delete(sel.getId()); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });

        HBox buttons = new HBox(8, btnAdd, btnEdit, btnDel);
        VBox box = new VBox(8, tablaTramos, buttons);
        box.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private Optional<TarifaTramo> dialogoTramo(Tarifa tarifa, TarifaTramo tramo) {
        Dialog<TarifaTramo> dlg = new Dialog<>();
        dlg.setTitle(tramo.getId() == 0 ? "Añadir tramo" : "Editar tramo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        TextField fTiempo = tf(tramo.getTiempoMinutos() > 0 ? String.valueOf(tramo.getTiempoMinutos()) : "");
        TextField fPrecio = tf(tramo.getPrecioTiempo() > 0 ? String.valueOf(tramo.getPrecioTiempo()) : "");
        grid.addRow(0, lbl("Tiempo (min, múltiplo de 5) *"), fTiempo);
        grid.addRow(1, lbl("Precio (€) *"), fPrecio);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            int tiempo = parseInt(fTiempo.getText(), 0);
            if (tiempo <= 0 || tiempo % 5 != 0) {
                alerta("El tiempo debe ser mayor que 0 y múltiplo de 5."); return null;
            }
            double precio = parseDouble(fPrecio.getText());
            if (precio <= 0) {
                alerta("El precio debe ser mayor que 0."); return null;
            }
            tramo.setTarifaId(tarifa.getId());
            tramo.setTiempoMinutos(tiempo);
            tramo.setPrecioTiempo(precio);
            return tramo;
        });
        return dlg.showAndWait();
    }

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar tarifas");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos importables (CSV, Excel, JSON)", "*.csv", "*.xlsx", "*.xls", "*.json"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File archivo = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        SoundService.play(SoundService.Sound.START);
        final File f = archivo;
        Thread.ofVirtual().start(() -> {
            try {
                var parsed = new ImportService().parseFile(f);
                var preview = parsed.rows.subList(0, Math.min(3, parsed.rows.size()));
                Platform.runLater(() -> {
                    var dlg = new ColumnMappingDialog(
                        getScene() != null ? getScene().getWindow() : null,
                        Tarifa.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Tarifa.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
                                Platform.runLater(() -> {
                                    cargar();
                                    SoundService.play(SoundService.Sound.COMPLETE);
                                    mostrarResultadoImportacion(result);
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    SoundService.play(SoundService.Sound.ERROR);
                                    mostrarError(ex);
                                });
                            }
                        })
                    );
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    mostrarError(e);
                });
            }
        });
    }

    private void mostrarResultadoImportacion(org.gipsybuho.service.importer.ImportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Importación completada en %.1f s.%n", r.duracion().toMillis() / 1000.0));
        sb.append(String.format("✓ %d filas importadas%n", r.filasImportadas()));
        sb.append(String.format("✓ %d filas actualizadas%n", r.filasActualizadas()));
        sb.append(String.format("✗ %d filas descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append("\n\nErrores (primeros 10):");
            r.errores().stream().limit(10).forEach(e ->
                sb.append(String.format("%n  Fila %d — %s: %s",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle("Resultado de importación");
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        if (getScene() != null) a.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        a.showAndWait();
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de tarifas como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de la tabla tarifas.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todas las tarifas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de tarifas como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de tarifas en documento Word (.docx), editable.", "docx"},
            {"excel",  "📗  Exportar a Excel (.xlsx)",
                "Hoja de cálculo Excel (.xlsx), compatible con Microsoft Excel y LibreOffice Calc.", "xlsx"}
        };

        ToggleGroup grupo = new ToggleGroup();
        VBox opBox = new VBox(4);
        for (String[] f : formatos) {
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(grupo);
            rb.setUserData(f);

            Label nombre = new Label(f[1]);
            nombre.setStyle("-fx-font-weight:bold; -fx-font-size:12px;");
            Label desc = new Label(f[2]);
            desc.setStyle("-fx-font-size:11px; -fx-text-fill:-c-text-muted;");

            VBox texto = new VBox(2, nombre, desc);
            HBox fila = new HBox(10, rb, texto);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new Insets(7, 12, 7, 12));
            fila.setStyle("-fx-background-radius:6; -fx-cursor:hand;");
            fila.setOnMouseClicked(e -> rb.setSelected(true));
            opBox.getChildren().add(fila);
        }
        grupo.getToggles().get(0).setSelected(true);

        Label lblSelecciona = new Label("Selecciona el formato de exportación:");
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Exportar tarifas");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(460);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("Exportar →");

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarExportacion);
    }

    private void lanzarExportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar exportación — " + fmt[1]);
        fc.setInitialFileName("Tarifas_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Tarifas", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Tarifa> selExp = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarTarifasCSV(destino);
                    case "sql"    -> ExportService.exportarTarifasSQL(destino);
                    case "json"   -> ExportService.exportarTarifasJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Tarifa t = selExp.get(0);
                            Path pdf = new PDFService().generarFichaTarifa(t);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarTarifasPDF(destino, dao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            ExportService.exportarTarifaDetalladaWord(destino, selExp.get(0));
                        } else {
                            ExportService.exportarTarifasWord(destino, dao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarTarifasExcel(destino, dao.findAll());
                }
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Exportación completada:\n" + destino, ButtonType.OK);
                    ok.setTitle("Exportación completada");
                    ok.setHeaderText(null);
                    if (getScene() != null) ok.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    ok.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    setDisable(false);
                    mostrarError(e);
                });
            }
        });
    }

    private void previsualizar() {
        List<Tarifa> sel = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Tarifa> lista = sel.isEmpty() ? new ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Tarifa t = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaTarifa(t);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Tarifa " + t.getNombre();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarTarifas(lista);
                    tituloVentana = "Previsualización — Tarifas (" + lista.size() + " registro(s))";
                }
                final byte[] bytes = pdfBytes; final String titulo = tituloVentana;
                Platform.runLater(() -> {
                    setDisable(false);
                    SoundService.play(SoundService.Sound.COMPLETE);
                    PdfPreviewWindow.mostrar(bytes, titulo);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setDisable(false);
                    SoundService.play(SoundService.Sound.ERROR);
                    mostrarError(ex);
                });
            }
        });
    }

    private Button btn(String t, String color, Runnable r) {
        String label = t.replaceFirst("^\\P{L}+", "").strip();
        Button b = new Button(label);
        b.getStyleClass().add("btn-toolbar");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
