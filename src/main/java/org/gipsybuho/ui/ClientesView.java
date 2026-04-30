package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ClientesView extends VBox {

    private final ClienteDAO dao = new ClienteDAO();
    private final ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private final TableView<Cliente> tabla = new TableView<>(datos);

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
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre, apellido, NIF o email...");
        txtBuscar.setPrefWidth(280);
        txtBuscar.textProperty().addListener((o, a, b) -> buscar(b));

        Button btnNuevo    = btn("+ Nuevo",      "#4C9BE8", this::nuevo);
        Button btnEditar   = btn("✏ Editar",    "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",   "#E74C3C", this::borrar);
        Button btnExportar = btn("📤 Exportar", "#8E44AD", this::exportar);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, txtBuscar, spacer, btnNuevo, btnEditar, btnBorrar, btnExportar);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    @SuppressWarnings("unchecked")
    private TableView<Cliente> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tabla.getColumns().addAll(
            col("Nombre", "nombre", 160),
            col("Apellido", "apellidos", 160),
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
        dlg.getDialogPane().setPrefWidth(520);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        TextField fNombre    = tf(c.getNombre());
        TextField fApellido  = tf(c.getApellidos());
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
        grid.addRow(r++, lbl("Nombre *"), fNombre, lbl("Apellidos"), fApellido);
        grid.addRow(r++, lbl("Tipo"), fTipo, lbl("NIF/CIF"), fNif);
        grid.addRow(r++, lbl("Teléfono"), fTelefono, lbl("Email"), fEmail);
        grid.addRow(r++, lbl("Ciudad"), fCiudad, lbl("C.P."), fCp);
        grid.add(lbl("Dirección"), 0, r); grid.add(fDireccion, 1, r, 3, 1);
        r++;
        grid.add(lbl("Notas"), 0, r);
        grid.add(fNotas, 1, r, 3, 1);

        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                c.setNombre(fNombre.getText().trim());
                c.setApellidos(fApellido.getText().trim());
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

    private <T> TableColumn<Cliente, T> col(String titulo, String campo, double ancho) {
        TableColumn<Cliente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new PropertyValueFactory<>(campo));
        c.setPrefWidth(ancho);
        return c;
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de clientes como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de la tabla clientes.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todos los clientes en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de clientes como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de clientes en documento Word (.docx), editable.", "docx"}
        };

        ToggleGroup grupo = new ToggleGroup();
        VBox opBox = new VBox(4);
        for (String[] f : formatos) {
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(grupo);
            rb.setUserData(f);

            Label nombre = new Label(f[1]);
            nombre.setStyle("-fx-font-weight:bold; -fx-font-size:12px;");
            Label desc = new Label(f[2]);
            desc.setStyle("-fx-font-size:11px; -fx-text-fill:-c-text-muted;");

            VBox texto = new VBox(2, nombre, desc);
            HBox fila = new HBox(10, rb, texto);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new Insets(7, 12, 7, 12));
            fila.setStyle("-fx-background-radius:6; -fx-cursor:hand;");
            fila.setOnMouseClicked(e -> rb.setSelected(true));
            opBox.getChildren().add(fila);
        }
        grupo.getToggles().get(0).setSelected(true);

        Label lblSelecciona = new Label("Selecciona el formato de exportación:");
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Exportar clientes");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(460);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("Exportar →");

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarExportacion);
    }

    private void lanzarExportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar exportación — " + fmt[1]);
        fc.setInitialFileName("Clientes_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Clientes", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarClientesCSV(destino);
                    case "sql"    -> ExportService.exportarClientesSQL(destino);
                    case "json"   -> ExportService.exportarClientesJSON(destino);
                    case "pdf"    -> ExportService.exportarClientesPDF(destino, dao.findAll());
                    case "word"   -> ExportService.exportarClientesWord(destino, dao.findAll());
                }
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Exportación completada:\n" + destino, ButtonType.OK);
                    ok.setTitle("Exportación completada");
                    ok.setHeaderText(null);
                    if (getScene() != null) ok.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
                    ok.showAndWait();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    setDisable(false);
                    mostrarError(e);
                });
            }
        });
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
