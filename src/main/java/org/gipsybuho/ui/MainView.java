package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.SoundService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MainView extends BorderPane {

    private final StackPane contentArea = new StackPane();
    private VBox sidebar;
    private final IAView iaView = new IAView();

    public MainView(Stage stage) {
        setLeft(buildSidebar());
        setCenter(contentArea);
        getStyleClass().add("main-view");
        mostrarVista(new DashboardView());
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

        // Separador
        Region sep = new Region();
        sep.getStyleClass().add("sidebar-sep");
        sep.setPrefHeight(1);
        sidebar.getChildren().add(sep);

        // ── Botones de navegación ─────────────────────────────────────────
        sidebar.getChildren().addAll(
            navBtn("🏠  Panel principal",    DashboardView::new),
            navBtn("👥  Clientes",            ClientesView::new),
            navBtn("📋  Presupuestos",        PresupuestosView::new),
            navBtn("🧾  Facturas",            FacturasView::new),
            navBtn("📋  Albaranes",           AlbaranesView::new),
            navBtn("📦  Pedidos",             PedidosView::new),
            navBtn("💰  Tarifas",             TarifasView::new),
            navBtn("📦  Materiales",          MaterialesView::new),
            navBtn("👤  Empleados",           EmpleadosView::new),
            navBtn("💼  Nóminas",             NominasView::new),
            navBtn("📊  Estadísticas",        EstadisticasView::new),
            // IAView: clic izquierdo reutiliza la instancia compartida (conserva el chat);
            // clic derecho / popup abre una instancia nueva independiente
            navBtnEspecial("🤖  Asistente IA",
                () -> mostrarVista(iaView),
                IAView::new),
            navBtn("📅  Calendario",          CalendarioView::new),
            navBtn("📥  Importar Backup",     ImportBackupView::new),
            navBtn("💾  Exportar / Backup",   ExportView::new),
            navBtn("⚙  Configuración",        ConfiguracionView::new)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button btnSalir = new Button("⏻  Cerrar");
        btnSalir.setMaxWidth(Double.MAX_VALUE);
        btnSalir.setStyle(
            "-fx-background-color:#E74C3C; -fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-font-size:10px; -fx-padding:5 10; -fx-background-radius:0; -fx-cursor:hand;");
        btnSalir.setOnAction(e -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas cerrar Gráficas Mulberry?\n" +
                "Se cerrarán también todas las ventanas emergentes abiertas y se " +
                "detendrá el proceso de Ollama si está activo.",
                ButtonType.YES, ButtonType.NO);
            conf.setTitle("Cerrar aplicación");
            conf.setHeaderText(null);
            if (getScene() != null) conf.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
            conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                ModuloWindowManager.cerrarTodas();
                Platform.exit();
            });
        });
        sidebar.getChildren().add(btnSalir);

        Label version = new Label("v6.0 · Almería, España");
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
        return navBtnImpl(texto, () -> mostrarVista(factory.get()), factory);
    }

    /**
     * Botón especial: permite separar la acción de clic izquierdo (p.ej. reusar
     * una instancia existente) de la fábrica usada para crear ventanas emergentes.
     */
    private StackPane navBtnEspecial(String texto, Runnable accionPrincipal,
                                     Supplier<javafx.scene.Parent> popupFactory) {
        return navBtnImpl(texto, accionPrincipal, popupFactory);
    }

    private StackPane navBtnImpl(String texto, Runnable accionPrincipal,
                                  Supplier<javafx.scene.Parent> popupFactory) {
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
        ContextMenu ctx = buildContextMenu(texto, popupFactory, pane);
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

    private void mostrarVista(Node vista) {
        contentArea.getChildren().setAll(vista);
    }
}
