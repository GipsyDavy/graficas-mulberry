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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.gipsybuho.dao.ConsumoMaterialDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.dao.PagoMaterialDAO;
import org.gipsybuho.model.ConsumoMaterial;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.PagoMaterial;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class MaterialesView extends VBox {

    private static final String[] CATEGORIAS = {
        "tintas", "pantallas", "sustratos", "vinilos", "consumibles", "bordado", "gran formato"
    };
    private static final String[] TECNICAS = {
        "Serigrafía", "DTF", "Bordado", "Vinilo", "Sublimación", "Gran Formato"
    };
    private static final String[] FORMAS_PAGO = {
        "Contado", "15 días", "30 días", "45 días", "60 días", "90 días", "120 días", "Personalizado"
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final MaterialDAO      dao       = new MaterialDAO();
    private final ConsumoMaterialDAO consumoDao = new ConsumoMaterialDAO();
    private final PagoMaterialDAO  pagoDao   = new PagoMaterialDAO();

    // ── Stock tab ─────────────────────────────────────────────────────────────
    private final ObservableList<Material> datos = FXCollections.observableArrayList();
    private final TableView<Material>      tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("nombre", "Nombre");
        COLUMNAS_BASE.put("referencia", "Referencia");
        COLUMNAS_BASE.put("categoria", "Categoría");
        COLUMNAS_BASE.put("stock_actual", "Stock actual");
        COLUMNAS_BASE.put("stock_minimo", "Stock mín.");
        COLUMNAS_BASE.put("unidad", "Unidad");
        COLUMNAS_BASE.put("precio_unidad", "Precio/ud.");
        COLUMNAS_BASE.put("proveedor", "Proveedor");
        COLUMNAS_BASE.put("updated_at", "Actualizado");
    }
    private final DynamicColumnRuntime<Material> dynamicColumns =
        new DynamicColumnRuntime<>("materiales", "Materiales", COLUMNAS_BASE, tabla, datos, Material::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private CheckBox chkSoloAlerta;

    // ── Consumo tab ───────────────────────────────────────────────────────────
    private final ObservableList<ConsumoMaterial> datosConsumo = FXCollections.observableArrayList();
    private final TableView<ConsumoMaterial>      tablaConsumo = new TableView<>(datosConsumo);

    // ── Pagos tab ─────────────────────────────────────────────────────────────
    private final ObservableList<PagoMaterial> datosPagos = FXCollections.observableArrayList();
    private final TableView<PagoMaterial>      tablaPagos = new TableView<>(datosPagos);
    private String filtroPagos = "todos";  // "todos" | "pendiente" | "vencido" | "proximo" | "pagado"

    // Tarjetas de resumen de pagos
    private Label lblTotalPendiente = new Label("—");
    private Label lblVencidos       = new Label("—");
    private Label lblProximos       = new Label("—");

    // ── Constructor ───────────────────────────────────────────────────────────

    public MaterialesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Control de Materiales");
        titulo.getStyleClass().add("view-title");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox stockBox = new VBox(12, buildToolbarStock(), buildTablaStock());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(stockBox, Priority.ALWAYS);

        tabs.getTabs().addAll(
            new Tab("📦  Stock",              stockBox),
            new Tab("⚙  Consumo por técnica", buildTabConsumo()),
            new Tab("💳  Pagos",              buildTabPagos())
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(titulo, tabs);

        cargar();
        dynamicColumns.apply();
        cargarConsumo();
        cargarPagos();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB STOCK
    // ═════════════════════════════════════════════════════════════════════════

    private HBox buildToolbarStock() {
        chkSoloAlerta = new CheckBox("Solo materiales con stock bajo");
        chkSoloAlerta.setOnAction(e -> cargar());

        Button btnNuevo    = btn("+ Nuevo",          "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",          "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",         "#E74C3C", this::borrar);
        Button btnEntrada  = btn("📥 Entrada",        "#27AE60", this::ajustarEntrada);
        Button btnSalida   = btn("📤 Salida",         "#E67E22", this::ajustarSalida);
        Button btnImportar = btn("📂 Importar",       "#1ABC9C", this::importar);
        Button btnExportar   = btn("📤 Exportar",       "#8E44AD", this::exportar);
        Button btnPreview    = btn("👁 Previsualizar",  "#6B2D5E", this::previsualizar);
        Button btnColumnas   = btn("⚙ Columnas",        "#34495E", dynamicColumns::configure);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, chkSoloAlerta, sp, btnEntrada, btnSalida, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private TableView<Material> buildTablaStock() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Material, String> colEstado = new TableColumn<>("");
        colEstado.setPrefWidth(30);
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Circle circle = new Circle(6);
                circle.setFill(getTableRow().getItem().isBajoStock()
                    ? Color.web("#E74C3C") : Color.web("#27AE60"));
                setGraphic(circle);
            }
        });

        TableColumn<Material, Double> colStock = new TableColumn<>("Stock actual");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStock.setUserData("stock_actual");
        colStock.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                Material m = getTableRow().getItem();
                setText(v + " " + (m != null ? m.getUnidad() : ""));
                setStyle(m != null && m.isBajoStock() ? "-fx-text-fill:#E74C3C;-fx-font-weight:bold;" : "");
            }
        });

        TableColumn<Material, Double> colMin = new TableColumn<>("Stock mín.");
        colMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colMin.setUserData("stock_minimo");
        colMin.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || getTableRow().getItem() == null) { setText(null); return; }
                setText(v + " " + ((Material) getTableRow().getItem()).getUnidad());
            }
        });

        TableColumn<Material, Double> colPrecio = new TableColumn<>("Precio/ud.");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnidad"));
        colPrecio.setUserData("precio_unidad");
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(colEstado,
            col("Nombre", "nombre", 200), col("Referencia", "referencia", 100),
            col("Categoría", "categoria", 100), colStock, colMin, colPrecio,
            col("Proveedor", "proveedor", 140));
        tabla.setPlaceholder(new Label("No hay materiales registrados"));
        return tabla;
    }

    private void cargar() {
        try {
            datos.setAll(chkSoloAlerta != null && chkSoloAlerta.isSelected()
                ? dao.findBajoStock() : dao.findAll());
            dynamicColumns.apply();
        } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo()   { dialogo(new Material()).ifPresent(m -> { try { dao.save(m); dynamicColumns.saveFormFields(m, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); } }); }
    private void editar()  {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un material para editar."); return; }
        dialogo(sel).ifPresent(m -> { try { dao.save(m); dynamicColumns.saveFormFields(m, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); } });
    }
    private void borrar() {
        List<Material> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta("Selecciona uno o varios materiales para borrar."); return; }
        String mensaje = seleccionados.size() == 1
            ? "¿Eliminar el material \"" + seleccionados.get(0).getNombre() + "\"?"
            : "¿Eliminar " + seleccionados.size() + " materiales seleccionados?";
        conf(mensaje, () -> {
            try {
                for (Material material : seleccionados) dao.delete(material.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }
    private void ajustarEntrada() { ajustarStock("entrada"); }
    private void ajustarSalida()  { ajustarStock("salida"); }

    private void ajustarStock(String tipo) {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un material."); return; }
        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle(tipo.equals("entrada") ? "Entrada de stock" : "Salida de stock");
        dlg.setHeaderText("Material: " + sel.getNombre() + "\nStock actual: " + sel.getStockActual() + " " + sel.getUnidad());
        dlg.setContentText("Cantidad (" + sel.getUnidad() + "):");
        dlg.showAndWait().ifPresent(s -> {
            try {
                double cantidad = Double.parseDouble(s.replace(",", "."));
                dao.ajustarStock(sel.getId(), cantidad, tipo,
                    tipo.equals("entrada") ? "Entrada manual" : "Salida manual");
                cargar();
            } catch (NumberFormatException ex) { alerta("Introduce una cantidad válida."); }
            catch (Exception ex) { mostrarError(ex); }
        });
    }

    private Optional<Material> dialogo(Material m) {
        Dialog<Material> dlg = new Dialog<>();
        dlg.setTitle(m.getId() == 0 ? "Nuevo material" : "Editar material");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        TextField fNombre    = tf(m.getNombre());
        TextField fRef       = tf(m.getReferencia());
        ComboBox<String> fCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        fCat.setValue(m.getCategoria() != null ? m.getCategoria() : CATEGORIAS[0]);
        TextField fStock     = tf(m.getStockActual() > 0 ? String.valueOf(m.getStockActual()) : "0");
        TextField fStockMin  = tf(m.getStockMinimo() > 0 ? String.valueOf(m.getStockMinimo()) : "0");
        TextField fUnidad    = tf(m.getUnidad() != null ? m.getUnidad() : "ud");
        TextField fPrecio    = tf(m.getPrecioUnidad() > 0 ? String.valueOf(m.getPrecioUnidad()) : "0");
        TextField fProveedor = tf(m.getProveedor());
        grid.addRow(0, lbl("Nombre *"), fNombre, lbl("Referencia"), fRef);
        grid.addRow(1, lbl("Categoría"), fCat, lbl("Unidad"), fUnidad);
        grid.addRow(2, lbl("Stock actual"), fStock, lbl("Stock mínimo"), fStockMin);
        grid.addRow(3, lbl("Precio/ud. (€)"), fPrecio, lbl("Proveedor"), fProveedor);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(grid, 4, m, dialogExtraFields);
        dlg.getDialogPane().setContent(grid);
        Node ok = dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> ok.setDisable(b.isBlank()));
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            m.setNombre(fNombre.getText().trim());
            m.setReferencia(fRef.getText().trim());
            m.setCategoria(fCat.getValue());
            m.setStockActual(parseDouble(fStock.getText()));
            m.setStockMinimo(parseDouble(fStockMin.getText()));
            m.setUnidad(fUnidad.getText().trim());
            m.setPrecioUnidad(parseDouble(fPrecio.getText()));
            m.setProveedor(fProveedor.getText().trim());
            return m;
        });
        return dlg.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB CONSUMO POR TÉCNICA (sin cambios)
    // ═════════════════════════════════════════════════════════════════════════

    private Node buildTabConsumo() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        Label info = new Label("Define cuánto material se consume por unidad de cada técnica. El stock se descuenta automáticamente al crear una factura.");
        info.setStyle("-fx-text-fill:#666;-fx-font-size:12;");
        info.setWrapText(true);
        tablaConsumo.getStyleClass().add("data-table");
        tablaConsumo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ConsumoMaterial, Double> colCant = new TableColumn<>("Cant./unidad");
        colCant.setCellValueFactory(new PropertyValueFactory<>("cantidadPorUnidad"));
        colCant.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                ConsumoMaterial cm = getTableRow().getItem();
                setText(v + (cm != null && cm.getUnidad() != null ? " " + cm.getUnidad() : ""));
            }
        });
        tablaConsumo.getColumns().addAll(
            colConsumo("Técnica", "tecnica", 150), colConsumo("Material", "materialNombre", 220),
            colCant, colConsumo("Unidad", "unidad", 80));
        tablaConsumo.setPlaceholder(new Label("Sin reglas de consumo."));
        VBox.setVgrow(tablaConsumo, Priority.ALWAYS);
        Button btnAdd  = btn("+ Añadir regla", "#4C9BE8", this::nuevaRegla);
        Button btnEdit = btn("✏ Editar",        "#F39C12", this::editarRegla);
        Button btnDel  = btn("🗑 Eliminar",      "#E74C3C", this::eliminarRegla);
        box.getChildren().addAll(info, tablaConsumo, new HBox(8, btnAdd, btnEdit, btnDel));
        return box;
    }

    private void cargarConsumo() {
        try { datosConsumo.setAll(consumoDao.findAll()); } catch (Exception e) { mostrarError(e); }
    }
    private void nuevaRegla() { dialogoConsumo(new ConsumoMaterial()).ifPresent(c -> { try { consumoDao.save(c); cargarConsumo(); } catch (Exception e) { mostrarError(e); } }); }
    private void editarRegla() {
        ConsumoMaterial sel = tablaConsumo.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una regla para editar."); return; }
        dialogoConsumo(sel).ifPresent(c -> { try { consumoDao.save(c); cargarConsumo(); } catch (Exception e) { mostrarError(e); } });
    }
    private void eliminarRegla() {
        ConsumoMaterial sel = tablaConsumo.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una regla."); return; }
        conf("¿Eliminar la regla: " + sel.getTecnica() + " → " + sel.getMaterialNombre() + "?",
            () -> { try { consumoDao.delete(sel.getId()); cargarConsumo(); } catch (Exception e) { mostrarError(e); } });
    }
    private Optional<ConsumoMaterial> dialogoConsumo(ConsumoMaterial c) {
        Dialog<ConsumoMaterial> dlg = new Dialog<>();
        dlg.setTitle(c.getId() == 0 ? "Nueva regla de consumo" : "Editar regla de consumo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(440);
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        ComboBox<String> fTecnica = new ComboBox<>(FXCollections.observableArrayList(TECNICAS));
        fTecnica.setValue(c.getTecnica() != null ? c.getTecnica() : TECNICAS[0]);
        fTecnica.setMaxWidth(Double.MAX_VALUE);
        List<Material> materiales;
        try { materiales = dao.findAll(); } catch (Exception ex) { materiales = List.of(); }
        ComboBox<Material> fMaterial = new ComboBox<>(FXCollections.observableArrayList(materiales));
        fMaterial.setConverter(new StringConverter<>() {
            @Override public String toString(Material m) { return m == null ? "" : m.getNombre() + " (" + m.getUnidad() + ")"; }
            @Override public Material fromString(String s) { return null; }
        });
        materiales.stream().filter(m -> m.getId() == c.getMaterialId()).findFirst().ifPresent(fMaterial::setValue);
        fMaterial.setMaxWidth(Double.MAX_VALUE);
        TextField fCantidad = tf(c.getCantidadPorUnidad() > 0 ? String.valueOf(c.getCantidadPorUnidad()) : "");
        grid.addRow(0, lbl("Técnica *"), fTecnica);
        grid.addRow(1, lbl("Material *"), fMaterial);
        grid.addRow(2, lbl("Cant. por unidad *"), fCantidad);
        GridPane.setHgrow(fTecnica, Priority.ALWAYS);
        GridPane.setHgrow(fMaterial, Priority.ALWAYS);
        GridPane.setHgrow(fCantidad, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fMaterial.getValue() == null) { alerta("Selecciona un material."); event.consume(); }
            else if (parseDouble(fCantidad.getText()) <= 0) { alerta("La cantidad debe ser mayor que 0."); event.consume(); }
        });
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            c.setTecnica(fTecnica.getValue());
            c.setMaterialId(fMaterial.getValue().getId());
            c.setMaterialNombre(fMaterial.getValue().getNombre());
            c.setUnidad(fMaterial.getValue().getUnidad());
            c.setCantidadPorUnidad(parseDouble(fCantidad.getText()));
            return c;
        });
        return dlg.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB PAGOS
    // ═════════════════════════════════════════════════════════════════════════

    private Node buildTabPagos() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        // ── Tarjetas de resumen ───────────────────────────────────────────────
        box.getChildren().add(buildResumenPagos());

        // ── Barra de filtros y acciones ───────────────────────────────────────
        box.getChildren().add(buildToolbarPagos());

        // ── Tabla de pagos ────────────────────────────────────────────────────
        buildTablaPagos();
        VBox.setVgrow(tablaPagos, Priority.ALWAYS);
        box.getChildren().add(tablaPagos);

        return box;
    }

    private HBox buildResumenPagos() {
        HBox row = new HBox(12);

        VBox cardPendiente = tarjetaResumen("💶  Total pendiente", lblTotalPendiente, "#F39C12");
        VBox cardVencido   = tarjetaResumen("🔴  Vencidos",        lblVencidos,       "#E74C3C");
        VBox cardProximo   = tarjetaResumen("⏰  Próximos 7 días", lblProximos,       "#E67E22");

        row.getChildren().addAll(cardPendiente, cardVencido, cardProximo);
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

    private HBox buildToolbarPagos() {
        // Botones de filtro (tipo toggle)
        ToggleGroup tg = new ToggleGroup();
        ToggleButton bTodos     = filtroBtn("Todos",      "todos",     tg);
        ToggleButton bPendiente = filtroBtn("Pendientes", "pendiente", tg);
        ToggleButton bVencido   = filtroBtn("Vencidos",   "vencido",   tg);
        ToggleButton bProximo   = filtroBtn("Próximos",   "proximo",   tg);
        ToggleButton bPagado    = filtroBtn("Pagados",    "pagado",    tg);
        bTodos.setSelected(true);

        HBox filtros = new HBox(2, bTodos, bPendiente, bVencido, bProximo, bPagado);
        filtros.setStyle("-fx-background-color:-c-tab-bg;-fx-background-radius:5;-fx-padding:3;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNuevo   = btn("+ Nueva compra",   "#4C9BE8", this::nuevoPago);
        Button btnPagado  = btn("✓ Marcar pagado",  "#27AE60", this::marcarPagado);
        Button btnEditar  = btn("✏ Editar",          "#F39C12", this::editarPago);
        Button btnBorrar  = btn("🗑 Eliminar",        "#E74C3C", this::eliminarPago);

        HBox bar = new HBox(10, filtros, sp, btnNuevo, btnPagado, btnEditar, btnBorrar);
        bar.setAlignment(Pos.CENTER_LEFT);
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
                cargarPagos();
            } else {
                tb.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:5 12;" +
                    "-fx-background-radius:4;-fx-font-size:12px;");
            }
        });
        return tb;
    }

    private void buildTablaPagos() {
        tablaPagos.getStyleClass().add("data-table");
        tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Columna estado (indicador visual)
        TableColumn<PagoMaterial, Void> colEst = new TableColumn<>("");
        colEst.setPrefWidth(36);
        colEst.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); setText(null); return; }
                PagoMaterial p = getTableRow().getItem();
                Circle dot = new Circle(7);
                Tooltip tip = new Tooltip();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"   -> { dot.setFill(Color.web("#27AE60")); tip.setText("Pagado"); }
                    case "vencido"  -> { dot.setFill(Color.web("#E74C3C")); tip.setText("Vencido"); }
                    case "proximo"  -> { dot.setFill(Color.web("#E67E22")); tip.setText("Próximo a vencer"); }
                    default         -> { dot.setFill(Color.web("#4C9BE8")); tip.setText("Pendiente"); }
                }
                Tooltip.install(dot, tip);
                setGraphic(dot);
                setText(null);
            }
        });

        // Columna material
        TableColumn<PagoMaterial, String> colMat = new TableColumn<>("Material");
        colMat.setCellValueFactory(new PropertyValueFactory<>("materialNombre"));
        colMat.setPrefWidth(160);

        // Columna proveedor
        TableColumn<PagoMaterial, String> colProv = new TableColumn<>("Proveedor");
        colProv.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colProv.setPrefWidth(130);

        // Columna nº factura
        TableColumn<PagoMaterial, String> colNum = new TableColumn<>("Nº Factura");
        colNum.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colNum.setPrefWidth(100);

        // Columna fecha compra
        TableColumn<PagoMaterial, LocalDate> colFc = new TableColumn<>("Fecha compra");
        colFc.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFc.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });
        colFc.setPrefWidth(105);

        // Columna importe
        TableColumn<PagoMaterial, Double> colImp = new TableColumn<>("Importe");
        colImp.setCellValueFactory(new PropertyValueFactory<>("importeTotal"));
        colImp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        colImp.setPrefWidth(90);

        // Columna forma de pago
        TableColumn<PagoMaterial, String> colFp = new TableColumn<>("Forma pago");
        colFp.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colFp.setPrefWidth(90);

        // Columna fecha vencimiento (con color)
        TableColumn<PagoMaterial, LocalDate> colFv = new TableColumn<>("Vencimiento");
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

        // Columna días restantes
        TableColumn<PagoMaterial, Void> colDias = new TableColumn<>("Días");
        colDias.setPrefWidth(70);
        colDias.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                PagoMaterial p = getTableRow().getItem();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"  -> { setText("PAGADO"); setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;"); }
                    case "vencido" -> { setText(p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;"); }
                    case "proximo" -> { setText("+" + p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E67E22;-fx-font-weight:bold;"); }
                    default        -> { setText("+" + p.getDiasRestantes() + "d"); setStyle(""); }
                }
            }
        });

        // Columna notas
        TableColumn<PagoMaterial, String> colNotas = new TableColumn<>("Notas");
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        colNotas.setPrefWidth(140);

        tablaPagos.getColumns().addAll(
            colEst, colMat, colProv, colNum, colFc, colImp, colFp, colFv, colDias, colNotas);
        tablaPagos.setPlaceholder(new Label("No hay pagos registrados"));

        // Doble clic para editar
        tablaPagos.setRowFactory(tv -> {
            TableRow<PagoMaterial> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) editarPago(); });
            return row;
        });
    }

    private void cargarPagos() {
        try {
            List<PagoMaterial> todos = pagoDao.findAll();

            List<PagoMaterial> filtrados = switch (filtroPagos) {
                case "pendiente" -> todos.stream().filter(p -> "pendiente".equals(p.getEstadoEfectivo())).toList();
                case "vencido"   -> todos.stream().filter(p -> "vencido".equals(p.getEstadoEfectivo())).toList();
                case "proximo"   -> todos.stream().filter(p -> "proximo".equals(p.getEstadoEfectivo())).toList();
                case "pagado"    -> todos.stream().filter(p -> "pagado".equals(p.getEstado())).toList();
                default          -> todos;
            };
            datosPagos.setAll(filtrados);
            actualizarResumen(todos);
        } catch (Exception e) { mostrarError(e); }
    }

    private void actualizarResumen(List<PagoMaterial> todos) {
        double totalPend = todos.stream()
            .filter(p -> !"pagado".equals(p.getEstado()))
            .mapToDouble(PagoMaterial::getImporteTotal).sum();

        long countVenc = todos.stream()
            .filter(p -> "vencido".equals(p.getEstadoEfectivo())).count();
        double sumVenc = todos.stream()
            .filter(p -> "vencido".equals(p.getEstadoEfectivo()))
            .mapToDouble(PagoMaterial::getImporteTotal).sum();

        long countProx = todos.stream()
            .filter(p -> "proximo".equals(p.getEstadoEfectivo())).count();

        lblTotalPendiente.setText(String.format("%.2f €", totalPend));
        lblVencidos.setText(countVenc + (countVenc == 1 ? " pago" : " pagos") +
            (countVenc > 0 ? String.format("  (%.0f €)", sumVenc) : ""));
        lblProximos.setText(countProx + (countProx == 1 ? " pago" : " pagos"));
    }

    // ── Acciones sobre pagos ──────────────────────────────────────────────────

    private void nuevoPago() {
        dialogoPago(new PagoMaterial()).ifPresent(p -> {
            try { pagoDao.save(p); cargarPagos(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editarPago() {
        PagoMaterial sel = tablaPagos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago para editar."); return; }
        dialogoPago(sel).ifPresent(p -> {
            try { pagoDao.save(p); cargarPagos(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void marcarPagado() {
        PagoMaterial sel = tablaPagos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago para marcarlo como pagado."); return; }
        if ("pagado".equals(sel.getEstado())) { alerta("Este pago ya está marcado como pagado."); return; }

        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Marcar como pagado");
        dlg.setHeaderText(sel.getMaterialNombre() + " — " + String.format("%.2f €", sel.getImporteTotal()));

        DatePicker dp = new DatePicker(LocalDate.now());
        VBox content = new VBox(8, new Label("Fecha en que se realizó el pago:"), dp);
        content.setPadding(new Insets(14));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("✓ Confirmar pago");

        if (getScene() != null)
            dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        dlg.setResultConverter(bt -> bt == ButtonType.OK ? dp.getValue() : null);
        dlg.showAndWait().ifPresent(fecha -> {
            try { pagoDao.marcarPagado(sel.getId(), fecha); cargarPagos(); }
            catch (Exception e) { mostrarError(e); }
        });
    }

    private void eliminarPago() {
        PagoMaterial sel = tablaPagos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago para eliminar."); return; }
        conf("¿Eliminar el pago de \"" + sel.getMaterialNombre() + "\" (" +
            String.format("%.2f €", sel.getImporteTotal()) + ")?",
            () -> { try { pagoDao.delete(sel.getId()); cargarPagos(); } catch (Exception e) { mostrarError(e); } });
    }

    // ── Diálogo crear / editar pago ───────────────────────────────────────────

    private Optional<PagoMaterial> dialogoPago(PagoMaterial p) {
        Dialog<PagoMaterial> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? "Nueva compra de material" : "Editar pago");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(520);

        // ── Material ──────────────────────────────────────────────────────────
        List<Material> materiales;
        try { materiales = dao.findAll(); } catch (Exception ex) { materiales = List.of(); }
        ComboBox<Material> cbMaterial = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMaterial.setConverter(new StringConverter<>() {
            @Override public String toString(Material m) { return m == null ? "" : m.getNombre() + " (" + m.getUnidad() + ")"; }
            @Override public Material fromString(String s) { return null; }
        });
        cbMaterial.setMaxWidth(Double.MAX_VALUE);
        if (p.getMaterialId() > 0)
            materiales.stream().filter(m -> m.getId() == p.getMaterialId()).findFirst().ifPresent(cbMaterial::setValue);

        // ── Campos de texto ───────────────────────────────────────────────────
        TextField tfProveedor = tf(p.getProveedor());
        TextField tfNumFact   = tf(p.getNumeroFactura());
        TextField tfCantidad  = tf(p.getCantidadComprada() > 0 ? String.valueOf(p.getCantidadComprada()) : "");
        TextField tfImporte   = tf(p.getImporteTotal() > 0 ? String.format("%.2f", p.getImporteTotal()) : "");
        TextArea  taNotas     = new TextArea(p.getNotas() != null ? p.getNotas() : "");
        taNotas.setPrefRowCount(2);
        taNotas.setWrapText(true);

        // ── Fechas ────────────────────────────────────────────────────────────
        DatePicker dpCompra     = new DatePicker(p.getFechaCompra() != null ? p.getFechaCompra() : LocalDate.now());
        DatePicker dpVencimiento = new DatePicker(p.getFechaVencimiento());
        dpCompra.setMaxWidth(Double.MAX_VALUE);
        dpVencimiento.setMaxWidth(Double.MAX_VALUE);

        // ── Forma de pago ─────────────────────────────────────────────────────
        ComboBox<String> cbFormaPago = new ComboBox<>(FXCollections.observableArrayList(FORMAS_PAGO));
        cbFormaPago.setValue(p.getFormaPago() != null ? p.getFormaPago() : "30 días");
        cbFormaPago.setMaxWidth(Double.MAX_VALUE);

        // Auto-calcular vencimiento según forma de pago
        Runnable calcVencimiento = () -> {
            LocalDate compra = dpCompra.getValue();
            if (compra == null) return;
            dpVencimiento.setValue(switch (cbFormaPago.getValue()) {
                case "Contado"   -> compra;
                case "15 días"   -> compra.plusDays(15);
                case "30 días"   -> compra.plusDays(30);
                case "45 días"   -> compra.plusDays(45);
                case "60 días"   -> compra.plusDays(60);
                case "90 días"   -> compra.plusDays(90);
                case "120 días"  -> compra.plusDays(120);
                default          -> dpVencimiento.getValue(); // Personalizado: no cambiar
            });
        };
        cbFormaPago.setOnAction(e -> calcVencimiento.run());
        dpCompra.setOnAction(e -> calcVencimiento.run());

        // Auto-rellenar proveedor al seleccionar material
        cbMaterial.setOnAction(e -> {
            if (cbMaterial.getValue() != null && tfProveedor.getText().isBlank())
                tfProveedor.setText(cbMaterial.getValue().getProveedor() != null
                    ? cbMaterial.getValue().getProveedor() : "");
        });

        // ── Formulario ────────────────────────────────────────────────────────
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
        grid.addRow(r++, lbl("Material *"), cbMaterial);
        grid.addRow(r++, lbl("Proveedor"),   tfProveedor);
        grid.addRow(r++, lbl("Nº factura"),  tfNumFact);
        grid.addRow(r++, lbl("Fecha compra *"), dpCompra);
        grid.addRow(r++, lbl("Forma de pago"), cbFormaPago);
        grid.addRow(r++, lbl("Vencimiento *"), dpVencimiento);
        grid.addRow(r++, lbl("Cantidad comprada"), tfCantidad);
        grid.addRow(r++, lbl("Importe total (€) *"), tfImporte);
        grid.addRow(r,   lbl("Notas"), taNotas);

        dlg.getDialogPane().setContent(grid);

        // Validación
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbMaterial.getValue() == null) { alerta("Selecciona un material."); ev.consume(); return; }
            if (dpVencimiento.getValue() == null) { alerta("Introduce la fecha de vencimiento."); ev.consume(); return; }
            if (parseDouble(tfImporte.getText()) <= 0) { alerta("El importe total debe ser mayor que 0."); ev.consume(); }
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

    // ═════════════════════════════════════════════════════════════════════════
    // IMPORTAR
    // ═════════════════════════════════════════════════════════════════════════

    private void importar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar materiales");
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
                        Material.IMPORT_SPEC, parsed.headers, preview);
                    if (getScene() != null)
                        dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    dlg.showAndWait().ifPresent(mr ->
                        Thread.ofVirtual().start(() -> {
                            try {
                                var result = new EntityImportService().importar(
                                    Material.IMPORT_SPEC, parsed.rows, mr.mapping(), mr.policy());
                                Platform.runLater(() -> {
                                    cargar(); cargarConsumo(); cargarPagos();
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

    // ═════════════════════════════════════════════════════════════════════════
    // EXPORTAR
    // ═════════════════════════════════════════════════════════════════════════

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de materiales como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de materiales, consumo y pagos.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todos los materiales (con consumo y pagos) en formato JSON.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de materiales con estado de stock en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de materiales en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar materiales");
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
        fc.setInitialFileName("Materiales_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Materiales", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Material> selExp = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarMaterialesCSV(destino);
                    case "sql"    -> ExportService.exportarMaterialesSQL(destino);
                    case "json"   -> ExportService.exportarMaterialesJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Material m = selExp.get(0);
                            Path pdf = new PDFService().generarFichaMaterial(m);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarMaterialesPDF(destino, dao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            ExportService.exportarMaterialDetalladoWord(destino, selExp.get(0));
                        } else {
                            ExportService.exportarMaterialesWord(destino, dao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarMaterialesExcel(destino, dao.findAll());
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

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS COMUNES
    // ═════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Material, T> col(String t, String campo, double ancho) {
        TableColumn<Material, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        c.setUserData(toDbColumn(campo));
        return c;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<ConsumoMaterial, T> colConsumo(String t, String campo, double ancho) {
        TableColumn<ConsumoMaterial, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        return c;
    }

    private void previsualizar() {
        List<Material> sel = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Material> lista = sel.isEmpty() ? new java.util.ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Material m = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaMaterial(m);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Material " + m.getNombre();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarMateriales(lista);
                    tituloVentana = "Previsualización — Materiales (" + lista.size() + " registro(s))";
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
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:4;");
        b.setOnAction(e -> r.run());
        return b;
    }

    private void conf(String mensaje, Runnable accion) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText(null);
        dlg.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> accion.run());
    }

    private TextField tf(String v)    { return new TextField(v != null ? v : ""); }
    private Label     lbl(String t)   { return new Label(t); }
    private String toDbColumn(String campo) {
        return switch (campo) {
            case "stockActual" -> "stock_actual";
            case "stockMinimo" -> "stock_minimo";
            case "precioUnidad" -> "precio_unidad";
            default -> campo;
        };
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return 0; }
    }
    private void alerta(String m)     { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) {
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait();
    }
}
