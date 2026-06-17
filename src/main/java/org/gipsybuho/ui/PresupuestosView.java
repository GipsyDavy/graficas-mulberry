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
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.dao.TarifaTramoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.importer.ImportResult;
import org.gipsybuho.service.PDFService; // Importar PDFService
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.ToastService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

public class PresupuestosView extends VBox {

    private final PresupuestoDAO dao = new PresupuestoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Presupuesto> datos = FXCollections.observableArrayList();
    private final TableView<Presupuesto> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("numero", "Número");
        COLUMNAS_BASE.put("cliente_id", "Cliente");
        COLUMNAS_BASE.put("fecha", "Fecha");
        COLUMNAS_BASE.put("fecha_validez", "Validez");
        COLUMNAS_BASE.put("estado", "Estado");
        COLUMNAS_BASE.put("base_imponible", "Base imponible");
        COLUMNAS_BASE.put("iva_porcentaje", "IVA %");
        COLUMNAS_BASE.put("iva_importe", "IVA");
        COLUMNAS_BASE.put("total", "Total");
        COLUMNAS_BASE.put("notas", "Notas");
        COLUMNAS_BASE.put("condiciones", "Condiciones");
        COLUMNAS_BASE.put("created_at", "Creado");
    }
    private final DynamicColumnRuntime<Presupuesto> dynamicColumns =
        new DynamicColumnRuntime<>("presupuestos", "Presupuestos", COLUMNAS_BASE, tabla, datos, Presupuesto::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private TextField txtBuscar;
    private Label lblContador = new Label();

    public PresupuestosView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label(t("presupuestos.titulo"));
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn(t("presupuestos.btn.nuevo"),    this::nuevo);
        Button btnEditar   = btn(t("presupuestos.btn.editar"),   this::editar);
        Button btnBorrar   = btn(t("presupuestos.btn.borrar"),   this::borrar);
        Button btnImportar = btn(t("presupuestos.btn.importar"), this::importar);
        Button btnExportar = btn(t("presupuestos.btn.exportar"), this::exportar);
        Button btnPedido   = btn(t("presupuestos.btn.crear_pedido"),  this::crearPedido);
        Button btnAlbaran  = btn(t("presupuestos.btn.crear_albaran"), this::crearAlbaran);
        Button btnFacturar = btn(t("presupuestos.btn.crear_factura"), this::crearFactura);
        Button btnPreview  = btn(t("presupuestos.btn.previsualizar"), this::previsualizar);
        Button btnColumnas = btn(t("presupuestos.btn.columnas"), dynamicColumns::configure);
        btnNuevo.setTooltip(new Tooltip(t("presupuestos.btn.nuevo.tip")));
        btnEditar.setTooltip(new Tooltip(t("presupuestos.btn.editar.tip")));
        btnBorrar.setTooltip(new Tooltip(t("presupuestos.btn.borrar.tip")));
        btnImportar.setTooltip(new Tooltip(t("presupuestos.btn.importar.tip")));
        btnExportar.setTooltip(new Tooltip(t("presupuestos.btn.exportar.tip")));
        btnPedido.setTooltip(new Tooltip(t("presupuestos.btn.crear_pedido.tip")));
        btnAlbaran.setTooltip(new Tooltip(t("presupuestos.btn.crear_albaran.tip")));
        btnFacturar.setTooltip(new Tooltip(t("presupuestos.btn.crear_factura.tip")));
        btnPreview.setTooltip(new Tooltip(t("presupuestos.btn.previsualizar.tip")));
        btnColumnas.setTooltip(new Tooltip(t("presupuestos.btn.columnas.tip")));

        txtBuscar = new TextField();
        txtBuscar.setPromptText(t("presupuestos.buscar.prompt"));
        txtBuscar.setPrefWidth(220);
        txtBuscar.textProperty().addListener((o, a, b) -> cargar());
        txtBuscar.setTooltip(new Tooltip(t("presupuestos.buscar.tooltip")));

        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, txtBuscar, lblContador, sp, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnPedido, btnAlbaran, btnFacturar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Presupuesto> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Presupuesto, String> colEstado = new TableColumn<>(t("presupuestos.tabla.col.estado"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setUserData("estado");
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setGraphic(null); return; }
                String variant = switch (v) {
                    case "aceptado"  -> "success";
                    case "rechazado" -> "danger";
                    case "enviado"   -> "warning";
                    case "facturado" -> "info";
                    default          -> "neutral";
                };
                setText(null);
                setGraphic(Icons.statusBadge(v, variant));
            }
        });

        TableColumn<Presupuesto, Double> colTotal = new TableColumn<>(t("presupuestos.tabla.col.total"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setUserData("total");
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(
            col(t("presupuestos.tabla.col.numero"),  "numero",        140),
            col(t("presupuestos.tabla.col.cliente"), "clienteNombre", 200),
            col(t("presupuestos.tabla.col.fecha"),   "fecha",         100),
            col(t("presupuestos.tabla.col.validez"), "fechaValidez",  100),
            colEstado,
            colTotal
        );
        tabla.setPlaceholder(Icons.emptyState(t("presupuestos.tabla.vacio")));
        return tabla;
    }

    private void cargar() {
        try {
            String q = txtBuscar != null ? txtBuscar.getText().strip().toLowerCase() : "";
            var lista = dao.findAll();
            if (!q.isBlank()) lista = lista.stream()
                .filter(p -> contiene(p.getNumero(), q) || contiene(p.getClienteNombre(), q) || contiene(p.getEstado(), q))
                .toList();
            datos.setAll(lista);
            lblContador.setText(tf("presupuestos.contador", lista.size()));
            dynamicColumns.apply(); TableColumnSizing.animarFilas(tabla);
        } catch (Exception e) { mostrarError(e); }
    }

    private boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase().contains(q);
    }

    private void nuevo() {
        try {
            List<Cliente> clientes = clienteDAO.findAll();
            if (clientes.isEmpty()) { alerta(t("presupuestos.nuevo.sin_cliente")); return; }
            Presupuesto p = new Presupuesto();
            p.setNumero(DatabaseManager.generarNumeroPresupuesto());
            p.setFecha(LocalDate.now().toString());
            p.setFechaValidez(LocalDate.now().plusDays(30).toString());
            p.setEstado("borrador");
            p.setIvaPorcentaje(21.0);
            p.setCondiciones(DatabaseManager.getConfig("empresa_nombre") +
                " · Presupuesto válido por 30 días. Precios sin IVA.");
            dialogoPresupuesto(p, clientes).ifPresent(pr -> {
                try { dao.save(pr); dynamicColumns.saveFormFields(pr, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("presupuestos.editar.sin_seleccion")); return; }
        try {
            Presupuesto p = dao.findById(sel.getId());
            List<Cliente> clientes = clienteDAO.findAll();
            dialogoPresupuesto(p, clientes).ifPresent(pr -> {
                try { dao.save(pr); dynamicColumns.saveFormFields(pr, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        List<Presupuesto> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta(t("presupuestos.borrar.sin_seleccion")); return; }
        String mensaje = seleccionados.size() == 1
            ? tf("presupuestos.borrar.confirmar.uno",   seleccionados.get(0).getNumero())
            : tf("presupuestos.borrar.confirmar.varios", seleccionados.size());
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje, ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Presupuesto presupuesto : seleccionados) dao.delete(presupuesto.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void crearPedido() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("presupuestos.sin_seleccion")); return; }
        if (!"aceptado".equals(sel.getEstado())) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                t("presupuestos.crear_pedido.confirmar_no_aceptado"),
                ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        try {
            new PedidoDAO().crearDesdePresupuesto(sel.getId());
            cargar();
            new Alert(Alert.AlertType.INFORMATION, t("presupuestos.crear_pedido.exito"), ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private void crearFactura() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("presupuestos.sin_seleccion")); return; }
        if ("facturado".equals(sel.getEstado())) { alerta(t("presupuestos.crear_factura.ya_facturado")); return; }
        if (!"aceptado".equals(sel.getEstado())) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                t("presupuestos.crear_factura.confirmar_no_aceptado"),
                ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        try {
            new FacturaDAO().crearDesdePresupuesto(sel.getId());
            cargar();
            new Alert(Alert.AlertType.INFORMATION, t("presupuestos.crear_factura.exito"), ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private void crearAlbaran() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("presupuestos.sin_seleccion")); return; }
        if (!"aceptado".equals(sel.getEstado())) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                t("presupuestos.crear_albaran.confirmar_no_aceptado"),
                ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        try {
            new AlbaranDAO().crearDesdePresupuesto(sel.getId());
            cargar();
            new Alert(Alert.AlertType.INFORMATION, t("presupuestos.crear_albaran.exito"), ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private Optional<Presupuesto> dialogoPresupuesto(Presupuesto p, List<Cliente> clientes) {
        Dialog<Presupuesto> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? t("presupuestos.dialogo.nuevo") : tf("presupuestos.dialogo.editar", p.getNumero()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(800);
        dlg.getDialogPane().setPrefHeight(600);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Datos generales
        GridPane gGeneral = new GridPane();
        gGeneral.setHgap(10); gGeneral.setVgap(10); gGeneral.setPadding(new Insets(16));

        ComboBox<Cliente> fCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        clientes.stream().filter(c -> c.getId() == p.getClienteId()).findFirst().ifPresent(fCliente::setValue);
        if (fCliente.getValue() == null && !clientes.isEmpty()) fCliente.setValue(clientes.get(0));

        TextField fNumero    = txf(p.getNumero());
        fNumero.setEditable(false);
        TextField fFecha     = txf(p.getFecha());
        TextField fValidez   = txf(p.getFechaValidez());
        ComboBox<String> fEstado = new ComboBox<>(FXCollections.observableArrayList(
            "borrador","enviado","aceptado","rechazado"));
        fEstado.setValue(p.getEstado());
        TextField fIva       = txf(String.valueOf(p.getIvaPorcentaje()));
        TextArea fNotas      = new TextArea(nvl(p.getNotas())); fNotas.setPrefRowCount(3);
        TextArea fCondiciones = new TextArea(nvl(p.getCondiciones())); fCondiciones.setPrefRowCount(3);

        gGeneral.addRow(0, lbl(t("presupuestos.campo.numero")),    fNumero,  lbl(t("presupuestos.campo.estado")),  fEstado);
        gGeneral.addRow(1, lbl(t("presupuestos.campo.cliente")),   fCliente, lbl(t("presupuestos.campo.iva")),    fIva);
        gGeneral.addRow(2, lbl(t("presupuestos.campo.fecha")),     fFecha,   lbl(t("presupuestos.campo.validez")), fValidez);
        gGeneral.add(lbl(t("presupuestos.campo.notas")),       0, 3); gGeneral.add(fNotas,       1, 3, 3, 1);
        gGeneral.add(lbl(t("presupuestos.campo.condiciones")), 0, 4); gGeneral.add(fCondiciones, 1, 4, 3, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(gGeneral, 5, p, dialogExtraFields);
        tabs.getTabs().add(new Tab(t("presupuestos.tab.datos"), gGeneral));

        // Tab 2: Servicios / Técnicas (líneas de trabajo)
        ObservableList<LineaPresupuesto> lineasServ = FXCollections.observableArrayList(
            p.getLineas().stream().filter(l -> !"📦 Material".equals(l.getTecnica())).toList());

        // Tab 3: Materiales del stock
        ObservableList<LineaPresupuesto> lineasMat = FXCollections.observableArrayList(
            p.getLineas().stream().filter(l -> "📦 Material".equals(l.getTecnica())).toList());

        tabs.getTabs().add(new Tab(t("presupuestos.tab.servicios"), buildTablaLineas(lineasServ)));
        tabs.getTabs().add(new Tab(t("presupuestos.tab.materiales"), buildTabMateriales(lineasMat)));

        dlg.getDialogPane().setContent(tabs);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            if (fCliente.getValue() == null) { alerta(t("presupuestos.validacion.cliente")); return null; }
            p.setClienteId(fCliente.getValue().getId());
            p.setClienteNombre(fCliente.getValue().getNombreCompleto());
            p.setFecha(fFecha.getText().trim());
            p.setFechaValidez(fValidez.getText().trim());
            p.setEstado(fEstado.getValue());
            p.setIvaPorcentaje(parseDouble(fIva.getText()));
            p.setNotas(fNotas.getText().trim());
            p.setCondiciones(fCondiciones.getText().trim());
            java.util.List<LineaPresupuesto> todasLineas = new java.util.ArrayList<>(lineasServ);
            todasLineas.addAll(lineasMat);
            p.setLineas(todasLineas);
            p.calcularTotales();
            return p;
        });
        return dlg.showAndWait();
    }

    private Node buildTablaLineas(ObservableList<LineaPresupuesto> lineas) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        TableView<LineaPresupuesto> tLineas = new TableView<>(lineas);
        tLineas.setEditable(true);
        tLineas.setPrefHeight(280);

        TableColumn<LineaPresupuesto,String> cDesc = new TableColumn<>(t("presupuestos.linea.col.descripcion"));
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        cDesc.setPrefWidth(250);

        TableColumn<LineaPresupuesto,String> cTec = new TableColumn<>(t("presupuestos.linea.col.tecnica"));
        cTec.setCellValueFactory(new PropertyValueFactory<>("tecnica"));
        cTec.setPrefWidth(100);

        TableColumn<LineaPresupuesto,Integer> cCant = new TableColumn<>(t("presupuestos.linea.col.cantidad"));
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(60);

        TableColumn<LineaPresupuesto,Double> cPrecio = new TableColumn<>(t("presupuestos.linea.col.precio_ud"));
        cPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(90);

        TableColumn<LineaPresupuesto,Double> cDto = new TableColumn<>(t("presupuestos.linea.col.descuento"));
        cDto.setCellValueFactory(new PropertyValueFactory<>("descuento"));
        cDto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : (v > 0 ? v + "%" : "-"));
            }
        });
        cDto.setPrefWidth(60);

        TableColumn<LineaPresupuesto,Double> cTotal = new TableColumn<>(t("presupuestos.linea.col.total"));
        cTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cTotal.setPrefWidth(90);

        tLineas.getColumns().addAll(cDesc, cTec, cCant, cPrecio, cDto, cTotal);

        HBox buttons = new HBox(8);
        Button btnAdd = btn(t("presupuestos.linea.btn.anadir"), () -> dialogoLinea(null, lineas));
        Button btnEdit = btn(t("presupuestos.linea.btn.editar"), () -> {
            LineaPresupuesto sel = tLineas.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoLinea(sel, lineas);
        });
        Button btnDel = btn(t("presupuestos.linea.btn.quitar"), () -> {
            LineaPresupuesto sel = tLineas.getSelectionModel().getSelectedItem();
            if (sel != null) lineas.remove(sel);
        });
        buttons.getChildren().addAll(btnAdd, btnEdit, btnDel);

        box.getChildren().addAll(tLineas, buttons);
        return box;
    }

    private void dialogoLinea(LineaPresupuesto linea, ObservableList<LineaPresupuesto> lista) {
        boolean esNueva = linea == null;
        if (esNueva) linea = new LineaPresupuesto();
        LineaPresupuesto l = linea;

        Dialog<LineaPresupuesto> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? t("presupuestos.linea.dialogo.nuevo") : t("presupuestos.linea.dialogo.editar"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextArea fDesc   = new TextArea(nvl(l.getDescripcion())); fDesc.setPrefRowCount(3); fDesc.setPrefWidth(300);
        TextField fTec   = txf(l.getTecnica());
        TextField fCant  = txf(l.getCantidad() > 0 ? String.valueOf(l.getCantidad()) : "1");
        TextField fPrecio = txf(l.getPrecioUnit() > 0 ? String.valueOf(l.getPrecioUnit()) : "");
        TextField fDto   = txf(l.getDescuento() > 0 ? String.valueOf(l.getDescuento()) : "0");

        // Botón para insertar desde tarifa
        try {
            List<Tarifa> tarifas = new TarifaDAO().findAll();
            ComboBox<Tarifa> cbTarifa = new ComboBox<>(FXCollections.observableArrayList(tarifas));
            cbTarifa.setPromptText(t("presupuestos.linea.tarifa.prompt"));
            cbTarifa.setOnAction(e -> {
                Tarifa tarifa = cbTarifa.getValue();
                if (tarifa == null) return;
                if (tarifa.isUsaTiempo()) {
                    aplicarTarifaTiempo(tarifa, fDesc, fTec, fPrecio);
                } else {
                    if (fDesc.getText().isBlank()) fDesc.setText(tarifa.getNombre() + (tarifa.getDescripcion() != null ? " - " + tarifa.getDescripcion() : ""));
                    fTec.setText(tarifa.getTecnica());
                    fPrecio.setText(String.valueOf(tarifa.getPrecioUnit()));
                }
            });
            grid.add(lbl(t("presupuestos.linea.campo.tarifa")), 0, 0); grid.add(cbTarifa, 1, 0, 3, 1);
        } catch (Exception ignored) {}

        grid.addRow(1, lbl(t("presupuestos.linea.campo.descripcion")), fDesc);
        GridPane.setColumnSpan(fDesc, 3);
        grid.addRow(2, lbl(t("presupuestos.linea.campo.tecnica")),   fTec,    lbl(t("presupuestos.linea.campo.cantidad")),  fCant);
        grid.addRow(3, lbl(t("presupuestos.linea.campo.precio_ud")), fPrecio, lbl(t("presupuestos.linea.campo.descuento")), fDto);

        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            l.setDescripcion(fDesc.getText().trim());
            l.setTecnica(fTec.getText().trim());
            l.setCantidad(parseInt(fCant.getText(), 1));
            l.setPrecioUnit(parseDouble(fPrecio.getText()));
            l.setDescuento(parseDouble(fDto.getText()));
            l.calcularTotal();
            return l;
        });

        dlg.showAndWait().ifPresent(result -> {
            if (esNueva) lista.add(result);
        });
    }

    private void aplicarTarifaTiempo(Tarifa tarifaBase,
                                      TextArea fDesc, TextField fTec, TextField fPrecio) {
        Dialog<Integer> dlg = new Dialog<>();
        dlg.setTitle(tf("presupuestos.tarifa.tiempo.titulo", tarifaBase.getNombre()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (tabla.getScene() != null)
            dlg.getDialogPane().getStylesheets()
                .addAll(tabla.getScene().getStylesheets());

        Spinner<Integer> spMinutos = new Spinner<>(1, 9999, 10);
        spMinutos.setEditable(true);
        spMinutos.setPrefWidth(120);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.addRow(0, new Label(t("presupuestos.tarifa.tiempo.label_minutos")), spMinutos);
        dlg.getDialogPane().setContent(g);
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? spMinutos.getValue() : null);

        dlg.showAndWait().ifPresent(minutosReales -> {
            int redondeado = (int)(Math.ceil(minutosReales / 5.0) * 5);
            try {
                List<TarifaTramo> tramos =
                    new TarifaTramoDAO().findByTarifaId(tarifaBase.getId());
                TarifaTramo tramoUsado = tramos.stream()
                    .filter(tr -> tr.getTiempoMinutos() == redondeado)
                    .findFirst()
                    .orElseGet(() -> tramos.stream()
                        .filter(tr -> tr.getTiempoMinutos() > redondeado)
                        .min(Comparator.comparing(TarifaTramo::getTiempoMinutos))
                        .orElse(null));
                if (tramoUsado == null) {
                    new Alert(Alert.AlertType.WARNING,
                        tf("presupuestos.tarifa.tiempo.sin_tramo", redondeado), ButtonType.OK)
                        .showAndWait();
                    return;
                }
                boolean huboRedondeo = (minutosReales != redondeado);
                String desc = tarifaBase.getNombre() + " — "
                    + tramoUsado.getTiempoMinutos() + " min"
                    + (huboRedondeo ? " (real: " + minutosReales + " min)" : "");
                fDesc.setText(desc);
                fTec.setText(tarifaBase.getTecnica());
                fPrecio.setText(String.format("%.2f", tramoUsado.getPrecioTiempo()));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                    tf("presupuestos.tarifa.tiempo.error_tramos", ex.getMessage()), ButtonType.OK)
                    .showAndWait();
            }
        });
    }

    private javafx.scene.Node buildTabMateriales(ObservableList<LineaPresupuesto> lineasMat) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        // Tabla de materiales seleccionados
        TableView<LineaPresupuesto> tablaMat = new TableView<>(lineasMat);
        tablaMat.setPrefHeight(200);
        tablaMat.setPlaceholder(new Label(t("presupuestos.mat.vacio")));

        TableColumn<LineaPresupuesto, String> cNom = new TableColumn<>(t("presupuestos.mat.col.material"));
        cNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descripcion"));
        cNom.setPrefWidth(260);

        TableColumn<LineaPresupuesto, Integer> cCant = new TableColumn<>(t("presupuestos.mat.col.cantidad"));
        cCant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(80);

        TableColumn<LineaPresupuesto, Double> cPrecio = new TableColumn<>(t("presupuestos.mat.col.precio_ud"));
        cPrecio.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(100);

        TableColumn<LineaPresupuesto, Double> cTotal = new TableColumn<>(t("presupuestos.mat.col.total"));
        cTotal.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("total"));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cTotal.setPrefWidth(100);

        tablaMat.getColumns().addAll(cNom, cCant, cPrecio, cTotal);

        // Selector de material del stock
        Label lblPicker = new Label(t("presupuestos.mat.anadir_titulo"));
        lblPicker.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        List<Material> materiales;
        try { materiales = new MaterialDAO().findAll(); }
        catch (Exception e) { materiales = new java.util.ArrayList<>(); }

        ComboBox<Material> cbMat = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMat.setPromptText(t("presupuestos.mat.selector.prompt"));
        cbMat.setPrefWidth(290);

        Label lblInfo = new Label(t("presupuestos.mat.info.inicial"));
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        Spinner<Integer> spCant = new Spinner<>(1, 999999, 1);
        spCant.setEditable(true);
        spCant.setPrefWidth(100);

        cbMat.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m != null) lblInfo.setText(tf("presupuestos.mat.info",
                m.getStockActual(),
                m.getUnidad() != null && !m.getUnidad().isBlank() ? m.getUnidad() : "ud",
                m.getPrecioUnidad()));
        });

        Button btnAnadir = new Button(t("presupuestos.mat.btn.anadir"));
        btnAnadir.setStyle(
            "-fx-background-color:#27AE60; -fx-text-fill:white; " +
            "-fx-font-weight:bold; -fx-padding:6 16; -fx-background-radius:4;");
        btnAnadir.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m == null) { alerta(t("presupuestos.mat.sin_material")); return; }
            LineaPresupuesto lm = new LineaPresupuesto();
            lm.setDescripcion(m.getNombre() +
                (m.getReferencia() != null && !m.getReferencia().isBlank() ? " [" + m.getReferencia() + "]" : ""));
            lm.setTecnica("📦 Material");
            lm.setCantidad(spCant.getValue());
            lm.setPrecioUnit(m.getPrecioUnidad());
            lm.setDescuento(0);
            lm.calcularTotal();
            lineasMat.add(lm);
        });

        Button btnQuitar = btn(t("presupuestos.mat.btn.quitar"), () -> {
            LineaPresupuesto sel = tablaMat.getSelectionModel().getSelectedItem();
            if (sel != null) lineasMat.remove(sel);
        });

        Label lblTotalMat = new Label(tf("presupuestos.mat.total", 0.0));
        lblTotalMat.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
        lineasMat.addListener((javafx.collections.ListChangeListener<LineaPresupuesto>) c -> {
            double tot = lineasMat.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
            lblTotalMat.setText(tf("presupuestos.mat.total", tot));
        });
        // Calcular total inicial si ya había líneas
        double totInicial = lineasMat.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
        lblTotalMat.setText(tf("presupuestos.mat.total", totInicial));

        HBox pickerRow = new HBox(8, cbMat, new Label(t("presupuestos.mat.campo.cantidad")), spCant, btnAnadir);
        pickerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        box.getChildren().addAll(
            lblPicker, pickerRow, lblInfo,
            new Separator(),
            tablaMat,
            new HBox(8, btnQuitar),
            new Separator(),
            lblTotalMat
        );
        return box;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Presupuesto, T> col(String t, String campo, double ancho) {
        TableColumn<Presupuesto, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setUserData(toDbColumn(campo));
        c.setPrefWidth(ancho); return c;
    }

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle(t("presupuestos.importar.titulo"));
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(t("presupuestos.importar.filtro"), "*.csv", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.json"),
            new FileChooser.ExtensionFilter(t("presupuestos.importar.todos_archivos"), "*.*"));
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
                        Presupuesto.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Presupuesto.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
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
        sb.append(tf("presupuestos.importar.completada", r.duracion().toMillis() / 1000.0))
          .append(System.lineSeparator());
        sb.append(tf("presupuestos.importar.filas_importadas",  r.filasImportadas()))
          .append(System.lineSeparator());
        sb.append(tf("presupuestos.importar.filas_actualizadas", r.filasActualizadas()))
          .append(System.lineSeparator());
        sb.append(tf("presupuestos.importar.filas_descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append(t("presupuestos.importar.errores_header"));
            r.errores().stream().limit(10).forEach(e ->
                sb.append(tf("presupuestos.importar.error_fila",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle(t("presupuestos.importar.resultado.titulo"));
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        if (getScene() != null) a.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        a.showAndWait();
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", t("export.fmt.sqlite.label"), t("export.fmt.sqlite.desc"),            "db"},
            {"csv",    t("export.fmt.csv.label"),    t("presupuestos.export.csv.desc"),      "csv"},
            {"sql",    t("export.fmt.sql.label"),    t("presupuestos.export.sql.desc"),      "sql"},
            {"json",   t("export.fmt.json.label"),   t("presupuestos.export.json.desc"),     "json"},
            {"pdf",    t("export.fmt.pdf.label"),    t("presupuestos.export.pdf.desc"),      "pdf"},
            {"word",   t("export.fmt.word.label"),   t("presupuestos.export.word.desc"),     "docx"},
            {"excel",  t("export.fmt.excel.label"),  t("presupuestos.export.excel.desc"),    "xlsx"}
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
        dlg.setTitle(t("presupuestos.export.titulo"));
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
        fc.setInitialFileName("Presupuestos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(tf("presupuestos.export.filtro", fmt[3].toUpperCase()), "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        // Determinar la lista de presupuestos a exportar
        List<Presupuesto> presupuestosAExportar = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (presupuestosAExportar.isEmpty()) {
            // Si no hay selección, exportar todos los registros
            presupuestosAExportar.addAll(datos);
        }
        if (presupuestosAExportar.isEmpty()) {
            Platform.runLater(() -> {
                setDisable(false);
                alerta(t("presupuestos.export.sin_registros"));
            });
            return;
        }


        Thread.ofVirtual().start(() -> {
            try {
                if (presupuestosAExportar.size() == 1 && ("pdf".equals(fmt[0]) || "word".equals(fmt[0]))) {
                    // Exportar un único presupuesto como documento detallado
                    Presupuesto presupuestoSeleccionado = dao.findById(presupuestosAExportar.get(0).getId());
                    if (presupuestoSeleccionado == null)
                        throw new Exception("No se pudo cargar el presupuesto seleccionado.");
                    Cliente clienteAsociado = clienteDAO.findById(presupuestoSeleccionado.getClienteId());
                    if (clienteAsociado == null)
                        throw new Exception("No se pudo encontrar el cliente asociado al presupuesto.");

                    if ("pdf".equals(fmt[0])) {
                        PDFService pdfService = new PDFService();
                        Path tempPdfPath = pdfService.generarPresupuesto(presupuestoSeleccionado, clienteAsociado);
                        Files.copy(tempPdfPath, destino, StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(tempPdfPath);
                    } else {
                        ExportService.exportarPresupuestoDetalladoWord(destino, presupuestoSeleccionado, clienteAsociado);
                    }

                } else {
                    switch (fmt[0]) {
                        case "sqlite" -> ExportService.backupSQLite(destino);
                        case "csv"    -> ExportService.exportarPresupuestosCSV(destino);
                        case "sql"    -> ExportService.exportarPresupuestosSQL(destino);
                        case "json"   -> ExportService.exportarPresupuestosJSON(destino);
                        case "pdf"    -> ExportService.exportarPresupuestosPDF(destino, presupuestosAExportar);
                        case "word"   -> ExportService.exportarPresupuestosWord(destino, presupuestosAExportar);
                        case "excel"  -> ExportService.exportarPresupuestosExcel(destino, presupuestosAExportar);
                    }
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

    private void previsualizar() {
        List<Presupuesto> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) {
            alerta(t("presupuestos.previsualizar.sin_seleccion"));
            seleccionados.addAll(datos); // Si no hay selección, previsualizar todos
        }
        if (seleccionados.isEmpty()) { // Si aún después de añadir todos, sigue vacío
            alerta(t("presupuestos.previsualizar.vacio"));
            return;
        }

        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes;
                byte[] pdfImpresionBytes;
                String tituloVentana;

                if (seleccionados.size() == 1) {
                    // Previsualizar un único presupuesto detallado
                    Presupuesto presupuestoSeleccionado = dao.findById(seleccionados.get(0).getId());
                    if (presupuestoSeleccionado == null) {
                        throw new Exception("No se pudo cargar el presupuesto seleccionado.");
                    }
                    Cliente clienteAsociado = clienteDAO.findById(presupuestoSeleccionado.getClienteId());
                    if (clienteAsociado == null) {
                        // Manejar el caso de cliente no encontrado, quizás usar un cliente por defecto o lanzar error
                        // Por ahora, lanzamos un error para que el usuario sepa que falta información
                        throw new Exception("No se pudo encontrar el cliente asociado al presupuesto.");
                    }

                    PDFService pdfService = new PDFService();
                    Path pdfPath = pdfService.generarPresupuesto(presupuestoSeleccionado, clienteAsociado, true);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    Path pdfImpresionPath = pdfService.generarPresupuesto(presupuestoSeleccionado, clienteAsociado, false);
                    pdfImpresionBytes = Files.readAllBytes(pdfImpresionPath);
                    tituloVentana = tf("presupuestos.previsualizar.titulo.uno", presupuestoSeleccionado.getNumero());

                    // Opcional: eliminar el archivo temporal después de leerlo
                    Files.deleteIfExists(pdfPath);

                } else {
                    // Previsualizar un listado de múltiples presupuestos
                    pdfBytes = PdfPreviewService.previsualizarPresupuestos(seleccionados);
                    pdfImpresionBytes = pdfBytes;
                    tituloVentana = tf("presupuestos.previsualizar.titulo.varios", seleccionados.size());
                }

                final byte[] finalPdfBytes = pdfBytes;
                final byte[] finalPdfImpresionBytes = pdfImpresionBytes;
                final String finalTituloVentana = tituloVentana;

                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    PdfPreviewWindow.mostrar(finalPdfBytes, finalPdfImpresionBytes, finalTituloVentana);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    setDisable(false);
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
            case "clienteNombre" -> "cliente_id";
            case "fechaValidez" -> "fecha_validez";
            default -> campo;
        };
    }
    private String nvl(String s) { return s != null ? s : ""; }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) {
        SoundService.play(SoundService.Sound.ERROR);
        String msg = e.getMessage() != null ? e.getMessage() : t("common.error.desconocido");
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        if (w != null && msg.contains("UNIQUE constraint failed")) {
            ToastService.error(w, t("presupuestos.error.numero_duplicado"), "PRE-ERR-1");
        } else {
            new Alert(Alert.AlertType.ERROR, "Error: " + msg, ButtonType.OK).showAndWait();
        }
    }
}
