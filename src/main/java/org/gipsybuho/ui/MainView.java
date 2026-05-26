package org.gipsybuho.ui;

import javafx.application.Platform;
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
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.RotateTransition;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.stage.Stage;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserPermissions;
import org.gipsybuho.service.AuthService;
import org.gipsybuho.service.SoundService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MainView extends BorderPane {

    private final StackPane contentArea = new StackPane();
    private final VisualAssistantView visualAssistant;
    private VBox sidebar;
    private final IAView iaView;
    private final User loggedInUser;
    private final AuthService authService;
    private final Stage primaryStage;

    public MainView(Stage stage, User loggedInUser, AuthService authService) {
        this.primaryStage = stage;
        this.loggedInUser = loggedInUser;
        this.authService = authService;
        this.iaView = new IAView();
        this.visualAssistant = new VisualAssistantView();
        setLeft(buildSidebar());
        setCenter(new StackPane(contentArea, visualAssistant));
        getStyleClass().add("main-view");
        if (loggedInUser.hasPermission(UserPermissions.DASHBOARD)) {
            mostrarVista(new DashboardView(), "Panel principal");
        } else {
            mostrarVista(accesoLimitado(), "Inicio");
        }
    }

    private VBox buildSidebar() {
        sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(210);

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
        sidebar.getChildren().add(logoBox);

        Label userInfoLabel = new Label(loggedInUser.getUsername()
            + "  ·  " + loggedInUser.getRole().getLabel());
        userInfoLabel.setGraphic(Icons.person());
        userInfoLabel.setGraphicTextGap(8);
        userInfoLabel.getStyleClass().add("sidebar-user-info");
        VBox.setMargin(userInfoLabel, new Insets(10, 0, 0, 10));
        sidebar.getChildren().add(userInfoLabel);

        String lastLoginText = loggedInUser.getLastLogin() == null
            ? "Primera sesión"
            : "Última sesión: " + loggedInUser.getLastLogin()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Label lastLoginLabel = new Label(lastLoginText);
        lastLoginLabel.getStyleClass().add("sidebar-version");
        VBox.setMargin(lastLoginLabel, new Insets(2, 0, 0, 12));
        sidebar.getChildren().add(lastLoginLabel);

        // Separador
        Region sep = new Region();
        sep.getStyleClass().add("sidebar-sep");
        sep.setPrefHeight(1);
        sidebar.getChildren().add(sep);

        // ── Botones de navegación dentro de ScrollPane ───────────────────
        VBox navMenu = new VBox();
        addIfPermiso(navMenu, UserPermissions.DASHBOARD,
            navBtn(Icons.home(), "Panel principal", DashboardView::new));
        navMenu.getChildren().addAll(
            navGrupo("CLIENTES Y MAESTROS",
                navBtn(UserPermissions.CLIENTES, Icons.users(),      "Clientes",    ClientesView::new),
                navBtn(UserPermissions.TARIFAS,  Icons.tag(),        "Tarifas",     TarifasView::new)
            ),
            navGrupo("COMERCIAL",
                navBtn(UserPermissions.PRESUPUESTOS, Icons.assignment(), "Presupuestos", PresupuestosView::new),
                navBtn(UserPermissions.PEDIDOS,      Icons.cart(),       "Pedidos",      PedidosView::new),
                navBtn(UserPermissions.ALBARANES,    Icons.file(),       "Albaranes",    AlbaranesView::new),
                navBtn(UserPermissions.FACTURAS,     Icons.receipt(),    "Facturas",     FacturasView::new)
            ),
            navGrupo("ALMACÉN",
                navBtn(UserPermissions.MATERIALES, Icons.layers(), "Materiales", MaterialesView::new)
            ),
            navGrupo("PERSONAL",
                navBtn(UserPermissions.EMPLEADOS, Icons.person(),  "Empleados", EmpleadosView::new),
                navBtn(UserPermissions.NOMINAS,   Icons.work(),    "Nóminas",   NominasView::new)
            ),
            navGrupo("ANALÍTICA",
                navBtn(UserPermissions.ESTADISTICAS, Icons.barChart(),  "Estadísticas", EstadisticasView::new),
                navBtn(UserPermissions.CALENDARIO,   Icons.calendar(),  "Calendario",   CalendarioView::new),
                navBtn(UserPermissions.IA, Icons.robot(), "Asistente IA",
                    () -> mostrarVista(iaView, "Asistente IA"),
                    IAView::new)
            ),
            navGrupo("SISTEMA",
                navBtn(UserPermissions.IMPORTAR_BACKUP, Icons.download(), "Importar Backup",   ImportBackupView::new),
                navBtn(UserPermissions.EXPORTAR_BACKUP, Icons.upload(),   "Exportar / Backup", ExportView::new),
                navBtn(UserPermissions.CONFIGURACION,   Icons.settings(), "Configuración",
                    () -> new ConfiguracionView(visualAssistant)),
                loggedInUser.isAdmin()
                    ? navBtn(Icons.users(), "Gestión de usuarios",
                        () -> new UserManagementView(authService, loggedInUser))
                    : null
            )
        );

        ScrollPane scroll = new ScrollPane(navMenu);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.getChildren().add(scroll);

        Button btnCerrarApp = new Button("Salir");
        btnCerrarApp.setGraphic(Icons.power());
        btnCerrarApp.setGraphicTextGap(8);
        btnCerrarApp.getStyleClass().add("sidebar-exit-btn");
        btnCerrarApp.setMaxWidth(Double.MAX_VALUE);
        btnCerrarApp.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));
        btnCerrarApp.setOnAction(e -> {
            if (confirmarSalida()) {
                Platform.exit();
            }
        });
        sidebar.getChildren().add(btnCerrarApp);

        Label version = new Label("v13.5.0 · Almería, España");
        version.getStyleClass().add("sidebar-version");
        VBox.setMargin(version, new Insets(0, 0, 8, 0));
        sidebar.getChildren().add(version);

        return sidebar;
    }

    private void actualizarBotonAsistente(Label label) {
        label.setText(visualAssistant.isActivo() ? "Desactivar asistente" : "Activar asistente");
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
            "¿Deseas cerrar la aplicación?",
            ButtonType.YES,
            ButtonType.NO
        );
        confirmacion.setTitle("Salir");
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

    private javafx.scene.Parent accesoLimitado() {
        Label label = new Label("No tienes módulos asignados. Contacta con el administrador.");
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
        Label lbl = new Label(texto);
        if (icon != null) {
            lbl.setGraphic(icon);
            lbl.setGraphicTextGap(10);
        }
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.getStyleClass().add("nav-btn");

        StackPane pane = new StackPane(lbl);
        pane.getStyleClass().add("nav-btn-pane");
        pane.setOnMouseEntered(e -> SoundService.play(SoundService.Sound.HOVER));

        // Tooltip de ayuda para el clic derecho
        Tooltip tip = new Tooltip("Clic para abrir · Clic derecho → ventana emergente");
        tip.setStyle("-fx-font-size:10;");
        Tooltip.install(pane, tip);

        // ── Clic izquierdo: abrir en área principal ──────────────────────
        pane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                SoundService.play(SoundService.Sound.NAVIGATE);
                sidebar.lookupAll(".nav-btn-pane")
                       .forEach(n -> n.getStyleClass().remove("nav-btn-active"));
                pane.getStyleClass().add("nav-btn-active");
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
        MenuItem miVentana = new MenuItem("🪟  Abrir en ventana aparte");
        miVentana.setStyle("-fx-font-weight: bold;");
        miVentana.setOnAction(e -> {
            SoundService.play(SoundService.Sound.WINDOW_OPEN);
            visualAssistant.decir("Ventana emergente: este módulo se abre separado para que puedas trabajar sin perder la pantalla actual.");
            List<String> css = getScene() != null
                ? new ArrayList<>(getScene().getStylesheets())
                : List.of();
            ModuloWindowManager.abrirEnVentana(titulo, factory, css, visualAssistant::instalarAyudaAutomatica);
        });

        ctx.getItems().add(miVentana);
        ctx.getItems().add(new SeparatorMenuItem());

        // Elemento secundario: abrir en área principal y marcar activo
        MenuItem miPrincipal = new MenuItem("↩  Abrir en área principal");
        miPrincipal.setOnAction(e -> {
            SoundService.play(SoundService.Sound.NAVIGATE);
            sidebar.lookupAll(".nav-btn-pane")
                   .forEach(n -> n.getStyleClass().remove("nav-btn-active"));
            pane.getStyleClass().add("nav-btn-active");
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
                visualAssistant.decir(expand
                    ? "Grupo " + titulo + ": aquí tienes los módulos relacionados con esta área."
                    : "Grupo " + titulo + " contraído.");
            }
        });

        VBox grupo = new VBox(header, contenido);
        grupo.getStyleClass().add("nav-group");
        return grupo;
    }

    private void mostrarVista(Node vista, String titulo) {
        contentArea.getChildren().setAll(vista);
        if (vista instanceof Parent parent) {
            visualAssistant.instalarAyudaAutomatica(parent);
        }
        SoundService.play(SoundService.Sound.WINDOW_OPEN);
        visualAssistant.decirModulo(titulo);
    }

}
