package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.gipsybuho.db.DatabaseManager;

import java.util.Objects;

public class MainView extends BorderPane {

    private final StackPane contentArea = new StackPane();
    private VBox sidebar;

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

        // Botones de navegación
        sidebar.getChildren().addAll(
            navBtn("🏠  Panel principal",   () -> mostrarVista(new DashboardView())),
            navBtn("👥  Clientes",           () -> mostrarVista(new ClientesView())),
            navBtn("📋  Presupuestos",       () -> mostrarVista(new PresupuestosView())),
            navBtn("🧾  Facturas",           () -> mostrarVista(new FacturasView())),
            navBtn("📋  Albaranes",          () -> mostrarVista(new AlbaranesView())),
            navBtn("📦  Pedidos",            () -> mostrarVista(new PedidosView())),
            navBtn("💰  Tarifas",            () -> mostrarVista(new TarifasView())),
            navBtn("📦  Materiales",         () -> mostrarVista(new MaterialesView())),
            navBtn("👤  Empleados",          () -> mostrarVista(new EmpleadosView())),
            navBtn("💼  Nóminas",            () -> mostrarVista(new NominasView())),
            navBtn("📊  Estadísticas",       () -> mostrarVista(new EstadisticasView())),
            navBtn("🤖  Asistente IA",       () -> mostrarVista(new IAView())),
            navBtn("📅  Calendario",          () -> mostrarVista(new CalendarioView())),
            navBtn("📥  Importar Backup",        () -> mostrarVista(new ImportBackupView())),
            navBtn("💾  Exportar / Backup",   () -> mostrarVista(new ExportView())),
            navBtn("⚙  Configuración",        () -> mostrarVista(new ConfiguracionView()))
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
                "Se detendrá también el proceso de Ollama si está activo.",
                ButtonType.YES, ButtonType.NO);
            conf.setTitle("Cerrar aplicación");
            conf.setHeaderText(null);
            if (getScene() != null) conf.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
            conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> Platform.exit());
        });
        sidebar.getChildren().add(btnSalir);

        Label version = new Label("v4.1 · Almería, España");
        version.getStyleClass().add("sidebar-version");
        VBox.setMargin(version, new Insets(0, 0, 8, 0));
        sidebar.getChildren().add(version);

        return sidebar;
    }


    private StackPane navBtn(String texto, Runnable accion) {
        Label lbl = new Label(texto);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.getStyleClass().add("nav-btn");
        StackPane pane = new StackPane(lbl);
        pane.getStyleClass().add("nav-btn-pane");
        pane.setOnMouseClicked(e -> {
            sidebar.lookupAll(".nav-btn-pane").forEach(n -> n.getStyleClass().remove("nav-btn-active"));
            pane.getStyleClass().add("nav-btn-active");
            accion.run();
        });
        return pane;
    }

    private void mostrarVista(Node vista) {
        contentArea.getChildren().setAll(vista);
    }
}
