package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.gipsybuho.dao.*;

import java.time.LocalDate;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(20);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("view-title");

        Label subtitulo = new Label("Resumen de actividad — " + LocalDate.now());
        subtitulo.getStyleClass().add("view-subtitle");

        HBox tarjetas = buildTarjetas();
        getChildren().addAll(titulo, subtitulo, tarjetas);
        cargarDatos(tarjetas);
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
                tarjetas.getChildren().stream()
                    .filter(n -> n instanceof VBox)
                    .map(n -> (VBox) n)
                    .flatMap(vb -> vb.getChildren().stream())
                    .filter(n -> n instanceof Label && ids[i].equals(n.getUserData()))
                    .map(n -> (Label) n)
                    .findFirst()
                    .ifPresent(lbl -> lbl.setText(v));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
