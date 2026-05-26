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

    public PresupuestosView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Presupuestos");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn("+ Nuevo",           "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",           "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",          "#E74C3C", this::borrar);
        Button btnImportar = btn("📥 Importar",        "#27AE60", this::importar);
        Button btnExportar = btn("📤 Exportar",        "#8E44AD", this::exportar);
        Button btnAlbaran  = btn("📋 Crear Albarán",   "#7D3C98", this::crearAlbaran);
        Button btnFacturar = btn("🧾 Crear Factura",   "#9B59B6", this::crearFactura);
        Button btnPreview    = btn("👁 Previsualizar",   "#6B2D5E", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas",         "#34495E", dynamicColumns::configure);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnAlbaran, btnFacturar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Presupuesto> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Presupuesto, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setUserData("estado");
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toUpperCase());
                String color = switch(v) {
                    case "aceptado"  -> "#27AE60";
                    case "rechazado" -> "#E74C3C";
                    case "enviado"   -> "#F39C12";
                    case "facturado" -> "#9B59B6";
                    default          -> "#95A5A6";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });

        TableColumn<Presupuesto, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setUserData("total");
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(
            col("Número", "numero", 140),
            col("Cliente", "clienteNombre", 200),
            col("Fecha", "fecha", 100),
            col("Validez", "fechaValidez", 100),
            colEstado,
            colTotal
        );
        tabla.setPlaceholder(new Label("No hay presupuestos registrados"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); dynamicColumns.apply(); } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        try {
            List<Cliente> clientes = clienteDAO.findAll();
            if (clientes.isEmpty()) { alerta("Añade al menos un cliente antes de crear un presupuesto."); return; }
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
        if (sel == null) { alerta("Selecciona un presupuesto para editar."); return; }
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
        if (seleccionados.isEmpty()) { alerta("Selecciona uno o varios presupuestos para borrar."); return; }
        String mensaje = seleccionados.size() == 1
            ? "¿Eliminar el presupuesto " + seleccionados.get(0).getNumero() + "?"
            : "¿Eliminar " + seleccionados.size() + " presupuestos seleccionados?";
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

    private void crearFactura() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un presupuesto."); return; }
        if ("facturado".equals(sel.getEstado())) { alerta("Este presupuesto ya está facturado."); return; }
        if (!"aceptado".equals(sel.getEstado())) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "El presupuesto no está en estado 'aceptado'. ¿Crear factura igualmente?",
                ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        try {
            new FacturaDAO().crearDesdePresupuesto(sel.getId());
            cargar();
            new Alert(Alert.AlertType.INFORMATION, "Factura creada correctamente.", ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private void crearAlbaran() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un presupuesto."); return; }
        if (!"aceptado".equals(sel.getEstado())) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "El presupuesto no está en estado 'aceptado'. ¿Crear albarán igualmente?",
                ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            if (conf.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }
        try {
            new AlbaranDAO().crearDesdePresupuesto(sel.getId());
            cargar();
            new Alert(Alert.AlertType.INFORMATION, "Albarán creado correctamente.", ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private Optional<Presupuesto> dialogoPresupuesto(Presupuesto p, List<Cliente> clientes) {
        Dialog<Presupuesto> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? "Nuevo presupuesto" : "Editar presupuesto " + p.getNumero());
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

        TextField fNumero    = tf(p.getNumero());
        fNumero.setEditable(false);
        TextField fFecha     = tf(p.getFecha());
        TextField fValidez   = tf(p.getFechaValidez());
        ComboBox<String> fEstado = new ComboBox<>(FXCollections.observableArrayList(
            "borrador","enviado","aceptado","rechazado"));
        fEstado.setValue(p.getEstado());
        TextField fIva       = tf(String.valueOf(p.getIvaPorcentaje()));
        TextArea fNotas      = new TextArea(nvl(p.getNotas())); fNotas.setPrefRowCount(3);
        TextArea fCondiciones = new TextArea(nvl(p.getCondiciones())); fCondiciones.setPrefRowCount(3);

        gGeneral.addRow(0, lbl("Número"), fNumero, lbl("Estado"), fEstado);
        gGeneral.addRow(1, lbl("Cliente *"), fCliente, lbl("IVA (%)"), fIva);
        gGeneral.addRow(2, lbl("Fecha"), fFecha, lbl("Validez hasta"), fValidez);
        gGeneral.add(lbl("Notas"), 0, 3); gGeneral.add(fNotas, 1, 3, 3, 1);
        gGeneral.add(lbl("Condiciones"), 0, 4); gGeneral.add(fCondiciones, 1, 4, 3, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(gGeneral, 5, p, dialogExtraFields);
        tabs.getTabs().add(new Tab("Datos generales", gGeneral));

        // Tab 2: Servicios / Técnicas (líneas de trabajo)
        ObservableList<LineaPresupuesto> lineasServ = FXCollections.observableArrayList(
            p.getLineas().stream().filter(l -> !"📦 Material".equals(l.getTecnica())).toList());

        // Tab 3: Materiales del stock
        ObservableList<LineaPresupuesto> lineasMat = FXCollections.observableArrayList(
            p.getLineas().stream().filter(l -> "📦 Material".equals(l.getTecnica())).toList());

        tabs.getTabs().add(new Tab("Servicios / Técnicas", buildTablaLineas(lineasServ)));
        tabs.getTabs().add(new Tab("📦 Materiales del stock", buildTabMateriales(lineasMat)));

        dlg.getDialogPane().setContent(tabs);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            if (fCliente.getValue() == null) { alerta("Selecciona un cliente."); return null; }
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

        TableView<LineaPresupuesto> t = new TableView<>(lineas);
        t.setEditable(true);
        t.setPrefHeight(280);

        TableColumn<LineaPresupuesto,String> cDesc = new TableColumn<>("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        cDesc.setPrefWidth(250);

        TableColumn<LineaPresupuesto,String> cTec = new TableColumn<>("Técnica");
        cTec.setCellValueFactory(new PropertyValueFactory<>("tecnica"));
        cTec.setPrefWidth(100);

        TableColumn<LineaPresupuesto,Integer> cCant = new TableColumn<>("Cant.");
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(60);

        TableColumn<LineaPresupuesto,Double> cPrecio = new TableColumn<>("Precio ud.");
        cPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(90);

        TableColumn<LineaPresupuesto,Double> cDto = new TableColumn<>("Dto.");
        cDto.setCellValueFactory(new PropertyValueFactory<>("descuento"));
        cDto.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : (v > 0 ? v + "%" : "-"));
            }
        });
        cDto.setPrefWidth(60);

        TableColumn<LineaPresupuesto,Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cTotal.setPrefWidth(90);

        t.getColumns().addAll(cDesc, cTec, cCant, cPrecio, cDto, cTotal);

        HBox buttons = new HBox(8);
        Button btnAdd = btn("+ Añadir línea", "#4C9BE8", () -> dialogoLinea(null, lineas));
        Button btnEdit = btn("✏ Editar", "#F39C12", () -> {
            LineaPresupuesto sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoLinea(sel, lineas);
        });
        Button btnDel = btn("🗑 Quitar", "#E74C3C", () -> {
            LineaPresupuesto sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) lineas.remove(sel);
        });
        buttons.getChildren().addAll(btnAdd, btnEdit, btnDel);

        box.getChildren().addAll(t, buttons);
        return box;
    }

    private void dialogoLinea(LineaPresupuesto linea, ObservableList<LineaPresupuesto> lista) {
        boolean esNueva = linea == null;
        if (esNueva) linea = new LineaPresupuesto();
        LineaPresupuesto l = linea;

        Dialog<LineaPresupuesto> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? "Nueva línea" : "Editar línea");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextArea fDesc   = new TextArea(nvl(l.getDescripcion())); fDesc.setPrefRowCount(3); fDesc.setPrefWidth(300);
        TextField fTec   = tf(l.getTecnica());
        TextField fCant  = tf(l.getCantidad() > 0 ? String.valueOf(l.getCantidad()) : "1");
        TextField fPrecio = tf(l.getPrecioUnit() > 0 ? String.valueOf(l.getPrecioUnit()) : "");
        TextField fDto   = tf(l.getDescuento() > 0 ? String.valueOf(l.getDescuento()) : "0");

        // Botón para insertar desde tarifa
        try {
            List<Tarifa> tarifas = new TarifaDAO().findAll();
            ComboBox<Tarifa> cbTarifa = new ComboBox<>(FXCollections.observableArrayList(tarifas));
            cbTarifa.setPromptText("Seleccionar tarifa...");
            cbTarifa.setOnAction(e -> {
                Tarifa t = cbTarifa.getValue();
                if (t == null) return;
                if (t.isUsaTiempo()) {
                    aplicarTarifaTiempo(t, fDesc, fTec, fPrecio);
                } else {
                    if (fDesc.getText().isBlank()) fDesc.setText(t.getNombre() + (t.getDescripcion() != null ? " - " + t.getDescripcion() : ""));
                    fTec.setText(t.getTecnica());
                    fPrecio.setText(String.valueOf(t.getPrecioUnit()));
                }
            });
            grid.add(lbl("Tarifa:"), 0, 0); grid.add(cbTarifa, 1, 0, 3, 1);
        } catch (Exception ignored) {}

        grid.addRow(1, lbl("Descripción *"), fDesc);
        GridPane.setColumnSpan(fDesc, 3);
        grid.addRow(2, lbl("Técnica"), fTec, lbl("Cantidad"), fCant);
        grid.addRow(3, lbl("Precio ud. (€)"), fPrecio, lbl("Descuento (%)"), fDto);

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
        dlg.setTitle("Tiempo de ejecución — " + tarifaBase.getNombre());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (tabla.getScene() != null)
            dlg.getDialogPane().getStylesheets()
                .addAll(tabla.getScene().getStylesheets());

        Spinner<Integer> spMinutos = new Spinner<>(1, 9999, 10);
        spMinutos.setEditable(true);
        spMinutos.setPrefWidth(120);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(16));
        g.addRow(0, new Label("Minutos reales de ejecución:"), spMinutos);
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
                        "No existe tramo para " + redondeado + " min ni superior.\n"
                        + "Revisa los tramos de esta tarifa.", ButtonType.OK)
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
                    "Error al cargar tramos: " + ex.getMessage(), ButtonType.OK)
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
        tablaMat.setPlaceholder(new Label("No hay materiales añadidos al presupuesto"));

        TableColumn<LineaPresupuesto, String> cNom = new TableColumn<>("Material");
        cNom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descripcion"));
        cNom.setPrefWidth(260);

        TableColumn<LineaPresupuesto, Integer> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(80);

        TableColumn<LineaPresupuesto, Double> cPrecio = new TableColumn<>("Precio ud.");
        cPrecio.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(100);

        TableColumn<LineaPresupuesto, Double> cTotal = new TableColumn<>("Total");
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
        Label lblPicker = new Label("Añadir material del stock:");
        lblPicker.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        List<Material> materiales;
        try { materiales = new MaterialDAO().findAll(); }
        catch (Exception e) { materiales = new java.util.ArrayList<>(); }

        ComboBox<Material> cbMat = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMat.setPromptText("Seleccionar material...");
        cbMat.setPrefWidth(290);

        Label lblInfo = new Label("Selecciona un material para ver disponibilidad y precio");
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        Spinner<Integer> spCant = new Spinner<>(1, 999999, 1);
        spCant.setEditable(true);
        spCant.setPrefWidth(100);

        cbMat.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m != null) lblInfo.setText(String.format(
                "Stock disponible: %.2f %s  |  Precio unitario: %.2f €/ud.",
                m.getStockActual(),
                m.getUnidad() != null && !m.getUnidad().isBlank() ? m.getUnidad() : "ud",
                m.getPrecioUnidad()));
        });

        Button btnAnadir = new Button("➕ Añadir al presupuesto");
        btnAnadir.setStyle(
            "-fx-background-color:#27AE60; -fx-text-fill:white; " +
            "-fx-font-weight:bold; -fx-padding:6 16; -fx-background-radius:4;");
        btnAnadir.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m == null) { alerta("Selecciona un material del desplegable."); return; }
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

        Button btnQuitar = btn("🗑 Quitar seleccionado", "#E74C3C", () -> {
            LineaPresupuesto sel = tablaMat.getSelectionModel().getSelectedItem();
            if (sel != null) lineasMat.remove(sel);
        });

        Label lblTotalMat = new Label("Total materiales: 0.00 €");
        lblTotalMat.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
        lineasMat.addListener((javafx.collections.ListChangeListener<LineaPresupuesto>) c -> {
            double tot = lineasMat.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
            lblTotalMat.setText(String.format("Total materiales: %.2f €", tot));
        });
        // Calcular total inicial si ya había líneas
        double totInicial = lineasMat.stream().mapToDouble(LineaPresupuesto::getTotal).sum();
        lblTotalMat.setText(String.format("Total materiales: %.2f €", totInicial));

        HBox pickerRow = new HBox(8, cbMat, new Label("Cantidad:"), spCant, btnAnadir);
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
        fc.setTitle("Importar presupuestos");
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
                "Tabla de presupuestos como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con presupuestos y sus líneas (tabla completa).", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Presupuestos y líneas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de presupuestos como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de presupuestos en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar presupuestos");
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
        fc.setInitialFileName("Presupuestos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Presupuestos", "*." + fmt[3]));
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
                alerta("No hay registros para exportar.");
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
        List<Presupuesto> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) {
            alerta("No hay presupuestos seleccionados para previsualizar. Se previsualizarán todos los registros.");
            seleccionados.addAll(datos); // Si no hay selección, previsualizar todos
        }
        if (seleccionados.isEmpty()) { // Si aún después de añadir todos, sigue vacío
            alerta("No hay registros para previsualizar.");
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
                    tituloVentana = "Previsualización — Presupuesto " + presupuestoSeleccionado.getNumero();

                    // Opcional: eliminar el archivo temporal después de leerlo
                    Files.deleteIfExists(pdfPath);

                } else {
                    // Previsualizar un listado de múltiples presupuestos
                    pdfBytes = PdfPreviewService.previsualizarPresupuestos(seleccionados);
                    pdfImpresionBytes = pdfBytes;
                    tituloVentana = "Previsualización — Presupuestos (" + seleccionados.size() + " registro(s))";
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
            case "fechaValidez" -> "fecha_validez";
            default -> campo;
        };
    }
    private String nvl(String s) { return s != null ? s : ""; }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { SoundService.play(SoundService.Sound.ERROR); new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
