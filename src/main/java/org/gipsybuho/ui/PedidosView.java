package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.PagoPedidoDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.PagoPedido;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.service.ExportService;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PedidosView extends VBox {

    private static final String[] FORMAS_PAGO = {
        "Transferencia bancaria", "Efectivo", "Tarjeta", "Cheque", "Pagaré", "Domiciliación"
    };

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final PedidoDAO     pedidoDao  = new PedidoDAO();
    private final PagoPedidoDAO pagoDao    = new PagoPedidoDAO();
    private final ClienteDAO    clienteDao = new ClienteDAO();

    // ── Tab Pedidos ───────────────────────────────────────────────────────────
    private final ObservableList<Pedido>     datosPedidos     = FXCollections.observableArrayList();
    private final TableView<Pedido>          tablaPedidos     = new TableView<>(datosPedidos);
    private String filtroPedidos = "todos";
    private TextField txtBusqueda;

    // Sub-panel pagos del pedido seleccionado
    private final ObservableList<PagoPedido> datosPagosPedido = FXCollections.observableArrayList();
    private final TableView<PagoPedido>      tablaPagosPedido = new TableView<>(datosPagosPedido);
    private Label lblPedidoSeleccionado;

    // ── Tab Control de pagos ──────────────────────────────────────────────────
    private final ObservableList<PagoPedido> datosTodosPagos = FXCollections.observableArrayList();
    private final TableView<PagoPedido>      tablaTodosPagos = new TableView<>(datosTodosPagos);
    private String filtroTodosPagos = "todos";

    // ── Tarjetas de resumen ───────────────────────────────────────────────────
    private final Label lblActivos   = new Label("—");
    private final Label lblTotal     = new Label("—");
    private final Label lblCobrado   = new Label("—");
    private final Label lblPendiente = new Label("—");
    private final Label lblVencidos  = new Label("—");

    // ── Constructor ───────────────────────────────────────────────────────────

    public PedidosView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Gestión de Pedidos");
        titulo.getStyleClass().add("view-title");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("📋  Pedidos",           buildTabPedidos()),
            new Tab("💳  Control de pagos",  buildTabControlPagos())
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(titulo, buildResumen(), tabs);

        cargarPedidos();
        cargarTodosPagos();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESUMEN
    // ═════════════════════════════════════════════════════════════════════════

    private HBox buildResumen() {
        HBox row = new HBox(10);
        row.getChildren().addAll(
            tarjeta("📋  Activos",            lblActivos,   "#4C9BE8"),
            tarjeta("💶  Total facturado",     lblTotal,     "#8E44AD"),
            tarjeta("✅  Cobrado",             lblCobrado,   "#27AE60"),
            tarjeta("⏳  Pendiente de cobro",  lblPendiente, "#F39C12"),
            tarjeta("🔴  Pagos vencidos",      lblVencidos,  "#E74C3C")
        );
        return row;
    }

    private VBox tarjeta(String titulo, Label valor, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("dashboard-card");
        card.setPrefWidth(185);
        card.setStyle("-fx-border-color:" + color + ";-fx-border-width:0 0 0 4;");
        Label tit = new Label(titulo);
        tit.getStyleClass().add("card-title");
        valor.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        card.getChildren().addAll(tit, valor);
        return card;
    }

    private void actualizarResumen(List<Pedido> todos) {
        List<Pedido> activos = todos.stream()
            .filter(p -> !"entregado".equals(p.getEstado()) && !"cancelado".equals(p.getEstado()))
            .toList();
        double totalFact = activos.stream().mapToDouble(Pedido::getImporteTotal).sum();
        double cobrado   = activos.stream().mapToDouble(Pedido::getImportePagado).sum();
        double pendiente = totalFact - cobrado;
        long   vencidos  = activos.stream().mapToLong(p -> p.getPagosVencidos()).sum();

        lblActivos.setText(String.valueOf(activos.size()));
        lblTotal.setText(String.format("%.2f €", totalFact));
        lblCobrado.setText(String.format("%.2f €", cobrado));
        lblPendiente.setText(String.format("%.2f €", pendiente));
        lblVencidos.setText(String.valueOf(vencidos));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB PEDIDOS
    // ═════════════════════════════════════════════════════════════════════════

    private Node buildTabPedidos() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        box.getChildren().add(buildToolbarPedidos());

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.57);
        VBox.setVgrow(split, Priority.ALWAYS);

        // Panel inferior primero para que lblPedidoSeleccionado esté asignado
        // antes de que buildTablaPedidos() cree el listener de selección
        VBox pnlPagos = buildPanelPagosPedido();

        buildTablaPedidos();
        VBox pnlSup = new VBox(tablaPedidos);
        VBox.setVgrow(tablaPedidos, Priority.ALWAYS);

        split.getItems().addAll(pnlSup, pnlPagos);
        box.getChildren().add(split);
        return box;
    }

    private HBox buildToolbarPedidos() {
        ToggleGroup tg = new ToggleGroup();
        ToggleButton bTodos     = filtroBtn("Todos",      tg, () -> { filtroPedidos = "todos";      cargarPedidos(); });
        ToggleButton bPend      = filtroBtn("Pendientes", tg, () -> { filtroPedidos = "pendiente";  cargarPedidos(); });
        ToggleButton bProceso   = filtroBtn("En proceso", tg, () -> { filtroPedidos = "en_proceso"; cargarPedidos(); });
        ToggleButton bListo     = filtroBtn("Listos",     tg, () -> { filtroPedidos = "listo";      cargarPedidos(); });
        ToggleButton bEntregado = filtroBtn("Entregados", tg, () -> { filtroPedidos = "entregado";  cargarPedidos(); });
        bTodos.setSelected(true);

        HBox filtros = new HBox(2, bTodos, bPend, bProceso, bListo, bEntregado);
        filtros.setStyle("-fx-background-color:-c-tab-bg;-fx-background-radius:5;-fx-padding:3;");

        txtBusqueda = new TextField();
        txtBusqueda.setPromptText("🔍  Buscar cliente, nº pedido…");
        txtBusqueda.setPrefWidth(220);
        txtBusqueda.textProperty().addListener((o, a, b) -> cargarPedidos());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNuevo    = btn("+ Nuevo pedido", "#4C9BE8", this::nuevoPedido);
        Button btnEditar   = btn("✏ Editar",        "#F39C12", this::editarPedido);
        Button btnImportar = btn("📥 Importar",     "#27AE60", this::importar);
        Button btnExportar = btn("📤 Exportar",     "#8E44AD", this::exportar);
        Button btnBorrar     = btn("🗑 Eliminar",      "#E74C3C", this::eliminarPedido);
        Button btnPreview    = btn("👁 Previsualizar", "#6B2D5E", this::previsualizar);

        HBox bar = new HBox(10, filtros, txtBusqueda, sp, btnNuevo, btnEditar, btnImportar, btnExportar, btnBorrar, btnPreview);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTablaPedidos() {
        tablaPedidos.getStyleClass().add("data-table");
        tablaPedidos.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tablaPedidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Estado (dot)
        TableColumn<Pedido, Void> colEst = new TableColumn<>("");
        colEst.setPrefWidth(36);
        colEst.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Pedido p = getTableRow().getItem();
                Circle dot = new Circle(7);
                Tooltip tip = new Tooltip(p.getEstadoDisplay());
                dot.setFill(switch (p.getEstado() != null ? p.getEstado() : "") {
                    case "en_proceso" -> Color.web("#F39C12");
                    case "listo"      -> Color.web("#27AE60");
                    case "entregado"  -> Color.web("#8E44AD");
                    case "cancelado"  -> Color.web("#95A5A6");
                    default           -> Color.web("#4C9BE8");
                });
                Tooltip.install(dot, tip);
                setGraphic(dot); setText(null);
            }
        });

        TableColumn<Pedido, String> colNum = colPed("Nº Pedido", "numero", 115);

        TableColumn<Pedido, String> colCli = colPed("Cliente", "clienteNombre", 155);

        TableColumn<Pedido, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.format(FMT));
            }
        });
        colFecha.setPrefWidth(88);

        TableColumn<Pedido, LocalDate> colEntrega = new TableColumn<>("Entrega");
        colEntrega.setCellValueFactory(new PropertyValueFactory<>("fechaEntregaPrevista"));
        colEntrega.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.format(FMT));
                Pedido p = getTableRow() != null ? (Pedido) getTableRow().getItem() : null;
                boolean atrasada = p != null
                    && !"entregado".equals(p.getEstado())
                    && !"cancelado".equals(p.getEstado())
                    && v.isBefore(LocalDate.now());
                setStyle(atrasada ? "-fx-text-fill:#E74C3C;-fx-font-weight:bold;" : "");
            }
        });
        colEntrega.setPrefWidth(88);

        TableColumn<Pedido, String> colDesc = colPed("Descripción", "descripcion", 170);

        TableColumn<Pedido, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("importeTotal"));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        colTotal.setPrefWidth(90);

        TableColumn<Pedido, Double> colCobrado = new TableColumn<>("Cobrado");
        colCobrado.setCellValueFactory(new PropertyValueFactory<>("importePagado"));
        colCobrado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.format("%.2f €", v));
                setStyle(v > 0.001 ? "-fx-text-fill:#27AE60;" : "");
            }
        });
        colCobrado.setPrefWidth(90);

        TableColumn<Pedido, Void> colPend = new TableColumn<>("Pendiente");
        colPend.setPrefWidth(90);
        colPend.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                Pedido p = getTableRow().getItem();
                double pend = p.getImportePendiente();
                setText(String.format("%.2f €", pend));
                setStyle(pend > 0.001 ? "-fx-text-fill:#E74C3C;-fx-font-weight:bold;" : "-fx-text-fill:#27AE60;");
            }
        });

        TableColumn<Pedido, Integer> colVenc = new TableColumn<>("Vencidos");
        colVenc.setCellValueFactory(new PropertyValueFactory<>("pagosVencidos"));
        colVenc.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v > 0 ? v + " pago(s)" : "—");
                setStyle(v > 0 ? "-fx-text-fill:#E74C3C;-fx-font-weight:bold;" : "");
            }
        });
        colVenc.setPrefWidth(82);

        tablaPedidos.getColumns().addAll(
            colEst, colNum, colCli, colFecha, colEntrega, colDesc, colTotal, colCobrado, colPend, colVenc);
        tablaPedidos.setPlaceholder(new Label("No hay pedidos registrados"));

        // Al seleccionar un pedido, cargar sus pagos en el panel inferior
        tablaPedidos.getSelectionModel().selectedItemProperty().addListener((o, a, sel) -> {
            if (sel != null) {
                lblPedidoSeleccionado.setText(
                    "Pagos del pedido: " + sel.getNumero() + "  —  " + sel.getClienteNombre()
                    + "   (Total: " + String.format("%.2f", sel.getImporteTotal())
                    + " €  |  Cobrado: " + String.format("%.2f", sel.getImportePagado())
                    + " €  |  Pendiente: " + String.format("%.2f", sel.getImportePendiente()) + " €)");
                cargarPagosDePedido(sel.getId());
            } else {
                lblPedidoSeleccionado.setText("Selecciona un pedido para ver sus pagos");
                datosPagosPedido.clear();
            }
        });

        tablaPedidos.setRowFactory(tv -> {
            TableRow<Pedido> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) editarPedido(); });
            return row;
        });
    }

    // ── Panel inferior: pagos del pedido seleccionado ─────────────────────────

    private VBox buildPanelPagosPedido() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 4, 4, 4));

        lblPedidoSeleccionado = new Label("Selecciona un pedido para ver sus pagos");
        lblPedidoSeleccionado.setStyle("-fx-font-weight:bold;-fx-font-size:12px;");

        Button btnAnadir     = btn("+ Añadir pago",    "#4C9BE8", this::anadirPago);
        Button btnFraccionar = btn("⚡ Fraccionar",     "#8E44AD", this::fraccionarPago);
        Button btnMarcar     = btn("✓ Marcar cobrado",  "#27AE60", this::marcarPagoPedido);
        Button btnEliminar   = btn("🗑 Eliminar",        "#E74C3C", this::eliminarPagoPedido);

        HBox toolbar = new HBox(8, btnAnadir, btnFraccionar, btnMarcar, btnEliminar);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        buildTablaPagosPedido();
        VBox.setVgrow(tablaPagosPedido, Priority.ALWAYS);

        box.getChildren().addAll(lblPedidoSeleccionado, toolbar, tablaPagosPedido);
        return box;
    }

    private void buildTablaPagosPedido() {
        tablaPagosPedido.getStyleClass().add("data-table");
        tablaPagosPedido.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPagosPedido.setPlaceholder(new Label("Sin pagos registrados para este pedido"));
        tablaPagosPedido.getColumns().addAll(columnasPago(false));
        tablaPagosPedido.setRowFactory(tv -> {
            TableRow<PagoPedido> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) editarPagoPedido(); });
            return row;
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB CONTROL DE PAGOS
    // ═════════════════════════════════════════════════════════════════════════

    private Node buildTabControlPagos() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        box.getChildren().add(buildToolbarTodosPagos());

        tablaTodosPagos.getStyleClass().add("data-table");
        tablaTodosPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaTodosPagos.setPlaceholder(new Label("No hay pagos registrados"));
        tablaTodosPagos.getColumns().addAll(columnasPago(true));
        VBox.setVgrow(tablaTodosPagos, Priority.ALWAYS);
        tablaTodosPagos.setRowFactory(tv -> {
            TableRow<PagoPedido> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) marcarPagoDesdeTabla(tablaTodosPagos); });
            return row;
        });

        Button btnMarcar   = btn("✓ Marcar cobrado", "#27AE60", () -> marcarPagoDesdeTabla(tablaTodosPagos));
        Button btnEliminar = btn("🗑 Eliminar",        "#E74C3C", () -> eliminarPagoDesdeTabla(tablaTodosPagos));
        HBox acciones = new HBox(8, btnMarcar, btnEliminar);

        box.getChildren().addAll(tablaTodosPagos, acciones);
        return box;
    }

    private HBox buildToolbarTodosPagos() {
        ToggleGroup tg = new ToggleGroup();
        ToggleButton bTodos   = filtroBtn("Todos",       tg, () -> { filtroTodosPagos = "todos";     cargarTodosPagos(); });
        ToggleButton bPend    = filtroBtn("Pendientes",  tg, () -> { filtroTodosPagos = "pendiente"; cargarTodosPagos(); });
        ToggleButton bVenc    = filtroBtn("Vencidos",    tg, () -> { filtroTodosPagos = "vencido";   cargarTodosPagos(); });
        ToggleButton bProx    = filtroBtn("Próximos",    tg, () -> { filtroTodosPagos = "proximo";   cargarTodosPagos(); });
        ToggleButton bPagado  = filtroBtn("Cobrados",    tg, () -> { filtroTodosPagos = "pagado";    cargarTodosPagos(); });
        bTodos.setSelected(true);

        HBox filtros = new HBox(2, bTodos, bPend, bVenc, bProx, bPagado);
        filtros.setStyle("-fx-background-color:-c-tab-bg;-fx-background-radius:5;-fx-padding:3;");

        Region spPagos = new Region(); HBox.setHgrow(spPagos, Priority.ALWAYS);
        HBox bar = new HBox(10, filtros, spPagos);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ── Columnas compartidas para ambas tablas de pagos ───────────────────────

    private List<TableColumn<PagoPedido, ?>> columnasPago(boolean conPedidoCliente) {
        List<TableColumn<PagoPedido, ?>> cols = new ArrayList<>();

        TableColumn<PagoPedido, Void> colEst = new TableColumn<>("");
        colEst.setPrefWidth(36);
        colEst.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                PagoPedido p = getTableRow().getItem();
                Circle dot = new Circle(7);
                Tooltip tip = new Tooltip();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"  -> { dot.setFill(Color.web("#27AE60")); tip.setText("Cobrado"); }
                    case "vencido" -> { dot.setFill(Color.web("#E74C3C")); tip.setText("Vencido"); }
                    case "proximo" -> { dot.setFill(Color.web("#E67E22")); tip.setText("Próximo a vencer"); }
                    default        -> { dot.setFill(Color.web("#4C9BE8")); tip.setText("Pendiente"); }
                }
                Tooltip.install(dot, tip);
                setGraphic(dot); setText(null);
            }
        });
        cols.add(colEst);

        if (conPedidoCliente) {
            TableColumn<PagoPedido, String> colPed = colPago("Pedido",  "pedidoNumero",  110);
            TableColumn<PagoPedido, String> colCli = colPago("Cliente", "clienteNombre", 140);
            cols.add(colPed);
            cols.add(colCli);
        }

        cols.add(colPago("Concepto", "concepto", 145));

        TableColumn<PagoPedido, Double> colImp = new TableColumn<>("Importe");
        colImp.setCellValueFactory(new PropertyValueFactory<>("importe"));
        colImp.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });
        colImp.setPrefWidth(88);
        cols.add(colImp);

        TableColumn<PagoPedido, LocalDate> colFv = new TableColumn<>("Vencimiento");
        colFv.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));
        colFv.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(LocalDate v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                setText(v.format(FMT));
                PagoPedido p = getTableRow().getItem();
                setStyle(switch (p.getEstadoEfectivo()) {
                    case "vencido" -> "-fx-text-fill:#E74C3C;-fx-font-weight:bold;";
                    case "proximo" -> "-fx-text-fill:#E67E22;-fx-font-weight:bold;";
                    case "pagado"  -> "-fx-text-fill:#27AE60;";
                    default -> "";
                });
            }
        });
        colFv.setPrefWidth(100);
        cols.add(colFv);

        TableColumn<PagoPedido, Void> colDias = new TableColumn<>("Días");
        colDias.setPrefWidth(70);
        colDias.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); setStyle(""); return; }
                PagoPedido p = getTableRow().getItem();
                switch (p.getEstadoEfectivo()) {
                    case "pagado"  -> { setText("COBRADO"); setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;"); }
                    case "vencido" -> { setText(p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;"); }
                    case "proximo" -> { setText("+" + p.getDiasRestantes() + "d"); setStyle("-fx-text-fill:#E67E22;-fx-font-weight:bold;"); }
                    default        -> { setText("+" + p.getDiasRestantes() + "d"); setStyle(""); }
                }
            }
        });
        cols.add(colDias);

        cols.add(colPago("Forma pago", "formaPago", 130));
        cols.add(colPago("Notas", "notas", 120));

        return cols;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CARGA DE DATOS
    // ═════════════════════════════════════════════════════════════════════════

    private void cargarPedidos() {
        try {
            List<Pedido> todos = pedidoDao.findAll();
            actualizarResumen(todos);

            List<Pedido> resultado = todos;
            if (!"todos".equals(filtroPedidos)) {
                resultado = resultado.stream()
                    .filter(p -> filtroPedidos.equals(p.getEstado()))
                    .toList();
            }
            String q = txtBusqueda != null ? txtBusqueda.getText().strip().toLowerCase(Locale.ROOT) : "";
            if (!q.isBlank()) {
                final String query = q;
                resultado = resultado.stream()
                    .filter(p -> contiene(p.getClienteNombre(), query)
                              || contiene(p.getNumero(), query)
                              || contiene(p.getDescripcion(), query))
                    .toList();
            }
            datosPedidos.setAll(resultado);
        } catch (Exception e) { mostrarError(e); }
    }

    private void cargarPagosDePedido(int pedidoId) {
        try { datosPagosPedido.setAll(pagoDao.findByPedido(pedidoId)); }
        catch (Exception e) { mostrarError(e); }
    }

    private void cargarTodosPagos() {
        try {
            List<PagoPedido> todos = pagoDao.findAll();
            List<PagoPedido> filtrados = switch (filtroTodosPagos) {
                case "pendiente" -> todos.stream().filter(p -> "pendiente".equals(p.getEstadoEfectivo())).toList();
                case "vencido"   -> todos.stream().filter(p -> "vencido".equals(p.getEstadoEfectivo())).toList();
                case "proximo"   -> todos.stream().filter(p -> "proximo".equals(p.getEstadoEfectivo())).toList();
                case "pagado"    -> todos.stream().filter(p -> "pagado".equals(p.getEstado())).toList();
                default          -> todos;
            };
            datosTodosPagos.setAll(filtrados);
        } catch (Exception e) { mostrarError(e); }
    }

    private boolean contiene(String texto, String q) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(q);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ACCIONES — PEDIDOS
    // ═════════════════════════════════════════════════════════════════════════

    private void nuevoPedido() {
        Pedido nuevo = new Pedido();
        nuevo.setNumero(DatabaseManager.generarNumeroPedido());
        nuevo.setFecha(LocalDate.now());
        nuevo.setEstado("pendiente");
        nuevo.setIvaPorcentaje(21.0);
        dialogoPedido(nuevo).ifPresent(p -> {
            try { pedidoDao.save(p); cargarPedidos(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editarPedido() {
        Pedido sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pedido para editar."); return; }
        dialogoPedido(sel).ifPresent(p -> {
            try { pedidoDao.save(p); cargarPedidos(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void eliminarPedido() {
        List<Pedido> seleccionados = new ArrayList<>(tablaPedidos.getSelectionModel().getSelectedItems());
        if (seleccionados.isEmpty()) { alerta("Selecciona uno o varios pedidos para eliminar."); return; }
        String mensaje = seleccionados.size() == 1
            ? "¿Eliminar el pedido " + seleccionados.get(0).getNumero() + " de " + seleccionados.get(0).getClienteNombre()
                + "?\nSe eliminarán también todos sus pagos."
            : "¿Eliminar " + seleccionados.size() + " pedidos seleccionados?\nSe eliminarán también todos sus pagos.";
        conf(mensaje, () -> {
            try {
                for (Pedido pedido : seleccionados) pedidoDao.delete(pedido.getId());
                datosPagosPedido.clear();
                lblPedidoSeleccionado.setText("Selecciona un pedido para ver sus pagos");
                cargarPedidos();
                cargarTodosPagos();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // IMPORTAR
    // ═════════════════════════════════════════════════════════════════════════

    private void importar() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Funcionalidad próximamente");
        alert.setHeaderText("Importación de Pedidos");
        alert.setContentText(
            "Esta función estará disponible en una próxima versión. " +
            "Mientras tanto, los datos históricos se cargan desde CSV procesados manualmente. " +
            "Para más información consulta MIGRACION_HISTORICO.md.");
        if (getScene() != null)
            alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        alert.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EXPORTAR
    // ═════════════════════════════════════════════════════════════════════════

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de pedidos como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de la tabla pedidos.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todos los pedidos en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de pedidos como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de pedidos en documento Word (.docx), editable.", "docx"},
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
        dlg.setTitle("Exportar pedidos");
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
        fc.setInitialFileName("Pedidos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Pedidos", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Pedido> selExp = new ArrayList<>(tablaPedidos.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarPedidosCSV(destino);
                    case "sql"    -> ExportService.exportarPedidosSQL(destino);
                    case "json"   -> ExportService.exportarPedidosJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Pedido p = pedidoDao.findById(selExp.get(0).getId());
                            Cliente c = clienteDao.findById(p.getClienteId());
                            Path pdf = new PDFService().generarPedido(p, c);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarPedidosPDF(destino, pedidoDao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            Pedido p = pedidoDao.findById(selExp.get(0).getId());
                            Cliente c = clienteDao.findById(p.getClienteId());
                            ExportService.exportarPedidoDetalladoWord(destino, p, c);
                        } else {
                            ExportService.exportarPedidosWord(destino, pedidoDao.findAll());
                        }
                    }
                    case "excel"  -> ExportService.exportarPedidosExcel(destino, pedidoDao.findAll());
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
    // ACCIONES — PAGOS DEL PEDIDO SELECCIONADO
    // ═════════════════════════════════════════════════════════════════════════

    private void anadirPago() {
        Pedido sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona primero un pedido."); return; }
        PagoPedido nuevo = new PagoPedido();
        nuevo.setPedidoId(sel.getId());
        nuevo.setConcepto("Pago " + (datosPagosPedido.size() + 1));
        nuevo.setFechaEmision(LocalDate.now());
        nuevo.setFechaVencimiento(LocalDate.now().plusDays(30));
        nuevo.setFormaPago("Transferencia bancaria");
        nuevo.setEstado("pendiente");
        double restante = sel.getImportePendiente();
        if (restante > 0.001) nuevo.setImporte(restante);
        dialogoPago(nuevo, sel).ifPresent(p -> {
            try { pagoDao.save(p); recargarPagos(sel.getId()); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editarPagoPedido() {
        Pedido selPed = tablaPedidos.getSelectionModel().getSelectedItem();
        PagoPedido sel = tablaPagosPedido.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago para editar."); return; }
        dialogoPago(sel, selPed).ifPresent(p -> {
            try { pagoDao.save(p); recargarPagos(selPed != null ? selPed.getId() : sel.getPedidoId()); }
            catch (Exception e) { mostrarError(e); }
        });
    }

    private void fraccionarPago() {
        Pedido sel = tablaPedidos.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona primero un pedido."); return; }
        dialogoFraccionar(sel).ifPresent(lista -> {
            try {
                for (PagoPedido p : lista) pagoDao.save(p);
                recargarPagos(sel.getId());
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void marcarPagoPedido() { marcarPagoDesdeTabla(tablaPagosPedido); }
    private void eliminarPagoPedido() { eliminarPagoDesdeTabla(tablaPagosPedido); }

    private void recargarPagos(int pedidoId) throws Exception {
        cargarPagosDePedido(pedidoId);
        cargarPedidos();
        cargarTodosPagos();
    }

    private void marcarPagoDesdeTabla(TableView<PagoPedido> tabla) {
        PagoPedido sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago."); return; }
        if ("pagado".equals(sel.getEstado())) { alerta("Este pago ya está marcado como cobrado."); return; }

        Dialog<LocalDate> dlg = new Dialog<>();
        dlg.setTitle("Confirmar cobro");
        dlg.setHeaderText(sel.getConcepto() + "  —  " + String.format("%.2f €", sel.getImporte()));
        DatePicker dp = new DatePicker(LocalDate.now());
        VBox content = new VBox(8, new Label("Fecha en que se recibió el pago:"), dp);
        content.setPadding(new Insets(14));
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("✓ Confirmar cobro");
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? dp.getValue() : null);
        dlg.showAndWait().ifPresent(fecha -> {
            try {
                pagoDao.marcarPagado(sel.getId(), fecha);
                Pedido pedSel = tablaPedidos.getSelectionModel().getSelectedItem();
                if (pedSel != null) cargarPagosDePedido(pedSel.getId());
                cargarPedidos();
                cargarTodosPagos();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private void eliminarPagoDesdeTabla(TableView<PagoPedido> tabla) {
        PagoPedido sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un pago para eliminar."); return; }
        conf("¿Eliminar el pago \"" + sel.getConcepto() + "\" ("
            + String.format("%.2f €", sel.getImporte()) + ")?", () -> {
            try {
                pagoDao.delete(sel.getId());
                Pedido pedSel = tablaPedidos.getSelectionModel().getSelectedItem();
                if (pedSel != null) cargarPagosDePedido(pedSel.getId());
                cargarPedidos();
                cargarTodosPagos();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DIÁLOGOS
    // ═════════════════════════════════════════════════════════════════════════

    private Optional<Pedido> dialogoPedido(Pedido p) {
        Dialog<Pedido> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? "Nuevo pedido" : "Editar pedido");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(520);

        List<Cliente> clientes;
        try { clientes = clienteDao.findAll(); } catch (Exception e) { clientes = List.of(); }

        ComboBox<Cliente> cbCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        cbCliente.setConverter(new StringConverter<>() {
            @Override public String toString(Cliente c)  { return c == null ? "" : c.getNombreCompleto(); }
            @Override public Cliente fromString(String s){ return null; }
        });
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        if (p.getClienteId() > 0)
            clientes.stream().filter(c -> c.getId() == p.getClienteId()).findFirst().ifPresent(cbCliente::setValue);

        TextField  tfNumero   = tf(p.getNumero());
        DatePicker dpFecha    = new DatePicker(p.getFecha() != null ? p.getFecha() : LocalDate.now());
        DatePicker dpEntrega  = new DatePicker(p.getFechaEntregaPrevista());
        DatePicker dpReal     = new DatePicker(p.getFechaEntregaReal());
        for (DatePicker dp : new DatePicker[]{dpFecha, dpEntrega, dpReal}) dp.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbEstado = new ComboBox<>(FXCollections.observableArrayList(
            "pendiente", "en_proceso", "listo", "entregado", "cancelado"));
        cbEstado.setConverter(new StringConverter<>() {
            @Override public String toString(String s)   {
                if (s == null) return "";
                return switch (s) {
                    case "en_proceso" -> "En proceso";
                    case "listo"      -> "Listo";
                    case "entregado"  -> "Entregado";
                    case "cancelado"  -> "Cancelado";
                    default           -> "Pendiente";
                };
            }
            @Override public String fromString(String s) { return null; }
        });
        cbEstado.setValue(p.getEstado() != null ? p.getEstado() : "pendiente");
        cbEstado.setMaxWidth(Double.MAX_VALUE);

        TextField tfDesc    = tf(p.getDescripcion());
        TextField tfImporte = tf(p.getImporteTotal() > 0 ? String.format("%.2f", p.getImporteTotal()) : "");
        TextField tfIVA     = tf(p.getIvaPorcentaje() > 0 ? String.valueOf((int) p.getIvaPorcentaje()) : "21");
        TextArea  taNotas   = new TextArea(p.getNotas() != null ? p.getNotas() : "");
        taNotas.setPrefRowCount(2); taNotas.setWrapText(true);

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, lbl("Cliente *"),            cbCliente);
        grid.addRow(r++, lbl("Nº Pedido *"),           tfNumero);
        grid.addRow(r++, lbl("Fecha pedido"),          dpFecha);
        grid.addRow(r++, lbl("Entrega prevista"),      dpEntrega);
        grid.addRow(r++, lbl("Entrega real"),          dpReal);
        grid.addRow(r++, lbl("Estado"),                cbEstado);
        grid.addRow(r++, lbl("Descripción"),           tfDesc);
        grid.addRow(r++, lbl("Importe total (€) *"),   tfImporte);
        grid.addRow(r++, lbl("IVA (%)"),               tfIVA);
        grid.addRow(r,   lbl("Notas"),                 taNotas);

        dlg.getDialogPane().setContent(grid);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbCliente.getValue() == null)           { alerta("Selecciona un cliente."); ev.consume(); return; }
            if (tfNumero.getText().isBlank())            { alerta("El nº de pedido es obligatorio."); ev.consume(); return; }
            if (parseDouble(tfImporte.getText()) <= 0)  { alerta("El importe total debe ser mayor que 0."); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            p.setClienteId(cbCliente.getValue().getId());
            p.setClienteNombre(cbCliente.getValue().getNombreCompleto());
            p.setNumero(tfNumero.getText().trim());
            p.setFecha(dpFecha.getValue());
            p.setFechaEntregaPrevista(dpEntrega.getValue());
            p.setFechaEntregaReal(dpReal.getValue());
            p.setEstado(cbEstado.getValue());
            p.setDescripcion(tfDesc.getText().trim());
            p.setImporteTotal(parseDouble(tfImporte.getText()));
            p.setIvaPorcentaje(parseDouble(tfIVA.getText()) > 0 ? parseDouble(tfIVA.getText()) : 21.0);
            p.setNotas(taNotas.getText().trim());
            return p;
        });

        return dlg.showAndWait();
    }

    private Optional<PagoPedido> dialogoPago(PagoPedido p, Pedido pedido) {
        Dialog<PagoPedido> dlg = new Dialog<>();
        dlg.setTitle(p.getId() == 0 ? "Añadir pago" : "Editar pago");
        if (pedido != null)
            dlg.setHeaderText("Pedido " + pedido.getNumero() + "  —  " + pedido.getClienteNombre()
                + "  (Pendiente: " + String.format("%.2f €", pedido.getImportePendiente()) + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(460);

        TextField tfConcepto = tf(p.getConcepto() != null ? p.getConcepto() : "");
        TextField tfImporte  = tf(p.getImporte() > 0 ? String.format("%.2f", p.getImporte()) : "");
        DatePicker dpEmision = new DatePicker(p.getFechaEmision() != null ? p.getFechaEmision() : LocalDate.now());
        DatePicker dpVenc    = new DatePicker(p.getFechaVencimiento() != null ? p.getFechaVencimiento() : LocalDate.now().plusDays(30));
        dpEmision.setMaxWidth(Double.MAX_VALUE);
        dpVenc.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbForma = new ComboBox<>(FXCollections.observableArrayList(FORMAS_PAGO));
        cbForma.setValue(p.getFormaPago() != null ? p.getFormaPago() : "Transferencia bancaria");
        cbForma.setMaxWidth(Double.MAX_VALUE);

        TextArea taNotas = new TextArea(p.getNotas() != null ? p.getNotas() : "");
        taNotas.setPrefRowCount(2); taNotas.setWrapText(true);

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, lbl("Concepto *"),    tfConcepto);
        grid.addRow(r++, lbl("Importe (€) *"), tfImporte);
        grid.addRow(r++, lbl("Fecha emisión"), dpEmision);
        grid.addRow(r++, lbl("Vencimiento *"), dpVenc);
        grid.addRow(r++, lbl("Forma de pago"), cbForma);
        grid.addRow(r,   lbl("Notas"),         taNotas);

        dlg.getDialogPane().setContent(grid);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (tfConcepto.getText().isBlank())         { alerta("El concepto es obligatorio."); ev.consume(); return; }
            if (parseDouble(tfImporte.getText()) <= 0)  { alerta("El importe debe ser mayor que 0."); ev.consume(); return; }
            if (dpVenc.getValue() == null)              { alerta("La fecha de vencimiento es obligatoria."); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            p.setConcepto(tfConcepto.getText().trim());
            p.setImporte(parseDouble(tfImporte.getText()));
            p.setFechaEmision(dpEmision.getValue());
            p.setFechaVencimiento(dpVenc.getValue());
            p.setFormaPago(cbForma.getValue());
            p.setNotas(taNotas.getText().trim());
            if (p.getEstado() == null) p.setEstado("pendiente");
            return p;
        });

        return dlg.showAndWait();
    }

    private Optional<List<PagoPedido>> dialogoFraccionar(Pedido pedido) {
        Dialog<List<PagoPedido>> dlg = new Dialog<>();
        dlg.setTitle("Fraccionar pago");
        dlg.setHeaderText("Pedido: " + pedido.getNumero() + "  —  " + pedido.getClienteNombre()
            + "\nTotal: " + String.format("%.2f €", pedido.getImporteTotal())
            + "  |  Pendiente de cobro: " + String.format("%.2f €", pedido.getImportePendiente()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(480);

        TextField tfImporteTotal = tf(String.format("%.2f", pedido.getImportePendiente()));
        tfImporteTotal.setPromptText("Importe a fraccionar");

        Spinner<Integer> spPlazos = new Spinner<>(2, 24, 2);
        spPlazos.setEditable(true);
        spPlazos.setPrefWidth(90);

        DatePicker dpPrimero = new DatePicker(LocalDate.now().plusDays(30));
        dpPrimero.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbPeriodo = new ComboBox<>(FXCollections.observableArrayList(
            "15 días", "30 días", "60 días", "90 días"));
        cbPeriodo.setValue("30 días");
        cbPeriodo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbForma = new ComboBox<>(FXCollections.observableArrayList(FORMAS_PAGO));
        cbForma.setValue("Transferencia bancaria");
        cbForma.setMaxWidth(Double.MAX_VALUE);

        Label lblPreview = new Label();
        lblPreview.setStyle("-fx-text-fill:#666;-fx-font-size:11px;");

        Runnable preview = () -> {
            try {
                int plazos = spPlazos.getValue();
                double total = parseDouble(tfImporteTotal.getText());
                if (total > 0 && plazos > 0)
                    lblPreview.setText(plazos + " pagos de " + String.format("%.2f €", total / plazos) + " cada uno");
            } catch (Exception ignored) {}
        };
        spPlazos.valueProperty().addListener((o, a, b) -> preview.run());
        tfImporteTotal.textProperty().addListener((o, a, b) -> preview.run());
        preview.run();

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, lbl("Importe a fraccionar (€)"), tfImporteTotal);
        grid.addRow(r++, lbl("Número de plazos"),          spPlazos);
        grid.addRow(r++, lbl("Primer vencimiento"),        dpPrimero);
        grid.addRow(r++, lbl("Periodicidad"),              cbPeriodo);
        grid.addRow(r++, lbl("Forma de pago"),             cbForma);
        grid.add(lblPreview, 1, r);

        dlg.getDialogPane().setContent(grid);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (dpPrimero.getValue() == null)            { alerta("Selecciona la fecha del primer vencimiento."); ev.consume(); return; }
            if (parseDouble(tfImporteTotal.getText()) <= 0) { alerta("El importe debe ser mayor que 0."); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            int    plazos     = spPlazos.getValue();
            double total      = parseDouble(tfImporteTotal.getText());
            double porPlazo   = Math.round((total / plazos) * 100.0) / 100.0;
            int    diasPeriodo = switch (cbPeriodo.getValue()) {
                case "15 días" -> 15;
                case "60 días" -> 60;
                case "90 días" -> 90;
                default        -> 30;
            };
            LocalDate venc = dpPrimero.getValue();
            List<PagoPedido> lista = new ArrayList<>();
            for (int i = 1; i <= plazos; i++) {
                PagoPedido pp = new PagoPedido();
                pp.setPedidoId(pedido.getId());
                pp.setConcepto("Plazo " + i + "/" + plazos);
                // Último plazo absorbe el residuo de redondeo
                double imp = (i == plazos) ? Math.round((total - porPlazo * (plazos - 1)) * 100.0) / 100.0 : porPlazo;
                pp.setImporte(imp);
                pp.setFechaEmision(LocalDate.now());
                pp.setFechaVencimiento(venc);
                pp.setFormaPago(cbForma.getValue());
                pp.setEstado("pendiente");
                lista.add(pp);
                venc = venc.plusDays(diasPeriodo);
            }
            return lista;
        });

        return dlg.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Pedido, T> colPed(String titulo, String campo, double ancho) {
        TableColumn<Pedido, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        return c;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<PagoPedido, T> colPago(String titulo, String campo, double ancho) {
        TableColumn<PagoPedido, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        return c;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(150); cc0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS); cc1.setFillWidth(true);
        grid.getColumnConstraints().addAll(cc0, cc1);
        return grid;
    }

    private ToggleButton filtroBtn(String texto, ToggleGroup tg, Runnable onSelect) {
        ToggleButton tb = new ToggleButton(texto);
        tb.setToggleGroup(tg);
        String normal   = "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:5 12;-fx-background-radius:4;-fx-font-size:12px;";
        String selected = "-fx-background-color:-c-primary;-fx-text-fill:white;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:5 12;-fx-background-radius:4;-fx-font-size:12px;";
        tb.setStyle(normal);
        tb.selectedProperty().addListener((o, a, sel) -> {
            tb.setStyle(sel ? selected : normal);
            if (sel) onSelect.run();
        });
        return tb;
    }

    private void previsualizar() {
        List<Pedido> sel = new ArrayList<>(tablaPedidos.getSelectionModel().getSelectedItems());
        List<Pedido> lista = sel.isEmpty() ? new ArrayList<>(datosPedidos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Pedido p = pedidoDao.findById(lista.get(0).getId());
                    Cliente c = clienteDao.findById(p.getClienteId());
                    Path pdfPath = new PDFService().generarPedido(p, c);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Pedido " + p.getNumero();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarPedidos(lista);
                    tituloVentana = "Previsualización — Pedidos (" + lista.size() + " registro(s))";
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
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> accion.run());
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label     lbl(String t){ return new Label(t); }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); } catch (Exception e) { return 0; }
    }

    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) {
        new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait();
    }
}
