package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.model.Material;

import java.util.Optional;

public class MaterialesView extends VBox {

    private static final String[] CATEGORIAS = {"tintas", "pantallas", "sustratos", "vinilos", "consumibles", "bordado", "gran formato"};
    private final MaterialDAO dao = new MaterialDAO();
    private final ObservableList<Material> datos = FXCollections.observableArrayList();
    private final TableView<Material> tabla = new TableView<>(datos);
    private CheckBox chkSoloAlerta;

    public MaterialesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Control de Materiales");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        chkSoloAlerta = new CheckBox("Solo materiales con stock bajo");
        chkSoloAlerta.setOnAction(e -> cargar());

        Button btnNuevo    = btn("+ Nuevo",         "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",         "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",        "#E74C3C", this::borrar);
        Button btnEntrada  = btn("📥 Entrada",       "#27AE60", this::ajustarEntrada);
        Button btnSalida   = btn("📤 Salida",        "#E67E22", this::ajustarSalida);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, chkSoloAlerta, sp, btnEntrada, btnSalida, btnNuevo, btnEditar, btnBorrar);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    private TableView<Material> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Material, String> colEstado = new TableColumn<>("");
        colEstado.setPrefWidth(30);
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Material m = getTableRow().getItem();
                Circle circle = new Circle(6);
                circle.setFill(m.isBajoStock() ? Color.web("#E74C3C") : Color.web("#27AE60"));
                setGraphic(circle);
            }
        });

        tabla.getColumns().addAll(
            colEstado,
            col("Nombre", "nombre", 200),
            col("Referencia", "referencia", 100),
            col("Categoría", "categoria", 100)
        );

        TableColumn<Material, Double> colStock = new TableColumn<>("Stock actual");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colStock.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                Material m = getTableRow().getItem();
                setText(v + " " + (m != null ? m.getUnidad() : ""));
                setStyle(m != null && m.isBajoStock() ? "-fx-text-fill: #E74C3C; -fx-font-weight: bold;" : "");
            }
        });

        TableColumn<Material, Double> colMin = new TableColumn<>("Stock mín.");
        colMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colMin.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null || getTableRow().getItem() == null) { setText(null); return; }
                setText(v + " " + ((Material)getTableRow().getItem()).getUnidad());
            }
        });

        TableColumn<Material, Double> colPrecio = new TableColumn<>("Precio/ud.");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnidad"));
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tabla.getColumns().addAll(colStock, colMin, colPrecio, col("Proveedor", "proveedor", 140));
        tabla.setPlaceholder(new Label("No hay materiales registrados"));
        return tabla;
    }

    private void cargar() {
        try {
            if (chkSoloAlerta != null && chkSoloAlerta.isSelected())
                datos.setAll(dao.findBajoStock());
            else
                datos.setAll(dao.findAll());
        } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        dialogo(new Material()).ifPresent(m -> {
            try { dao.save(m); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un material para editar."); return; }
        dialogo(sel).ifPresent(m -> {
            try { dao.save(m); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un material para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar el material \"" + sel.getNombre() + "\"?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void ajustarEntrada() { ajustarStock("entrada"); }
    private void ajustarSalida()  { ajustarStock("salida"); }

    private void ajustarStock(String tipo) {
        Material sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un material."); return; }

        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle(tipo.equals("entrada") ? "Entrada de stock" : "Salida de stock");
        dlg.setHeaderText("Material: " + sel.getNombre() + "\nStock actual: " + sel.getStockActual() + " " + sel.getUnidad());
        dlg.setContentText("Cantidad (" + sel.getUnidad() + "):");
        dlg.showAndWait().ifPresent(s -> {
            try {
                double cantidad = Double.parseDouble(s.replace(",", "."));
                dao.ajustarStock(sel.getId(), cantidad, tipo, tipo.equals("entrada") ? "Entrada manual" : "Salida manual");
                cargar();
            } catch (NumberFormatException ex) {
                alerta("Introduce una cantidad válida.");
            } catch (Exception ex) { mostrarError(ex); }
        });
    }

    private Optional<Material> dialogo(Material m) {
        Dialog<Material> dlg = new Dialog<>();
        dlg.setTitle(m.getId() == 0 ? "Nuevo material" : "Editar material");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre     = tf(m.getNombre());
        TextField fRef        = tf(m.getReferencia());
        ComboBox<String> fCat = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS));
        fCat.setValue(m.getCategoria() != null ? m.getCategoria() : CATEGORIAS[0]);
        TextField fStock      = tf(m.getStockActual() > 0 ? String.valueOf(m.getStockActual()) : "0");
        TextField fStockMin   = tf(m.getStockMinimo() > 0 ? String.valueOf(m.getStockMinimo()) : "0");
        TextField fUnidad     = tf(m.getUnidad() != null ? m.getUnidad() : "ud");
        TextField fPrecio     = tf(m.getPrecioUnidad() > 0 ? String.valueOf(m.getPrecioUnidad()) : "0");
        TextField fProveedor  = tf(m.getProveedor());

        grid.addRow(0, lbl("Nombre *"), fNombre, lbl("Referencia"), fRef);
        grid.addRow(1, lbl("Categoría"), fCat, lbl("Unidad"), fUnidad);
        grid.addRow(2, lbl("Stock actual"), fStock, lbl("Stock mínimo"), fStockMin);
        grid.addRow(3, lbl("Precio/ud. (€)"), fPrecio, lbl("Proveedor"), fProveedor);

        dlg.getDialogPane().setContent(grid);

        Node ok = dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> ok.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                m.setNombre(fNombre.getText().trim());
                m.setReferencia(fRef.getText().trim());
                m.setCategoria(fCat.getValue());
                m.setStockActual(parseDouble(fStock.getText()));
                m.setStockMinimo(parseDouble(fStockMin.getText()));
                m.setUnidad(fUnidad.getText().trim());
                m.setPrecioUnidad(parseDouble(fPrecio.getText()));
                m.setProveedor(fProveedor.getText().trim());
                return m;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Material, T> col(String t, String campo, double ancho) {
        TableColumn<Material, T> c = new TableColumn<>(t);
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
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
