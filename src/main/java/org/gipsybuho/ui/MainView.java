package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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
            navBtn("💰  Tarifas",            () -> mostrarVista(new TarifasView())),
            navBtn("📦  Materiales",         () -> mostrarVista(new MaterialesView())),
            navBtn("👤  Empleados",          () -> mostrarVista(new EmpleadosView())),
            navBtn("💼  Nóminas",            () -> mostrarVista(new NominasView())),
            navBtn("🤖  Asistente IA",       () -> mostrarVista(new IAView())),
            navBtn("📅  Calendario",          () -> mostrarVista(new CalendarioView())),
            navBtn("⚙  Configuración",        () -> mostrarVista(new ConfiguracionView()))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label version = new Label("v1.0 · Almería, España");
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
