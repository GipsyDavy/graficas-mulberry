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
import org.gipsybuho.dao.AlbaranDAO;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.LineaAlbaran;
import org.gipsybuho.model.Material;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.importer.ImportResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AlbaranesView extends VBox {

    private final AlbaranDAO dao = new AlbaranDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Albaran> datos = FXCollections.observableArrayList();
    private final TableView<Albaran> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("numero", "Número");
        COLUMNAS_BASE.put("cliente_id", "Cliente");
        COLUMNAS_BASE.put("fecha", "Fecha");
        COLUMNAS_BASE.put("factura_id", "Factura ref.");
        COLUMNAS_BASE.put("pedido_id", "Pedido ref.");
        COLUMNAS_BASE.put("estado", "Estado");
        COLUMNAS_BASE.put("observaciones", "Observaciones");
        COLUMNAS_BASE.put("created_at", "Creado");
    }
    private final DynamicColumnRuntime<Albaran> dynamicColumns =
        new DynamicColumnRuntime<>("albaranes", "Albaranes", COLUMNAS_BASE, tabla, datos, Albaran::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();

    public AlbaranesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Albaranes");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn("+ Nuevo",            "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",            "#F39C12", this::editar);
        Button btnFirmado  = btn("✅ Marcar firmado",   "#27AE60", this::marcarFirmado);
        Button btnImportar = btn("📥 Importar",         "#16A085", this::importar);
        Button btnExportar = btn("📤 Exportar",         "#8E44AD", this::exportar);
        Button btnBorrar   = btn("🗑 Borrar",           "#E74C3C", this::borrar);
        Button btnPreview    = btn("👁 Previsualizar",    "#6B2D5E", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas",          "#34495E", dynamicColumns::configure);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnFirmado, btnImportar, btnExportar, btnBorrar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Albaran> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Albaran, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setUserData("estado");
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                String variant = switch (v) {
                    case "firmado"   -> "success";
                    case "entregado" -> "info";
                    default          -> "warning";
                };
                setText(null);
                setGraphic(Icons.statusBadge(v, variant));
            }
        });

        tabla.getColumns().addAll(
            col("Número",      "numero",         140),
            col("Cliente",     "clienteNombre",  200),
            col("Fecha",       "fecha",          100),
            col("Factura ref.", "facturaNumero",  130),
            col("Pedido ref.", "pedidoNumero",   130),
            colEstado
        );
        tabla.setPlaceholder(Icons.emptyState("No hay albaranes registrados todavía"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); dynamicColumns.apply(); } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        try {
            List<Cliente> clientes = clienteDAO.findAll();
            if (clientes.isEmpty()) { alerta("Añade al menos un cliente antes de crear un albarán."); return; }
            Albaran a = new Albaran();
            a.setNumero(DatabaseManager.generarNumeroAlbaran());
            a.setFecha(LocalDate.now().toString());
            a.setEstado("pendiente");
            dialogoAlbaran(a, clientes).ifPresent(alb -> {
                try { dao.save(alb); dynamicColumns.saveFormFields(alb, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán para editar."); return; }
        try {
            Albaran a = dao.findById(sel.getId());
            List<Cliente> clientes = clienteDAO.findAll();
            dialogoAlbaran(a, clientes).ifPresent(alb -> {
                try { dao.save(alb); dynamicColumns.saveFormFields(alb, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void marcarFirmado() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán."); return; }
        try { dao.updateEstado(sel.getId(), "firmado"); cargar(); } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        List<Albaran> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta("Selecciona uno o varios albaranes para borrar."); return; }
        String mensaje = seleccionados.size() == 1
            ? "¿Eliminar el albarán " + seleccionados.get(0).getNumero() + "?"
            : "¿Eliminar " + seleccionados.size() + " albaranes seleccionados?";
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje, ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Albaran albaran : seleccionados) dao.delete(albaran.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Albaran> dialogoAlbaran(Albaran a, List<Cliente> clientes) {
        Dialog<Albaran> dlg = new Dialog<>();
        dlg.setTitle(a.getId() == 0 ? "Nuevo albarán" : "Editar albarán " + a.getNumero());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(800);
        dlg.getDialogPane().setPrefHeight(580);

        TabPane tabs = new TabPane();

        // Tab 1: Datos generales
        GridPane gGeneral = new GridPane();
        gGeneral.setHgap(10); gGeneral.setVgap(10); gGeneral.setPadding(new Insets(16));

        ComboBox<Cliente> fCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        clientes.stream().filter(c -> c.getId() == a.getClienteId()).findFirst().ifPresent(fCliente::setValue);
        if (fCliente.getValue() == null && !clientes.isEmpty()) fCliente.setValue(clientes.get(0));

        TextField fNumero = tf(a.getNumero()); fNumero.setEditable(false);
        TextField fFecha  = tf(a.getFecha());
        ComboBox<String> fEstado = new ComboBox<>(FXCollections.observableArrayList("pendiente","entregado","firmado"));
        fEstado.setValue(a.getEstado() != null ? a.getEstado() : "pendiente");
        TextArea fObs = new TextArea(nvl(a.getObservaciones())); fObs.setPrefRowCount(3);

        gGeneral.addRow(0, lbl("Número"), fNumero, lbl("Estado"), fEstado);
        gGeneral.addRow(1, lbl("Cliente *"), fCliente, lbl("Fecha entrega"), fFecha);
        gGeneral.add(lbl("Observaciones"), 0, 2); gGeneral.add(fObs, 1, 2, 3, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(gGeneral, 3, a, dialogExtraFields);
        tabs.getTabs().add(new Tab("Datos generales", gGeneral));

        // Tab 2: Líneas
        ObservableList<LineaAlbaran> lineas = FXCollections.observableArrayList(a.getLineas());
        tabs.getTabs().add(new Tab("Artículos", buildTablaLineas(lineas)));

        dlg.getDialogPane().setContent(tabs);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            if (fCliente.getValue() == null) { alerta("Selecciona un cliente."); return null; }
            a.setClienteId(fCliente.getValue().getId());
            a.setClienteNombre(fCliente.getValue().getNombreCompleto());
            a.setFecha(fFecha.getText().trim());
            a.setEstado(fEstado.getValue());
            a.setObservaciones(fObs.getText().trim());
            a.setLineas(new java.util.ArrayList<>(lineas));
            return a;
        });
        return dlg.showAndWait();
    }

    private Node buildTablaLineas(ObservableList<LineaAlbaran> lineas) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        TableView<LineaAlbaran> t = new TableView<>(lineas);
        t.setPrefHeight(300);

        TableColumn<LineaAlbaran, String> cDesc = new TableColumn<>("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        cDesc.setPrefWidth(400);
        cDesc.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : descripcionVisible(v));
            }
        });

        TableColumn<LineaAlbaran, Integer> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(80);

        TableColumn<LineaAlbaran, String> cUnid = new TableColumn<>("Unidad");
        cUnid.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        cUnid.setPrefWidth(80);
        cUnid.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : ("__material__".equals(v) ? "ud" : v));
            }
        });

        t.getColumns().addAll(cDesc, cCant, cUnid);

        HBox buttons = new HBox(8);
        Button btnAdd      = btn("+ Añadir",         "#4C9BE8", () -> dialogoLinea(null, lineas));
        Button btnStock    = btn("📦 Desde stock",   "#9B59B6", () -> dialogoDesdeStock(lineas));
        Button btnEdit     = btn("✏ Editar",          "#F39C12", () -> {
            LineaAlbaran sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoLinea(sel, lineas);
        });
        Button btnDel      = btn("🗑 Quitar",         "#E74C3C", () -> {
            LineaAlbaran sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) lineas.remove(sel);
        });
        buttons.getChildren().addAll(btnAdd, btnStock, btnEdit, btnDel);
        box.getChildren().addAll(t, buttons);
        return box;
    }

    private void dialogoLinea(LineaAlbaran linea, ObservableList<LineaAlbaran> lista) {
        boolean esNueva = linea == null;
        if (esNueva) linea = new LineaAlbaran();
        LineaAlbaran l = linea;

        Dialog<LineaAlbaran> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? "Nuevo artículo" : "Editar artículo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        boolean esMaterial = esDescripcionMaterial(l.getDescripcion());
        TextArea fDesc = new TextArea(descripcionVisible(nvl(l.getDescripcion()))); fDesc.setPrefRowCount(3); fDesc.setPrefWidth(300);
        TextField fCant = tf(l.getCantidad() > 0 ? String.valueOf(l.getCantidad()) : "1");
        ComboBox<String> fUnidad = new ComboBox<>(FXCollections.observableArrayList(
            "ud", "m²", "m", "kg", "L", "caja", "pack", "rollo"));
        fUnidad.setEditable(true);
        fUnidad.setValue("__material__".equals(l.getUnidad()) ? "ud" : (l.getUnidad() != null ? l.getUnidad() : "ud"));

        grid.addRow(0, lbl("Descripción *"), fDesc);
        GridPane.setColumnSpan(fDesc, 3);
        grid.addRow(1, lbl("Cantidad"), fCant, lbl("Unidad"), fUnidad);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            l.setDescripcion((esMaterial ? "[MATERIAL] " : "") + fDesc.getText().trim());
            l.setCantidad(parseInt(fCant.getText(), 1));
            l.setUnidad(esMaterial ? "__material__" : (fUnidad.getValue() != null ? fUnidad.getValue().trim() : "ud"));
            return l;
        });

        dlg.showAndWait().ifPresent(result -> {
            if (esNueva) lista.add(result);
        });
    }

    private void dialogoDesdeStock(ObservableList<LineaAlbaran> lineas) {
        List<Material> materiales;
        try { materiales = new MaterialDAO().findAll(); }
        catch (Exception e) { mostrarError(e); return; }
        if (materiales.isEmpty()) { alerta("No hay materiales registrados en el stock."); return; }

        Dialog<LineaAlbaran> dlg = new Dialog<>();
        dlg.setTitle("Añadir material del stock al albarán");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12); grid.setPadding(new Insets(16));

        ComboBox<Material> cbMat = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMat.setPromptText("Seleccionar material del stock...");
        cbMat.setPrefWidth(340);

        Label lblInfo = new Label("Selecciona un material para ver su disponibilidad");
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        cbMat.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m != null) lblInfo.setText(String.format(
                "Stock disponible: %.2f %s  |  Categoría: %s",
                m.getStockActual(),
                m.getUnidad() != null && !m.getUnidad().isBlank() ? m.getUnidad() : "ud",
                m.getCategoria() != null ? m.getCategoria() : "-"));
        });

        Spinner<Integer> spCant = new Spinner<>(1, 999999, 1);
        spCant.setEditable(true);
        spCant.setPrefWidth(100);

        grid.addRow(0, lbl("Material:"), cbMat);
        grid.add(lblInfo, 1, 1);
        grid.addRow(2, lbl("Cantidad:"), spCant);

        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK || cbMat.getValue() == null) return null;
            Material m = cbMat.getValue();
            LineaAlbaran l = new LineaAlbaran();
            l.setDescripcion("[MATERIAL] " + m.getNombre() +
                (m.getReferencia() != null && !m.getReferencia().isBlank() ? " [" + m.getReferencia() + "]" : ""));
            l.setCantidad(spCant.getValue());
            l.setUnidad("__material__");
            return l;
        });

        dlg.showAndWait().ifPresent(lineas::add);
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Albaran, T> col(String t, String campo, double ancho) {
        TableColumn<Albaran, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setUserData(toDbColumn(campo));
        c.setPrefWidth(ancho); return c;
    }

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar albaranes");
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
                        Albaran.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Albaran.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
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

    private void mostrarResultadoImportacion(ImportResult r) {
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
                "Tabla de albaranes como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con albaranes y sus líneas (tabla completa).", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Albaranes y líneas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de albaranes como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de albaranes en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar albaranes");
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
        fc.setInitialFileName("Albaranes_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Albaranes", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Albaran> selExp = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarAlbaranesCSV(destino);
                    case "sql"    -> ExportService.exportarAlbaranesSQL(destino);
                    case "json"   -> ExportService.exportarAlbaranesJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Albaran a = dao.findById(selExp.get(0).getId());
                            Cliente c = clienteDAO.findById(a.getClienteId());
                            Path pdf = new PDFService().generarAlbaran(a, c);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarAlbaranesPDF(destino, dao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarAlbaranesExcel(destino, dao.findAll());
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            Albaran a = dao.findById(selExp.get(0).getId());
                            Cliente c = clienteDAO.findById(a.getClienteId());
                            ExportService.exportarAlbaranDetalladoWord(destino, a, c);
                        } else {
                            ExportService.exportarAlbaranesWord(destino, dao.findAll());
                        }
                    }
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
        List<Albaran> sel = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Albaran> lista = sel.isEmpty() ? new java.util.ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; byte[] pdfImpresionBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Albaran a = dao.findById(lista.get(0).getId());
                    Cliente c = clienteDAO.findById(a.getClienteId());
                    PDFService pdfService = new PDFService();
                    Path pdfPath = pdfService.generarAlbaran(a, c, true);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    Path pdfImpresionPath = pdfService.generarAlbaran(a, c, false);
                    pdfImpresionBytes = Files.readAllBytes(pdfImpresionPath);
                    tituloVentana = "Previsualización — Albarán " + a.getNumero();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarAlbaranes(lista);
                    pdfImpresionBytes = pdfBytes;
                    tituloVentana = "Previsualización — Albaranes (" + lista.size() + " registro(s))";
                }
                final byte[] bytes = pdfBytes; final byte[] bytesImpresion = pdfImpresionBytes; final String titulo = tituloVentana;
                Platform.runLater(() -> {
                    setDisable(false);
                    SoundService.play(SoundService.Sound.COMPLETE);
                    PdfPreviewWindow.mostrar(bytes, bytesImpresion, titulo);
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
    private String toDbColumn(String campo) {
        return switch (campo) {
            case "clienteNombre" -> "cliente_id";
            case "facturaNumero" -> "factura_id";
            case "pedidoNumero" -> "pedido_id";
            default -> campo;
        };
    }
    private String nvl(String s) { return s != null ? s : ""; }
    private boolean esDescripcionMaterial(String descripcion) {
        return nvl(descripcion).startsWith("[MATERIAL] ");
    }
    private String descripcionVisible(String descripcion) {
        String valor = nvl(descripcion);
        return valor.startsWith("[MATERIAL] ") ? valor.substring("[MATERIAL] ".length()) : valor;
    }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { SoundService.play(SoundService.Sound.ERROR); new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
