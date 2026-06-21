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
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.PreferenceService;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.ToastService;

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
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

    private final EmpleadoDAO dao;
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
        new DynamicColumnRuntime<>("empleados", t("nav.empleados"), COLUMNAS_BASE, tabla, datos, Empleado::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private CheckBox chkMostrarBajas;
    private TextField txtBuscar;
    private Label lblContador = new Label();

    public EmpleadosView() {
        try {
            dao = new EmpleadoDAO(DatabaseManager.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label(t("nav.empleados"));
        titulo.getStyleClass().add("view-title");

        Label hint = buildBeginnerHint();
        getChildren().addAll(titulo, hint, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private Label buildBeginnerHint() {
        Label hint = new Label(t("empleados.hint"));
        hint.getStyleClass().add("beginner-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        PreferenceService prefs = PreferenceService.getInstance();
        hint.visibleProperty().bind(prefs.beginnerModeProperty());
        hint.managedProperty().bind(prefs.beginnerModeProperty());
        return hint;
    }

    private HBox buildToolbar() {
        chkMostrarBajas = new CheckBox(t("empleados.toolbar.mostrar_bajas"));
        chkMostrarBajas.setOnAction(e -> cargar());

        Button btnNuevo     = btn(t("empleados.btn.nuevo"),      this::nuevo);
        Button btnEditar    = btn(t("empleados.btn.editar"),     this::editar);
        Button btnBaja      = btn(t("empleados.btn.baja"),       this::darDeBaja);
        Button btnReactivar = btn(t("empleados.btn.reactivar"),  this::reactivar);
        Button btnImportar  = btn(t("empleados.btn.importar"),   this::importar);
        Button btnExportar   = btn(t("empleados.btn.exportar"),   this::exportar);
        Button btnPreview    = btn(t("empleados.btn.previsualizar"), this::previsualizar);
        Button btnColumnas   = btn(t("empleados.btn.columnas"),   dynamicColumns::configure);
        chkMostrarBajas.setTooltip(new Tooltip(t("empleados.toolbar.mostrar_bajas.tip")));
        btnNuevo.setTooltip(new Tooltip(t("empleados.btn.nuevo.tip")));
        btnEditar.setTooltip(new Tooltip(t("empleados.btn.editar.tip")));
        btnBaja.setTooltip(new Tooltip(t("empleados.btn.baja.tip")));
        btnReactivar.setTooltip(new Tooltip(t("empleados.btn.reactivar.tip")));
        btnImportar.setTooltip(new Tooltip(t("empleados.btn.importar.tip")));
        btnExportar.setTooltip(new Tooltip(t("empleados.btn.exportar.tip")));
        btnPreview.setTooltip(new Tooltip(t("empleados.btn.previsualizar.tip")));
        btnColumnas.setTooltip(new Tooltip(t("empleados.btn.columnas.tip")));

        txtBuscar = new TextField();
        txtBuscar.setPromptText(t("empleados.buscar.prompt"));
        txtBuscar.setPrefWidth(220);
        txtBuscar.textProperty().addListener((o, a, b) -> cargar());
        txtBuscar.setTooltip(new Tooltip(t("empleados.buscar.tooltip")));

        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, chkMostrarBajas, txtBuscar, lblContador, sp, btnReactivar, btnBaja, btnEditar, btnNuevo, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Empleado> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Estado (activo / baja)
        TableColumn<Empleado, Boolean> colEstado = new TableColumn<>(t("empleados.col.estado"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                Empleado e = getTableRow().getItem();
                if (v) {
                    setText(t("empleados.estado.activo"));
                    setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;");
                } else {
                    String baja = (e != null && e.getFechaBaja() != null) ? " · " + e.getFechaBaja() : "";
                    setText(t("empleados.estado.baja") + baja);
                    setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;");
                }
            }
        });
        colEstado.setPrefWidth(140);
        colEstado.setUserData("activo");

        TableColumn<Empleado, Double> colSalario = new TableColumn<>(t("empleados.col.salario_base"));
        colSalario.setCellValueFactory(new PropertyValueFactory<>("salarioBase"));
        colSalario.setUserData("salario_base");
        colSalario.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Empleado, Double> colIrpf = new TableColumn<>(t("empleados.col.irpf"));
        colIrpf.setCellValueFactory(new PropertyValueFactory<>("irpf"));
        colIrpf.setUserData("irpf");
        colIrpf.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v + " %");
            }
        });

        tabla.getColumns().addAll(
            col(t("empleados.col.nombre"),    "nombre",    140),
            col(t("empleados.col.apellidos"),  "apellidos",  140),
            col(t("empleados.col.nif"),       "nif",        95),
            col(t("empleados.col.categoria"), "categoria", 120),
            colSalario,
            colIrpf,
            col(t("empleados.col.fecha_alta"), "fechaAlta",  100),
            col(t("empleados.col.telefono"),   "telefono",   110),
            col(t("empleados.col.email"),      "email",      170),
            colEstado
        );
        tabla.setPlaceholder(Icons.emptyState(t("empleados.tabla.vacia")));
        return tabla;
    }

    private void cargar() {
        try {
            var lista = (chkMostrarBajas != null && chkMostrarBajas.isSelected())
                ? dao.findAllIncluirBajas() : dao.findAll();
            String q = txtBuscar != null ? txtBuscar.getText().strip().toLowerCase() : "";
            if (!q.isBlank()) lista = lista.stream()
                .filter(e -> contiene(e.getNombre(), q) || contiene(e.getApellidos(), q)
                          || contiene(e.getEmail(), q)   || contiene(e.getNif(), q))
                .toList();
            datos.setAll(lista);
            lblContador.setText(tf("empleados.contador", lista.size()));
            dynamicColumns.apply(); TableColumnSizing.animarFilas(tabla);
        } catch (Exception e) { mostrarError(e); }
    }

    private boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase().contains(q);
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
        if (sel == null) { alerta(t("empleados.seleccion.editar")); return; }
        try {
            Empleado e = dao.findById(sel.getId());
            dialogo(e).ifPresent(emp -> {
                try { dao.save(emp); dynamicColumns.saveFormFields(emp, dialogExtraFields); cargar(); } catch (Exception ex) { mostrarError(ex); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void darDeBaja() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("empleados.seleccion.generica")); return; }
        if (!sel.isActivo()) { alerta(t("empleados.error.ya_baja")); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            tf("empleados.baja.confirmar", sel.getNombreCompleto()),
            ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void reactivar() {
        Empleado sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("empleados.seleccion.generica")); return; }
        if (sel.isActivo()) { alerta(t("empleados.error.ya_activo")); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            tf("empleados.reactivar.confirmar", sel.getNombreCompleto()), ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.reactivar(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Empleado> dialogo(Empleado e) {
        Dialog<Empleado> dlg = new Dialog<>();
        dlg.setTitle(e.getId() == 0 ? t("empleados.dialogo.nuevo") : tf("empleados.dialogo.editar", e.getNombreCompleto()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(540);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre    = txf(e.getNombre());
        TextField fApellido  = txf(e.getApellidos());
        TextField fNif       = txf(e.getNif());
        ComboBox<String> fCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        fCat.setValue(e.getCategoria() != null ? e.getCategoria() : CATEGORIAS[0]);
        fCat.setMaxWidth(Double.MAX_VALUE);
        TextField fFechaAlta = txf(e.getFechaAlta());
        TextField fSalario   = txf(e.getSalarioBase() > 0 ? String.valueOf(e.getSalarioBase()) : "1200");
        TextField fIrpf      = txf(e.getIrpf() > 0 ? String.valueOf(e.getIrpf()) : "15");
        TextField fTelefono  = txf(e.getTelefono());
        TextField fEmail     = txf(e.getEmail());
        TextField fIban      = txf(e.getIban());
        TextField fDireccion = txf(e.getDireccion());
        CheckBox  chkActivo  = new CheckBox(t("empleados.campo.activo"));
        chkActivo.setSelected(e.isActivo());

        // Fila 0: Nombre, Apellido
        grid.addRow(0, lbl(t("empleados.campo.nombre")), fNombre, lbl(t("empleados.campo.apellidos")), fApellido);
        // Fila 1: NIF, Categoría
        grid.addRow(1, lbl(t("empleados.campo.nif")), fNif, lbl(t("empleados.campo.categoria")), fCat);
        // Fila 2: Fecha alta + Salario + IRPF
        grid.addRow(2, lbl(t("empleados.campo.fecha_alta")), fFechaAlta, lbl(t("empleados.campo.salario_base")), fSalario);
        // Fila 3: IRPF + Teléfono
        grid.addRow(3, lbl(t("empleados.campo.irpf")), fIrpf, lbl(t("empleados.campo.telefono")), fTelefono);
        // Fila 4: Email (completo)
        grid.add(lbl(t("empleados.campo.email")), 0, 4); grid.add(fEmail, 1, 4, 3, 1);
        // Fila 5: IBAN (completo)
        grid.add(lbl(t("empleados.campo.iban")), 0, 5); grid.add(fIban, 1, 5, 3, 1);
        // Fila 6: Dirección (completo)
        grid.add(lbl(t("empleados.campo.direccion")), 0, 6); grid.add(fDireccion, 1, 6, 3, 1);
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
        List<String> css = getScene() != null
            ? new ArrayList<>(getScene().getStylesheets())
            : List.of();
        ModuloWindowManager.abrirEnVentana(
            t("empleados.importar.titulo"),
            () -> new ImportView(ImportService.TipoEntidad.EMPLEADOS, this::cargar),
            css
        );
    }

    private void mostrarResultadoImportacion(org.gipsybuho.service.importer.ImportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(tf("empleados.importar.completada", r.duracion().toMillis() / 1000.0)).append(System.lineSeparator());
        sb.append(tf("empleados.importar.filas_importadas", r.filasImportadas())).append(System.lineSeparator());
        sb.append(tf("empleados.importar.filas_actualizadas", r.filasActualizadas())).append(System.lineSeparator());
        sb.append(tf("empleados.importar.filas_descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append(t("empleados.importar.errores_header"));
            r.errores().stream().limit(10).forEach(e ->
                sb.append(tf("empleados.importar.error_fila",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle(t("empleados.importar.resultado.titulo"));
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        if (getScene() != null) a.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        a.showAndWait();
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", t("export.fmt.sqlite.label"),
                t("export.fmt.sqlite.desc"), "db"},
            {"csv",    t("export.fmt.csv.label"),
                t("empleados.export.csv.desc"), "csv"},
            {"sql",    t("export.fmt.sql.label"),
                t("empleados.export.sql.desc"), "sql"},
            {"json",   t("export.fmt.json.label"),
                t("empleados.export.json.desc"), "json"},
            {"pdf",    t("export.fmt.pdf.label"),
                t("empleados.export.pdf.desc"), "pdf"},
            {"word",   t("export.fmt.word.label"),
                t("empleados.export.word.desc"), "docx"},
            {"excel",  t("export.fmt.excel.label"),
                t("empleados.export.excel.desc"), "xlsx"}
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

        Label lblSelecciona = new Label(t("export.dialog.instruccion"));
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle(t("empleados.exportar.titulo"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(460);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText(t("export.dialog.btn"));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarExportacion);
    }

    private void lanzarExportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle(tf("export.dialog.guardar", fmt[1]));
        fc.setInitialFileName(t("nav.empleados") + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(tf("empleados.export.filtro", fmt[3].toUpperCase()), "*." + fmt[3]));
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
                        tf("export.exito.mensaje", destino), ButtonType.OK);
                    ok.setTitle(t("export.exito.titulo"));
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
        if (lista.isEmpty()) { alerta(t("empleados.previsualizar.sin_seleccion")); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Empleado emp = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaEmpleado(emp);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = tf("empleados.previsualizar.titulo.uno", emp.getNombreCompleto());
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarEmpleados(lista);
                    tituloVentana = tf("empleados.previsualizar.titulo.varios", lista.size());
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

    private TextField txf(String v) { return new TextField(v != null ? v : ""); }
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
    private void mostrarError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : t("common.error.desconocido");
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        if (w != null && msg.contains("UNIQUE constraint failed")) {
            ToastService.error(w, t("empleados.error.nif_duplicado"), "EMP-ERR-1");
        } else {
            new Alert(Alert.AlertType.ERROR, "Error: " + msg, ButtonType.OK).showAndWait();
        }
    }
}
