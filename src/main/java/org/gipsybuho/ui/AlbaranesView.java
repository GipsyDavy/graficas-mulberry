package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.gipsybuho.dao.AlbaranDAO;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.LineaAlbaran;
import org.gipsybuho.model.Material;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.SoundService;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AlbaranesView extends VBox {

    private final AlbaranDAO dao = new AlbaranDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Albaran> datos = FXCollections.observableArrayList();
    private final TableView<Albaran> tabla = new TableView<>(datos);

    public AlbaranesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Albaranes");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn("+ Nuevo",            "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",            "#F39C12", this::editar);
        Button btnFirmado  = btn("✅ Marcar firmado",   "#27AE60", this::marcarFirmado);
        Button btnPDF      = btn("📄 Exportar PDF",     "#9B59B6", this::exportarPDF);
        Button btnBorrar   = btn("🗑 Borrar",           "#E74C3C", this::borrar);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnFirmado, btnPDF, btnBorrar);
        bar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Albaran> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Albaran, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v.toUpperCase());
                String color = switch (v) {
                    case "firmado"   -> "#27AE60";
                    case "entregado" -> "#4C9BE8";
                    default          -> "#F39C12";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });

        tabla.getColumns().addAll(
            col("Número",      "numero",         140),
            col("Cliente",     "clienteNombre",  200),
            col("Fecha",       "fecha",          100),
            col("Factura ref.", "facturaNumero",  130),
            col("Pedido ref.", "pedidoNumero",   130),
            colEstado
        );
        tabla.setPlaceholder(new Label("No hay albaranes registrados"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        try {
            List<Cliente> clientes = clienteDAO.findAll();
            if (clientes.isEmpty()) { alerta("Añade al menos un cliente antes de crear un albarán."); return; }
            Albaran a = new Albaran();
            a.setNumero(DatabaseManager.generarNumeroAlbaran());
            a.setFecha(LocalDate.now().toString());
            a.setEstado("pendiente");
            dialogoAlbaran(a, clientes).ifPresent(alb -> {
                try { dao.save(alb); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void editar() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán para editar."); return; }
        try {
            Albaran a = dao.findById(sel.getId());
            List<Cliente> clientes = clienteDAO.findAll();
            dialogoAlbaran(a, clientes).ifPresent(alb -> {
                try { dao.save(alb); cargar(); } catch (Exception e) { mostrarError(e); }
            });
        } catch (Exception e) { mostrarError(e); }
    }

    private void marcarFirmado() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán."); return; }
        try { dao.updateEstado(sel.getId(), "firmado"); cargar(); } catch (Exception e) { mostrarError(e); }
    }

    private void exportarPDF() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán para exportar."); return; }
        try {
            Albaran a = dao.findById(sel.getId());
            Cliente c = clienteDAO.findById(a.getClienteId());
            Path pdf = new PDFService().generarAlbaran(a, c);
            SoundService.play(SoundService.Sound.SUCCESS);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf.toFile());
            new Alert(Alert.AlertType.INFORMATION, "PDF generado:\n" + pdf, ButtonType.OK).showAndWait();
        } catch (Exception e) { mostrarError(e); }
    }

    private void borrar() {
        Albaran sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un albarán para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar el albarán " + sel.getNumero() + "?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Albaran> dialogoAlbaran(Albaran a, List<Cliente> clientes) {
        Dialog<Albaran> dlg = new Dialog<>();
        dlg.setTitle(a.getId() == 0 ? "Nuevo albarán" : "Editar albarán " + a.getNumero());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(800);
        dlg.getDialogPane().setPrefHeight(580);

        TabPane tabs = new TabPane();

        // Tab 1: Datos generales
        GridPane gGeneral = new GridPane();
        gGeneral.setHgap(10); gGeneral.setVgap(10); gGeneral.setPadding(new Insets(16));

        ComboBox<Cliente> fCliente = new ComboBox<>(FXCollections.observableArrayList(clientes));
        clientes.stream().filter(c -> c.getId() == a.getClienteId()).findFirst().ifPresent(fCliente::setValue);
        if (fCliente.getValue() == null && !clientes.isEmpty()) fCliente.setValue(clientes.get(0));

        TextField fNumero = tf(a.getNumero()); fNumero.setEditable(false);
        TextField fFecha  = tf(a.getFecha());
        ComboBox<String> fEstado = new ComboBox<>(FXCollections.observableArrayList("pendiente","entregado","firmado"));
        fEstado.setValue(a.getEstado() != null ? a.getEstado() : "pendiente");
        TextArea fObs = new TextArea(nvl(a.getObservaciones())); fObs.setPrefRowCount(3);

        gGeneral.addRow(0, lbl("Número"), fNumero, lbl("Estado"), fEstado);
        gGeneral.addRow(1, lbl("Cliente *"), fCliente, lbl("Fecha entrega"), fFecha);
        gGeneral.add(lbl("Observaciones"), 0, 2); gGeneral.add(fObs, 1, 2, 3, 1);
        tabs.getTabs().add(new Tab("Datos generales", gGeneral));

        // Tab 2: Líneas
        ObservableList<LineaAlbaran> lineas = FXCollections.observableArrayList(a.getLineas());
        tabs.getTabs().add(new Tab("Artículos", buildTablaLineas(lineas)));

        dlg.getDialogPane().setContent(tabs);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            if (fCliente.getValue() == null) { alerta("Selecciona un cliente."); return null; }
            a.setClienteId(fCliente.getValue().getId());
            a.setClienteNombre(fCliente.getValue().getNombreCompleto());
            a.setFecha(fFecha.getText().trim());
            a.setEstado(fEstado.getValue());
            a.setObservaciones(fObs.getText().trim());
            a.setLineas(new java.util.ArrayList<>(lineas));
            return a;
        });
        return dlg.showAndWait();
    }

    private Node buildTablaLineas(ObservableList<LineaAlbaran> lineas) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        TableView<LineaAlbaran> t = new TableView<>(lineas);
        t.setPrefHeight(300);

        TableColumn<LineaAlbaran, String> cDesc = new TableColumn<>("Descripción");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        cDesc.setPrefWidth(400);

        TableColumn<LineaAlbaran, Integer> cCant = new TableColumn<>("Cantidad");
        cCant.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        cCant.setPrefWidth(80);

        TableColumn<LineaAlbaran, String> cUnid = new TableColumn<>("Unidad");
        cUnid.setCellValueFactory(new PropertyValueFactory<>("unidad"));
        cUnid.setPrefWidth(80);

        t.getColumns().addAll(cDesc, cCant, cUnid);

        HBox buttons = new HBox(8);
        Button btnAdd      = btn("+ Añadir",         "#4C9BE8", () -> dialogoLinea(null, lineas));
        Button btnStock    = btn("📦 Desde stock",   "#9B59B6", () -> dialogoDesdeStock(lineas));
        Button btnEdit     = btn("✏ Editar",          "#F39C12", () -> {
            LineaAlbaran sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoLinea(sel, lineas);
        });
        Button btnDel      = btn("🗑 Quitar",         "#E74C3C", () -> {
            LineaAlbaran sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) lineas.remove(sel);
        });
        buttons.getChildren().addAll(btnAdd, btnStock, btnEdit, btnDel);
        box.getChildren().addAll(t, buttons);
        return box;
    }

    private void dialogoLinea(LineaAlbaran linea, ObservableList<LineaAlbaran> lista) {
        boolean esNueva = linea == null;
        if (esNueva) linea = new LineaAlbaran();
        LineaAlbaran l = linea;

        Dialog<LineaAlbaran> dlg = new Dialog<>();
        dlg.setTitle(esNueva ? "Nuevo artículo" : "Editar artículo");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextArea fDesc = new TextArea(nvl(l.getDescripcion())); fDesc.setPrefRowCount(3); fDesc.setPrefWidth(300);
        TextField fCant = tf(l.getCantidad() > 0 ? String.valueOf(l.getCantidad()) : "1");
        ComboBox<String> fUnidad = new ComboBox<>(FXCollections.observableArrayList(
            "ud", "m²", "m", "kg", "L", "caja", "pack", "rollo"));
        fUnidad.setEditable(true);
        fUnidad.setValue(l.getUnidad() != null ? l.getUnidad() : "ud");

        grid.addRow(0, lbl("Descripción *"), fDesc);
        GridPane.setColumnSpan(fDesc, 3);
        grid.addRow(1, lbl("Cantidad"), fCant, lbl("Unidad"), fUnidad);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            l.setDescripcion(fDesc.getText().trim());
            l.setCantidad(parseInt(fCant.getText(), 1));
            l.setUnidad(fUnidad.getValue() != null ? fUnidad.getValue().trim() : "ud");
            return l;
        });

        dlg.showAndWait().ifPresent(result -> {
            if (esNueva) lista.add(result);
        });
    }

    private void dialogoDesdeStock(ObservableList<LineaAlbaran> lineas) {
        List<Material> materiales;
        try { materiales = new MaterialDAO().findAll(); }
        catch (Exception e) { mostrarError(e); return; }
        if (materiales.isEmpty()) { alerta("No hay materiales registrados en el stock."); return; }

        Dialog<LineaAlbaran> dlg = new Dialog<>();
        dlg.setTitle("Añadir material del stock al albarán");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12); grid.setPadding(new Insets(16));

        ComboBox<Material> cbMat = new ComboBox<>(FXCollections.observableArrayList(materiales));
        cbMat.setPromptText("Seleccionar material del stock...");
        cbMat.setPrefWidth(340);

        Label lblInfo = new Label("Selecciona un material para ver su disponibilidad");
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        cbMat.setOnAction(e -> {
            Material m = cbMat.getValue();
            if (m != null) lblInfo.setText(String.format(
                "Stock disponible: %.2f %s  |  Categoría: %s",
                m.getStockActual(),
                m.getUnidad() != null && !m.getUnidad().isBlank() ? m.getUnidad() : "ud",
                m.getCategoria() != null ? m.getCategoria() : "-"));
        });

        Spinner<Integer> spCant = new Spinner<>(1, 999999, 1);
        spCant.setEditable(true);
        spCant.setPrefWidth(100);

        grid.addRow(0, lbl("Material:"), cbMat);
        grid.add(lblInfo, 1, 1);
        grid.addRow(2, lbl("Cantidad:"), spCant);

        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK || cbMat.getValue() == null) return null;
            Material m = cbMat.getValue();
            LineaAlbaran l = new LineaAlbaran();
            l.setDescripcion(m.getNombre() +
                (m.getReferencia() != null && !m.getReferencia().isBlank() ? " [" + m.getReferencia() + "]" : ""));
            l.setCantidad(spCant.getValue());
            l.setUnidad(m.getUnidad() != null && !m.getUnidad().isBlank() ? m.getUnidad() : "ud");
            return l;
        });

        dlg.showAndWait().ifPresent(lineas::add);
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Albaran, T> col(String t, String campo, double ancho) {
        TableColumn<Albaran, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho); return c;
    }

    private Button btn(String t, String color, Runnable r) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private String nvl(String s) { return s != null ? s : ""; }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { SoundService.play(SoundService.Sound.ERROR); new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
