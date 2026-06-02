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
import org.gipsybuho.dao.EmpleadoDAO;
import org.gipsybuho.model.Empleado;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmpleadosView extends VBox {

    private static final String[] CATEGORIAS = {
        "Diseñadora", "Contable", "Administrativo", "Taller", "Jefe de Taller", "Prácticas"
    };

    private final EmpleadoDAO dao = new EmpleadoDAO();
    private final ObservableList<Empleado> datos = FXCollections.observableArrayList();
    private final TableView<Empleado> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("nombre", "Nombre");
        COLUMNAS_BASE.put("apellidos", "Apellidos");
        COLUMNAS_BASE.put("apellido", "Apellido");
        COLUMNAS_BASE.put("nif", "NIF");
        COLUMNAS_BASE.put("categoria", "Categoría");
        COLUMNAS_BASE.put("salario_base", "Salario base");
        COLUMNAS_BASE.put("fecha_alta", "Fecha alta");
        COLUMNAS_BASE.put("fecha_baja", "Fecha baja");
        COLUMNAS_BASE.put("iban", "IBAN");
        COLUMNAS_BASE.put("irpf", "IRPF");
        COLUMNAS_BASE.put("activo", "Estado");
        COLUMNAS_BASE.put("telefono", "Teléfono");
        COLUMNAS_BASE.put("email", "Email");
        COLUMNAS_BASE.put("direccion", "Dirección");
    }
    private final DynamicColumnRuntime<Empleado> dynamicColumns =
        new DynamicColumnRuntime<>("empleados", "Empleados", COLUMNAS_BASE, tabla, datos, Empleado::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private CheckBox chkMostrarBajas;

    public EmpleadosView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Empleados");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        chkMostrarBajas = new CheckBox("Mostrar empleados dados de baja");
        chkMostrarBajas.setOnAction(e -> cargar());

        Button btnNuevo     = btn("+ Nuevo", this::nuevo);
        Button btnEditar    = btn("✏ Editar", this::editar);
        Button btnBaja      = btn("🚫 Dar de baja", this::darDeBaja);
        Button btnReactivar = btn("✅ Reactivar", this::reactivar);
        Button btnImportar  = btn("📥 Importar", this::importar);
        Button btnExportar   = btn("📤 Exportar", this::exportar);
        Button btnPreview    = btn("👁 Previsualizar", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas", dynamicColumns::configure);
        chkMostrarBajas.setTooltip(new Tooltip("Incluir en la tabla los empleados dados de baja"));
        btnNuevo.setTooltip(new Tooltip("Crear un nuevo empleado"));
        btnEditar.setTooltip(new Tooltip("Editar el empleado seleccionado"));
        btnBaja.setTooltip(new Tooltip("Dar de baja al empleado seleccionado"));
        btnReactivar.setTooltip(new Tooltip("Reactivar un empleado dado de baja"));
        btnImportar.setTooltip(new Tooltip("Importar empleados desde CSV, Excel o JSON"));
        btnExportar.setTooltip(new Tooltip("Exportar empleados a PDF, Excel u otros formatos"));
        btnPreview.setTooltip(new Tooltip("Previsualizar ficha del empleado en PDF"));
        btnColumnas.setTooltip(new Tooltip("Mostrar u ocultar columnas de la tabla"));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, chkMostrarBajas, sp, btnReactivar, btnBaja, btnEditar, btnNuevo, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Empleado> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Estado (activo / baja)
        TableColumn<Empleado, Boolean> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                Empleado e = getTableRow().getItem();
                if (v) {
                    setText("ACTIVO");
                    setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;");
                } else {
                    String baja = (e != null && e.getFechaBaja() != null) ? " · " + e.getFechaBaja() : "";
                    setText("BAJA" + baja);
                    setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;");
                }
            }
        });
        colEstado.setPrefWidth(140);
        colEstado.setUserData("activo");

        TableColumn<Empleado, Double> colSalario = new TableColumn<>("Salario base");
        colSalario.setCellValueFactory(new PropertyValueFactory<>("salarioBase"));
        colSalario.setUserData("salario_base");
        colSalario.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Empleado, Double> colIrpf = new TableColumn<>("IRPF");
        colIrpf.setCellValueFactory(new PropertyValueFactory<>("irpf"));
        colIrpf.setUserData("irpf");
        colIrpf.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v + " %");
            }
        });

        tabla.getColumns().addAll(
            col("Nombre",    "nombre",    140),
            col("Apellidos",  "apellidos",  140),
            col("NIF",       "nif",        95),
            col("Categoría", "categoria", 120),
            colSalario,
            colIrpf,
            col("Fecha alta", "fechaAlta",  100),
            col("Teléfono",   "telefono",   110),
            col("Email",      "email",      170),
            colEstado
        );
        tabla.setPlaceholder(Icons.emptyState("No hay empleados registrados todavía"));
        return tabla;
    }

    private void cargar() {
        try {
            if (chkMostrarBajas != null && chkMostrarBajas.isSelected())
                datos.setAll(dao.findAllIncluirBajas());
            else
                datos.setAll(dao.findAll());
            dynamicColumns.apply();
        } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        Empleado e = new Empleado();
        e.setFechaAlta(LocalDate.now().toString());
        e.setActivo(true);
        e.setIrpf(15.0);
        e.setSalarioBase(1200.0);
        dialogo(e).ifPresent(emp -> {
            try { dao.save(emp); dynamicColumns.saveFormFields(emp, dialogExtraFields); cargar(); } catch (Exception ex) { mostrarError(ex); }
        });
    }

    private void editar() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un empleado para editar."); return; }
        try {
            Empleado e = dao.findById(sel.getId());
            dialogo(e).ifPresent(emp -> {
                try { dao.save(emp); dynamicColumns.saveFormFields(emp, dialogExtraFields); cargar(); } catch (Exception ex) { mostrarError(ex); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void darDeBaja() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un empleado."); return; }
        if (!sel.isActivo()) { alerta("El empleado ya está dado de baja."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Dar de baja a " + sel.getNombreCompleto() + "?\nSe registrará la fecha de hoy como fecha de baja.",
            ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void reactivar() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un empleado."); return; }
        if (sel.isActivo()) { alerta("El empleado ya está activo."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Reactivar a " + sel.getNombreCompleto() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.reactivar(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Empleado> dialogo(Empleado e) {
        Dialog<Empleado> dlg = new Dialog<>();
        dlg.setTitle(e.getId() == 0 ? "Nuevo empleado" : "Editar empleado — " + e.getNombreCompleto());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(540);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre    = tf(e.getNombre());
        TextField fApellido  = tf(e.getApellidos());
        TextField fNif       = tf(e.getNif());
        ComboBox<String> fCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        fCat.setValue(e.getCategoria() != null ? e.getCategoria() : CATEGORIAS[0]);
        fCat.setMaxWidth(Double.MAX_VALUE);
        TextField fFechaAlta = tf(e.getFechaAlta());
        TextField fSalario   = tf(e.getSalarioBase() > 0 ? String.valueOf(e.getSalarioBase()) : "1200");
        TextField fIrpf      = tf(e.getIrpf() > 0 ? String.valueOf(e.getIrpf()) : "15");
        TextField fTelefono  = tf(e.getTelefono());
        TextField fEmail     = tf(e.getEmail());
        TextField fIban      = tf(e.getIban());
        TextField fDireccion = tf(e.getDireccion());
        CheckBox  chkActivo  = new CheckBox("Activo");
        chkActivo.setSelected(e.isActivo());

        // Fila 0: Nombre, Apellido
        grid.addRow(0, lbl("Nombre *"), fNombre, lbl("Apellidos"), fApellido);
        // Fila 1: NIF, Categoría → reubicado abajo
        // Fila 1: NIF, Categoría
        grid.addRow(1, lbl("NIF"), fNif, lbl("Categoría"), fCat);
        // Fila 2: Fecha alta desplazada
        // Fila 2: Fecha alta + Salario + IRPF
        grid.addRow(2, lbl("Fecha alta"), fFechaAlta, lbl("Salario base (€)"), fSalario);
        // Fila 3: IRPF + Teléfono
        grid.addRow(3, lbl("IRPF (%)"), fIrpf, lbl("Teléfono"), fTelefono);
        // Fila 4: Email (completo)
        grid.add(lbl("Email"), 0, 4); grid.add(fEmail, 1, 4, 3, 1);
        // Fila 5: IBAN (completo)
        grid.add(lbl("IBAN"), 0, 5); grid.add(fIban, 1, 5, 3, 1);
        // Fila 6: Dirección (completo)
        grid.add(lbl("Dirección"), 0, 6); grid.add(fDireccion, 1, 6, 3, 1);
        // Fila 7: Activo
        grid.add(chkActivo, 1, 7);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(grid, 8, e, dialogExtraFields);

        // Hacer que los campos de texto se expandan
        for (int col : new int[]{1, 3}) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
        }
        GridPane.setHgrow(fNombre,    Priority.ALWAYS);
        GridPane.setHgrow(fNif,       Priority.ALWAYS);
        GridPane.setHgrow(fCat,       Priority.ALWAYS);
        GridPane.setHgrow(fFechaAlta, Priority.ALWAYS);
        GridPane.setHgrow(fSalario,   Priority.ALWAYS);
        GridPane.setHgrow(fIrpf,      Priority.ALWAYS);
        GridPane.setHgrow(fTelefono,  Priority.ALWAYS);
        GridPane.setHgrow(fEmail,     Priority.ALWAYS);
        GridPane.setHgrow(fIban,      Priority.ALWAYS);
        GridPane.setHgrow(fDireccion, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);

        Icons.markRequired(fNombre);
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            e.setNombre(fNombre.getText().trim());
            e.setApellidos(fApellido.getText().trim());
            e.setNif(fNif.getText().trim());
            e.setCategoria(fCat.getValue());
            e.setFechaAlta(fFechaAlta.getText().trim());
            e.setSalarioBase(parseDouble(fSalario.getText()));
            e.setIrpf(parseDouble(fIrpf.getText()));
            e.setTelefono(fTelefono.getText().trim());
            e.setEmail(fEmail.getText().trim());
            e.setIban(fIban.getText().trim());
            e.setDireccion(fDireccion.getText().trim());
            e.setActivo(chkActivo.isSelected());
            return e;
        });

        return dlg.showAndWait();
    }

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar empleados");
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
                        Empleado.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Empleado.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
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
                "Tabla de empleados como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de empleados y nóminas.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todos los empleados (con nóminas) en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de empleados como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de empleados en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar empleados");
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
        fc.setInitialFileName("Empleados_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Empleados", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Empleado> selExp = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarEmpleadosCSV(destino);
                    case "sql"    -> ExportService.exportarEmpleadosSQL(destino);
                    case "json"   -> ExportService.exportarEmpleadosJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Empleado emp = selExp.get(0);
                            Path pdf = new PDFService().generarFichaEmpleado(emp);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarEmpleadosPDF(destino, dao.findAllIncluirBajas());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            ExportService.exportarEmpleadoDetalladoWord(destino, selExp.get(0));
                        } else {
                            ExportService.exportarEmpleadosWord(destino, dao.findAllIncluirBajas());
                        }
                    }
                    case "excel"  -> ExportService.exportarEmpleadosExcel(destino, dao.findAllIncluirBajas());
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

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Empleado, T> col(String t, String campo, double ancho) {
        TableColumn<Empleado, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setUserData(toDbColumn(campo));
        c.setPrefWidth(ancho); return c;
    }

    private void previsualizar() {
        List<Empleado> sel = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Empleado> lista = sel.isEmpty() ? new ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Empleado emp = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaEmpleado(emp);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Empleado " + emp.getNombreCompleto();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarEmpleados(lista);
                    tituloVentana = "Previsualización — Empleados (" + lista.size() + " registro(s))";
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

    private Button btn(String t, Runnable r) {
        String label = t.replaceFirst("^\\P{L}+", "").strip();
        Button b = new Button(label);
        b.getStyleClass().add("btn-toolbar");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private String toDbColumn(String campo) {
        return switch (campo) {
            case "fechaAlta" -> "fecha_alta";
            case "fechaBaja" -> "fecha_baja";
            case "salarioBase" -> "salario_base";
            default -> campo;
        };
    }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
