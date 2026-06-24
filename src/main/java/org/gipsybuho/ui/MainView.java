package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.stage.Stage;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserPermissions;
import org.gipsybuho.service.AuthService;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.ToastService;
import org.gipsybuho.util.AppConstants;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

public class MainView extends BorderPane {

    private static final Map<String, String> TITULO_A_MODULO = Map.ofEntries(
        Map.entry("nav.inicio",               "general"),
        Map.entry("nav.panel",                "general"),
        Map.entry("nav.clientes",             "clientes"),
        Map.entry("nav.tarifas",              "tarifas"),
        Map.entry("nav.presupuestos",         "presupuestos"),
        Map.entry("nav.pedidos",              "pedidos"),
        Map.entry("nav.albaranes",            "albaranes"),
        Map.entry("nav.facturas",             "facturas"),
        Map.entry("nav.materiales",           "materiales"),
        Map.entry("nav.compras",              "compras"),
        Map.entry("nav.empleados",            "empleados"),
        Map.entry("nav.nominas",              "nominas"),
        Map.entry("nav.estadisticas",         "estadisticas"),
        Map.entry("nav.calendario",           "calendario"),
        Map.entry("asistentes.tab.ia",        "ia"),
        Map.entry("asistentes.tab.visual",    "ia"),
        Map.entry("main.footer.backup",       "backups"),
        Map.entry("main.footer.exportar",     "exportacion"),
        Map.entry("main.footer.configuracion","configuracion"),
        Map.entry("main.footer.usuarios",     "usuarios"),
        Map.entry("nav.ayuda",                "general")
    );

    private final StackPane contentArea = new StackPane();
    private final VisualAssistantView visualAssistant;
    private VBox sidebar;
    private boolean sidebarCollapsed = false;
    private TextField tfBusqueda;
    private Region navPill;
    private StackPane navPillContainer;
    private final User loggedInUser;
    private final AuthService authService;
    private final Stage primaryStage;
    private final Runnable onLogout;
    private String currentModuleId = "general";

    public MainView(Stage stage, User loggedInUser, AuthService authService, Runnable onLogout) {
        this.primaryStage = stage;
        this.loggedInUser = loggedInUser;
        this.authService = authService;
        this.onLogout = onLogout;
        this.visualAssistant = new VisualAssistantView();
        ToastService.setArticleNavigator(id -> mostrarVista(HelpView.forArticle(id), "nav.ayuda"));
        setLeft(buildSidebar());
        setCenter(new StackPane(contentArea, visualAssistant));
        getStyleClass().add("main-view");
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.K) {
                toggleBusqueda();
                e.consume();
            }
            if (e.getCode() == KeyCode.F1) {
                mostrarVista(HelpView.forModule(currentModuleId), "nav.ayuda");
                e.consume();
            }
        });
        if (loggedInUser.hasPermission(UserPermissions.DASHBOARD)) {
            mostrarVista(new DashboardView(), "nav.panel");
        } else {
            mostrarVista(accesoLimitado(), "nav.inicio");
        }
    }

    private VBox buildSidebar() {
        sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        // Logo
        VBox logoBox = new VBox(6);
        logoBox.getStyleClass().add("sidebar-logo");
        logoBox.setAlignment(Pos.CENTER);
        try {
            Image logo = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")));
            ImageView iv = new ImageView(logo);
            iv.setFitWidth(90);
            iv.setPreserveRatio(true);
            logoBox.getChildren().add(iv);
        } catch (Exception ignored) {}
        Label lblEmpresa = new Label("Gráficas Mulberry");
        lblEmpresa.getStyleClass().add("sidebar-empresa");
        logoBox.getChildren().add(lblEmpresa);

        // Toggle colapso del sidebar
        StackPane collapseArrow = Icons.sidebarToggle();
        Button btnCollapse = new Button();
        btnCollapse.setGraphic(collapseArrow);
        btnCollapse.getStyleClass().add("sidebar-collapse-btn");
        Tooltip.install(btnCollapse, Tooltips.of(t("main.sidebar.colapsar")));
        btnCollapse.setOnAction(e -> {
            sidebarCollapsed = !sidebarCollapsed;
            if (sidebarCollapsed) {
                sidebar.getStyleClass().add("sidebar-collapsed");
                if (navPill != null) navPill.setVisible(false);
            } else {
                sidebar.getStyleClass().remove("sidebar-collapsed");
            }
            RotateTransition rt = new RotateTransition(Duration.millis(180), collapseArrow);
            rt.setToAngle(sidebarCollapsed ? 180 : 0);
            rt.play();
        });
        HBox collapseRow = new HBox(new Region(), btnCollapse);
        HBox.setHgrow(collapseRow.getChildren().get(0), Priority.ALWAYS);
        collapseRow.getStyleClass().add("sidebar-collapse-row");
        sidebar.getChildren().addAll(collapseRow, logoBox);

        Label userInfoLabel = new Label(loggedInUser.getUsername()
            + "  ·  " + loggedInUser.getRole().getLabel());
        userInfoLabel.setGraphic(Icons.person());
        userInfoLabel.setGraphicTextGap(8);
        userInfoLabel.getStyleClass().add("sidebar-user-info");
        VBox.setMargin(userInfoLabel, new Insets(10, 0, 0, 10));
        sidebar.getChildren().add(userInfoLabel);

        String lastLoginText = loggedInUser.getLastLogin() == null
            ? t("main.sesion.primera")
            : tf("main.sesion.ultima", loggedInUser.getLastLogin()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        Label lastLoginLabel = new Label(lastLoginText);
        lastLoginLabel.getStyleClass().add("sidebar-version");
        VBox.setMargin(lastLoginLabel, new Insets(2, 0, 0, 12));
        sidebar.getChildren().add(lastLoginLabel);

        // Separador
        Region sep = new Region();
        sep.getStyleClass().add("sidebar-sep");
        sep.setPrefHeight(1);
        sidebar.getChildren().add(sep);

        // Buscador de módulos (Ctrl+K)
        tfBusqueda = new TextField();
        tfBusqueda.setPromptText(t("main.search.prompt"));
        tfBusqueda.getStyleClass().add("sidebar-search");
        tfBusqueda.setTooltip(Tooltips.of(t("main.search.tooltip")));
        tfBusqueda.setVisible(false);
        tfBusqueda.setManaged(false);
        tfBusqueda.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) toggleBusqueda();
        });
        sidebar.getChildren().add(tfBusqueda);

        // ── Botones de navegación dentro de ScrollPane ───────────────────
        VBox navMenu = new VBox();
        addIfPermiso(navMenu, UserPermissions.DASHBOARD,
            navBtn(Icons.home(), "nav.inicio", DashboardView::new));
        navMenu.getChildren().addAll(
            navGrupo(t("nav.grupo.clientes"),
                navBtn(UserPermissions.CLIENTES, Icons.users(), "nav.clientes", ClientesView::new),
                navBtn(UserPermissions.TARIFAS,  Icons.tag(),   "nav.tarifas",  TarifasView::new)
            ),
            navGrupo(t("nav.grupo.comercial"),
                navBtn(UserPermissions.MATERIALES,   Icons.layers(),      "nav.materiales",   MaterialesView::new),
                navBtn(UserPermissions.COMPRAS,      Icons.shoppingBag(), "nav.compras",      ComprasProveedorView::new),
                navBtn(UserPermissions.PEDIDOS,      Icons.cart(),        "nav.pedidos",      PedidosView::new),
                navBtn(UserPermissions.PRESUPUESTOS, Icons.assignment(),  "nav.presupuestos", PresupuestosView::new),
                navBtn(UserPermissions.ALBARANES,    Icons.file(),        "nav.albaranes",    AlbaranesView::new),
                navBtn(UserPermissions.FACTURAS,     Icons.receipt(),     "nav.facturas",     FacturasView::new)
            ),
            navGrupo(t("nav.grupo.personal"),
                navBtn(UserPermissions.EMPLEADOS, Icons.person(), "nav.empleados", EmpleadosView::new),
                navBtn(UserPermissions.NOMINAS,   Icons.work(),   "nav.nominas",   NominasView::new)
            ),
            navGrupo(t("nav.grupo.analitica"),
                navBtn(UserPermissions.ESTADISTICAS, Icons.barChart(), "nav.estadisticas", EstadisticasView::new),
                navBtn(UserPermissions.CALENDARIO,   Icons.calendar(), "nav.calendario",   CalendarioView::new)
            ),
            navGrupo(t("nav.grupo.asistente"),
                navBtn(UserPermissions.IA, Icons.robot(), "asistentes.tab.ia", IAView::new),
                navBtn(UserPermissions.IA, Icons.settings(), "asistentes.tab.visual", this::visualAssistantConfigScroll)
            )
        );

        tfBusqueda.textProperty().addListener((obs, old, q) -> filtrarNav(q, navMenu));

        navPill = new Region();
        navPill.getStyleClass().add("nav-pill");
        navPill.setManaged(false);
        navPill.setMouseTransparent(true);
        navPill.setVisible(false);
        navPill.setPrefHeight(36);

        navPillContainer = new StackPane(navMenu);
        navPillContainer.getChildren().add(0, navPill);
        navPillContainer.setAlignment(Pos.TOP_LEFT);
        navPill.prefWidthProperty().bind(navPillContainer.widthProperty());

        ScrollPane scroll = new ScrollPane(navPillContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.getChildren().add(scroll);

        // ── Footer de sistema ────────────────────────────────────────────
        Region sepFooter = new Region();
        sepFooter.getStyleClass().add("sidebar-sep");
        sepFooter.setPrefHeight(1);
        sidebar.getChildren().add(sepFooter);

        HBox footerIconos = new HBox(4);
        footerIconos.getStyleClass().add("sidebar-footer-icons");
        footerIconos.setAlignment(Pos.CENTER_LEFT);

        if (loggedInUser.hasPermission(UserPermissions.IMPORTAR_BACKUP)) {
            Button btnBackup = buildFooterBtn(Icons.download(), "main.footer.backup",
                () -> mostrarVista(new ImportBackupView(), "main.footer.backup"),
                ImportBackupView::new);
            footerIconos.getChildren().add(btnBackup);
        }
        if (loggedInUser.hasPermission(UserPermissions.EXPORTAR_BACKUP)) {
            Button btnExport = buildFooterBtn(Icons.upload(), "main.footer.exportar",
                () -> mostrarVista(new ExportView(), "main.footer.exportar"),
                ExportView::new);
            footerIconos.getChildren().add(btnExport);
        }
        if (loggedInUser.hasPermission(UserPermissions.CONFIGURACION)) {
            Button btnConfig = buildFooterBtn(Icons.settings(), "main.footer.configuracion",
                () -> mostrarVista(new ConfiguracionView(), "main.footer.configuracion"),
                () -> new ConfiguracionView());
            footerIconos.getChildren().add(btnConfig);
        }
        if (loggedInUser.isAdmin()) {
            Button btnUsers = buildFooterBtn(Icons.users(), "main.footer.usuarios",
                () -> mostrarVista(new UserManagementView(authService, loggedInUser), "main.footer.usuarios"),
                () -> new UserManagementView(authService, loggedInUser));
            footerIconos.getChildren().add(btnUsers);
        }

        Button btnCerrarApp = new Button(t("main.footer.salir"));
        btnCerrarApp.setGraphic(Icons.power());
        btnCerrarApp.setGraphicTextGap(8);
        btnCerrarApp.getStyleClass().add("sidebar-exit-btn");
        btnCerrarApp.setMaxWidth(Double.MAX_VALUE);
        btnCerrarApp.setTooltip(Tooltips.of(t("main.footer.salir.tooltip")));
        btnCerrarApp.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));
        btnCerrarApp.setOnAction(e -> {
            if (confirmarSalida()) {
                Platform.exit();
            }
        });

        Button btnLogout = new Button();
        btnLogout.setGraphic(Icons.logout());
        btnLogout.getStyleClass().add("sidebar-footer-btn");
        Tooltip.install(btnLogout, Tooltips.of(t("main.footer.logout.tooltip")));
        btnLogout.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));
        btnLogout.setOnAction(e -> {
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                t("main.dialogo.logout.pregunta"), ButtonType.YES, ButtonType.NO);
            confirmacion.setTitle(t("main.dialogo.logout.titulo"));
            confirmacion.setHeaderText(null);
            confirmacion.initOwner(primaryStage);
            confirmacion.showAndWait()
                .filter(r -> r == ButtonType.YES)
                .ifPresent(r -> onLogout.run());
        });
        footerIconos.getChildren().add(btnLogout);

        Button btnHelp = buildFooterBtn(Icons.help(), "main.footer.ayuda",
            () -> mostrarVista(HelpView.forModule(currentModuleId), "nav.ayuda"),
            () -> HelpView.forModule(currentModuleId));
        footerIconos.getChildren().add(btnHelp);

        if (!footerIconos.getChildren().isEmpty()) {
            sidebar.getChildren().add(footerIconos);
        }
        sidebar.getChildren().add(btnCerrarApp);

        Label version = new Label(AppConstants.APP_VERSION + " · " + t("main.version.lugar"));
        version.getStyleClass().add("sidebar-version");
        VBox.setMargin(version, new Insets(0, 0, 8, 0));
        sidebar.getChildren().add(version);

        return sidebar;
    }

    private Button buildFooterBtn(javafx.scene.Node icon, String tooltipText, Runnable accion,
                                   Supplier<javafx.scene.Parent> popupFactory) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add("sidebar-footer-btn");
        Tooltip tip = Tooltips.of(t(tooltipText) + t("main.footer.popup.suffix"));
        tip.setStyle("-fx-font-size:11;");
        Tooltip.install(btn, tip);
        btn.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));
        btn.setOnAction(e -> {
            SoundService.play(SoundService.Sound.NAVIGATE);
            accion.run();
        });
        ContextMenu ctx = new ContextMenu();
        MenuItem miVentana = new MenuItem(t("main.ctx.abrir_ventana"));
        miVentana.setStyle("-fx-font-weight: bold;");
        miVentana.setOnAction(e -> {
            SoundService.play(SoundService.Sound.WINDOW_OPEN);
            List<String> css = getScene() != null
                ? new ArrayList<>(getScene().getStylesheets()) : List.of();
            ModuloWindowManager.abrirEnVentana(t(tooltipText), popupFactory, css,
                visualAssistant::instalarAyudaAutomatica);
        });
        ctx.getItems().add(miVentana);
        btn.setOnContextMenuRequested(e -> ctx.show(btn, e.getScreenX(), e.getScreenY()));
        return btn;
    }

    private void actualizarBotonAsistente(Label label) {
        label.setText(visualAssistant.isActivo() ? t("main.asistente.desactivar") : t("main.asistente.activar"));
    }

    private StackPane buildBotonAsistenteVisual() {
        Label label = new Label();
        label.setGraphic(Icons.compass());
        label.setGraphicTextGap(10);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("nav-btn");
        StackPane boton = new StackPane(label);
        boton.getStyleClass().add("nav-btn-pane");
        actualizarBotonAsistente(label);
        visualAssistant.activoProperty().addListener((obs, old, activo) ->
            actualizarBotonAsistente(label));
        boton.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));
        boton.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            SoundService.play(SoundService.Sound.CLICK);
            visualAssistant.setActivo(!visualAssistant.isActivo());
        });
        return boton;
    }

    public boolean confirmarSalida() {
        Alert confirmacion = new Alert(
            Alert.AlertType.CONFIRMATION,
            t("main.dialogo.salir.pregunta"),
            ButtonType.YES,
            ButtonType.NO
        );
        confirmacion.setTitle(t("main.dialogo.salir.titulo"));
        confirmacion.setHeaderText(null);
        if (getScene() != null) {
            confirmacion.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        return confirmacion.showAndWait()
            .filter(ButtonType.YES::equals)
            .isPresent();
    }

    // ── Constructores de botón ────────────────────────────────────────────────

    /**
     * Botón estándar: clic izquierdo crea una nueva instancia del módulo y la
     * muestra en el área central; clic derecho abre el menú contextual con la
     * opción de abrir en ventana emergente (también crea una nueva instancia).
     */
    private StackPane navBtn(Node icon, String texto, Supplier<javafx.scene.Parent> factory) {
        return navBtnImpl(icon, texto, () -> mostrarVista(factory.get(), texto), factory, texto);
    }

    private StackPane navBtn(String permiso, Node icon, String texto, Supplier<javafx.scene.Parent> factory) {
        return loggedInUser.hasPermission(permiso) ? navBtn(icon, texto, factory) : null;
    }

    private void addIfPermiso(VBox parent, String permiso, StackPane node) {
        if (loggedInUser.hasPermission(permiso) && node != null) parent.getChildren().add(node);
    }

    private javafx.scene.Parent visualAssistantConfigScroll() {
        ScrollPane scroll = new ScrollPane(new VisualAssistantConfigView(visualAssistant));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private javafx.scene.Parent accesoLimitado() {
        Label label = new Label(t("main.acceso_limitado"));
        label.getStyleClass().add("view-title");
        javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(label);
        pane.setPadding(new Insets(24));
        return pane;
    }

    /**
     * Botón especial: permite separar la acción de clic izquierdo (p.ej. reusar
     * una instancia existente) de la fábrica usada para crear ventanas emergentes.
     */
    private StackPane navBtnEspecial(Node icon, String texto, Runnable accionPrincipal,
                                     Supplier<javafx.scene.Parent> popupFactory) {
        return navBtnImpl(icon, texto, accionPrincipal, popupFactory, texto);
    }

    private StackPane navBtn(String permiso, Node icon, String texto, Runnable accion,
                             Supplier<javafx.scene.Parent> popupFactory) {
        return loggedInUser.hasPermission(permiso)
            ? navBtnEspecial(icon, texto, accion, popupFactory) : null;
    }

    private StackPane navBtnImpl(Node icon, String texto, Runnable accionPrincipal,
                                  Supplier<javafx.scene.Parent> popupFactory, String titulo) {
        Label lbl = new Label(t(texto));
        if (icon != null) {
            lbl.setGraphic(icon);
            lbl.setGraphicTextGap(10);
        }
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.getStyleClass().add("nav-btn");

        StackPane pane = new StackPane(lbl);
        pane.getStyleClass().add("nav-btn-pane");
        pane.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));

        String tipDesc = switch (texto) {
            case "nav.inicio"       -> t("nav.tooltip.inicio");
            case "nav.clientes"     -> t("nav.tooltip.clientes");
            case "nav.tarifas"      -> t("nav.tooltip.tarifas");
            case "nav.presupuestos" -> t("nav.tooltip.presupuestos");
            case "nav.pedidos"      -> t("nav.tooltip.pedidos");
            case "nav.albaranes"    -> t("nav.tooltip.albaranes");
            case "nav.facturas"     -> t("nav.tooltip.facturas");
            case "nav.materiales"   -> t("nav.tooltip.materiales");
            case "nav.compras"      -> t("nav.tooltip.compras");
            case "nav.empleados"    -> t("nav.tooltip.empleados");
            case "nav.nominas"      -> t("nav.tooltip.nominas");
            case "nav.estadisticas" -> t("nav.tooltip.estadisticas");
            case "nav.calendario"   -> t("nav.tooltip.calendario");
            case "nav.importacion"  -> t("nav.tooltip.importacion");
            case "asistentes.tab.ia" -> t("nav.tooltip.asistente");
            default                 -> t(texto);
        };
        Tooltip tip = Tooltips.of(tipDesc + t("main.footer.popup.suffix"));
        tip.setStyle("-fx-font-size:11;");
        Tooltip.install(pane, tip);

        // ── Clic izquierdo: abrir en área principal ──────────────────────
        pane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                SoundService.play(SoundService.Sound.NAVIGATE);
                sidebar.lookupAll(".nav-btn-pane")
                       .forEach(n -> n.getStyleClass().remove("nav-btn-active"));
                pane.getStyleClass().add("nav-btn-active");
                moverPill(pane);
                accionPrincipal.run();
            }
        });

        // ── Clic derecho: menú contextual ────────────────────────────────
        ContextMenu ctx = buildContextMenu(titulo, popupFactory, pane);
        pane.setOnContextMenuRequested(e ->
            ctx.show(pane, e.getScreenX(), e.getScreenY()));

        return pane;
    }

    private ContextMenu buildContextMenu(String titulo, Supplier<javafx.scene.Parent> factory,
                                          StackPane pane) {
        ContextMenu ctx = new ContextMenu();

        // Elemento principal
        MenuItem miVentana = new MenuItem(t("main.ctx.abrir_ventana"));
        miVentana.setStyle("-fx-font-weight: bold;");
        miVentana.setOnAction(e -> {
            SoundService.play(SoundService.Sound.WINDOW_OPEN);
            visualAssistant.decir(t("main.asistente.ventana"));
            List<String> css = getScene() != null
                ? new ArrayList<>(getScene().getStylesheets())
                : List.of();
            ModuloWindowManager.abrirEnVentana(t(titulo), factory, css, visualAssistant::instalarAyudaAutomatica);
        });

        ctx.getItems().add(miVentana);
        ctx.getItems().add(new SeparatorMenuItem());

        // Elemento secundario: abrir en área principal y marcar activo
        MenuItem miPrincipal = new MenuItem(t("main.ctx.abrir_principal"));
        miPrincipal.setOnAction(e -> {
            SoundService.play(SoundService.Sound.NAVIGATE);
            sidebar.lookupAll(".nav-btn-pane")
                   .forEach(n -> n.getStyleClass().remove("nav-btn-active"));
            pane.getStyleClass().add("nav-btn-active");
            moverPill(pane);
            mostrarVista(factory.get(), titulo);
        });
        ctx.getItems().add(miPrincipal);

        return ctx;
    }

    private VBox navGrupo(String titulo, StackPane... botones) {
        List<StackPane> visibles = new ArrayList<>();
        for (StackPane boton : botones) {
            if (boton != null) {
                visibles.add(boton);
            }
        }
        if (visibles.isEmpty()) {
            VBox empty = new VBox();
            empty.setManaged(false);
            empty.setVisible(false);
            return empty;
        }

        StackPane arrow = Icons.navArrow();

        Label lblTitulo = new Label(titulo);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        lblTitulo.getStyleClass().add("nav-group-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        HBox header = new HBox(8, arrow, lblTitulo);
        header.getStyleClass().add("nav-group-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));

        arrow.setRotate(0);

        VBox contenido = new VBox();
        contenido.getStyleClass().add("nav-group-content");
        contenido.getChildren().addAll(visibles);
        contenido.setVisible(false);
        contenido.setManaged(false);

        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                SoundService.play(SoundService.Sound.NAVIGATE);
                boolean expand = !contenido.isVisible();
                contenido.setVisible(expand);
                contenido.setManaged(expand);
                RotateTransition rt = new RotateTransition(Duration.millis(180), arrow);
                rt.setToAngle(expand ? 90 : 0);
                rt.play();
                if (expand) {
                    contenido.setOpacity(0);
                    FadeTransition ftGroup = new FadeTransition(Duration.millis(150), contenido);
                    ftGroup.setFromValue(0);
                    ftGroup.setToValue(1);
                    ftGroup.play();
                }
                visualAssistant.decir(expand
                    ? tf("main.asistente.grupo.expandir", titulo)
                    : tf("main.asistente.grupo.contraer", titulo));
            }
        });

        VBox grupo = new VBox(header, contenido);
        grupo.getStyleClass().add("nav-group");
        return grupo;
    }

    private void toggleBusqueda() {
        boolean show = !tfBusqueda.isVisible();
        tfBusqueda.setVisible(show);
        tfBusqueda.setManaged(show);
        if (show) {
            tfBusqueda.requestFocus();
            tfBusqueda.clear();
        } else {
            tfBusqueda.clear();
        }
    }

    private void filtrarNav(String query, VBox navMenu) {
        boolean blank = query == null || query.isBlank();
        String q = blank ? "" : query.toLowerCase();

        navMenu.getChildren().forEach(node -> {
            if (blank) {
                node.setVisible(true);
                node.setManaged(true);
                if (node instanceof VBox grupo) {
                    grupo.getChildren().stream()
                        .filter(c -> c.getStyleClass().contains("nav-group-content"))
                        .forEach(c -> { c.setVisible(true); c.setManaged(true); });
                }
                return;
            }
            if (node instanceof VBox grupo) {
                boolean hasMatch = grupo.getChildren().stream()
                    .filter(c -> c.getStyleClass().contains("nav-group-content"))
                    .flatMap(c -> ((VBox) c).getChildren().stream())
                    .filter(p -> p instanceof StackPane)
                    .flatMap(p -> ((StackPane) p).getChildren().stream())
                    .filter(l -> l instanceof Label)
                    .anyMatch(l -> ((Label) l).getText().toLowerCase().contains(q));
                node.setVisible(hasMatch);
                node.setManaged(hasMatch);
                if (hasMatch) {
                    grupo.getChildren().stream()
                        .filter(c -> c.getStyleClass().contains("nav-group-content"))
                        .forEach(c -> { c.setVisible(true); c.setManaged(true); });
                }
            } else if (node instanceof StackPane pane) {
                boolean matches = pane.getChildren().stream()
                    .filter(l -> l instanceof Label)
                    .anyMatch(l -> ((Label) l).getText().toLowerCase().contains(q));
                node.setVisible(matches);
                node.setManaged(matches);
            }
        });
    }

    private void moverPill(StackPane boton) {
        if (navPill == null || navPillContainer == null) return;
        Platform.runLater(() -> {
            Bounds bScene = boton.localToScene(boton.getBoundsInLocal());
            if (bScene == null) return;
            Bounds bLocal = navPillContainer.sceneToLocal(bScene);
            double targetY = bLocal.getMinY();
            double h = boton.getHeight() > 0 ? boton.getHeight() : 36;
            navPill.setPrefHeight(h);
            if (!navPill.isVisible()) {
                navPill.setTranslateY(targetY);
                navPill.setVisible(true);
                FadeTransition ft = new FadeTransition(Duration.millis(150), navPill);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            } else {
                TranslateTransition tt = new TranslateTransition(Duration.millis(200), navPill);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setToY(targetY);
                tt.play();
            }
        });
    }

    private void mostrarVista(Node vista, String titulo) {
        if (!(vista instanceof HelpView)) {
            currentModuleId = TITULO_A_MODULO.getOrDefault(titulo, "general");
        }
        vista.setOpacity(0);
        vista.setTranslateX(24);
        contentArea.getChildren().setAll(vista);
        FadeTransition ft = new FadeTransition(Duration.millis(220), vista);
        ft.setFromValue(0);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), vista);
        tt.setFromX(24);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
        tt.play();
        if (vista instanceof Parent parent) {
            visualAssistant.instalarAyudaAutomatica(parent);
        }
        SoundService.play(SoundService.Sound.WINDOW_OPEN);
        visualAssistant.decirModulo(t(titulo));
    }

}
