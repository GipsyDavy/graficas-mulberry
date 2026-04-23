package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.model.Cliente;

import java.util.Optional;

public class ClientesView extends VBox {

    private final ClienteDAO dao = new ClienteDAO();
    private final ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private final TableView<Cliente> tabla = new TableView<>(datos);
    private TextField txtBuscar;

    public ClientesView() {
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label("Clientes");
        titulo.getStyleClass().add("view-title");

        getChildren().addAll(titulo, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
    }

    private HBox buildToolbar() {
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre, NIF o email...");
        txtBuscar.setPrefWidth(280);
        txtBuscar.textProperty().addListener((o, a, b) -> buscar(b));

        Button btnNuevo  = btn("+ Nuevo",   "#4C9BE8", this::nuevo);
        Button btnEditar = btn("✏ Editar",  "#F39C12", this::editar);
        Button btnBorrar = btn("🗑 Borrar", "#E74C3C", this::borrar);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, txtBuscar, spacer, btnNuevo, btnEditar, btnBorrar);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return bar;
    }

    private TableView<Cliente> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tabla.getColumns().addAll(
            col("Nombre", "nombre", 250),
            col("Tipo", "tipo", 80),
            col("NIF/CIF", "nif", 100),
            col("Teléfono", "telefono", 110),
            col("Email", "email", 180),
            col("Ciudad", "ciudad", 120)
        );
        tabla.setPlaceholder(new Label("No hay clientes registrados"));
        return tabla;
    }

    private void cargar() {
        try { datos.setAll(dao.findAll()); } catch (Exception e) { mostrarError(e); }
    }

    private void buscar(String texto) {
        try {
            if (texto.isBlank()) datos.setAll(dao.findAll());
            else datos.setAll(dao.search(texto));
        } catch (Exception e) { mostrarError(e); }
    }

    private void nuevo() {
        dialogo(new Cliente()).ifPresent(c -> {
            try { dao.save(c); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un cliente para editar."); return; }
        dialogo(sel).ifPresent(c -> {
            try { dao.save(c); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        Cliente sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un cliente para borrar."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar el cliente \"" + sel.getNombre() + "\"?", ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar"); conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try { dao.delete(sel.getId()); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Cliente> dialogo(Cliente c) {
        Dialog<Cliente> dlg = new Dialog<>();
        dlg.setTitle(c.getId() == 0 ? "Nuevo cliente" : "Editar cliente");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().addAll(getScene() != null ? getScene().getStylesheets() : java.util.List.of());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre    = tf(c.getNombre());
        ComboBox<String> fTipo = new ComboBox<>(FXCollections.observableArrayList("empresa", "particular"));
        fTipo.setValue(c.getTipo() != null ? c.getTipo() : "empresa");
        TextField fNif       = tf(c.getNif());
        TextField fDireccion = tf(c.getDireccion());
        TextField fCiudad    = tf(c.getCiudad() != null ? c.getCiudad() : "Almería");
        TextField fCp        = tf(c.getCp());
        TextField fTelefono  = tf(c.getTelefono());
        TextField fEmail     = tf(c.getEmail());
        TextArea fNotas      = new TextArea(nvl(c.getNotas()));
        fNotas.setPrefRowCount(3);

        int r = 0;
        grid.addRow(r++, lbl("Nombre *"), fNombre, lbl("Tipo"), fTipo);
        grid.addRow(r++, lbl("NIF/CIF"), fNif, lbl("Teléfono"), fTelefono);
        grid.addRow(r++, lbl("Email"), fEmail, lbl("Ciudad"), fCiudad);
        grid.addRow(r++, lbl("Dirección"), fDireccion, lbl("C.P."), fCp);
        grid.add(lbl("Notas"), 0, r);
        grid.add(fNotas, 1, r, 3, 1);

        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                c.setNombre(fNombre.getText().trim());
                c.setTipo(fTipo.getValue());
                c.setNif(fNif.getText().trim());
                c.setDireccion(fDireccion.getText().trim());
                c.setCiudad(fCiudad.getText().trim());
                c.setCp(fCp.getText().trim());
                c.setTelefono(fTelefono.getText().trim());
                c.setEmail(fEmail.getText().trim());
                c.setNotas(fNotas.getText().trim());
                return c;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Cliente, T> col(String titulo, String campo, double ancho) {
        TableColumn<Cliente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        return c;
    }

    private Button btn(String texto, String color, Runnable accion) {
        Button b = new Button(texto);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 14;");
        b.setOnAction(e -> accion.run());
        return b;
    }

    private TextField tf(String valor) { return new TextField(nvl(valor)); }
    private Label lbl(String t) { return new Label(t); }
    private String nvl(String s) { return s != null ? s : ""; }
    private void alerta(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
