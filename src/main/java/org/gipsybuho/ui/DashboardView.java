package org.gipsybuho.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.gipsybuho.dao.*;
import org.gipsybuho.model.NotaCalendario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(20);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("view-title");

        Label subtitulo = new Label("Resumen de actividad — " + LocalDate.now());
        subtitulo.getStyleClass().add("view-subtitle");

        FlowPane tarjetas = buildTarjetas();
        VBox avisos       = buildAvisosCalendario();
        getChildren().addAll(titulo, subtitulo, tarjetas, avisos);
        cargarDatos(tarjetas);
    }

    private FlowPane buildTarjetas() {
        FlowPane flow = new FlowPane(16, 12);
        flow.getChildren().addAll(
            tarjeta("c1", "Clientes",            "—", "#4C9BE8", "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z",        "Total en BD"),
            tarjeta("c2", "Presupuestos pend.",  "—", "#F39C12", "M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z",                                                                                                                                                                                                     "Estado: enviado"),
            tarjeta("c3", "Facturas pendientes", "—", "#E74C3C", "M18 17H6v-2h12v2zm0-4H6v-2h12v2zm0-4H6V7h12v2zM3 22l1.5-1.5L6 22l1.5-1.5L9 22l1.5-1.5L12 22l1.5-1.5L15 22l1.5-1.5L18 22l1.5-1.5L21 22V2l-1.5 1.5L18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2 4.5 3.5 3 2v20z",                                                                                                             "Estado: pendiente"),
            tarjeta("c4", "Bajo stock",          "—", "#9B59B6", "M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27-7.38 5.74zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16z",                                                                                                                                                                                                                             "Por debajo del mínimo"),
            tarjeta("c5", "Facturado este año",  "—", "#27AE60", "M15 18.5c-2.51 0-4.68-1.42-5.76-3.5H15l1-2H8.58c-.05-.33-.08-.66-.08-1s.03-.67.08-1H15l1-2H9.24C10.32 6.92 12.5 5.5 15 5.5c1.61 0 3.09.59 4.23 1.57L21 5.3C19.41 3.87 17.3 3 15 3c-3.92 0-7.24 2.51-8.48 6H3l-1 2h4.06c-.04.33-.06.66-.06 1s.02.67.06 1H3l-1 2h4.52c1.24 3.49 4.56 6 8.48 6 2.31 0 4.41-.87 6-2.3l-1.78-1.77C18.09 17.91 16.6 18.5 15 18.5z", "Facturas cobradas")
        );
        return flow;
    }

    private VBox tarjeta(String id, String titulo, String valor, String color, String iconPath, String contexto) {
        VBox card = new VBox(6);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(200);
        card.setStyle("-fx-border-color: " + color + "; -fx-border-width: 0 0 0 4;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Node ico = Icons.cardIcon(iconPath, color, 22);
        Label tit = new Label(titulo);
        tit.getStyleClass().add("card-title");
        header.getChildren().addAll(ico, tit);

        Label val = new Label(valor);
        val.getStyleClass().add("card-value");
        val.setStyle("-fx-text-fill: " + color + ";");
        val.setUserData(id);

        Label ctx = new Label(contexto);
        ctx.getStyleClass().add("card-context");

        card.getChildren().addAll(header, val, ctx);

        String tipText = switch (id) {
            case "c1" -> "Total de clientes registrados en la base de datos";
            case "c2" -> "Presupuestos enviados pendientes de confirmación";
            case "c3" -> "Facturas emitidas pendientes de cobro";
            case "c4" -> "Materiales con stock por debajo del mínimo configurado";
            case "c5" -> "Total facturado y cobrado en el año en curso";
            default   -> titulo;
        };
        Tooltip.install(card, new Tooltip(tipText));

        TranslateTransition liftIn  = new TranslateTransition(Duration.millis(200), card);
        liftIn.setToY(-2);
        TranslateTransition liftOut = new TranslateTransition(Duration.millis(200), card);
        liftOut.setToY(0);
        card.setOnMouseEntered(e -> liftIn.playFromStart());
        card.setOnMouseExited(e  -> liftOut.playFromStart());

        return card;
    }

    private VBox buildAvisosCalendario() {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("avisos-panel");

        StackPane calIcon = Icons.calendar();
        calIcon.getChildren().get(0).getStyleClass().add("content-icon");
        Label titulo = new Label("Próximos recordatorios");
        titulo.setGraphic(calIcon);
        titulo.setGraphicTextGap(8);
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

    private void cargarDatos(FlowPane tarjetas) {
        try {
            int clientes     = new ClienteDAO().count();
            int presupPend   = new PresupuestoDAO().countByEstado("enviado");
            int factPend     = new FacturaDAO().countByEstado("pendiente");
            int bajoStock    = new MaterialDAO().countBajoStock();
            double facturado = new FacturaDAO().totalFacturadoAnio(LocalDate.now().getYear());

            int[]    enteros = {clientes, presupPend, factPend, bajoStock};
            String[] idsEnt  = {"c1", "c2", "c3", "c4"};
            for (int i = 0; i < idsEnt.length; i++) {
                final int    target = enteros[i];
                final String id     = idsEnt[i];
                tarjetas.getChildren().stream()
                    .filter(n -> n instanceof VBox).map(n -> (VBox) n)
                    .flatMap(vb -> vb.getChildren().stream())
                    .filter(n -> n instanceof Label && id.equals(n.getUserData()))
                    .map(n -> (Label) n)
                    .findFirst()
                    .ifPresent(lbl -> animarEntero(lbl, target));
            }

            tarjetas.getChildren().stream()
                .filter(n -> n instanceof VBox).map(n -> (VBox) n)
                .flatMap(vb -> vb.getChildren().stream())
                .filter(n -> n instanceof Label && "c5".equals(n.getUserData()))
                .map(n -> (Label) n)
                .findFirst()
                .ifPresent(lbl -> animarMoneda(lbl, facturado));

        } catch (Exception e) {
            System.err.println("Error al cargar datos del dashboard: " + e.getMessage());
        }
    }

    private void animarEntero(Label lbl, int target) {
        SimpleIntegerProperty prop = new SimpleIntegerProperty(0);
        prop.addListener((obs, oldV, newV) -> lbl.setText(String.valueOf(newV.intValue())));
        int durMs = Math.min(900, Math.max(400, target * 4)); // escala con el valor, máx 900ms
        new Timeline(
            new KeyFrame(Duration.ZERO,         new KeyValue(prop, 0)),
            new KeyFrame(Duration.millis(durMs), new KeyValue(prop, target, Interpolator.EASE_OUT))
        ).play();
    }

    private void animarMoneda(Label lbl, double target) {
        SimpleDoubleProperty prop = new SimpleDoubleProperty(0);
        prop.addListener((obs, oldV, newV) -> lbl.setText(String.format("%.0f €", newV.doubleValue())));
        new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(prop, 0.0)),
            new KeyFrame(Duration.millis(900), new KeyValue(prop, target, Interpolator.EASE_OUT))
        ).play();
    }
}
