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
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportBackupService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class PresupuestosView extends VBox {

    private final PresupuestoDAO dao = new PresupuestoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Presupuesto> datos = FXCollections.observableArrayList();
    private final TableView<Presupuesto> tabla = new TableView<>(datos);

    public PresupuestosView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Presupuestos");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn("+ Nuevo",           "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",           "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",          "#E74C3C", this::borrar);
        Button btnImportar = btn("📥 Importar",        "#27AE60", this::importar);
        Button btnExportar = btn("📤 Exportar",        "#8E44AD", this::exportar);
        Button btnFacturar = btn("🧾 Crear Factura",   "#9B59B6", this::crearFactura);
        Button btnPreview  = btn("👁 Previsualizar",   "#6B2D5E", this::previsualizar);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnFacturar, btnPreview);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Presupuesto> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Presupuesto, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
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
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
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
                try { dao.save(pr); cargar(); } catch (Exception e) { mostrarError(e); }
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
                try { dao.save(pr); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        Presupuesto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un presupuesto para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar el presupuesto " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
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
                if (t != null) {
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
        c.setPrefWidth(ancho); return c;
    }

    private void importar() {
        String[][] formatos = {
            {"csv",   "📊  CSV",
                "Archivo .csv con cabecera de columnas (separador «;»). Compatible con Excel y LibreOffice.", "csv"},
            {"excel", "📗  Excel",
                "Libro Excel (.xlsx, .xls, .xlsb, .xlsm, .xltx). Hoja 1 = presupuestos · Hoja 2 = líneas (opcional).", "xlsx"},
            {"sql",   "🗄️  Volcado SQL",
                "Script .sql con presupuestos y sus líneas generado por la exportación SQL.", "sql"},
            {"json",  "{ }  JSON",
                "Archivo .json con presupuestos y líneas generado por la exportación JSON o por el backup completo.", "json"},
            {"word",  "📝  Word",
                "Documento Word (.docx/.doc). Tabla 1 = presupuestos · Tabla 2 = líneas (opcional).", "docx"},
            {"pdf",   "📄  PDF",
                "Documento PDF con tabla de presupuestos (columnas separadas por tabulador, «|» o dobles espacios).", "pdf"}
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
            "ℹ  Los presupuestos importados se añaden o actualizan. " +
            "Los registros con el mismo ID serán sobreescritos.");
        aviso.setWrapText(true);
        aviso.setStyle("-fx-font-size:11px; -fx-text-fill:-c-text-muted;");

        Label lblSelecciona = new Label("Selecciona el formato del archivo:");
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox, aviso);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Importar presupuestos");
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
        fc.setTitle("Seleccionar archivo de presupuestos — " + fmt[1]);
        switch (fmt[0]) {
            case "excel" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel — Presupuestos", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.xltx", "*.xltm"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            case "word" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word — Presupuestos", "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            default -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(fmt[1].replaceAll("[^\\w ]", "").trim() + " — Presupuestos", "*." + fmt[3]),
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
                    case "csv"   -> ImportBackupService.importarPresupuestosCSV(origen);
                    case "sql"   -> ImportBackupService.importarPresupuestosSQL(origen);
                    case "json"  -> ImportBackupService.importarPresupuestosJSON(origen);
                    case "excel" -> ImportBackupService.importarPresupuestosExcel(origen);
                    case "word"  -> ImportBackupService.importarPresupuestosWord(origen);
                    case "pdf"   -> ImportBackupService.importarPresupuestosPDF(origen);
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
                "Tabla de presupuestos como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con presupuestos y sus líneas (tabla completa).", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Presupuestos y líneas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de presupuestos como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de presupuestos en documento Word (.docx), editable.", "docx"}
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

        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarPresupuestosCSV(destino);
                    case "sql"    -> ExportService.exportarPresupuestosSQL(destino);
                    case "json"   -> ExportService.exportarPresupuestosJSON(destino);
                    case "pdf"    -> ExportService.exportarPresupuestosPDF(destino, dao.findAll());
                    case "word"   -> ExportService.exportarPresupuestosWord(destino, dao.findAll());
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
        List<Presupuesto> sel = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Presupuesto> lista = sel.isEmpty() ? new java.util.ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdf = PdfPreviewService.previsualizarPresupuestos(lista);
                Platform.runLater(() -> {
                    setDisable(false);
                    PdfPreviewWindow.mostrar(pdf,
                        "Previsualización — Presupuestos (" + lista.size() + " registro(s))");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> { setDisable(false); mostrarError(ex); });
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
    private String nvl(String s) { return s != null ? s : ""; }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { SoundService.play(SoundService.Sound.ERROR); new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
