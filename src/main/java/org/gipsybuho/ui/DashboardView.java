package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.gipsybuho.dao.*;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.User; // Importar User

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class DashboardView extends VBox {

    private final User loggedInUser; // Añadir campo para el usuario logueado

    public DashboardView(User loggedInUser) { // Constructor modificado
        this.loggedInUser = loggedInUser; // Asignar el usuario logueado

        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(20);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("view-title");

        Label subtitulo = new Label("Resumen de actividad — " + LocalDate.now());
        subtitulo.getStyleClass().add("view-subtitle");

        // Mostrar información del último acceso
        Label lastLoginLabel = new Label();
        lastLoginLabel.getStyleClass().add("last-login-info");
        updateLastLoginInfo(lastLoginLabel);

        HBox tarjetas = buildTarjetas();
        VBox avisos   = buildAvisosCalendario();
        getChildren().addAll(titulo, subtitulo, lastLoginLabel, tarjetas, avisos); // Añadir lastLoginLabel
        cargarDatos(tarjetas);
    }

    private void updateLastLoginInfo(Label label) {
        if (loggedInUser != null && loggedInUser.getLastLogin() != null) {
            LocalDateTime lastLogin = loggedInUser.getLastLogin();
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                                        .withLocale(new Locale("es", "ES"));
            String formattedLastLogin = lastLogin.format(formatter);
            label.setText("Último acceso: " + formattedLastLogin + " — " + loggedInUser.getUsername());
        } else {
            label.setText("Último acceso: No registrado");
        }
    }

    private HBox buildTarjetas() {
        HBox row = new HBox(16);
        row.getChildren().addAll(
            tarjeta("c1", "Clientes",            "—", "#4C9BE8", "👥"),
            tarjeta("c2", "Presupuestos pend.",  "—", "#F39C12", "📋"),
            tarjeta("c3", "Facturas pendientes", "—", "#E74C3C", "🧾"),
            tarjeta("c4", "Bajo stock",          "—", "#9B59B6", "📦"),
            tarjeta("c5", "Facturado este año",  "—", "#27AE60", "💰")
        );
        return row;
    }

    private VBox tarjeta(String id, String titulo, String valor, String color, String icono) {
        VBox card = new VBox(6);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(200);
        card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label ico = new Label(icono);
        ico.setStyle("-fx-font-size: 20;");
        Label tit = new Label(titulo);
        tit.getStyleClass().add("card-title");
        header.getChildren().addAll(ico, tit);

        Label val = new Label(valor);
        val.getStyleClass().add("card-value");
        val.setStyle("-fx-text-fill: " + color + ";");
        val.setUserData(id);

        card.getChildren().addAll(header, val);
        return card;
    }

    private VBox buildAvisosCalendario() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("avisos-panel");

        Label titulo = new Label("📅  Próximos recordatorios");
        titulo.getStyleClass().add("avisos-titulo");
        panel.getChildren().add(titulo);

        try {
            String rawDias = org.gipsybuho.db.DatabaseManager.getConfig("cal_dias_panel");
            int diasPanel = rawDias.isBlank() ? 7 : Integer.parseInt(rawDias);
            List<NotaCalendario> proximas = new NotaCalendarioDAO().findProximas(diasPanel);
            LocalDate hoy = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "ES"));

            if (proximas.isEmpty()) {
                Label vacio = new Label("No hay recordatorios en los próximos 7 días");
                vacio.getStyleClass().add("avisos-vacio");
                panel.getChildren().add(vacio);
            } else {
                for (NotaCalendario nota : proximas) {
                    long dias = ChronoUnit.DAYS.between(hoy, nota.getFecha());
                    String urgenciaClass = dias == 0 ? "aviso-hoy"
                        : dias <= 2 ? "aviso-urgente" : "aviso-proximo";

                    HBox fila = new HBox(12);
                    fila.getStyleClass().addAll("aviso-fila", urgenciaClass);
                    fila.setAlignment(Pos.CENTER_LEFT);

                    Label lblDias = new Label(dias == 0 ? "HOY" : "+" + dias + "d");
                    lblDias.getStyleClass().add("aviso-badge");

                    VBox textos = new VBox(2);
                    Label lblTit = new Label(nota.getTitulo());
                    lblTit.getStyleClass().add("aviso-titulo-nota");
                    String capFecha = nota.getFecha().format(fmt);
                    capFecha = Character.toUpperCase(capFecha.charAt(0)) + capFecha.substring(1);
                    Label lblFecha = new Label(capFecha);
                    lblFecha.getStyleClass().add("aviso-fecha-nota");
                    textos.getChildren().addAll(lblTit, lblFecha);

                    fila.getChildren().addAll(lblDias, textos);
                    panel.getChildren().add(fila);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar avisos del calendario: " + e.getMessage());
        }

        return panel;
    }

    private void cargarDatos(HBox tarjetas) {
        try {
            int clientes    = new ClienteDAO().count();
            int presupPend  = new PresupuestoDAO().countByEstado("enviado");
            int factPend    = new FacturaDAO().countByEstado("pendiente");
            int bajoStock   = new MaterialDAO().countBajoStock();
            double facturado = new FacturaDAO().totalFacturadoAnio(LocalDate.now().getYear());

            String[] valores = {
                String.valueOf(clientes), String.valueOf(presupPend),
                String.valueOf(factPend), String.valueOf(bajoStock),
                String.format("%.0f €", facturado)
            };
            String[] ids = {"c1","c2","c3","c4","c5"};
            for (int i = 0; i < ids.length; i++) {
                final String v = valores[i];
                final String id = ids[i];
                tarjetas.getChildren().stream()
                    .filter(n -> n instanceof VBox)
                    .map(n -> (VBox) n)
                    .flatMap(vb -> vb.getChildren().stream())
                    .filter(n -> n instanceof Label && id.equals(n.getUserData()))
                    .map(n -> (Label) n)
                    .findFirst()
                    .ifPresent(lbl -> lbl.setText(v));
            }
        } catch (Exception e) {
            System.err.println("Error al cargar datos del dashboard: " + e.getMessage());
        }
    }
}
