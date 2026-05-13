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
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.model.*;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportBackupService;
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

public class FacturasView extends VBox {

    private final FacturaDAO dao = new FacturaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Factura> datos = FXCollections.observableArrayList();
    private final TableView<Factura> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("numero", "Número");
        COLUMNAS_BASE.put("presupuesto_id", "Presupuesto");
        COLUMNAS_BASE.put("cliente_id", "Cliente");
        COLUMNAS_BASE.put("fecha", "Fecha");
        COLUMNAS_BASE.put("fecha_vencimiento", "Vencimiento");
        COLUMNAS_BASE.put("estado", "Estado");
        COLUMNAS_BASE.put("forma_pago", "Forma de pago");
        COLUMNAS_BASE.put("base_imponible", "Base imponible");
        COLUMNAS_BASE.put("iva_porcentaje", "IVA %");
        COLUMNAS_BASE.put("iva_importe", "IVA");
        COLUMNAS_BASE.put("total", "Total");
        COLUMNAS_BASE.put("notas", "Notas");
        COLUMNAS_BASE.put("created_at", "Creado");
    }
    private final DynamicColumnRuntime<Factura> dynamicColumns =
        new DynamicColumnRuntime<>("facturas", "Facturas", COLUMNAS_BASE, tabla, datos, Factura::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();

    public FacturasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Facturas");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnEditar   = btn("✏ Editar",           "#F39C12", this::editar);
        Button btnImportar = btn("📥 Importar",        "#27AE60", this::importar);
        Button btnExportar = btn("📤 Exportar",        "#8E44AD", this::exportar);
        Button btnAlbaran  = btn("📋 Crear Albarán",   "#9B59B6", this::crearAlbaran);
        Button btnPagada   = btn("✅ Marcar pagada",   "#4C9BE8", this::marcarPagada);
        Button btnAnular   = btn("❌ Anular",           "#E74C3C", this::anular);
        Button btnBorrar   = btn("🗑 Borrar",           "#95A5A6", this::borrar);
        Button btnPreview    = btn("👁 Previsualizar",   "#6B2D5E", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas",         "#34495E", dynamicColumns::configure);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnEditar, btnImportar, btnExportar, btnAlbaran, btnPagada, btnAnular, btnBorrar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Factura> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Factura, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setUserData("estado");
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toUpperCase());
                String color = switch(v) {
                    case "pagada"   -> "#27AE60";
                    case "vencida"  -> "#E74C3C";
                    case "anulada"  -> "#95A5A6";
                    default         -> "#F39C12";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });

        TableColumn<Factura, Double> colTotal = new TableColumn<>("Total");
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
            col("Vencimiento", "fechaVencimiento", 110),
            col("Forma de pago", "formaPago", 150),
            colEstado,
            colTotal
        );
        tabla.setPlaceholder(new Label("No hay facturas registradas"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); dynamicColumns.apply(); } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura para editar."); return; }
        try {
            Factura f = dao.findById(sel.getId());
            dialogoFactura(f).ifPresent(actualizada -> {
                try { dao.save(actualizada); dynamicColumns.saveFormFields(actualizada, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private Optional<Factura> dialogoFactura(Factura f) {
        Dialog<Factura> dlg = new Dialog<>();
        dlg.setTitle("Editar factura " + f.getNumero());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(820);
        dlg.getDialogPane().setPrefHeight(600);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1 — Datos generales
        GridPane gDatos = new GridPane();
        gDatos.setHgap(10); gDatos.setVgap(10); gDatos.setPadding(new Insets(16));

        TextField fNumero = tf(f.getNumero()); fNumero.setEditable(false);
        TextField fFecha = tf(f.getFecha());
        TextField fVto = tf(f.getFechaVencimiento());
        ComboBox<String> fFormaPago = new ComboBox<>(FXCollections.observableArrayList(
            "Transferencia bancaria", "Efectivo", "Tarjeta", "Cheque", "Domiciliación bancaria"));
        fFormaPago.setEditable(true);
        fFormaPago.setValue(f.getFormaPago() != null ? f.getFormaPago() : "Transferencia bancaria");
        ComboBox<String> fEstado = new ComboBox<>(FXCollections.observableArrayList(
            "pendiente", "pagada", "vencida", "anulada"));
        fEstado.setValue(f.getEstado());
        TextField fIva = tf(String.valueOf(f.getIvaPorcentaje()));
        TextArea fNotas = new TextArea(f.getNotas() != null ? f.getNotas() : ""); fNotas.setPrefRowCount(3);

        gDatos.addRow(0, lbl("Número"),       fNumero,    lbl("Estado"),      fEstado);
        gDatos.addRow(1, lbl("Fecha"),         fFecha,     lbl("Vencimiento"), fVto);
        gDatos.addRow(2, lbl("Forma de pago"), fFormaPago, lbl("IVA (%)"),     fIva);
        gDatos.add(lbl("Notas"), 0, 3); gDatos.add(fNotas, 1, 3, 3, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(gDatos, 4, f, dialogExtraFields);
        tabs.getTabs().add(new Tab("Datos generales", gDatos));

        // Tab 2 — Servicios / Técnicas
        ObservableList<LineaFactura> lineasServ = FXCollections.observableArrayList(
            f.getLineas().stream().filter(l -> !"📦 Material".equals(l.getTecnica())).toList());

        // Tab 3 — Materiales del stock
        ObservableList<LineaFactura> lineasMat = FXCollections.observableArrayList(
            f.getLineas().stream().filter(l -> "📦 Material".equals(l.getTecnica())).toList());

        tabs.getTabs().add(new Tab("Servicios / Técnicas",    buildTablaLineasFactura(lineasServ)));
        tabs.getTabs().add(new Tab("📦 Materiales del stock", buildTabMaterialesFactura(lineasMat)));

        dlg.getDialogPane().setContent(tabs);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            f.setFecha(fFecha.getText().trim());
            f.setFechaVencimiento(fVto.getText().trim());
            f.setFormaPago(fFormaPago.getValue());
            f.setEstado(fEstado.getValue());
            f.setIvaPorcentaje(parseDouble(fIva.getText()));
            f.setNotas(fNotas.getText().trim());
            List<LineaFactura> todas = new java.util.ArrayList<>(lineasServ);
            todas.addAll(lineasMat);
            f.setLineas(todas);
            f.calcularTotales();
            return f;
        });
        return dlg.showAndWait();
    }

    private Node buildTablaLineasFactura(ObservableList<LineaFactura> lineas) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        TableView<LineaFactura> t = new TableView<>(lineas);
        t.setEditable(true);
        t.setPrefHeight(280);
        t.setPlaceholder(new Label("No hay servicios en esta factura"));

        TableColumn<LineaFactura, String> cDesc = new TableColumn<>("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion")); cDesc.setPrefWidth(250);
        TableColumn<LineaFactura, String> cTec = new TableColumn<>("Técnica");
        cTec.setCellValueFactory(new PropertyValueFactory<>("tecnica")); cTec.setPrefWidth(100);
        TableColumn<LineaFactura, Integer> cCant = new TableColumn<>("Cant.");
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad")); cCant.setPrefWidth(60);
        TableColumn<LineaFactura, Double> cPrecio = new TableColumn<>("Precio ud.");
        cPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(90);
        TableColumn<LineaFactura, Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cTotal.setPrefWidth(90);

        t.getColumns().addAll(cDesc, cTec, cCant, cPrecio, cTotal);

        Button btnAdd  = btn("+ Añadir",  "#4C9BE8", () -> dialogoLineaFactura(null, lineas, t));
        Button btnEdit = btn("✏ Editar",  "#F39C12", () -> {
            LineaFactura sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoLineaFactura(sel, lineas, t);
        });
        Button btnDel  = btn("🗑 Quitar", "#E74C3C", () -> {
            LineaFactura sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) lineas.remove(sel);
        });

        box.getChildren().addAll(t, new HBox(8, btnAdd, btnEdit, btnDel));
        return box;
    }

    private void dialogoLineaFactura(LineaFactura linea, ObservableList<LineaFactura> lista,
                                     TableView<LineaFactura> tabla) {
        boolean esNueva = linea == null;
        if (esNueva) linea = new LineaFactura();
        LineaFactura l = linea;

        Dialog<LineaFactura> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? "Nueva línea de servicio" : "Editar línea");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextArea fDesc = new TextArea(l.getDescripcion() != null ? l.getDescripcion() : "");
        fDesc.setPrefRowCount(3); fDesc.setPrefWidth(300);
        TextField fTec   = tf(l.getTecnica());
        TextField fCant  = tf(l.getCantidad() > 0 ? String.valueOf(l.getCantidad()) : "1");
        TextField fPrecio = tf(l.getPrecioUnit() > 0 ? String.valueOf(l.getPrecioUnit()) : "");
        TextField fDto   = tf(l.getDescuento() > 0 ? String.valueOf(l.getDescuento()) : "0");

        try {
            List<Tarifa> tarifas = new TarifaDAO().findAll();
            ComboBox<Tarifa> cbTarifa = new ComboBox<>(FXCollections.observableArrayList(tarifas));
            cbTarifa.setPromptText("Seleccionar tarifa...");
            cbTarifa.setOnAction(e -> {
                Tarifa tar = cbTarifa.getValue();
                if (tar != null) {
                    if (fDesc.getText().isBlank())
                        fDesc.setText(tar.getNombre() + (tar.getDescripcion() != null ? " - " + tar.getDescripcion() : ""));
                    fTec.setText(tar.getTecnica());
                    fPrecio.setText(String.valueOf(tar.getPrecioUnit()));
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
            tabla.refresh();
        });
    }

    private Node buildTabMaterialesFactura(ObservableList<LineaFactura> lineasMat) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        TableView<LineaFactura> tablaMat = new TableView<>(lineasMat);
        tablaMat.setPrefHeight(200);
        tablaMat.setPlaceholder(new Label("No hay materiales añadidos a esta factura"));

        TableColumn<LineaFactura, String> cNom = new TableColumn<>("Material");
        cNom.setCellValueFactory(new PropertyValueFactory<>("descripcion")); cNom.setPrefWidth(260);
        TableColumn<LineaFactura, Integer> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad")); cCant.setPrefWidth(80);
        TableColumn<LineaFactura, Double> cPrecio = new TableColumn<>("Precio ud.");
        cPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        cPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cPrecio.setPrefWidth(100);
        TableColumn<LineaFactura, Double> cTotal = new TableColumn<>("Total");
        cTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        cTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        cTotal.setPrefWidth(100);

        tablaMat.getColumns().addAll(cNom, cCant, cPrecio, cTotal);

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

        Button btnAnadir = new Button("➕ Añadir a la factura");
        btnAnadir.setStyle(
            "-fx-background-color:#27AE60; -fx-text-fill:white; " +
            "-fx-font-weight:bold; -fx-padding:6 16; -fx-background-radius:4;");
        btnAnadir.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m == null) { alerta("Selecciona un material del desplegable."); return; }
            LineaFactura lf = new LineaFactura();
            lf.setDescripcion(m.getNombre() +
                (m.getReferencia() != null && !m.getReferencia().isBlank() ? " [" + m.getReferencia() + "]" : ""));
            lf.setTecnica("📦 Material");
            lf.setCantidad(spCant.getValue());
            lf.setPrecioUnit(m.getPrecioUnidad());
            lf.setDescuento(0);
            lf.calcularTotal();
            lineasMat.add(lf);
        });

        Button btnQuitar = btn("🗑 Quitar", "#E74C3C", () -> {
            LineaFactura sel = tablaMat.getSelectionModel().getSelectedItem();
            if (sel != null) lineasMat.remove(sel);
        });

        Label lblTotal = new Label("Total materiales: 0.00 €");
        lblTotal.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
        lineasMat.addListener((javafx.collections.ListChangeListener<LineaFactura>) c -> {
            double tot = lineasMat.stream().mapToDouble(LineaFactura::getTotal).sum();
            lblTotal.setText(String.format("Total materiales: %.2f €", tot));
        });
        double totInicial = lineasMat.stream().mapToDouble(LineaFactura::getTotal).sum();
        lblTotal.setText(String.format("Total materiales: %.2f €", totInicial));

        HBox pickerRow = new HBox(8, cbMat, new Label("Cantidad:"), spCant, btnAnadir);
        pickerRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
            lblPicker, pickerRow, lblInfo,
            new Separator(),
            tablaMat, new HBox(8, btnQuitar),
            new Separator(),
            lblTotal
        );
        return box;
    }

    private void crearAlbaran() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        if ("anulada".equals(sel.getEstado())) { alerta("No se puede crear un albarán para una factura anulada."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Crear albarán de entrega para la factura " + sel.getNumero() + "?",
            ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                var albaran = new AlbaranDAO().crearDesdeFactura(sel.getId());
                new Alert(Alert.AlertType.INFORMATION,
                    "Albarán " + albaran.getNumero() + " creado correctamente.\nPuedes verlo en la sección Albaranes.",
                    ButtonType.OK).showAndWait();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void marcarPagada() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        try { dao.updateEstado(sel.getId(), "pagada"); cargar(); } catch (Exception e) { mostrarError(e); }
    }

    private void anular() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Anular la factura " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.updateEstado(sel.getId(), "anulada"); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar permanentemente la factura " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Factura, T> col(String t, String campo, double ancho) {
        TableColumn<Factura, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setUserData(toDbColumn(campo));
        c.setPrefWidth(ancho); return c;
    }

    private void importar() {
        String[][] formatos = {
            {"csv",   "📊  CSV",
                "Archivo .csv con cabecera de columnas (separador «;»). Compatible con Excel y LibreOffice.", "csv"},
            {"excel", "📗  Excel",
                "Libro Excel (.xlsx, .xls, .xlsb, .xlsm, .xltx). Hoja 1 = facturas · Hoja 2 = líneas (opcional).", "xlsx"},
            {"sql",   "🗄️  Volcado SQL",
                "Script .sql con facturas y sus líneas generado por la exportación SQL.", "sql"},
            {"json",  "{ }  JSON",
                "Archivo .json con facturas y líneas generado por la exportación JSON o por el backup completo.", "json"},
            {"word",  "📝  Word",
                "Documento Word (.docx/.doc). Tabla 1 = facturas · Tabla 2 = líneas (opcional).", "docx"},
            {"pdf",   "📄  PDF",
                "Documento PDF con tabla de facturas (columnas separadas por tabulador, «|» o dobles espacios).", "pdf"}
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
            HBox fila  = new HBox(10, rb, texto);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new Insets(7, 12, 7, 12));
            fila.setStyle("-fx-background-radius:6; -fx-cursor:hand;");
            fila.setOnMouseClicked(e -> rb.setSelected(true));
            opBox.getChildren().add(fila);
        }
        grupo.getToggles().get(0).setSelected(true);

        Label aviso = new Label(
            "ℹ  Las facturas importadas se añaden o actualizan. " +
            "Los registros con el mismo ID serán sobreescritos.");
        aviso.setWrapText(true);
        aviso.setStyle("-fx-font-size:11px; -fx-text-fill:-c-text-muted;");

        Label lblSelecciona = new Label("Selecciona el formato del archivo:");
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox, aviso);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Importar facturas");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(460);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("Seleccionar archivo →");

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarImportacion);
    }

    private void lanzarImportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo de facturas — " + fmt[1]);
        switch (fmt[0]) {
            case "excel" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel — Facturas", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.xltx", "*.xltm"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            case "word" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word — Facturas", "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            default -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(fmt[1].replaceAll("[^\\w ]", "").trim() + " — Facturas", "*." + fmt[3]),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        }
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path origen = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        Thread.ofVirtual().start(() -> {
            try {
                int importados = switch (fmt[0]) {
                    case "csv"   -> ImportBackupService.importarFacturasCSV(origen);
                    case "sql"   -> ImportBackupService.importarFacturasSQL(origen);
                    case "json"  -> ImportBackupService.importarFacturasJSON(origen);
                    case "excel" -> ImportBackupService.importarFacturasExcel(origen);
                    case "word"  -> ImportBackupService.importarFacturasWord(origen);
                    case "pdf"   -> ImportBackupService.importarFacturasPDF(origen);
                    default      -> throw new Exception("Formato desconocido: " + fmt[0]);
                };
                final int n = importados;
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    cargar();
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Se han importado o actualizado " + n + " registro(s) correctamente.",
                        ButtonType.OK);
                    ok.setTitle("Importación completada");
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

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de facturas como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con facturas y sus líneas (tabla completa).", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Facturas y líneas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de facturas como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de facturas en documento Word (.docx), editable.", "docx"}
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
        dlg.setTitle("Exportar facturas");
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
        fc.setInitialFileName("Facturas_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Facturas", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Factura> facturasAExportar = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (facturasAExportar.isEmpty()) facturasAExportar.addAll(datos);
        if (facturasAExportar.isEmpty()) {
            Platform.runLater(() -> { setDisable(false); alerta("No hay registros para exportar."); });
            return;
        }
        final List<Factura> listaFinal = facturasAExportar;

        Thread.ofVirtual().start(() -> {
            try {
                if (listaFinal.size() == 1 && ("pdf".equals(fmt[0]) || "word".equals(fmt[0]))) {
                    // Exportar una única factura como documento detallado
                    Factura facturaSeleccionada = dao.findById(listaFinal.get(0).getId());
                    if (facturaSeleccionada == null)
                        throw new Exception("No se pudo cargar la factura seleccionada.");
                    Cliente clienteAsociado = clienteDAO.findById(facturaSeleccionada.getClienteId());
                    if (clienteAsociado == null)
                        throw new Exception("No se pudo encontrar el cliente asociado a la factura.");

                    if ("pdf".equals(fmt[0])) {
                        PDFService pdfService = new PDFService();
                        Path tempPdfPath = pdfService.generarFactura(facturaSeleccionada, clienteAsociado);
                        Files.copy(tempPdfPath, destino, StandardCopyOption.REPLACE_EXISTING);
                        Files.deleteIfExists(tempPdfPath);
                    } else {
                        ExportService.exportarFacturaDetalladaWord(destino, facturaSeleccionada, clienteAsociado);
                    }

                } else {
                    switch (fmt[0]) {
                        case "sqlite" -> ExportService.backupSQLite(destino);
                        case "csv"    -> ExportService.exportarFacturasCSV(destino);
                        case "sql"    -> ExportService.exportarFacturasSQL(destino);
                        case "json"   -> ExportService.exportarFacturasJSON(destino);
                        case "pdf"    -> ExportService.exportarFacturasPDF(destino, listaFinal);
                        case "word"   -> ExportService.exportarFacturasWord(destino, listaFinal);
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
        List<Factura> sel = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Factura> lista = sel.isEmpty() ? new java.util.ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes;
                String tituloVentana;

                if (lista.size() == 1) {
                    Factura facturaSeleccionada = dao.findById(lista.get(0).getId());
                    if (facturaSeleccionada == null)
                        throw new Exception("No se pudo cargar la factura seleccionada.");
                    Cliente clienteAsociado = clienteDAO.findById(facturaSeleccionada.getClienteId());
                    if (clienteAsociado == null)
                        throw new Exception("No se pudo encontrar el cliente asociado a la factura.");
                    PDFService pdfService = new PDFService();
                    Path pdfPath = pdfService.generarFactura(facturaSeleccionada, clienteAsociado);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Factura " + facturaSeleccionada.getNumero();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarFacturas(lista);
                    tituloVentana = "Previsualización — Facturas (" + lista.size() + " registro(s))";
                }

                final byte[] finalPdfBytes = pdfBytes;
                final String finalTitulo = tituloVentana;
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    PdfPreviewWindow.mostrar(finalPdfBytes, finalTitulo);
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
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private String toDbColumn(String campo) {
        return switch (campo) {
            case "clienteNombre" -> "cliente_id";
            case "fechaVencimiento" -> "fecha_vencimiento";
            case "formaPago" -> "forma_pago";
            default -> campo;
        };
    }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { SoundService.play(SoundService.Sound.ERROR); new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
