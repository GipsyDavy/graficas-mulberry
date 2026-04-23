package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.gipsybuho.dao.EmpleadoDAO;
import org.gipsybuho.dao.NominaDAO;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.service.NominaService;
import org.gipsybuho.service.PDFService;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class NominasView extends VBox {

    private final NominaDAO dao = new NominaDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final NominaService nominaService = new NominaService();
    private final ObservableList<Nomina> datos = FXCollections.observableArrayList();
    private final TableView<Nomina> tabla = new TableView<>(datos);

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
    }

    private HBox buildToolbar() {
        Button btnNueva  = btn("+ Nueva nómina",    "#4C9BE8", this::nueva);
        Button btnEditar = btn("✏ Editar",            "#F39C12", this::editar);
        Button btnBorrar = btn("🗑 Borrar",           "#E74C3C", this::borrar);
        Button btnPDF    = btn("📄 Exportar PDF",     "#27AE60", this::exportarPDF);
        Button btnGenMes = btn("⚡ Generar mes para todos", "#9B59B6", this::generarMesCompleto);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNueva, btnEditar, btnBorrar, btnPDF, btnGenMes);
        bar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Nomina> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Nomina, Double> colNeto = new TableColumn<>("Neto");
        colNeto.setCellValueFactory(new PropertyValueFactory<>("neto"));
        colNeto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
                if (!empty && v != null) setStyle("-fx-font-weight:bold; -fx-text-fill:#27AE60;");
            }
        });

        TableColumn<Nomina, Double> colBruto = new TableColumn<>("Bruto");
        colBruto.setCellValueFactory(new PropertyValueFactory<>("totalBruto"));
        colBruto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Nomina, Double> colCosteEmp = new TableColumn<>("Coste empresa");
        colCosteEmp.setCellValueFactory(new PropertyValueFactory<>("costeTotalEmpresa"));
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
        tabla.setPlaceholder(new Label("No hay nóminas registradas"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void nueva() {
        try {
            List<Empleado> empleados = empleadoDAO.findAll();
            if (empleados.isEmpty()) { alerta("Añade empleados antes de crear nóminas."); return; }
            dialogoNomina(null, empleados).ifPresent(n -> {
                try { dao.save(n); cargar(); } catch (Exception e) { mostrarError(e); }
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
                try { dao.save(nm); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        Nomina sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una nómina para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar la nómina de " + sel.getEmpleadoNombre() + " - " + sel.getPeriodo() + "?",
            ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void exportarPDF() {
        Nomina sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una nómina para exportar."); return; }
        try {
            Nomina n = dao.findById(sel.getId());
            Empleado e = empleadoDAO.findById(n.getEmpleadoId());
            Path pdf = new PDFService().generarNomina(n, e);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf.toFile());
            new Alert(Alert.AlertType.INFORMATION, "PDF generado:\n" + pdf, ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
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
            n.setEmpleadoNombre(emp.getNombre());
            if (nomina != null) n.setId(nomina.getId());
            return n;
        });
        return dlg.showAndWait();
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
        c.setPrefWidth(ancho); return c;
    }

    private Button btn(String t, String color, Runnable r) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;");
        b.setOnAction(e -> r.run()); return b;
    }

    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
