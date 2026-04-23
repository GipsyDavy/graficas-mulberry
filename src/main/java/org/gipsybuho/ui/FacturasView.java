package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Factura;
import org.gipsybuho.service.PDFService;

import java.awt.Desktop;
import java.nio.file.Path;

public class FacturasView extends VBox {

    private final FacturaDAO dao = new FacturaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Factura> datos = FXCollections.observableArrayList();
    private final TableView<Factura> tabla = new TableView<>(datos);

    public FacturasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Facturas");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        Button btnPDF     = btn("📄 Exportar PDF",  "#27AE60", this::exportarPDF);
        Button btnPagada  = btn("✅ Marcar pagada", "#4C9BE8", this::marcarPagada);
        Button btnAnular  = btn("❌ Anular",         "#E74C3C", this::anular);
        Button btnBorrar  = btn("🗑 Borrar",         "#95A5A6", this::borrar);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnPDF, btnPagada, btnAnular, btnBorrar);
        bar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Factura> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Factura, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toUpperCase());
                String color = switch(v) {
                    case "pagada"   -> "#27AE60";
                    case "vencida"  -> "#E74C3C";
                    case "anulada"  -> "#95A5A6";
                    default         -> "#F39C12";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });

        TableColumn<Factura, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(
            col("Número", "numero", 140),
            col("Cliente", "clienteNombre", 200),
            col("Fecha", "fecha", 100),
            col("Vencimiento", "fechaVencimiento", 110),
            col("Forma de pago", "formaPago", 150),
            colEstado,
            colTotal
        );
        tabla.setPlaceholder(new Label("No hay facturas registradas"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void exportarPDF() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura para exportar."); return; }
        try {
            Factura f = dao.findById(sel.getId());
            Cliente c = clienteDAO.findById(f.getClienteId());
            Path pdf = new PDFService().generarFactura(f, c);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf.toFile());
            new Alert(Alert.AlertType.INFORMATION, "PDF generado:\n" + pdf, ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private void marcarPagada() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        try { dao.updateEstado(sel.getId(), "pagada"); cargar(); } catch (Exception e) { mostrarError(e); }
    }

    private void anular() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Anular la factura " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.updateEstado(sel.getId(), "anulada"); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        Factura sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una factura."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar permanentemente la factura " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Factura, T> col(String t, String campo, double ancho) {
        TableColumn<Factura, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho); return c;
    }

    private Button btn(String t, String color, Runnable r) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;");
        b.setOnAction(e -> r.run()); return b;
    }

    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
