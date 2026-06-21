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
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.ConsumoMaterial;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.PagoMaterial;
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
import java.sql.Connection;
import java.sql.SQLException;
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
    private final MaterialDAO      dao;
    private final ConsumoMaterialDAO consumoDao;
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
        new DynamicColumnRuntime<>("materiales", t("nav.materiales"), COLUMNAS_BASE, tabla, datos, Material::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private CheckBox chkSoloAlerta;
    private ComboBox<String> cbCategoriaFiltro;
    private boolean updatingCategoriaFiltro;
    private TextField txtBuscar;
    private Label lblContador = new Label();

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
        try {
            Connection conn = DatabaseManager.getConnection();
            consumoDao = new ConsumoMaterialDAO(conn);
            dao = new MaterialDAO(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label(t("materiales.titulo"));
        titulo.getStyleClass().add("view-title");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox stockBox = new VBox(12, buildToolbarStock(), buildTablaStock());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox.setVgrow(stockBox, Priority.ALWAYS);

        tabs.getTabs().addAll(
            new Tab(t("materiales.tab.stock"),   stockBox),
            new Tab(t("materiales.tab.consumo"), buildTabConsumo()),
            new Tab(t("materiales.tab.pagos"),   buildTabPagos())
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        Label hint = buildBeginnerHint();
        getChildren().addAll(titulo, hint, tabs);

        cargar();
        dynamicColumns.apply();
        cargarConsumo();
        cargarPagos();
    }

    private Label buildBeginnerHint() {
        Label hint = new Label(t("materiales.hint"));
        hint.getStyleClass().add("beginner-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        PreferenceService prefs = PreferenceService.getInstance();
        hint.visibleProperty().bind(prefs.beginnerModeProperty());
        hint.managedProperty().bind(prefs.beginnerModeProperty());
        return hint;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB STOCK
    // ═════════════════════════════════════════════════════════════════════════

    private HBox buildToolbarStock() {
        chkSoloAlerta = new CheckBox(t("materiales.chk.solo_alerta"));
        chkSoloAlerta.setOnAction(e -> cargar());
        cbCategoriaFiltro = new ComboBox<>();
        cbCategoriaFiltro.setPrefWidth(150);
        cbCategoriaFiltro.setTooltip(new Tooltip(t("materiales.filtro.categoria.tip")));
        cbCategoriaFiltro.setOnAction(e -> {
            if (!updatingCategoriaFiltro) cargar();
        });

        txtBuscar = new TextField();
        txtBuscar.setPromptText(t("materiales.buscar.prompt"));
        txtBuscar.setPrefWidth(240);
        txtBuscar.textProperty().addListener((o, a, b) -> cargar());
        Button btnNuevo    = btn(t("materiales.btn.nuevo"),         this::nuevo);
        Button btnEditar   = btn(t("materiales.btn.editar"),        this::editar);
        Button btnBorrar   = btn(t("materiales.btn.borrar"),        this::borrar);
        Button btnEntrada  = btn(t("materiales.btn.entrada"),       this::ajustarEntrada);
        Button btnSalida   = btn(t("materiales.btn.salida"),        this::ajustarSalida);
        Button btnImportar = btn(t("materiales.btn.importar"),      this::importar);
        Button btnExportar   = btn(t("materiales.btn.exportar"),      this::exportar);
        Button btnPreview    = btn(t("materiales.btn.previsualizar"), this::previsualizar);
        Button btnColumnas   = btn(t("materiales.btn.columnas"),      dynamicColumns::configure);
        chkSoloAlerta.setTooltip(new Tooltip(t("materiales.chk.solo_alerta.tip")));
        btnNuevo.setTooltip(new Tooltip(t("materiales.btn.nuevo.tip")));
        btnEditar.setTooltip(new Tooltip(t("materiales.btn.editar.tip")));
        btnBorrar.setTooltip(new Tooltip(t("materiales.btn.borrar.tip")));
        btnEntrada.setTooltip(new Tooltip(t("materiales.btn.entrada.tip")));
        btnSalida.setTooltip(new Tooltip(t("materiales.btn.salida.tip")));
        btnImportar.setTooltip(new Tooltip(t("materiales.btn.importar.tip")));
        btnExportar.setTooltip(new Tooltip(t("materiales.btn.exportar.tip")));
        btnPreview.setTooltip(new Tooltip(t("materiales.btn.previsualizar.tip")));
        btnColumnas.setTooltip(new Tooltip(t("materiales.btn.columnas.tip")));

        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, chkSoloAlerta, cbCategoriaFiltro, txtBuscar, lblContador, sp, btnEntrada, btnSalida, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Material> buildTablaStock() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

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

        TableColumn<Material, Double> colStock = new TableColumn<>(t("materiales.col.stock_actual"));
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

        TableColumn<Material, Double> colMin = new TableColumn<>(t("materiales.col.stock_minimo"));
        colMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colMin.setUserData("stock_minimo");
        colMin.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || getTableRow().getItem() == null) { setText(null); return; }
                setText(v + " " + ((Material) getTableRow().getItem()).getUnidad());
            }
        });

        TableColumn<Material, Double> colPrecio = new TableColumn<>(t("materiales.col.precio_unidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnidad"));
        colPrecio.setUserData("precio_unidad");
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(colEstado,
            col(t("materiales.col.nombre"), "nombre", 200), col(t("materiales.col.referencia"), "referencia", 100),
            col(t("materiales.col.categoria"), "categoria", 100), colStock, colMin, colPrecio,
            col(t("materiales.col.proveedor"), "proveedor", 140));
        tabla.setPlaceholder(Icons.emptyState(t("materiales.tabla.vacia")));
        return tabla;
    }

    private void cargar() {
        try {
            List<Material> lista = chkSoloAlerta != null && chkSoloAlerta.isSelected()
                ? dao.findBajoStock() : dao.findAll();
            actualizarFiltroCategorias(lista);
            if (cbCategoriaFiltro != null && cbCategoriaFiltro.getValue() != null
                    && !t("materiales.filtro.todos").equals(cbCategoriaFiltro.getValue())) {
                String categoria = cbCategoriaFiltro.getValue();
                lista = lista.stream()
                    .filter(m -> categoria.equals(m.getCategoria()))
                    .toList();
            }
            String q = txtBuscar != null ? txtBuscar.getText().strip().toLowerCase() : "";
            if (!q.isBlank()) lista = lista.stream()
                .filter(m -> contiene(m.getNombre(), q) || contiene(m.getReferencia(), q) || contiene(m.getProveedor(), q))
                .toList();
            datos.setAll(lista);
            lblContador.setText(tf("materiales.contador", lista.size()));
            dynamicColumns.apply();
            TableColumnSizing.animarFilas(tabla);
        } catch (Exception e) { mostrarError(e); }
    }

    private void actualizarFiltroCategorias(List<Material> materiales) {
        if (cbCategoriaFiltro == null) return;
        String selected = cbCategoriaFiltro.getValue();
        List<String> categorias = new java.util.ArrayList<>(materiales.stream()
            .map(Material::getCategoria)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList());
        categorias.add(0, t("materiales.filtro.todos"));
        updatingCategoriaFiltro = true;
        cbCategoriaFiltro.setItems(FXCollections.observableArrayList(categorias));
        cbCategoriaFiltro.setValue(categorias.contains(selected) ? selected : t("materiales.filtro.todos"));
        updatingCategoriaFiltro = false;
    }

    private void nuevo()   { dialogo(new Material()).ifPresent(m -> { try { dao.save(m); dynamicColumns.saveFormFields(m, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); } }); }
    private void editar()  {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("materiales.alerta.sin_seleccion_editar")); return; }
        dialogo(sel).ifPresent(m -> { try { dao.save(m); dynamicColumns.saveFormFields(m, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); } });
    }
    private void borrar() {
        List<Material> seleccionados = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta(t("materiales.alerta.sin_seleccion_borrar")); return; }
        String mensaje = seleccionados.size() == 1
            ? tf("materiales.borrar.confirmar.uno", seleccionados.get(0).getNombre())
            : tf("materiales.borrar.confirmar.varios", seleccionados.size());
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
        if (sel == null) { alerta(t("materiales.alerta.sin_seleccion")); return; }
        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle(tipo.equals("entrada") ? t("materiales.entrada.titulo") : t("materiales.salida.titulo"));
        dlg.setHeaderText(tf("materiales.ajuste.header", sel.getNombre(), sel.getStockActual(), sel.getUnidad()));
        dlg.setContentText(tf("materiales.ajuste.cantidad", sel.getUnidad()));
        dlg.showAndWait().ifPresent(s -> {
            try {
                double cantidad = Double.parseDouble(s.replace(",", "."));
                dao.ajustarStock(sel.getId(), cantidad, tipo,
                    tipo.equals("entrada") ? "Entrada manual" : "Salida manual");
                cargar();
            } catch (NumberFormatException ex) { alerta(t("materiales.alerta.cantidad_invalida")); }
            catch (Exception ex) { mostrarError(ex); }
        });
    }

    private Optional<Material> dialogo(Material m) {
        Dialog<Material> dlg = new Dialog<>();
        dlg.setTitle(m.getId() == 0 ? t("materiales.dialogo.nuevo") : tf("materiales.dialogo.editar", m.getNombre()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        TextField fNombre    = txf(m.getNombre());
        TextField fRef       = txf(m.getReferencia());
        ComboBox<String> fCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        fCat.setValue(m.getCategoria() != null ? m.getCategoria() : CATEGORIAS[0]);
        TextField fStock     = txf(m.getStockActual() > 0 ? String.valueOf(m.getStockActual()) : "0");
        TextField fStockMin  = txf(m.getStockMinimo() > 0 ? String.valueOf(m.getStockMinimo()) : "0");
        TextField fUnidad    = txf(m.getUnidad() != null ? m.getUnidad() : "ud");
        TextField fPrecio    = txf(m.getPrecioUnidad() > 0 ? String.valueOf(m.getPrecioUnidad()) : "0");
        TextField fProveedor = txf(m.getProveedor());
        grid.addRow(0, lbl(t("materiales.campo.nombre")), fNombre, lbl(t("materiales.campo.referencia")), fRef);
        grid.addRow(1, lbl(t("materiales.campo.categoria")), fCat, lbl(t("materiales.campo.unidad")), fUnidad);
        grid.addRow(2, lbl(t("materiales.campo.stock_actual")), fStock, lbl(t("materiales.campo.stock_minimo")), fStockMin);
        grid.addRow(3, lbl(t("materiales.campo.precio_unidad")), fPrecio, lbl(t("materiales.campo.proveedor")), fProveedor);
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
        Label info = new Label(t("materiales.consumo.info"));
        info.setStyle("-fx-text-fill:#666;-fx-font-size:12;");
        info.setWrapText(true);
        tablaConsumo.getStyleClass().add("data-table");
        tablaConsumo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ConsumoMaterial, Double> colCant = new TableColumn<>(t("materiales.consumo.col.cantidad"));
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
            colConsumo(t("materiales.consumo.col.tecnica"), "tecnica", 150), colConsumo(t("materiales.consumo.col.material"), "materialNombre", 220),
            colCant, colConsumo(t("materiales.consumo.col.unidad"), "unidad", 80));
        tablaConsumo.setPlaceholder(new Label(t("materiales.consumo.vacia")));
        VBox.setVgrow(tablaConsumo, Priority.ALWAYS);
        Button btnAdd  = btn(t("materiales.consumo.btn.anadir"),  this::nuevaRegla);
        Button btnEdit = btn(t("materiales.btn.editar"),          this::editarRegla);
        Button btnDel  = btn(t("materiales.consumo.btn.eliminar"), this::eliminarRegla);
        box.getChildren().addAll(info, tablaConsumo, new HBox(8, btnAdd, btnEdit, btnDel));
        return box;
    }

    private void cargarConsumo() {
        try { datosConsumo.setAll(consumoDao.findAll()); } catch (Exception e) { mostrarError(e); }
    }
    private void nuevaRegla() { dialogoConsumo(new ConsumoMaterial()).ifPresent(c -> { try { consumoDao.save(c); cargarConsumo(); } catch (Exception e) { mostrarError(e); } }); }
    private void editarRegla() {
        ConsumoMaterial sel = tablaConsumo.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("materiales.consumo.alerta.editar")); return; }
        dialogoConsumo(sel).ifPresent(c -> { try { consumoDao.save(c); cargarConsumo(); } catch (Exception e) { mostrarError(e); } });
    }
    private void eliminarRegla() {
        ConsumoMaterial sel = tablaConsumo.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("materiales.consumo.alerta.seleccion")); return; }
        conf(tf("materiales.consumo.eliminar.confirmar", sel.getTecnica(), sel.getMaterialNombre()),
            () -> { try { consumoDao.delete(sel.getId()); cargarConsumo(); } catch (Exception e) { mostrarError(e); } });
    }
    private Optional<ConsumoMaterial> dialogoConsumo(ConsumoMaterial c) {
        Dialog<ConsumoMaterial> dlg = new Dialog<>();
        dlg.setTitle(c.getId() == 0 ? t("materiales.consumo.dialogo.nuevo") : t("materiales.consumo.dialogo.editar"));
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
        TextField fCantidad = txf(c.getCantidadPorUnidad() > 0 ? String.valueOf(c.getCantidadPorUnidad()) : "");
        grid.addRow(0, lbl(t("materiales.consumo.campo.tecnica")), fTecnica);
        grid.addRow(1, lbl(t("materiales.consumo.campo.material")), fMaterial);
        grid.addRow(2, lbl(t("materiales.consumo.campo.cantidad")), fCantidad);
        GridPane.setHgrow(fTecnica, Priority.ALWAYS);
        GridPane.setHgrow(fMaterial, Priority.ALWAYS);
        GridPane.setHgrow(fCantidad, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fMaterial.getValue() == null) { alerta(t("materiales.alerta.sin_seleccion")); event.consume(); }
            else if (parseDouble(fCantidad.getText()) <= 0) { alerta(t("materiales.consumo.alerta.cantidad_invalida")); event.consume(); }
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

        VBox cardPendiente = tarjetaResumen(t("materiales.pagos.card.pendiente"), lblTotalPendiente, "#F39C12");
        VBox cardVencido   = tarjetaResumen(t("materiales.pagos.card.vencidos"),  lblVencidos,       "#E74C3C");
        VBox cardProximo   = tarjetaResumen(t("materiales.pagos.card.proximos"),  lblProximos,       "#E67E22");

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
        ToggleButton bTodos     = filtroBtn(t("materiales.pagos.filtro.todos"),      "todos",     tg);
        ToggleButton bPendiente = filtroBtn(t("materiales.pagos.filtro.pendientes"), "pendiente", tg);
        ToggleButton bVencido   = filtroBtn(t("materiales.pagos.filtro.vencidos"),   "vencido",   tg);
        ToggleButton bProximo   = filtroBtn(t("materiales.pagos.filtro.proximos"),   "proximo",   tg);
        ToggleButton bPagado    = filtroBtn(t("materiales.pagos.filtro.pagados"),    "pagado",    tg);
        bTodos.setSelected(true);

        HBox filtros = new HBox(2, bTodos, bPendiente, bVencido, bProximo, bPagado);
        filtros.setStyle("-fx-background-color:-c-tab-bg;-fx-background-radius:5;-fx-padding:3;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNuevo   = btn(t("materiales.pagos.btn.nueva"),          this::nuevoPago);
        Button btnPagado  = btn(t("materiales.pagos.btn.marcar_pagado"), this::marcarPagado);
        Button btnEditar  = btn(t("materiales.btn.editar"),               this::editarPago);
        Button btnBorrar  = btn(t("materiales.pagos.btn.eliminar"),       this::eliminarPago);

        HBox bar = new HBox(10, filtros, sp, btnNuevo, btnPagado, btnEditar, btnBorrar);
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
                    case "pagado"   -> { dot.setFill(Color.web("#27AE60")); tip.setText(t("materiales.pagos.estado.pagado")); }
                    case "vencido"  -> { dot.setFill(Color.web("#E74C3C")); tip.setText(t("materiales.pagos.estado.vencido")); }
                    case "proximo"  -> { dot.setFill(Color.web("#E67E22")); tip.setText(t("materiales.pagos.estado.proximo")); }
                    default         -> { dot.setFill(Color.web("#4C9BE8")); tip.setText(t("materiales.pagos.estado.pendiente")); }
                }
                Tooltip.install(dot, tip);
                setGraphic(dot);
                setText(null);
            }
        });

        // Columna material
        TableColumn<PagoMaterial, String> colMat = new TableColumn<>(t("materiales.pagos.col.material"));
        colMat.setCellValueFactory(new PropertyValueFactory<>("materialNombre"));
        colMat.setPrefWidth(160);

        // Columna proveedor
        TableColumn<PagoMaterial, String> colProv = new TableColumn<>(t("materiales.pagos.col.proveedor"));
        colProv.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colProv.setPrefWidth(130);

        // Columna nº factura
        TableColumn<PagoMaterial, String> colNum = new TableColumn<>(t("materiales.pagos.col.num_factura"));
        colNum.setCellValueFactory(new PropertyValueFactory<>("numeroFactura"));
        colNum.setPrefWidth(100);

        // Columna fecha compra
        TableColumn<PagoMaterial, LocalDate> colFc = new TableColumn<>(t("materiales.pagos.col.fecha_compra"));
        colFc.setCellValueFactory(new PropertyValueFactory<>("fechaCompra"));
        colFc.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });
        colFc.setPrefWidth(105);

        // Columna importe
        TableColumn<PagoMaterial, Double> colImp = new TableColumn<>(t("materiales.pagos.col.importe"));
        colImp.setCellValueFactory(new PropertyValueFactory<>("importeTotal"));
        colImp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        colImp.setPrefWidth(90);

        // Columna forma de pago
        TableColumn<PagoMaterial, String> colFp = new TableColumn<>(t("materiales.pagos.col.forma_pago"));
        colFp.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colFp.setPrefWidth(90);

        // Columna fecha vencimiento (con color)
        TableColumn<PagoMaterial, LocalDate> colFv = new TableColumn<>(t("materiales.pagos.col.vencimiento"));
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
        TableColumn<PagoMaterial, Void> colDias = new TableColumn<>(t("materiales.pagos.col.dias"));
        colDias.setPrefWidth(70);
        colDias.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                PagoMaterial p = getTableRow().getItem();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"  -> { setText(t("materiales.pagos.dias.pagado")); setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;"); }
                    case "vencido" -> { setText(p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;"); }
                    case "proximo" -> { setText("+" + p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E67E22;-fx-font-weight:bold;"); }
                    default        -> { setText("+" + p.getDiasRestantes() + "d"); setStyle(""); }
                }
            }
        });

        // Columna notas
        TableColumn<PagoMaterial, String> colNotas = new TableColumn<>(t("materiales.pagos.col.notas"));
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        colNotas.setPrefWidth(140);

        tablaPagos.getColumns().addAll(
            colEst, colMat, colProv, colNum, colFc, colImp, colFp, colFv, colDias, colNotas);
        tablaPagos.setPlaceholder(new Label(t("materiales.pagos.tabla.vacia")));

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
        if (sel == null) { alerta(t("materiales.pagos.alerta.editar")); return; }
        dialogoPago(sel).ifPresent(p -> {
            try { pagoDao.save(p); cargarPagos(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void marcarPagado() {
        PagoMaterial sel = tablaPagos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("materiales.pagos.alerta.marcar")); return; }
        if ("pagado".equals(sel.getEstado())) { alerta(t("materiales.pagos.alerta.ya_pagado")); return; }

        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle(t("materiales.pagos.marcar.titulo"));
        dlg.setHeaderText(tf("materiales.pagos.marcar.header", sel.getMaterialNombre(), sel.getImporteTotal()));

        DatePicker dp = new DatePicker(LocalDate.now());
        VBox content = new VBox(8, new Label(t("materiales.pagos.marcar.fecha_label")), dp);
        content.setPadding(new Insets(14));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText(t("materiales.pagos.marcar.confirmar"));

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
        if (sel == null) { alerta(t("materiales.pagos.alerta.eliminar")); return; }
        conf(tf("materiales.pagos.eliminar.confirmar", sel.getMaterialNombre(), sel.getImporteTotal()),
            () -> { try { pagoDao.delete(sel.getId()); cargarPagos(); } catch (Exception e) { mostrarError(e); } });
    }

    // ── Diálogo crear / editar pago ───────────────────────────────────────────

    private Optional<PagoMaterial> dialogoPago(PagoMaterial p) {
        Dialog<PagoMaterial> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? t("materiales.pagos.dialogo.nueva") : t("materiales.pagos.dialogo.editar"));
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
        TextField tfProveedor = txf(p.getProveedor());
        TextField tfNumFact   = txf(p.getNumeroFactura());
        TextField tfCantidad  = txf(p.getCantidadComprada() > 0 ? String.valueOf(p.getCantidadComprada()) : "");
        TextField tfImporte   = txf(p.getImporteTotal() > 0 ? String.format("%.2f", p.getImporteTotal()) : "");
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
        grid.addRow(r++, lbl(t("materiales.pagos.campo.material")), cbMaterial);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.proveedor")),   tfProveedor);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.num_factura")),  tfNumFact);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.fecha_compra")), dpCompra);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.forma_pago")), cbFormaPago);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.vencimiento")), dpVencimiento);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.cantidad")), tfCantidad);
        grid.addRow(r++, lbl(t("materiales.pagos.campo.importe")), tfImporte);
        grid.addRow(r,   lbl(t("materiales.pagos.campo.notas")), taNotas);

        dlg.getDialogPane().setContent(grid);

        // Validación
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbMaterial.getValue() == null) { alerta(t("materiales.alerta.sin_seleccion")); ev.consume(); return; }
            if (dpVencimiento.getValue() == null) { alerta(t("materiales.pagos.alerta.sin_vencimiento")); ev.consume(); return; }
            if (parseDouble(tfImporte.getText()) <= 0) { alerta(t("materiales.pagos.alerta.importe_invalido")); ev.consume(); }
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
        List<String> css = getScene() != null
            ? new java.util.ArrayList<>(getScene().getStylesheets())
            : List.of();
        ModuloWindowManager.abrirEnVentana(
            t("materiales.importar.titulo"),
            () -> new ImportView(ImportService.TipoEntidad.MATERIALES, () -> {
                cargar();
                cargarConsumo();
                cargarPagos();
            }),
            css
        );
    }

    private void mostrarResultadoImportacion(org.gipsybuho.service.importer.ImportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(tf("materiales.importar.completada", r.duracion().toMillis() / 1000.0)).append(System.lineSeparator());
        sb.append(tf("materiales.importar.filas_importadas", r.filasImportadas())).append(System.lineSeparator());
        sb.append(tf("materiales.importar.filas_actualizadas", r.filasActualizadas())).append(System.lineSeparator());
        sb.append(tf("materiales.importar.filas_descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append(t("materiales.importar.errores_header"));
            r.errores().stream().limit(10).forEach(e ->
                sb.append(tf("materiales.importar.error_fila",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle(t("materiales.importar.resultado.titulo"));
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
            {"sqlite", t("export.fmt.sqlite.label"), t("export.fmt.sqlite.desc"), "db"},
            {"csv",    t("export.fmt.csv.label"),    t("materiales.export.csv.desc"), "csv"},
            {"sql",    t("export.fmt.sql.label"),    t("materiales.export.sql.desc"), "sql"},
            {"json",   t("export.fmt.json.label"),   t("materiales.export.json.desc"), "json"},
            {"pdf",    t("export.fmt.pdf.label"),    t("materiales.export.pdf.desc"), "pdf"},
            {"word",   t("export.fmt.word.label"),   t("materiales.export.word.desc"), "docx"},
            {"excel",  t("export.fmt.excel.label"),  t("materiales.export.excel.desc"), "xlsx"}
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
        dlg.setTitle(t("materiales.exportar.titulo"));
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
        fc.setInitialFileName(t("nav.materiales") + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(tf("materiales.export.filtro", fmt[3].toUpperCase()), "*." + fmt[3]));
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
        if (lista.isEmpty()) { alerta(t("materiales.previsualizar.sin_seleccion")); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Material m = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaMaterial(m);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = tf("materiales.previsualizar.titulo.uno", m.getNombre());
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarMateriales(lista);
                    tituloVentana = tf("materiales.previsualizar.titulo.varios", lista.size());
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
        b.setOnAction(e -> r.run());
        return b;
    }

    private void conf(String mensaje, Runnable accion) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        dlg.setHeaderText(null);
        dlg.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> accion.run());
    }

    private TextField txf(String v)   { return new TextField(v != null ? v : ""); }
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
    private boolean contiene(String texto, String q) { return texto != null && texto.toLowerCase().contains(q); }
    private void alerta(String m)     { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : t("common.error.desconocido");
        javafx.stage.Window w = getScene() != null ? getScene().getWindow() : null;
        if (w != null && msg.contains("UNIQUE constraint failed")) {
            ToastService.error(w, t("materiales.error.codigo_duplicado"), "MAT-ERR-1");
        } else {
            new Alert(Alert.AlertType.ERROR, "Error: " + msg, ButtonType.OK).showAndWait();
        }
    }
}
