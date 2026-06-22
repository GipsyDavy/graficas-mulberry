package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.dao.EmpleadoDAO;
import org.gipsybuho.dao.NominaDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.NominaService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NominasView extends VBox {

    private final NominaDAO dao;
    private final EmpleadoDAO empleadoDAO;
    private TextField txtBuscar;
    private final NominaService nominaService = new NominaService();
    private Label lblContador = new Label();
    private final ObservableList<Nomina> datos = FXCollections.observableArrayList();
    private final TableView<Nomina> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("empleado_id", "Empleado");
        COLUMNAS_BASE.put("mes", "Mes");
        COLUMNAS_BASE.put("anio", "Año");
        COLUMNAS_BASE.put("salario_base", "Salario base");
        COLUMNAS_BASE.put("complementos", "Complementos");
        COLUMNAS_BASE.put("horas_extra_normales", "H. extra normales");
        COLUMNAS_BASE.put("precio_hora_extra", "Precio hora extra");
        COLUMNAS_BASE.put("horas_extra_festivas", "H. extra festivas");
        COLUMNAS_BASE.put("precio_hora_festiva", "Precio hora festiva");
        COLUMNAS_BASE.put("percepciones_no_salariales", "No salarial");
        COLUMNAS_BASE.put("total_bruto", "Bruto");
        COLUMNAS_BASE.put("irpf_porcentaje", "IRPF %");
        COLUMNAS_BASE.put("irpf_importe", "IRPF");
        COLUMNAS_BASE.put("ss_trabajador", "SS trabajador");
        COLUMNAS_BASE.put("total_deducciones", "Deducciones");
        COLUMNAS_BASE.put("neto", "Neto");
        COLUMNAS_BASE.put("ss_empresa", "SS empresa");
        COLUMNAS_BASE.put("coste_total_empresa", "Coste empresa");
        COLUMNAS_BASE.put("created_at", "Creado");
    }
    private final DynamicColumnRuntime<Nomina> dynamicColumns =
        new DynamicColumnRuntime<>("nominas", t("nav.nominas"), COLUMNAS_BASE, tabla, datos, Nomina::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();

    public NominasView() {
        try {
            Connection conn = DatabaseManager.getConnection();
            dao = new NominaDAO(conn);
            empleadoDAO = new EmpleadoDAO(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label(t("nominas.titulo"));
        titulo.getStyleClass().add("view-title");
        Label sub = new Label(t("nominas.subtitulo"));
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNueva    = btn(t("nominas.btn.nueva"),        this::nueva);
        Button btnEditar   = btn(t("nominas.btn.editar"),       this::editar);
        Button btnBorrar   = btn(t("nominas.btn.borrar"),       this::borrar);
        Button btnImportar = btn(t("nominas.btn.importar"),     this::importar);
        Button btnExportar = btn(t("nominas.btn.exportar"),     this::exportar);
        Button btnGenMes   = btn(t("nominas.btn.generar_mes"),  this::generarMesCompleto);
        Button btnPreview  = btn(t("nominas.btn.previsualizar"), this::previsualizar);
        Button btnColumnas = btn(t("nominas.btn.columnas"),     dynamicColumns::configure);
        btnNueva.setTooltip(Tooltips.of(t("nominas.btn.nueva.tip")));
        btnEditar.setTooltip(Tooltips.of(t("nominas.btn.editar.tip")));
        btnBorrar.setTooltip(Tooltips.of(t("nominas.btn.borrar.tip")));
        btnImportar.setTooltip(Tooltips.of(t("nominas.btn.importar.tip")));
        btnExportar.setTooltip(Tooltips.of(t("nominas.btn.exportar.tip")));
        btnGenMes.setTooltip(Tooltips.of(t("nominas.btn.generar_mes.tip")));
        btnPreview.setTooltip(Tooltips.of(t("nominas.btn.previsualizar.tip")));
        btnColumnas.setTooltip(Tooltips.of(t("nominas.btn.columnas.tip")));

        txtBuscar = new TextField();
        txtBuscar.setPromptText(t("nominas.buscar.prompt"));
        txtBuscar.setPrefWidth(220);
        txtBuscar.textProperty().addListener((o, a, b) -> cargar());
        txtBuscar.setTooltip(Tooltips.of(t("nominas.buscar.tooltip")));

        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, txtBuscar, lblContador, sp, btnNueva, btnEditar, btnBorrar, btnImportar, btnExportar, btnGenMes, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Nomina> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Nomina, Double> colNeto = new TableColumn<>(t("nominas.col.neto"));
        colNeto.setCellValueFactory(new PropertyValueFactory<>("neto"));
        colNeto.setUserData("neto");
        colNeto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
                if (!empty && v != null) setStyle("-fx-font-weight:bold; -fx-text-fill:#27AE60;");
            }
        });

        TableColumn<Nomina, Double> colBruto = new TableColumn<>(t("nominas.col.bruto"));
        colBruto.setCellValueFactory(new PropertyValueFactory<>("totalBruto"));
        colBruto.setUserData("total_bruto");
        colBruto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Nomina, Double> colCosteEmp = new TableColumn<>(t("nominas.col.coste_empresa"));
        colCosteEmp.setCellValueFactory(new PropertyValueFactory<>("costeTotalEmpresa"));
        colCosteEmp.setUserData("coste_total_empresa");
        colCosteEmp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
                if (!empty) setStyle("-fx-text-fill:#E74C3C;");
            }
        });

        tabla.getColumns().addAll(
            col(t("nominas.col.empleado"), "empleadoNombre", 180),
            col(t("nominas.col.periodo"), "periodo", 130),
            colBruto, colNeto, colCosteEmp
        );
        tabla.setPlaceholder(Icons.emptyState(t("nominas.placeholder")));
        return tabla;
    }

    private void cargar() {
        try {
            String q = txtBuscar != null ? txtBuscar.getText().strip().toLowerCase() : "";
            var lista = dao.findAll();
            if (!q.isBlank()) lista = lista.stream()
                .filter(n -> contiene(n.getEmpleadoNombre(), q) || contiene(n.getPeriodo(), q))
                .toList();
            datos.setAll(lista);
            lblContador.setText(tf("nominas.contador", lista.size()));
            dynamicColumns.apply(); TableColumnSizing.animarFilas(tabla);
        } catch (Exception e) { mostrarError(e); }
    }

    private boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase().contains(q);
    }

    private void nueva() {
        try {
            List<Empleado> empleados = empleadoDAO.findAll();
            if (empleados.isEmpty()) { alerta(t("nominas.nueva.sin_empleados")); return; }
            dialogoNomina(null, empleados).ifPresent(n -> {
                try { dao.save(n); dynamicColumns.saveFormFields(n, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Nomina sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("nominas.editar.sin_seleccion")); return; }
        try {
            List<Empleado> empleados = empleadoDAO.findAll();
            Nomina n = dao.findById(sel.getId());
            dialogoNomina(n, empleados).ifPresent(nm -> {
                try { dao.save(nm); dynamicColumns.saveFormFields(nm, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        List<Nomina> seleccionadas = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionadas.isEmpty()) { alerta(t("nominas.borrar.sin_seleccion")); return; }
        String mensaje = seleccionadas.size() == 1
            ? tf("nominas.borrar.confirmar.uno", seleccionadas.get(0).getEmpleadoNombre(), seleccionadas.get(0).getPeriodo())
            : tf("nominas.borrar.confirmar.varios", seleccionadas.size());
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje,
            ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Nomina nomina : seleccionadas) dao.delete(nomina.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void generarMesCompleto() {
        LocalDate hoy = LocalDate.now();
        // Pedir mes/año
        Dialog<int[]> dlg = new Dialog<>();
        dlg.setTitle(t("nominas.generar_mes.titulo"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        ComboBox<String> fMes = new ComboBox<>(FXCollections.observableArrayList(Nomina.MESES));
        fMes.setValue(Nomina.MESES[hoy.getMonthValue() - 1]);
        TextField fAnio = new TextField(String.valueOf(hoy.getYear()));
        grid.addRow(0, new Label(t("nominas.generar_mes.campo_mes")), fMes);
        grid.addRow(1, new Label(t("nominas.generar_mes.campo_anio")), fAnio);
        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(bt -> bt == ButtonType.OK ?
            new int[]{java.util.Arrays.asList(Nomina.MESES).indexOf(fMes.getValue()) + 1,
                parseInt(fAnio.getText(), hoy.getYear())} : null);
        dlg.showAndWait().ifPresent(periodoArr -> {
            try {
                List<Empleado> empleados = empleadoDAO.findAll();
                int creadas = 0;
                for (Empleado emp : empleados) {
                    try {
                        Nomina n = nominaService.calcular(emp, periodoArr[0], periodoArr[1], 0, 0, 15, 0, 22, 0);
                        dao.save(n);
                        creadas++;
                    } catch (Exception ex) { /* ya existe este mes */ }
                }
                cargar();
                new Alert(Alert.AlertType.INFORMATION, tf("nominas.generar_mes.resultado", creadas), ButtonType.OK).showAndWait();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private java.util.Optional<Nomina> dialogoNomina(Nomina nomina, List<Empleado> empleados) {
        boolean esNueva = nomina == null;
        LocalDate hoy = LocalDate.now();

        Dialog<Nomina> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? t("nominas.dialogo.nueva") : t("nominas.dialogo.editar"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));

        ComboBox<Empleado> fEmp = new ComboBox<>(FXCollections.observableArrayList(empleados));
        ComboBox<String> fMes = new ComboBox<>(FXCollections.observableArrayList(Nomina.MESES));
        TextField fAnio = new TextField(nomina != null ? String.valueOf(nomina.getAnio()) : String.valueOf(hoy.getYear()));
        TextField fSalBase = new TextField();
        TextField fComplementos = new TextField("0");
        TextField fHorasN = new TextField("0");
        TextField fPrecioHN = new TextField("15.00");
        TextField fHorasF = new TextField("0");
        TextField fPrecioHF = new TextField("22.00");
        TextField fNoSalarial = new TextField("0");
        TextField fIrpf = new TextField();

        Label lblBruto = new Label("—");
        Label lblSS = new Label("—");
        Label lblIRPF = new Label("—");
        Label lblNeto = new Label("—");
        Label lblCoste = new Label("—");
        lblNeto.setStyle("-fx-font-weight:bold; -fx-text-fill:#27AE60; -fx-font-size:14;");

        if (nomina != null) {
            empleados.stream().filter(e -> e.getId() == nomina.getEmpleadoId()).findFirst().ifPresent(fEmp::setValue);
            fMes.setValue(Nomina.MESES[nomina.getMes() - 1]);
            fSalBase.setText(String.valueOf(nomina.getSalarioBase()));
            fComplementos.setText(String.valueOf(nomina.getComplementos()));
            fHorasN.setText(String.valueOf(nomina.getHorasExtraNormales()));
            fPrecioHN.setText(String.valueOf(nomina.getPrecioHoraExtra()));
            fHorasF.setText(String.valueOf(nomina.getHorasExtraFestivas()));
            fPrecioHF.setText(String.valueOf(nomina.getPrecioHoraFestiva()));
            fNoSalarial.setText(String.valueOf(nomina.getPercepcionesNoSalariales()));
            fIrpf.setText(String.valueOf(nomina.getIrpfPorcentaje()));
        } else {
            if (!empleados.isEmpty()) fEmp.setValue(empleados.get(0));
            fMes.setValue(Nomina.MESES[hoy.getMonthValue() - 1]);
        }

        fEmp.setOnAction(e -> {
            Empleado emp = fEmp.getValue();
            if (emp != null) {
                fSalBase.setText(String.valueOf(emp.getSalarioBase()));
                fIrpf.setText(String.valueOf(emp.getIrpf()));
            }
        });
        if (fEmp.getValue() != null) {
            fSalBase.setText(String.valueOf(fEmp.getValue().getSalarioBase()));
            if (fIrpf.getText().isBlank()) fIrpf.setText(String.valueOf(fEmp.getValue().getIrpf()));
        }

        Runnable recalc = () -> {
            Empleado emp = fEmp.getValue();
            if (emp == null) return;
            Empleado empCalc = new Empleado();
            empCalc.setId(emp.getId());
            empCalc.setNombre(emp.getNombre());
            empCalc.setApellidos(emp.getApellidos());
            empCalc.setSalarioBase(parseDouble(fSalBase.getText()));
            empCalc.setIrpf(parseDouble(fIrpf.getText()));
            Nomina tmp = nominaService.calcular(empCalc,
                java.util.Arrays.asList(Nomina.MESES).indexOf(fMes.getValue()) + 1,
                parseInt(fAnio.getText(), hoy.getYear()),
                parseDouble(fComplementos.getText()),
                parseInt(fHorasN.getText(), 0), parseDouble(fPrecioHN.getText()),
                parseInt(fHorasF.getText(), 0), parseDouble(fPrecioHF.getText()),
                parseDouble(fNoSalarial.getText())
            );
            lblBruto.setText(String.format("%.2f €", tmp.getTotalBruto()));
            lblSS.setText(String.format("%.2f €", tmp.getSsTrabajador()));
            lblIRPF.setText(String.format("%.2f €", tmp.getIrpfImporte()));
            lblNeto.setText(String.format("%.2f €", tmp.getNeto()));
            lblCoste.setText(String.format("%.2f €", tmp.getCosteTotalEmpresa()));
        };

        for (TextField tf : new TextField[]{fSalBase,fComplementos,fHorasN,fPrecioHN,fHorasF,fPrecioHF,fNoSalarial,fIrpf})
            tf.textProperty().addListener((o,a,b) -> recalc.run());

        int r = 0;
        grid.addRow(r++, new Label(t("nominas.campo.empleado")), fEmp);
        grid.addRow(r++, new Label(t("nominas.campo.mes")), fMes, new Label(t("nominas.campo.anio")), fAnio);
        grid.add(new Separator(), 0, r++, 4, 1);
        grid.addRow(r++, new Label(t("nominas.campo.salario_base")), fSalBase, new Label(t("nominas.campo.irpf")), fIrpf);
        grid.addRow(r++, new Label(t("nominas.campo.complementos")), fComplementos, new Label(t("nominas.campo.no_salarial")), fNoSalarial);
        grid.addRow(r++, new Label(t("nominas.campo.horas_extra_normales")), fHorasN, new Label(t("nominas.campo.precio_hora")), fPrecioHN);
        grid.addRow(r++, new Label(t("nominas.campo.horas_extra_festivas")), fHorasF, new Label(t("nominas.campo.precio_hora")), fPrecioHF);
        grid.add(new Separator(), 0, r++, 4, 1);

        VBox resumen = new VBox(4,
            row2(t("nominas.resumen.total_bruto"), lblBruto),
            row2(t("nominas.resumen.ss_trabajador"), lblSS),
            row2(t("nominas.resumen.irpf"), lblIRPF),
            row2(t("nominas.resumen.neto"), lblNeto),
            row2(t("nominas.resumen.coste_empresa"), lblCoste)
        );
        resumen.setPadding(new Insets(8));
        resumen.setStyle("-fx-background-color:#F5F5F5; -fx-border-radius:4; -fx-background-radius:4;");
        dialogExtraFields = new LinkedHashMap<>();
        r = dynamicColumns.addFormFields(grid, r, nomina != null ? nomina : new Nomina(), dialogExtraFields);
        grid.add(resumen, 0, r, 4, 1);

        recalc.run();
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK || fEmp.getValue() == null) return null;
            Empleado emp = fEmp.getValue();
            Empleado empCalc = new Empleado();
            empCalc.setId(emp.getId());
            empCalc.setSalarioBase(parseDouble(fSalBase.getText()));
            empCalc.setIrpf(parseDouble(fIrpf.getText()));
            Nomina n = nominaService.calcular(empCalc,
                java.util.Arrays.asList(Nomina.MESES).indexOf(fMes.getValue()) + 1,
                parseInt(fAnio.getText(), hoy.getYear()),
                parseDouble(fComplementos.getText()),
                parseInt(fHorasN.getText(), 0), parseDouble(fPrecioHN.getText()),
                parseInt(fHorasF.getText(), 0), parseDouble(fPrecioHF.getText()),
                parseDouble(fNoSalarial.getText())
            );
            n.setEmpleadoId(emp.getId());
            n.setEmpleadoNombre(emp.getNombreCompleto());
            if (nomina != null) n.setId(nomina.getId());
            return n;
        });
        return dlg.showAndWait();
    }

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle(t("nominas.importar.titulo"));
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(t("nominas.importar.filtro"), "*.csv", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.json"),
            new FileChooser.ExtensionFilter(t("nominas.importar.todos_archivos"), "*.*"));
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
                        Nomina.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Nomina.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
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
        sb.append(tf("nominas.importar.completada", r.duracion().toMillis() / 1000.0)).append(System.lineSeparator());
        sb.append(tf("nominas.importar.filas_importadas", r.filasImportadas())).append(System.lineSeparator());
        sb.append(tf("nominas.importar.filas_actualizadas", r.filasActualizadas())).append(System.lineSeparator());
        sb.append(tf("nominas.importar.filas_descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append(t("nominas.importar.errores_header"));
            r.errores().stream().limit(10).forEach(e ->
                sb.append(tf("nominas.importar.error_fila",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle(t("nominas.importar.resultado.titulo"));
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        if (getScene() != null) a.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        a.showAndWait();
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", t("export.fmt.sqlite.label"), t("export.fmt.sqlite.desc"),        "db"},
            {"csv",    t("export.fmt.csv.label"),    t("nominas.export.csv.desc"),        "csv"},
            {"sql",    t("export.fmt.sql.label"),    t("nominas.export.sql.desc"),        "sql"},
            {"json",   t("export.fmt.json.label"),   t("nominas.export.json.desc"),       "json"},
            {"pdf",    t("export.fmt.pdf.label"),    t("nominas.export.pdf.desc"),        "pdf"},
            {"word",   t("export.fmt.word.label"),   t("nominas.export.word.desc"),       "docx"},
            {"excel",  t("export.fmt.excel.label"),  t("nominas.export.excel.desc"),      "xlsx"}
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
        dlg.setTitle(t("nominas.exportar.titulo"));
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
        fc.setInitialFileName(t("nav.nominas") + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(tf("nominas.export.filtro", fmt[3].toUpperCase()), "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Nomina> selExp = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarNominasCSV(destino);
                    case "sql"    -> ExportService.exportarNominasSQL(destino);
                    case "json"   -> ExportService.exportarNominasJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Nomina n = dao.findById(selExp.get(0).getId());
                            Empleado emp = empleadoDAO.findById(n.getEmpleadoId());
                            Path pdf = new PDFService().generarNomina(n, emp);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarNominasPDF(destino, dao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            Nomina n = dao.findById(selExp.get(0).getId());
                            Empleado emp = empleadoDAO.findById(n.getEmpleadoId());
                            ExportService.exportarNominaDetalladaWord(destino, n, emp);
                        } else {
                            ExportService.exportarNominasWord(destino, dao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarNominasExcel(destino, dao.findAll());
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

    private HBox row2(String label, Label valor) {
        Label lbl = new Label(label);
        lbl.setMinWidth(160);
        HBox h = new HBox(8, lbl, valor);
        h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return h;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Nomina, T> col(String t, String campo, double ancho) {
        TableColumn<Nomina, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setUserData(toDbColumn(campo));
        c.setPrefWidth(ancho); return c;
    }

    private void previsualizar() {
        List<Nomina> sel = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Nomina> lista = sel.isEmpty() ? new ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta(t("nominas.previsualizar.sin_seleccion")); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Nomina n = dao.findById(lista.get(0).getId());
                    Empleado emp = empleadoDAO.findById(n.getEmpleadoId());
                    Path pdfPath = new PDFService().generarNomina(n, emp);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = tf("nominas.previsualizar.titulo.uno", n.getEmpleadoNombre(), n.getPeriodo());
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarNominas(lista);
                    tituloVentana = tf("nominas.previsualizar.titulo.varios", lista.size());
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

    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private String toDbColumn(String campo) {
        return switch (campo) {
            case "empleadoNombre" -> "empleado_id";
            case "periodo" -> "mes";
            case "totalBruto" -> "total_bruto";
            case "costeTotalEmpresa" -> "coste_total_empresa";
            default -> campo;
        };
    }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("nominas.empleado_id, nominas.mes, nominas.anio")) {
            msg = t("nominas.error.duplicado_nomina");
        } else if (msg != null && msg.contains("UNIQUE constraint failed")) {
            msg = t("nominas.error.duplicado_generico");
        }
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
