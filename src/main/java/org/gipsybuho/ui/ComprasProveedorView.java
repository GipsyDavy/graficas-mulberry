package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.PagoMaterialDAO;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.PagoMaterial;
import org.gipsybuho.service.PreferenceService;

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ComprasProveedorView extends VBox {

    private static final String[] FORMAS_PAGO = {
        "Contado", "15 días", "30 días", "45 días", "60 días", "90 días", "120 días", "Personalizado"
    };
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagoMaterialDAO pagoDao     = new PagoMaterialDAO();
    private final MaterialDAO     materialDao  = new MaterialDAO();

    private final ObservableList<PagoMaterial> datos = FXCollections.observableArrayList();
    private final TableView<PagoMaterial>      tabla = new TableView<>(datos);
    private final Label lblContador       = new Label();
    private final Label lblTotalPendiente = new Label("—");
    private final Label lblVencidos       = new Label("—");
    private final Label lblProximos       = new Label("—");
    private String filtroPagos = "todos";

    public ComprasProveedorView() {
        getStyleClass().add("module-view");
        Label titulo = new Label(t("compras.titulo"));
        titulo.getStyleClass().add("module-title");
        Label hint = buildBeginnerHint();
        buildTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        getChildren().addAll(titulo, hint, buildResumen(), buildToolbar(), tabla);
        cargar();
    }

    private Label buildBeginnerHint() {
        Label hint = new Label(t("compras.hint"));
        hint.getStyleClass().add("beginner-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        PreferenceService prefs = PreferenceService.getInstance();
        hint.visibleProperty().bind(prefs.beginnerModeProperty());
        hint.managedProperty().bind(prefs.beginnerModeProperty());
        return hint;
    }

    private HBox buildResumen() {
        HBox row = new HBox(12);
        row.getChildren().addAll(
            tarjetaResumen(t("compras.resumen.pendiente"), lblTotalPendiente, "#F39C12"),
            tarjetaResumen(t("compras.resumen.vencidos"),  lblVencidos,       "#E74C3C"),
            tarjetaResumen(t("compras.resumen.proximos"),  lblProximos,       "#E67E22")
        );
        return row;
    }

    private VBox tarjetaResumen(String titulo, Label valor, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("dashboard-card");
        card.setPrefWidth(220);
        card.setStyle("-fx-border-color:" + color + ";-fx-border-width:0 0 0 4;");
        Label tit = new Label(titulo);
        tit.getStyleClass().add("card-title");
        valor.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        card.getChildren().addAll(tit, valor);
        return card;
    }

    private HBox buildToolbar() {
        ToggleGroup tg = new ToggleGroup();
        ToggleButton bTodos     = filtroBtn(t("compras.filtro.todos"),      "todos",     tg);
        ToggleButton bPendiente = filtroBtn(t("compras.filtro.pendientes"), "pendiente", tg);
        ToggleButton bVencido   = filtroBtn(t("compras.filtro.vencidos"),   "vencido",   tg);
        ToggleButton bProximo   = filtroBtn(t("compras.filtro.proximos"),   "proximo",   tg);
        ToggleButton bPagado    = filtroBtn(t("compras.filtro.pagados"),    "pagado",    tg);
        bTodos.setSelected(true);

        HBox filtros = new HBox(2, bTodos, bPendiente, bVencido, bProximo, bPagado);
        filtros.setStyle("-fx-background-color:-c-tab-bg;-fx-background-radius:5;-fx-padding:3;");

        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNuevo  = btn(t("compras.btn.nuevo"),         this::nuevoPago);
        Button btnPagado = btn(t("compras.btn.marcar_pagado"), this::marcarPagado);
        Button btnEditar = btn(t("compras.btn.editar"),        this::editarPago);
        Button btnBorrar = btn(t("compras.btn.eliminar"),      this::eliminarPago);

        HBox bar = new HBox(10, filtros, sp, lblContador, btnNuevo, btnPagado, btnEditar, btnBorrar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private ToggleButton filtroBtn(String texto, String estado, ToggleGroup tg) {
        ToggleButton tb = new ToggleButton(texto);
        tb.setToggleGroup(tg);
        tb.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:5 12;" +
            "-fx-background-radius:4;-fx-font-size:12px;");
        tb.selectedProperty().addListener((o, a, sel) -> {
            if (sel) {
                tb.setStyle("-fx-background-color:-c-primary;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 12;-fx-background-radius:4;-fx-font-size:12px;");
                filtroPagos = estado;
                cargar();
            } else {
                tb.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:5 12;" +
                    "-fx-background-radius:4;-fx-font-size:12px;");
            }
        });
        return tb;
    }

    private void buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PagoMaterial, Void> colEst = new TableColumn<>("");
        colEst.setPrefWidth(36);
        colEst.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                PagoMaterial p = getTableRow().getItem();
                Circle dot = new Circle(7);
                Tooltip tip = new Tooltip();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"  -> { dot.setFill(Color.web("#27AE60")); tip.setText(t("compras.estado.pagado")); }
                    case "vencido" -> { dot.setFill(Color.web("#E74C3C")); tip.setText(t("compras.estado.vencido")); }
                    case "proximo" -> { dot.setFill(Color.web("#E67E22")); tip.setText(t("compras.estado.proximo")); }
                    default        -> { dot.setFill(Color.web("#4C9BE8")); tip.setText(t("compras.estado.pendiente")); }
                }
                Tooltip.install(dot, tip);
                setGraphic(dot);
                setText(null);
            }
        });

        TableColumn<PagoMaterial, String> colMat  = new TableColumn<>(t("compras.tabla.material"));
        colMat.setCellValueFactory(new PropertyValueFactory<>("materialNombre"));
        colMat.setPrefWidth(160);

        TableColumn<PagoMaterial, String> colProv = new TableColumn<>(t("compras.tabla.proveedor"));
        colProv.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colProv.setPrefWidth(130);

        TableColumn<PagoMaterial, String> colNum  = new TableColumn<>(t("compras.tabla.num_factura"));
        colNum.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colNum.setPrefWidth(100);

        TableColumn<PagoMaterial, LocalDate> colFc = new TableColumn<>(t("compras.tabla.fecha_compra"));
        colFc.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFc.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });
        colFc.setPrefWidth(105);

        TableColumn<PagoMaterial, Double> colImp  = new TableColumn<>(t("compras.tabla.importe"));
        colImp.setCellValueFactory(new PropertyValueFactory<>("importeTotal"));
        colImp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        colImp.setPrefWidth(90);

        TableColumn<PagoMaterial, String> colFp   = new TableColumn<>(t("compras.tabla.forma_pago"));
        colFp.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colFp.setPrefWidth(90);

        TableColumn<PagoMaterial, LocalDate> colFv = new TableColumn<>(t("compras.tabla.vencimiento"));
        colFv.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colFv.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                setText(v.format(FMT));
                PagoMaterial p = getTableRow().getItem();
                setStyle(switch (p.getEstadoEfectivo()) {
                    case "vencido" -> "-fx-text-fill:#E74C3C;-fx-font-weight:bold;";
                    case "proximo" -> "-fx-text-fill:#E67E22;-fx-font-weight:bold;";
                    case "pagado"  -> "-fx-text-fill:#27AE60;";
                    default        -> "";
                });
            }
        });
        colFv.setPrefWidth(105);

        TableColumn<PagoMaterial, Void> colDias = new TableColumn<>(t("compras.tabla.dias"));
        colDias.setPrefWidth(65);
        colDias.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                PagoMaterial p = getTableRow().getItem();
                if ("pagado".equals(p.getEstado())) { setText("—"); setStyle(""); return; }
                long d = p.getDiasRestantes();
                setText(d < 0 ? t("compras.estado.vencido") : tf("compras.tabla.dias_valor", d));
                setStyle(d <= 0 ? "-fx-text-fill:#E74C3C;" : d <= 7 ? "-fx-text-fill:#E67E22;" : "");
            }
        });

        TableColumn<PagoMaterial, String> colNotas = new TableColumn<>(t("compras.tabla.notas"));
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        colNotas.setPrefWidth(140);

        tabla.getColumns().addAll(colEst, colMat, colProv, colNum, colFc, colImp, colFp, colFv, colDias, colNotas);

        tabla.setRowFactory(tv -> {
            TableRow<PagoMaterial> row = new TableRow<>();
            row.itemProperty().addListener((o, a, p) -> {
                if (p == null) { row.setStyle(""); return; }
                row.setStyle(switch (p.getEstadoEfectivo()) {
                    case "vencido" -> "-fx-background-color:rgba(231,76,60,0.07);";
                    case "proximo" -> "-fx-background-color:rgba(230,126,34,0.07);";
                    default        -> "";
                });
            });
            return row;
        });
    }

    private void cargar() {
        try {
            List<PagoMaterial> todos = pagoDao.findAll();
            List<PagoMaterial> filtrados = switch (filtroPagos) {
                case "pendiente" -> todos.stream().filter(p -> "pendiente".equals(p.getEstadoEfectivo())).toList();
                case "vencido"   -> todos.stream().filter(p -> "vencido".equals(p.getEstadoEfectivo())).toList();
                case "proximo"   -> todos.stream().filter(p -> "proximo".equals(p.getEstadoEfectivo())).toList();
                case "pagado"    -> todos.stream().filter(p -> "pagado".equals(p.getEstado())).toList();
                default          -> todos;
            };
            datos.setAll(filtrados);
            lblContador.setText(tf("compras.contador", filtrados.size()));
            actualizarResumen(todos);
        } catch (Exception e) {
            alerta(tf("compras.error.cargar", e.getMessage()));
        }
    }

    private void actualizarResumen(List<PagoMaterial> todos) {
        double totalPend = todos.stream()
            .filter(p -> !"pagado".equals(p.getEstado()))
            .mapToDouble(PagoMaterial::getImporteTotal).sum();
        long countVenc = todos.stream().filter(p -> "vencido".equals(p.getEstadoEfectivo())).count();
        double sumVenc  = todos.stream().filter(p -> "vencido".equals(p.getEstadoEfectivo()))
            .mapToDouble(PagoMaterial::getImporteTotal).sum();
        long countProx = todos.stream().filter(p -> "proximo".equals(p.getEstadoEfectivo())).count();
        lblTotalPendiente.setText(tf("compras.resumen.importe", totalPend));
        String pagosVenc = countVenc == 1 ? tf("compras.resumen.pagos_uno", countVenc) : tf("compras.resumen.pagos_varios", countVenc);
        lblVencidos.setText(pagosVenc + (countVenc > 0 ? "  " + tf("compras.resumen.importe_paren", sumVenc) : ""));
        lblProximos.setText(countProx == 1 ? tf("compras.resumen.pagos_uno", countProx) : tf("compras.resumen.pagos_varios", countProx));
    }

    private void nuevoPago() {
        dialogoPago(new PagoMaterial()).ifPresent(p -> {
            try { pagoDao.save(p); cargar(); }
            catch (Exception e) { alerta(tf("compras.error.guardar", e.getMessage())); }
        });
    }

    private void editarPago() {
        PagoMaterial sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("compras.alerta.editar")); return; }
        dialogoPago(sel).ifPresent(p -> {
            try { pagoDao.save(p); cargar(); }
            catch (Exception e) { alerta(tf("compras.error.guardar", e.getMessage())); }
        });
    }

    private void marcarPagado() {
        PagoMaterial sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("compras.alerta.marcar")); return; }
        if ("pagado".equals(sel.getEstado())) { alerta(t("compras.alerta.ya_pagado")); return; }

        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle(t("compras.marcar.titulo"));
        dlg.setHeaderText(tf("compras.marcar.header", sel.getMaterialNombre(), sel.getImporteTotal()));
        DatePicker dp = new DatePicker(LocalDate.now());
        VBox content = new VBox(8, new Label(t("compras.marcar.fecha_label")), dp);
        content.setPadding(new Insets(14));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText(t("compras.marcar.confirmar"));
        if (getScene() != null)
            dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? dp.getValue() : null);
        dlg.showAndWait().ifPresent(fecha -> {
            try { pagoDao.marcarPagado(sel.getId(), fecha); cargar(); }
            catch (Exception e) { alerta(tf("compras.error.marcar", e.getMessage())); }
        });
    }

    private void eliminarPago() {
        PagoMaterial sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("compras.alerta.eliminar")); return; }
        conf(tf("compras.eliminar.confirmar", sel.getMaterialNombre(), sel.getImporteTotal()),
            () -> { try { pagoDao.delete(sel.getId()); cargar(); }
                    catch (Exception e) { alerta(tf("compras.error.eliminar", e.getMessage())); } });
    }

    private Optional<PagoMaterial> dialogoPago(PagoMaterial p) {
        Dialog<PagoMaterial> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? t("compras.dialogo.nueva") : t("compras.dialogo.editar"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(520);

        List<Material> materiales;
        try { materiales = materialDao.findAll(); } catch (Exception ex) { materiales = List.of(); }

        ComboBox<Material> cbMaterial = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMaterial.setConverter(new StringConverter<>() {
            @Override public String toString(Material m) { return m == null ? "" : m.getNombre() + " (" + m.getUnidad() + ")"; }
            @Override public Material fromString(String s) { return null; }
        });
        cbMaterial.setMaxWidth(Double.MAX_VALUE);
        if (p.getMaterialId() > 0)
            materiales.stream().filter(m -> m.getId() == p.getMaterialId()).findFirst().ifPresent(cbMaterial::setValue);

        TextField tfProveedor = txf(p.getProveedor());
        TextField tfNumFact   = txf(p.getNumeroFactura());
        TextField tfCantidad  = txf(p.getCantidadComprada() > 0 ? String.valueOf(p.getCantidadComprada()) : "");
        TextField tfImporte   = txf(p.getImporteTotal() > 0 ? String.format("%.2f", p.getImporteTotal()) : "");
        TextArea  taNotas     = new TextArea(p.getNotas() != null ? p.getNotas() : "");
        taNotas.setPrefRowCount(2);
        taNotas.setWrapText(true);

        DatePicker dpCompra      = new DatePicker(p.getFechaCompra() != null ? p.getFechaCompra() : LocalDate.now());
        DatePicker dpVencimiento = new DatePicker(p.getFechaVencimiento());
        dpCompra.setMaxWidth(Double.MAX_VALUE);
        dpVencimiento.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbFormaPago = new ComboBox<>(FXCollections.observableArrayList(FORMAS_PAGO));
        cbFormaPago.setValue(p.getFormaPago() != null ? p.getFormaPago() : "30 días");
        cbFormaPago.setMaxWidth(Double.MAX_VALUE);

        Runnable calcVencimiento = () -> {
            LocalDate compra = dpCompra.getValue();
            if (compra == null) return;
            dpVencimiento.setValue(switch (cbFormaPago.getValue()) {
                case "Contado"  -> compra;
                case "15 días"  -> compra.plusDays(15);
                case "30 días"  -> compra.plusDays(30);
                case "45 días"  -> compra.plusDays(45);
                case "60 días"  -> compra.plusDays(60);
                case "90 días"  -> compra.plusDays(90);
                case "120 días" -> compra.plusDays(120);
                default         -> dpVencimiento.getValue();
            });
        };
        cbFormaPago.setOnAction(e -> calcVencimiento.run());
        dpCompra.setOnAction(e -> calcVencimiento.run());
        cbMaterial.setOnAction(e -> {
            if (cbMaterial.getValue() != null && tfProveedor.getText().isBlank())
                tfProveedor.setText(cbMaterial.getValue().getProveedor() != null
                    ? cbMaterial.getValue().getProveedor() : "");
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(130);
        cc0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        cc1.setFillWidth(true);
        grid.getColumnConstraints().addAll(cc0, cc1);

        int r = 0;
        grid.addRow(r++, lbl(t("compras.campo.material")),     cbMaterial);
        grid.addRow(r++, lbl(t("compras.campo.proveedor")),    tfProveedor);
        grid.addRow(r++, lbl(t("compras.campo.num_factura")),  tfNumFact);
        grid.addRow(r++, lbl(t("compras.campo.fecha_compra")), dpCompra);
        grid.addRow(r++, lbl(t("compras.campo.forma_pago")),   cbFormaPago);
        grid.addRow(r++, lbl(t("compras.campo.vencimiento")),  dpVencimiento);
        grid.addRow(r++, lbl(t("compras.campo.cantidad")),     tfCantidad);
        grid.addRow(r++, lbl(t("compras.campo.importe")),      tfImporte);
        grid.addRow(r,   lbl(t("compras.campo.notas")),        taNotas);
        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbMaterial.getValue() == null)       { alerta(t("compras.validacion.material")); ev.consume(); return; }
            if (dpVencimiento.getValue() == null)    { alerta(t("compras.validacion.vencimiento")); ev.consume(); return; }
            if (parseDouble(tfImporte.getText()) <= 0) { alerta(t("compras.validacion.importe")); ev.consume(); }
        });

        if (getScene() != null)
            dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            p.setMaterialId(cbMaterial.getValue().getId());
            p.setMaterialNombre(cbMaterial.getValue().getNombre());
            p.setUnidad(cbMaterial.getValue().getUnidad());
            p.setProveedor(tfProveedor.getText().trim());
            p.setNumeroFactura(tfNumFact.getText().trim());
            p.setFechaCompra(dpCompra.getValue());
            p.setFormaPago(cbFormaPago.getValue());
            p.setFechaVencimiento(dpVencimiento.getValue());
            p.setCantidadComprada(parseDouble(tfCantidad.getText()));
            p.setImporteTotal(parseDouble(tfImporte.getText()));
            p.setNotas(taNotas.getText().trim());
            if (p.getEstado() == null) p.setEstado("pendiente");
            return p;
        });

        return dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Button btn(String t, Runnable r) {
        String label = t.replaceFirst("^\\P{L}+", "").strip();
        Button b = new Button(label);
        b.getStyleClass().add("btn-toolbar");
        b.setOnAction(e -> r.run());
        return b;
    }

    private void conf(String mensaje, Runnable accion) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText(null);
        dlg.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> accion.run());
    }

    private TextField txf(String v) { return new TextField(v != null ? v : ""); }
    private Label     lbl(String t) { return new Label(t); }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return 0; }
    }

    private void alerta(String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait();
    }
}
