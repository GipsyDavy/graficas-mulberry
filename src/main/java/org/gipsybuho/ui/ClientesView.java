package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.ColumnConfigDAO.ColumnConfig;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportarClientesService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ClientesView extends VBox {

    private final ClienteDAO dao = new ClienteDAO();
    private final ColumnConfigDAO columnConfigDAO = new ColumnConfigDAO();
    private final ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private final TableView<Cliente> tabla = new TableView<>(datos);
    private static final String TABLE_NAME = "clientes";
    private static final Set<String> COLUMNAS_IGNORADAS = Set.of("apellido");

    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("nombre", "Nombre");
        COLUMNAS_BASE.put("apellidos", "Apellidos");
        COLUMNAS_BASE.put("tipo", "Tipo");
        COLUMNAS_BASE.put("nif", "NIF/CIF");
        COLUMNAS_BASE.put("telefono", "Teléfono");
        COLUMNAS_BASE.put("email", "Email");
        COLUMNAS_BASE.put("ciudad", "Ciudad");
        COLUMNAS_BASE.put("direccion", "Dirección");
        COLUMNAS_BASE.put("cp", "C.P.");
        COLUMNAS_BASE.put("notas", "Notas");
        COLUMNAS_BASE.put("created_at", "Creado");
    }
    private static final Set<String> COLS_BASE_IDS = COLUMNAS_BASE.keySet();
    private static final Set<String> COLS_NO_DINAMICAS = java.util.stream.Stream
        .concat(COLS_BASE_IDS.stream(), COLUMNAS_IGNORADAS.stream())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public ClientesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Clientes");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        actualizarColumnasDinamicas();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre, apellido, NIF o email…");
        txtBuscar.setPrefWidth(280);
        txtBuscar.textProperty().addListener((o, a, b) -> buscar(b));

        Button btnNuevo    = btn("+ Nuevo", this::nuevo);
        Button btnEditar   = btn("✏ Editar", this::editar);
        Button btnBorrar   = btn("🗑 Borrar", this::borrar);
        Button btnImportar = btn("📥 Importar", this::importar);
        Button btnExportar = btn("📤 Exportar", this::exportar);
        Button btnPreview    = btn("👁 Previsualizar", this::previsualizar);
        Button btnColumnas = btn("⚙ Columnas", this::configurarColumnas);
        txtBuscar.setTooltip(new Tooltip("Filtrar por nombre, apellido, NIF o email"));
        btnNuevo.setTooltip(new Tooltip("Crear un nuevo cliente"));
        btnEditar.setTooltip(new Tooltip("Editar el cliente seleccionado"));
        btnBorrar.setTooltip(new Tooltip("Eliminar el cliente seleccionado"));
        btnImportar.setTooltip(new Tooltip("Importar clientes desde CSV, Excel o JSON"));
        btnExportar.setTooltip(new Tooltip("Exportar clientes a PDF, Excel, Word u otros formatos"));
        btnPreview.setTooltip(new Tooltip("Previsualizar ficha del cliente en PDF"));
        btnColumnas.setTooltip(new Tooltip("Mostrar u ocultar columnas de la tabla"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, txtBuscar, spacer, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Cliente> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setEditable(true);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getColumns().addAll(
            col("Nombre",    "nombre",    160),
            col("Apellidos", "apellidos", 160),
            col("Tipo",      "tipo",       80),
            col("NIF/CIF",   "nif",       100),
            col("Teléfono",  "telefono",  110),
            col("Email",     "email",     180),
            col("Ciudad",    "ciudad",    120)
        );
        tabla.setPlaceholder(Icons.emptyState("No hay clientes registrados todavía"));
        return tabla;
    }

    /**
     * Revisa las columnas extra que existen en la BD y las añade a la tabla si no están ya.
     * Se llama al construir la vista y tras cada importación.
     */
    private void actualizarColumnasDinamicas() {
        try {
            columnConfigDAO.syncTable(TABLE_NAME, COLUMNAS_BASE, COLUMNAS_IGNORADAS);
            Map<String, String> labels = columnConfigDAO.visibleLabels(TABLE_NAME);
            for (TableColumn<Cliente, ?> column : tabla.getColumns()) {
                Object key = column.getUserData();
                if (key instanceof String colName && COLS_BASE_IDS.contains(colName)) {
                    column.setText(labels.getOrDefault(colName, COLUMNAS_BASE.get(colName)));
                }
            }

            tabla.getColumns().removeIf(column -> {
                Object key = column.getUserData();
                return key instanceof String colName && !COLS_BASE_IDS.contains(colName);
            });

            for (ColumnConfig config : columnConfigDAO.findVisibleDynamic(TABLE_NAME, COLS_NO_DINAMICAS)) {
                final String colKey = config.columnName();
                TableColumn<Cliente, String> tc = new TableColumn<>(config.label());
                tc.setUserData(colKey);
                tc.setCellValueFactory(data ->
                    new SimpleStringProperty(nvl(data.getValue().getExtra(colKey))));
                tc.setCellFactory(TextFieldTableCell.forTableColumn());
                tc.setOnEditCommit(event -> {
                    Cliente cliente = event.getRowValue();
                    cliente.setExtra(colKey, event.getNewValue());
                    try {
                        dao.save(cliente);
                    } catch (Exception e) {
                        mostrarError(e);
                        cargar();
                    }
                });
                tc.setPrefWidth(130);
                tabla.getColumns().add(tc);
            }
        } catch (Exception e) { mostrarError(e); }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void buscar(String texto) {
        try {
            if (texto.isBlank()) datos.setAll(dao.findAll());
            else datos.setAll(dao.search(texto));
        } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        dialogo(new Cliente()).ifPresent(c -> {
            try { dao.save(c); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un cliente para editar."); return; }
        dialogo(sel).ifPresent(c -> {
            try { dao.save(c); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        List<Cliente> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta("Selecciona uno o varios clientes para borrar."); return; }
        String mensaje = seleccionados.size() == 1
            ? "¿Eliminar el cliente \"" + seleccionados.get(0).getNombre() + "\"?"
            : "¿Eliminar " + seleccionados.size() + " clientes seleccionados?";
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje, ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar"); conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Cliente cliente : seleccionados) dao.delete(cliente.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void configurarColumnas() {
        try {
            List<String> stylesheets = getScene() != null ? getScene().getStylesheets() : List.of();
            boolean changed = new ColumnConfiguratorDialog(
                TABLE_NAME, "Clientes", COLUMNAS_BASE, COLUMNAS_IGNORADAS, stylesheets).show();
            if (changed) {
                actualizarColumnasDinamicas();
                cargar();
            }
        } catch (Exception e) {
            mostrarError(e);
        }
    }

    private Optional<Cliente> dialogo(Cliente c) {
        Dialog<Cliente> dlg = new Dialog<>();
        dlg.setTitle(c.getId() == 0 ? "Nuevo cliente" : "Editar cliente");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().addAll(getScene() != null ? getScene().getStylesheets() : List.of());
        dlg.getDialogPane().setPrefWidth(520);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre    = tf(c.getNombre());
        TextField fApellido  = tf(c.getApellidos());
        ComboBox<String> fTipo = new ComboBox<>(FXCollections.observableArrayList("empresa", "particular"));
        fTipo.setValue(c.getTipo() != null ? c.getTipo() : "empresa");
        TextField fNif       = tf(c.getNif());
        TextField fDireccion = tf(c.getDireccion());
        TextField fCiudad    = tf(c.getCiudad() != null ? c.getCiudad() : "Almería");
        TextField fCp        = tf(c.getCp());
        TextField fTelefono  = tf(c.getTelefono());
        TextField fEmail     = tf(c.getEmail());
        TextArea  fNotas     = new TextArea(nvl(c.getNotas()));
        fNotas.setPrefRowCount(3);
        Map<String, TextField> extraFields = new LinkedHashMap<>();

        int r = 0;
        grid.addRow(r++, lbl("Nombre *"), fNombre, lbl("Apellidos"), fApellido);
        grid.addRow(r++, lbl("Tipo"), fTipo, lbl("NIF/CIF"), fNif);
        grid.addRow(r++, lbl("Teléfono"), fTelefono, lbl("Email"), fEmail);
        grid.addRow(r++, lbl("Ciudad"), fCiudad, lbl("C.P."), fCp);
        grid.add(lbl("Dirección"), 0, r); grid.add(fDireccion, 1, r, 3, 1); r++;
        grid.add(lbl("Notas"), 0, r);     grid.add(fNotas,     1, r, 3, 1);
        r++;

        try {
            List<ColumnConfig> extras = columnConfigDAO.findVisibleDynamic(TABLE_NAME, COLS_NO_DINAMICAS);
            if (!extras.isEmpty()) {
                Separator separator = new Separator();
                grid.add(separator, 0, r++, 4, 1);
                grid.add(lbl("Datos adicionales"), 0, r++, 4, 1);
                for (ColumnConfig config : extras) {
                    TextField field = tf(c.getExtra(config.columnName()));
                    extraFields.put(config.columnName(), field);
                    grid.add(lbl(config.label()), 0, r);
                    grid.add(field, 1, r, 3, 1);
                    r++;
                }
            }
        } catch (Exception e) {
            mostrarError(e);
        }

        dlg.getDialogPane().setContent(grid);

        Icons.markRequired(fNombre);
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                c.setNombre(fNombre.getText().trim());
                c.setApellidos(fApellido.getText().trim());
                c.setTipo(fTipo.getValue());
                c.setNif(fNif.getText().trim());
                c.setDireccion(fDireccion.getText().trim());
                c.setCiudad(fCiudad.getText().trim());
                c.setCp(fCp.getText().trim());
                c.setTelefono(fTelefono.getText().trim());
                c.setEmail(fEmail.getText().trim());
                c.setNotas(fNotas.getText().trim());
                extraFields.forEach((key, field) -> c.setExtra(key, field.getText().trim()));
                return c;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    // ── Importar ──────────────────────────────────────────────────────────────

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar clientes");
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
                        Cliente.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Cliente.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
                                Platform.runLater(() -> {
                                    cargar();
                                    actualizarColumnasDinamicas();
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

    // ── Exportar ──────────────────────────────────────────────────────────────

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
             "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
             "Tabla de clientes como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
             "Script SQL con la estructura y los datos de la tabla clientes.", "sql"},
            {"json",   "{ }  Exportar a JSON",
             "Datos de todos los clientes en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
             "Listado de clientes como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
             "Tabla de clientes en documento Word (.docx), editable.", "docx"},
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

        Label lblTitulo = new Label("Selecciona el formato de exportación:");
        lblTitulo.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblTitulo, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Exportar clientes");
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
        fc.setInitialFileName("Clientes_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Clientes", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Cliente> selExp = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarClientesCSV(destino);
                    case "sql"    -> ExportService.exportarClientesSQL(destino);
                    case "json"   -> ExportService.exportarClientesJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Cliente c = selExp.get(0);
                            Path pdf = new PDFService().generarFichaCliente(c);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarClientesPDF(destino, dao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            ExportService.exportarClienteDetalladoWord(destino, selExp.get(0));
                        } else {
                            ExportService.exportarClientesWord(destino, dao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarClientesExcel(destino, dao.findAll());
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

    // ── Previsualizar ─────────────────────────────────────────────────────────

    private void previsualizar() {
        List<Cliente> sel = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Cliente> lista = sel.isEmpty() ? new java.util.ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Cliente c = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaCliente(c);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Cliente " + c.getNombreCompleto();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarClientes(lista);
                    tituloVentana = "Previsualización — Clientes (" + lista.size() + " registro(s))";
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> TableColumn<Cliente, T> col(String titulo, String campo, double ancho) {
        TableColumn<Cliente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        c.setUserData(campo);
        return c;
    }

    private Button btn(String t, Runnable r) {
        String label = t.replaceFirst("^\\P{L}+", "").strip();
        Button b = new Button(label);
        b.getStyleClass().add("btn-toolbar");
        b.setOnAction(e -> r.run());
        return b;
    }

    private TextField tf(String valor) { return new TextField(nvl(valor)); }
    private Label lbl(String t) { return new Label(t); }
    private String nvl(String s) { return s != null ? s : ""; }
    private void alerta(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
