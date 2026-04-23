package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.model.Tarifa;

import java.util.Optional;

public class TarifasView extends VBox {

    private static final String[] TECNICAS = {"Serigrafía", "DTF", "Bordado", "Vinilo", "Sublimación", "Gran Formato", "Offset", "Otros"};
    private final TarifaDAO dao = new TarifaDAO();
    private final ObservableList<Tarifa> datos = FXCollections.observableArrayList();
    private final TableView<Tarifa> tabla = new TableView<>(datos);

    public TarifasView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Tarifas");
        titulo.getStyleClass().add("view-title");
        Label sub = new Label("Precios por técnica de impresión");
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        Button btnNuevo  = btn("+ Nueva tarifa",  "#4C9BE8", this::nueva);
        Button btnEditar = btn("✏ Editar",         "#F39C12", this::editar);
        Button btnBorrar = btn("🗑 Borrar",        "#E74C3C", this::borrar);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnBorrar);
        bar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Tarifa> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Tarifa, String> colTecnica = new TableColumn<>("Técnica");
        colTecnica.setCellValueFactory(new PropertyValueFactory<>("tecnica"));
        colTecnica.setPrefWidth(120);

        TableColumn<Tarifa, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(220);

        TableColumn<Tarifa, Double> colPrecio = new TableColumn<>("Precio/ud.");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Double> colSetup = new TableColumn<>("Setup");
        colSetup.setCellValueFactory(new PropertyValueFactory<>("precioSetup"));
        colSetup.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Integer> colMin = new TableColumn<>("Mín. uds.");
        colMin.setCellValueFactory(new PropertyValueFactory<>("minimoUnidades"));

        tabla.getColumns().addAll(colTecnica, colNombre, colPrecio, colSetup, colMin);
        tabla.setPlaceholder(new Label("No hay tarifas registradas"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void nueva() {
        dialogo(new Tarifa()).ifPresent(t -> {
            try { dao.save(t); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una tarifa para editar."); return; }
        dialogo(sel).ifPresent(t -> {
            try { dao.save(t); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona una tarifa para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar la tarifa \"" + sel.getNombre() + "\"?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Tarifa> dialogo(Tarifa t) {
        Dialog<Tarifa> dlg = new Dialog<>();
        dlg.setTitle(t.getId() == 0 ? "Nueva tarifa" : "Editar tarifa");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        ComboBox<String> fTecnica = new ComboBox<>(FXCollections.observableArrayList(TECNICAS));
        fTecnica.setValue(t.getTecnica() != null ? t.getTecnica() : TECNICAS[0]);
        TextField fNombre      = tf(t.getNombre());
        TextField fDescripcion = tf(t.getDescripcion());
        TextField fPrecioUnit  = tf(t.getPrecioUnit() > 0 ? String.valueOf(t.getPrecioUnit()) : "");
        TextField fPrecioSetup = tf(t.getPrecioSetup() > 0 ? String.valueOf(t.getPrecioSetup()) : "0");
        TextField fMinimo      = tf(t.getMinimoUnidades() > 0 ? String.valueOf(t.getMinimoUnidades()) : "1");

        grid.addRow(0, lbl("Técnica *"), fTecnica);
        grid.addRow(1, lbl("Nombre *"), fNombre);
        grid.addRow(2, lbl("Descripción"), fDescripcion);
        grid.addRow(3, lbl("Precio/ud. (€) *"), fPrecioUnit);
        grid.addRow(4, lbl("Setup (€)"), fPrecioSetup);
        grid.addRow(5, lbl("Mínimo uds."), fMinimo);

        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                t.setTecnica(fTecnica.getValue());
                t.setNombre(fNombre.getText().trim());
                t.setDescripcion(fDescripcion.getText().trim());
                t.setPrecioUnit(parseDouble(fPrecioUnit.getText()));
                t.setPrecioSetup(parseDouble(fPrecioSetup.getText()));
                t.setMinimoUnidades(parseInt(fMinimo.getText(), 1));
                t.setActiva(true);
                return t;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    private Button btn(String texto, String color, Runnable r) {
        Button b = new Button(texto);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 14;");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField tf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
