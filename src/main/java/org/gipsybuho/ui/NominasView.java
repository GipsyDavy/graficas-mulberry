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
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.NominaService;
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

public class NominasView extends VBox {

    private final NominaDAO dao = new NominaDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final NominaService nominaService = new NominaService();
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
        new DynamicColumnRuntime<>("nominas", "Nóminas", COLUMNAS_BASE, tabla, datos, Nomina::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();

    public NominasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Nóminas");
        titulo.getStyleClass().add("view-title");
        Label sub = new Label("Gestión de nóminas según legislación española — SS 2024");
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNueva    = btn("+ Nueva nómina",         "#4C9BE8", this::nueva);
        Button btnEditar   = btn("✏ Editar",                "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",               "#E74C3C", this::borrar);
        Button btnImportar = btn("📥 Importar",              "#27AE60", this::importar);
        Button btnExportar = btn("📤 Exportar",              "#8E44AD", this::exportar);
        Button btnGenMes    = btn("⚡ Generar mes para todos","#9B59B6", this::generarMesCompleto);
        Button btnPreview   = btn("👁 Previsualizar",         "#6B2D5E", this::previsualizar);
        Button btnColumnas  = btn("⚙ Columnas",               "#34495E", dynamicColumns::configure);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNueva, btnEditar, btnBorrar, btnImportar, btnExportar, btnGenMes, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Nomina> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Nomina, Double> colNeto = new TableColumn<>("Neto");
        colNeto.setCellValueFactory(new PropertyValueFactory<>("neto"));
        colNeto.setUserData("neto");
        colNeto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
                if (!empty && v != null) setStyle("-fx-font-weight:bold; -fx-text-fill:#27AE60;");
            }
        });

        TableColumn<Nomina, Double> colBruto = new TableColumn<>("Bruto");
        colBruto.setCellValueFactory(new PropertyValueFactory<>("totalBruto"));
        colBruto.setUserData("total_bruto");
        colBruto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Nomina, Double> colCosteEmp = new TableColumn<>("Coste empresa");
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
            col("Empleado", "empleadoNombre", 180),
            col("Período", "periodo", 130),
            colBruto, colNeto, colCosteEmp
        );
        tabla.setPlaceholder(Icons.emptyState("No hay nóminas registradas todavía"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); dynamicColumns.apply(); } catch (Exception e) { mostrarError(e); }
    }

    private void nueva() {
        try {
            List<Empleado> empleados = empleadoDAO.findAll();
            if (empleados.isEmpty()) { alerta("Añade empleados antes de crear nóminas."); return; }
            dialogoNomina(null, empleados).ifPresent(n -> {
                try { dao.save(n); dynamicColumns.saveFormFields(n, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Nomina sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una nómina para editar."); return; }
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
        if (seleccionadas.isEmpty()) { alerta("Selecciona una o varias nóminas para borrar."); return; }
        String mensaje = seleccionadas.size() == 1
            ? "¿Eliminar la nómina de " + seleccionadas.get(0).getEmpleadoNombre() + " - " + seleccionadas.get(0).getPeriodo() + "?"
            : "¿Eliminar " + seleccionadas.size() + " nóminas seleccionadas?";
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
        dlg.setTitle("Generar nóminas del mes");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        ComboBox<String> fMes = new ComboBox<>(FXCollections.observableArrayList(Nomina.MESES));
        fMes.setValue(Nomina.MESES[hoy.getMonthValue() - 1]);
        TextField fAnio = new TextField(String.valueOf(hoy.getYear()));
        grid.addRow(0, new Label("Mes:"), fMes);
        grid.addRow(1, new Label("Año:"), fAnio);
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
                new Alert(Alert.AlertType.INFORMATION, "Se generaron " + creadas + " nóminas.", ButtonType.OK).showAndWait();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private java.util.Optional<Nomina> dialogoNomina(Nomina nomina, List<Empleado> empleados) {
        boolean esNueva = nomina == null;
        LocalDate hoy = LocalDate.now();

        Dialog<Nomina> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? "Nueva nómina" : "Editar nómina");
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
        grid.addRow(r++, new Label("Empleado *"), fEmp);
        grid.addRow(r++, new Label("Mes"), fMes, new Label("Año"), fAnio);
        grid.add(new Separator(), 0, r++, 4, 1);
        grid.addRow(r++, new Label("Salario base (€)"), fSalBase, new Label("IRPF (%)"), fIrpf);
        grid.addRow(r++, new Label("Complementos (€)"), fComplementos, new Label("No salarial (€)"), fNoSalarial);
        grid.addRow(r++, new Label("H. extra normales"), fHorasN, new Label("Precio/hora (€)"), fPrecioHN);
        grid.addRow(r++, new Label("H. extra festivas"), fHorasF, new Label("Precio/hora (€)"), fPrecioHF);
        grid.add(new Separator(), 0, r++, 4, 1);

        VBox resumen = new VBox(4,
            row2("Total bruto:", lblBruto),
            row2("S.S. trabajador:", lblSS),
            row2("IRPF:", lblIRPF),
            row2("LÍQUIDO NETO:", lblNeto),
            row2("Coste empresa:", lblCoste)
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
        fc.setTitle("Importar nóminas");
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
                "Tabla de nóminas como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de la tabla nóminas.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todas las nóminas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de nóminas como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de nóminas en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar nóminas");
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
        fc.setInitialFileName("Nominas_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Nóminas", "*." + fmt[3]));
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
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
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
                    tituloVentana = "Previsualización — Nómina " + n.getEmpleadoNombre() + " " + n.getPeriodo();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarNominas(lista);
                    tituloVentana = "Previsualización — Nóminas (" + lista.size() + " registro(s))";
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
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
