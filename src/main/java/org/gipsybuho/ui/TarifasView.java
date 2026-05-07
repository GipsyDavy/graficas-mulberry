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
import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.model.Tarifa;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportBackupService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
        Button btnNuevo    = btn("+ Nueva tarifa",  "#4C9BE8", this::nueva);
        Button btnEditar   = btn("✏ Editar",         "#F39C12", this::editar);
        Button btnBorrar   = btn("🗑 Borrar",        "#E74C3C", this::borrar);
        Button btnImportar   = btn("📥 Importar",      "#27AE60", this::importar);
        Button btnExportar   = btn("📤 Exportar",      "#8E44AD", this::exportar);
        Button btnActualizar = btn("🔄 Actualizar",    "#1ABC9C", this::cargar);
        Button btnPreview    = btn("👁 Previsualizar", "#6B2D5E", this::previsualizar);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, sp, btnNuevo, btnEditar, btnBorrar, btnImportar, btnExportar, btnActualizar, btnPreview);
        bar.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private TableView<Tarifa> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

    private void importar() {
        String[][] formatos = {
            {"csv",   "📊  CSV",
                "Archivo .csv con cabecera de columnas (separador «;»). Compatible con Excel y LibreOffice.", "csv"},
            {"excel", "📗  Excel",
                "Libro Excel (.xlsx, .xls, .xlsb, .xlsm, .xltx). Hoja 1 = tarifas.", "xlsx"},
            {"sql",   "🗄️  Volcado SQL",
                "Importa tarifas desde un volcado SQL.", "sql"},
            {"json",  "{ }  JSON",
                "Importa tarifas desde un archivo JSON.", "json"},
            {"word",  "📝  Word",
                "Documento Word (.docx/.doc) con tabla de tarifas.", "docx"},
            {"pdf",   "📄  PDF",
                "Documento PDF con tabla de tarifas (columnas separadas por tabulador, «|» o dobles espacios).", "pdf"}
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

        Label lbl = new Label("Selecciona el formato a importar:");
        lbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lbl, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Importar tarifas");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(440);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText("Seleccionar archivo →");

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarImportacion);
    }

    private void lanzarImportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar tarifas — " + fmt[1]);
        switch (fmt[0]) {
            case "excel" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel — Tarifas", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.xltx", "*.xltm"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            case "word" -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word — Tarifas", "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
            default -> fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Tarifas", "*." + fmt[3]),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        }
        File archivo = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path origen = archivo.toPath();
        String tipo = fmt[0];
        SoundService.play(SoundService.Sound.START);

        Thread.ofVirtual().start(() -> {
            try {
                int n = switch (tipo) {
                    case "csv"   -> ImportBackupService.importarTarifasCSV(origen);
                    case "sql"   -> ImportBackupService.importarTarifasSQL(origen);
                    case "json"  -> ImportBackupService.importarTarifasJSON(origen);
                    case "excel" -> ImportBackupService.importarTarifasExcel(origen);
                    case "word"  -> ImportBackupService.importarTarifasWord(origen);
                    case "pdf"   -> ImportBackupService.importarTarifasPDF(origen);
                    default      -> throw new Exception("Formato desconocido: " + tipo);
                };
                int filas = n;
                Platform.runLater(() -> {
                    cargar();
                    SoundService.play(SoundService.Sound.COMPLETE);
                    alerta("Importación completada: " + filas + " registro(s) importado(s).");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.ERROR);
                    mostrarError(e);
                });
            }
        });
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", "💾  Copia de seguridad SQLite",
                "Copia completa y exacta de la base de datos. Ideal para restaurar en otro equipo.", "db"},
            {"csv",    "📊  Exportar a CSV (Excel / LibreOffice)",
                "Tabla de tarifas como hoja de cálculo. Compatible con Excel y LibreOffice.", "csv"},
            {"sql",    "🗄️  Volcado SQL",
                "Script SQL con la estructura y los datos de la tabla tarifas.", "sql"},
            {"json",   "{ }  Exportar a JSON",
                "Datos de todas las tarifas en formato JSON estructurado.", "json"},
            {"pdf",    "📄  Exportar a PDF",
                "Listado de tarifas como tabla en un documento PDF.", "pdf"},
            {"word",   "📝  Exportar a Word",
                "Tabla de tarifas en documento Word (.docx), editable.", "docx"}
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
        dlg.setTitle("Exportar tarifas");
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
        fc.setInitialFileName("Tarifas_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(fmt[3].toUpperCase() + " — Tarifas", "*." + fmt[3]));
        File docs = new File(System.getProperty("user.home"), "Documents");
        if (!docs.exists()) docs = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(docs);

        File archivo = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (archivo == null) return;

        Path destino = archivo.toPath();
        setDisable(true);
        SoundService.play(SoundService.Sound.START);

        List<Tarifa> selExp = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        Thread.ofVirtual().start(() -> {
            try {
                switch (fmt[0]) {
                    case "sqlite" -> ExportService.backupSQLite(destino);
                    case "csv"    -> ExportService.exportarTarifasCSV(destino);
                    case "sql"    -> ExportService.exportarTarifasSQL(destino);
                    case "json"   -> ExportService.exportarTarifasJSON(destino);
                    case "pdf"    -> {
                        if (selExp.size() == 1) {
                            Tarifa t = selExp.get(0);
                            Path pdf = new PDFService().generarFichaTarifa(t);
                            Files.copy(pdf, destino, StandardCopyOption.REPLACE_EXISTING);
                            Files.deleteIfExists(pdf);
                        } else {
                            ExportService.exportarTarifasPDF(destino, dao.findAll());
                        }
                    }
                    case "word"   -> {
                        if (selExp.size() == 1) {
                            ExportService.exportarTarifaDetalladaWord(destino, selExp.get(0));
                        } else {
                            ExportService.exportarTarifasWord(destino, dao.findAll());
                        }
                    }
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

    private void previsualizar() {
        List<Tarifa> sel = new ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        List<Tarifa> lista = sel.isEmpty() ? new ArrayList<>(datos) : sel;
        if (lista.isEmpty()) { alerta("No hay registros para previsualizar."); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Tarifa t = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaTarifa(t);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = "Previsualización — Tarifa " + t.getNombre();
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarTarifas(lista);
                    tituloVentana = "Previsualización — Tarifas (" + lista.size() + " registro(s))";
                }
                final byte[] bytes = pdfBytes; final String titulo = tituloVentana;
                Platform.runLater(() -> {
                    setDisable(false);
                    SoundService.play(SoundService.Sound.COMPLETE);
                    PdfPreviewWindow.mostrar(bytes, titulo);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setDisable(false);
                    SoundService.play(SoundService.Sound.ERROR);
                    mostrarError(ex);
                });
            }
        });
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
