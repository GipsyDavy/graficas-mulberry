package org.gipsybuho.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gipsybuho.App;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;
import org.gipsybuho.service.SoundService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MainView extends BorderPane {

    private final StackPane contentArea = new StackPane();
    private VBox sidebar;
    private final IAView iaView;
    private final AuthService authService;
    private final User loggedInUser;
    private final Stage primaryStage;
    private final Runnable onLockScreenRequested; // Nuevo campo para el callback de bloqueo

    private Timeline sessionTimeoutTimeline;
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 30; // Valor por defecto si no se encuentra en la DB

    public MainView(Stage stage, AuthService authService, User loggedInUser, Runnable onLockScreenRequested) {
        this.primaryStage = stage;
        this.authService = authService;
        this.loggedInUser = loggedInUser;
        this.onLockScreenRequested = onLockScreenRequested; // Asignar el callback
        this.iaView = new IAView();
        setLeft(buildSidebar());
        setCenter(contentArea);
        getStyleClass().add("main-view");
        mostrarVista(new DashboardView(loggedInUser)); // Se pasa el usuario al Dashboard

        // Inicializar el temporizador de inactividad
        initializeSessionTimeout();

        // Añadir listeners de actividad a la escena una vez que esté disponible
        Platform.runLater(() -> {
            if (getScene() != null) {
                getScene().addEventFilter(InputEvent.ANY, this::resetSessionTimeout);
            }
        });
    }

    private void initializeSessionTimeout() {
        int timeoutMinutes = DEFAULT_SESSION_TIMEOUT_MINUTES;
        try {
            String timeoutStr = DatabaseManager.getConfig("session_timeout_minutes");
            if (!timeoutStr.isBlank()) {
                timeoutMinutes = Integer.parseInt(timeoutStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error al leer 'session_timeout_minutes' de la configuración. Usando valor por defecto: " + DEFAULT_SESSION_TIMEOUT_MINUTES + " minutos.");
        }

        sessionTimeoutTimeline = new Timeline(new KeyFrame(Duration.minutes(timeoutMinutes), event -> {
            // El temporizador ha expirado, cerrar sesión
            Platform.runLater(this::logoutDueToInactivity);
        }));
        sessionTimeoutTimeline.setCycleCount(1); // Solo se ejecuta una vez
        sessionTimeoutTimeline.playFromStart(); // Iniciar el temporizador
    }

    private void resetSessionTimeout(InputEvent event) {
        if (sessionTimeoutTimeline != null) {
            sessionTimeoutTimeline.playFromStart(); // Reiniciar el temporizador
        }
    }

    private void logoutDueToInactivity() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sesión Expirada");
        alert.setHeaderText("Su sesión ha expirado debido a inactividad.");
        alert.setContentText("Por favor, inicie sesión de nuevo.");
        alert.showAndWait();

        performLogout();
    }

    private void performLogout() {
        if (sessionTimeoutTimeline != null) {
            sessionTimeoutTimeline.stop(); // Detener el temporizador al cerrar sesión
        }
        authService.logAccess(loggedInUser.getId(), loggedInUser.getUsername(), "logout");
        if (Platform.getApplication() instanceof App) {
            try {
                ((App) Platform.getApplication()).showLoginScreen();
            } catch (IOException e) {
                System.err.println("Error al volver a la pantalla de login: " + e.getMessage());
            }
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

        // Información del usuario logueado y último acceso
        Label userInfoLabel = new Label("Bienvenido, " + loggedInUser.getUsername());
        userInfoLabel.getStyleClass().add("sidebar-user-info");
        VBox.setMargin(userInfoLabel, new Insets(10, 0, 0, 10)); // Margen para separar del logo

        Label lastLoginLabel = new Label(authService.getLastLoginInfo(loggedInUser.getUsername()));
        lastLoginLabel.getStyleClass().add("sidebar-last-login");
        VBox.setMargin(lastLoginLabel, new Insets(0, 0, 10, 10)); // Margen inferior para separar del separador

        sidebar.getChildren().addAll(userInfoLabel, lastLoginLabel);

        // Separador
        Region sep = new Region();
        sep.getStyleClass().add("sidebar-sep");
        sep.setPrefHeight(1);
        sidebar.getChildren().add(sep);

        // ── Botones de navegación dentro de ScrollPane ───────────────────
        VBox navMenu = new VBox();
        navMenu.getChildren().add(navBtn("🏠  Panel principal", () -> new DashboardView(loggedInUser)));
        navMenu.getChildren().addAll(
            navGrupo("CLIENTES",
                navBtn("👥  Clientes",            ClientesView::new)
            ),
            navGrupo("COMERCIAL",
                navBtn("📋  Presupuestos",        PresupuestosView::new),
                navBtn("🧾  Facturas",            FacturasView::new),
                navBtn("📋  Albaranes",           AlbaranesView::new),
                navBtn("📦  Pedidos",             PedidosView::new)
            ),
            navGrupo("ALMACÉN",
                navBtn("💰  Tarifas",             TarifasView::new),
                navBtn("📦  Materiales",          MaterialesView::new)
            ),
            navGrupo("PERSONAL",
                navBtn("👤  Empleados",           EmpleadosView::new),
                navBtn("💼  Nóminas",             NominasView::new)
            ),
            navGrupo("ANALÍTICA",
                navBtn("📊  Estadísticas",        EstadisticasView::new),
                navBtn("📅  Calendario",          CalendarioView::new)
            ),
            navGrupo("SISTEMA",
                navBtnEspecial("🤖  Asistente IA",
                    () -> mostrarVista(iaView),
                    IAView::new),
                navBtn("👥  Gestión de Usuarios", () -> new UserManagementView(authService)), // Nuevo botón
                navBtn("📥  Importar Backup",     ImportBackupView::new),
                navBtn("💾  Exportar / Backup",   ExportView::new),
                navBtn("⚙  Configuración",        ConfiguracionView::new)
            )
        );

        ScrollPane scroll = new ScrollPane(navMenu);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        sidebar.getChildren().add(scroll);

        // Botón de Bloquear Pantalla
        Button btnLock = new Button("🔒  Bloquear Pantalla");
        btnLock.setMaxWidth(Double.MAX_VALUE);
        btnLock.setStyle(
            "-fx-background-color:#3498DB; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-font-size:10px; -fx-padding:5 10; -fx-background-radius:0; -fx-cursor:hand;");
        btnLock.setOnAction(e -> {
            if (onLockScreenRequested != null) {
                onLockScreenRequested.run();
            }
        });
        sidebar.getChildren().add(btnLock);

        Button btnSalir = new Button("⏻  Cerrar Sesión");
        btnSalir.setMaxWidth(Double.MAX_VALUE);
        btnSalir.setStyle(
            "-fx-background-color:#E74C3C; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-font-size:10px; -fx-padding:5 10; -fx-background-radius:0; -fx-cursor:hand;");
        btnSalir.setOnAction(e -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas cerrar la sesión actual?",
                ButtonType.YES, ButtonType.NO);
            conf.setTitle("Cerrar Sesión");
            conf.setHeaderText(null);
            if (getScene() != null) conf.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
            conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                performLogout();
            });
        });
        sidebar.getChildren().add(btnSalir);

        Label version = new Label("v7.0.0 · Almería, España");
        version.getStyleClass().add("sidebar-version");
        VBox.setMargin(version, new Insets(0, 0, 8, 0));
        sidebar.getChildren().add(version);

        return sidebar;
    }

    // ── Constructores de botón ────────────────────────────────────────────────

    /**
     * Botón estándar: clic izquierdo crea una nueva instancia del módulo y la
     * muestra en el área central; clic derecho abre el menú contextual con la
     * opción de abrir en ventana emergente (también crea una nueva instancia).
     */
    private StackPane navBtn(String texto, Supplier<javafx.scene.Parent> factory) {
        return navBtnImpl(texto, () -> mostrarVista(factory.get()), factory, texto);
    }

    /**
     * Botón especial: permite separar la acción de clic izquierdo (p.ej. reusar
     * una instancia existente) de la fábrica usada para crear ventanas emergentes.
     */
    private StackPane navBtnEspecial(String texto, Runnable accionPrincipal,
                                     Supplier<javafx.scene.Parent> popupFactory) {
        return navBtnImpl(texto, accionPrincipal, popupFactory, texto);
    }

    private StackPane navBtnImpl(String texto, Runnable accionPrincipal,
                                  Supplier<javafx.scene.Parent> popupFactory, String titulo) {
        Label lbl = new Label(texto);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.getStyleClass().add("nav-btn");

        StackPane pane = new StackPane(lbl);
        pane.getStyleClass().add("nav-btn-pane");

        // Tooltip de ayuda para el clic derecho
        Tooltip tip = new Tooltip("Clic para abrir · Clic derecho → ventana emergente");
        tip.setStyle("-fx-font-size:10;");
        Tooltip.install(pane, tip);

        // ── Clic izquierdo: abrir en área principal ──────────────────────
        pane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                SoundService.play(SoundService.Sound.CLICK);
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
            List<String> css = getScene() != null
                ? new ArrayList<>(getScene().getStylesheets())
                : List.of();
            ModuloWindowManager.abrirEnVentana(titulo, factory, css);
        });

        ctx.getItems().add(miVentana);
        ctx.getItems().add(new SeparatorMenuItem());

        // Elemento secundario: abrir en área principal y marcar activo
        MenuItem miPrincipal = new MenuItem("↩  Abrir en área principal");
        miPrincipal.setOnAction(e -> {
            SoundService.play(SoundService.Sound.CLICK);
            sidebar.lookupAll(".nav-btn-pane")
                   .forEach(n -> n.getStyleClass().remove("nav-btn-active"));
            pane.getStyleClass().add("nav-btn-active");
            mostrarVista(factory.get());
        });
        ctx.getItems().add(miPrincipal);

        return ctx;
    }

    private VBox navGrupo(String titulo, StackPane... botones) {
        Label arrow = new Label("▶");
        arrow.getStyleClass().add("nav-group-arrow");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);
        lblTitulo.getStyleClass().add("nav-group-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        HBox header = new HBox(8, arrow, lblTitulo);
        header.getStyleClass().add("nav-group-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        VBox contenido = new VBox();
        contenido.getStyleClass().add("nav-group-content");
        contenido.getChildren().addAll(botones);
        contenido.setVisible(false);
        contenido.setManaged(false);

        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                boolean expand = !contenido.isVisible();
                contenido.setVisible(expand);
                contenido.setManaged(expand);
                arrow.setText(expand ? "▼" : "▶");
            }
        });

        VBox grupo = new VBox(header, contenido);
        grupo.getStyleClass().add("nav-group");
        return grupo;
    }

    private void mostrarVista(Node vista) {
        contentArea.getChildren().setAll(vista);
    }
}
