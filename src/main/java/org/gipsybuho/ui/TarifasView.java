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
import org.gipsybuho.dao.TarifaTramoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Tarifa;
import org.gipsybuho.model.TarifaTramo;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ExportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.PDFService;
import org.gipsybuho.service.PdfPreviewService;
import org.gipsybuho.service.SoundService;

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TarifasView extends VBox {

    private static final String[] TECNICAS = {"Serigrafía", "DTF", "Bordado", "Vinilo", "Sublimación", "Gran Formato", "Offset", "Otros"};
    private final TarifaDAO dao;
    private final ObservableList<Tarifa> datos = FXCollections.observableArrayList();
    private final TableView<Tarifa> tabla = new TableView<>(datos);
    private static final Map<String, String> COLUMNAS_BASE = new LinkedHashMap<>();
    static {
        COLUMNAS_BASE.put("tecnica", "Técnica");
        COLUMNAS_BASE.put("nombre", "Nombre");
        COLUMNAS_BASE.put("descripcion", "Descripción");
        COLUMNAS_BASE.put("precio_unit", "Precio/ud.");
        COLUMNAS_BASE.put("precio_setup", "Setup");
        COLUMNAS_BASE.put("minimo_unidades", "Mín. uds.");
        COLUMNAS_BASE.put("activa", "Activa");
        COLUMNAS_BASE.put("usa_tiempo", "Tramos");
        COLUMNAS_BASE.put("updated_at", "Actualizado");
    }
    private final DynamicColumnRuntime<Tarifa> dynamicColumns =
        new DynamicColumnRuntime<>("tarifas", t("nav.tarifas"), COLUMNAS_BASE, tabla, datos, Tarifa::getId);
    private Map<String, TextField> dialogExtraFields = new LinkedHashMap<>();
    private ComboBox<String> cbTecnicaFiltro;
    private boolean updatingTecnicaFiltro;
    private TextField txtBuscar;
    private Label lblContador = new Label();

    public TarifasView() {
        try {
            dao = new TarifaDAO(DatabaseManager.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label titulo = new Label(t("nav.tarifas"));
        titulo.getStyleClass().add("view-title");
        Label sub = new Label(t("tarifas.subtitulo"));
        sub.getStyleClass().add("view-subtitle");

        getChildren().addAll(titulo, sub, buildToolbar(), buildTabla());
        VBox.setVgrow(tabla, Priority.ALWAYS);
        cargar();
        dynamicColumns.apply();
    }

    private HBox buildToolbar() {
        Button btnNuevo    = btn(t("tarifas.btn.nuevo"), this::nueva);
        Button btnEditar   = btn(t("tarifas.btn.editar"), this::editar);
        Button btnBorrar   = btn(t("tarifas.btn.borrar"), this::borrar);
        Button btnTramos   = btn(t("tarifas.btn.tramos"), this::verTramos);
        Button btnImportar   = btn(t("tarifas.btn.importar"), this::importar);
        Button btnExportar   = btn(t("tarifas.btn.exportar"), this::exportar);
        Button btnPreview    = btn(t("tarifas.btn.previsualizar"), this::previsualizar);
        Button btnColumnas   = btn(t("tarifas.btn.columnas"), dynamicColumns::configure);
        txtBuscar = new TextField();
        txtBuscar.setPromptText(t("tarifas.buscar.prompt"));
        txtBuscar.setPrefWidth(220);
        txtBuscar.textProperty().addListener((o, a, b) -> cargar());
        cbTecnicaFiltro = new ComboBox<>();
        cbTecnicaFiltro.setPrefWidth(150);
        cbTecnicaFiltro.setTooltip(new Tooltip(t("tarifas.filtro.tecnica.tip")));
        cbTecnicaFiltro.setOnAction(e -> {
            if (!updatingTecnicaFiltro) cargar();
        });
        btnNuevo.setTooltip(new Tooltip(t("tarifas.btn.nuevo.tip")));
        btnEditar.setTooltip(new Tooltip(t("tarifas.btn.editar.tip")));
        btnBorrar.setTooltip(new Tooltip(t("tarifas.btn.borrar.tip")));
        btnTramos.setTooltip(new Tooltip(t("tarifas.btn.tramos.tip")));
        btnImportar.setTooltip(new Tooltip(t("tarifas.btn.importar.tip")));
        btnExportar.setTooltip(new Tooltip(t("tarifas.btn.exportar.tip")));
        btnPreview.setTooltip(new Tooltip(t("tarifas.btn.previsualizar.tip")));
        btnColumnas.setTooltip(new Tooltip(t("tarifas.btn.columnas.tip")));
        lblContador.getStyleClass().add("row-counter");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(8, cbTecnicaFiltro, txtBuscar, lblContador, sp, btnNuevo, btnEditar, btnBorrar, btnTramos, btnImportar, btnExportar, btnPreview, btnColumnas);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private TableView<Tarifa> buildTabla() {
        tabla.getStyleClass().add("data-table");
        tabla.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<Tarifa, String> colTecnica = new TableColumn<>(t("tarifas.tabla.tecnica"));
        colTecnica.setCellValueFactory(new PropertyValueFactory<>("tecnica"));
        colTecnica.setPrefWidth(120);
        colTecnica.setUserData("tecnica");

        TableColumn<Tarifa, String> colNombre = new TableColumn<>(t("tarifas.tabla.nombre"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(220);
        colNombre.setUserData("nombre");

        TableColumn<Tarifa, Double> colPrecio = new TableColumn<>(t("tarifas.tabla.precio"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnit"));
        colPrecio.setUserData("precio_unit");
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Double> colSetup = new TableColumn<>(t("tarifas.tabla.setup"));
        colSetup.setCellValueFactory(new PropertyValueFactory<>("precioSetup"));
        colSetup.setUserData("precio_setup");
        colSetup.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        TableColumn<Tarifa, Integer> colMin = new TableColumn<>(t("tarifas.tabla.minimo"));
        colMin.setCellValueFactory(new PropertyValueFactory<>("minimoUnidades"));
        colMin.setUserData("minimo_unidades");

        TableColumn<Tarifa, String> colTramos = new TableColumn<>(t("tarifas.tabla.tramos"));
        colTramos.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
            c.getValue().isUsaTiempo() ? t("tarifas.tabla.tramos_si") : "—"));
        colTramos.setPrefWidth(90);
        colTramos.setUserData("usa_tiempo");

        tabla.getColumns().addAll(colTecnica, colNombre, colPrecio, colSetup, colMin, colTramos);
        tabla.setPlaceholder(Icons.emptyState(t("tarifas.tabla.vacio")));
        return tabla;
    }

    private void cargar() {
        try {
            List<Tarifa> lista = dao.findAll();
            actualizarFiltroTecnicas(lista);
            if (cbTecnicaFiltro != null && cbTecnicaFiltro.getValue() != null
                    && !t("tarifas.filtro.todas").equals(cbTecnicaFiltro.getValue())) {
                String tecnica = cbTecnicaFiltro.getValue();
                lista = lista.stream()
                    .filter(tarifa -> tecnica.equals(tarifa.getTecnica()))
                    .toList();
            }
            String q = txtBuscar != null ? txtBuscar.getText().strip().toLowerCase() : "";
            if (!q.isBlank()) lista = lista.stream()
                .filter(tarifa -> contiene(tarifa.getNombre(), q) || contiene(tarifa.getTecnica(), q) || contiene(tarifa.getDescripcion(), q))
                .toList();
            datos.setAll(lista);
            lblContador.setText(tf("tarifas.contador", lista.size()));
            dynamicColumns.apply();
            TableColumnSizing.animarFilas(tabla);
        } catch (Exception e) { mostrarError(e); }
    }

    private void actualizarFiltroTecnicas(List<Tarifa> tarifas) {
        if (cbTecnicaFiltro == null) return;
        String selected = cbTecnicaFiltro.getValue();
        List<String> tecnicas = new ArrayList<>(tarifas.stream()
            .map(Tarifa::getTecnica)
            .filter(tec -> tec != null && !tec.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList());
        tecnicas.add(0, t("tarifas.filtro.todas"));
        updatingTecnicaFiltro = true;
        cbTecnicaFiltro.setItems(FXCollections.observableArrayList(tecnicas));
        cbTecnicaFiltro.setValue(tecnicas.contains(selected) ? selected : t("tarifas.filtro.todas"));
        updatingTecnicaFiltro = false;
    }

    private void nueva() {
        dialogo(new Tarifa()).ifPresent(tarifa -> {
            try { dao.save(tarifa); dynamicColumns.saveFormFields(tarifa, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void editar() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("tarifas.editar.alerta_seleccion")); return; }
        dialogo(sel).ifPresent(tarifa -> {
            try { dao.save(tarifa); dynamicColumns.saveFormFields(tarifa, dialogExtraFields); cargar(); } catch (Exception e) { mostrarError(e); }
        });
    }

    private void borrar() {
        List<Tarifa> seleccionadas = new java.util.ArrayList<>(tabla.getSelectionModel().getSelectedItems());
        if (seleccionadas.isEmpty()) { alerta(t("tarifas.borrar.alerta_seleccion")); return; }
        String mensaje = seleccionadas.size() == 1
            ? tf("tarifas.borrar.confirmar_una", seleccionadas.get(0).getNombre())
            : tf("tarifas.borrar.confirmar_varias", seleccionadas.size());
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            mensaje, ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                for (Tarifa tarifa : seleccionadas) dao.delete(tarifa.getId());
                cargar();
            } catch (Exception e) { mostrarError(e); }
        });
    }

    private Optional<Tarifa> dialogo(Tarifa tarifa) {
        Dialog<Tarifa> dlg = new Dialog<>();
        dlg.setTitle(tarifa.getId() == 0 ? t("tarifas.dialogo.titulo.nueva") : t("tarifas.dialogo.titulo.editar"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));

        ComboBox<String> fTecnica = new ComboBox<>(FXCollections.observableArrayList(TECNICAS));
        fTecnica.setValue(tarifa.getTecnica() != null ? tarifa.getTecnica() : TECNICAS[0]);
        TextField fNombre      = txf(tarifa.getNombre());
        TextField fDescripcion = txf(tarifa.getDescripcion());
        TextField fPrecioUnit  = txf(tarifa.getPrecioUnit() > 0 ? String.valueOf(tarifa.getPrecioUnit()) : "");
        TextField fPrecioSetup = txf(tarifa.getPrecioSetup() > 0 ? String.valueOf(tarifa.getPrecioSetup()) : "0");
        TextField fMinimo      = txf(tarifa.getMinimoUnidades() > 0 ? String.valueOf(tarifa.getMinimoUnidades()) : "1");
        CheckBox chkUsaTiempo = new CheckBox(t("tarifas.dialogo.chk.tiempo"));
        chkUsaTiempo.setSelected(tarifa.isUsaTiempo());
        Label lblInfo = new Label(t("tarifas.dialogo.info_tramos"));
        lblInfo.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        grid.addRow(0, lbl(t("tarifas.dialogo.lbl.tecnica")), fTecnica);
        grid.addRow(1, lbl(t("tarifas.dialogo.lbl.nombre")), fNombre);
        grid.addRow(2, lbl(t("tarifas.dialogo.lbl.descripcion")), fDescripcion);
        grid.addRow(3, lbl(t("tarifas.dialogo.lbl.precio")), fPrecioUnit);
        grid.addRow(4, lbl(t("tarifas.dialogo.lbl.setup")), fPrecioSetup);
        grid.addRow(5, lbl(t("tarifas.dialogo.lbl.minimo")), fMinimo);
        grid.add(chkUsaTiempo, 0, 6, 2, 1);
        grid.add(lblInfo, 0, 7, 2, 1);
        dialogExtraFields = new LinkedHashMap<>();
        dynamicColumns.addFormFields(grid, 8, tarifa, dialogExtraFields);

        dlg.getDialogPane().setContent(grid);

        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(fNombre.getText().isBlank());
        fNombre.textProperty().addListener((o, a, b) -> okBtn.setDisable(b.isBlank()));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                tarifa.setTecnica(fTecnica.getValue());
                tarifa.setNombre(fNombre.getText().trim());
                tarifa.setDescripcion(fDescripcion.getText().trim());
                tarifa.setPrecioUnit(parseDouble(fPrecioUnit.getText()));
                tarifa.setPrecioSetup(parseDouble(fPrecioSetup.getText()));
                tarifa.setMinimoUnidades(parseInt(fMinimo.getText(), 1));
                tarifa.setActiva(true);
                tarifa.setUsaTiempo(chkUsaTiempo.isSelected());
                return tarifa;
            }
            return null;
        });
        return dlg.showAndWait();
    }

    private void verTramos() {
        Tarifa sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(t("tarifas.tramos.alerta_seleccion")); return; }
        if (!sel.isUsaTiempo()) {
            alerta(t("tarifas.tramos.alerta_no_tiempo")); return;
        }
        abrirVentanaTramos(sel);
    }

    private void abrirVentanaTramos(Tarifa tarifa) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(tf("tarifas.tramos.titulo", tarifa.getNombre()));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        ObservableList<TarifaTramo> tramos = FXCollections.observableArrayList();
        TableView<TarifaTramo> tablaTramos = new TableView<>(tramos);

        TableColumn<TarifaTramo, Integer> colTiempo = new TableColumn<>(t("tarifas.tramos.tabla.tiempo"));
        colTiempo.setCellValueFactory(new PropertyValueFactory<>("tiempoMinutos"));
        colTiempo.setPrefWidth(120);

        TableColumn<TarifaTramo, Double> colPrecio = new TableColumn<>(t("tarifas.tramos.tabla.precio"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioTiempo"));
        colPrecio.setPrefWidth(120);
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f €", v));
            }
        });

        tablaTramos.getColumns().addAll(colTiempo, colPrecio);
        tablaTramos.setPlaceholder(new Label(t("tarifas.tramos.tabla.vacio")));

        Runnable recargar = () -> {
            try { tramos.setAll(new TarifaTramoDAO(DatabaseManager.getConnection()).findByTarifaId(tarifa.getId())); } catch (Exception e) { mostrarError(e); }
        };
        recargar.run();

        Button btnAdd = btn(t("tarifas.tramos.btn.anadir"), () -> {
            dialogoTramo(tarifa, new TarifaTramo(tarifa.getId(), 0, 0)).ifPresent(tramo -> {
                try { new TarifaTramoDAO(DatabaseManager.getConnection()).save(tramo); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });
        Button btnEdit = btn(t("tarifas.tramos.btn.editar"), () -> {
            TarifaTramo sel = tablaTramos.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta(t("tarifas.tramos.alerta_sel_editar")); return; }
            dialogoTramo(tarifa, sel).ifPresent(tramo -> {
                try { new TarifaTramoDAO(DatabaseManager.getConnection()).save(tramo); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });
        Button btnDel = btn(t("tarifas.tramos.btn.borrar"), () -> {
            TarifaTramo sel = tablaTramos.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta(t("tarifas.tramos.alerta_sel_borrar")); return; }
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                t("tarifas.tramos.confirmar_borrar"), ButtonType.YES, ButtonType.NO);
            conf.setHeaderText(null);
            conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
                try { new TarifaTramoDAO(DatabaseManager.getConnection()).delete(sel.getId()); recargar.run(); } catch (Exception e) { mostrarError(e); }
            });
        });

        HBox buttons = new HBox(8, btnAdd, btnEdit, btnDel);
        VBox box = new VBox(8, tablaTramos, buttons);
        box.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private Optional<TarifaTramo> dialogoTramo(Tarifa tarifa, TarifaTramo tramo) {
        Dialog<TarifaTramo> dlg = new Dialog<>();
        dlg.setTitle(tramo.getId() == 0 ? t("tarifas.tramos.dialogo.titulo.anadir") : t("tarifas.tramos.dialogo.titulo.editar"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(16));
        TextField fTiempo = txf(tramo.getTiempoMinutos() > 0 ? String.valueOf(tramo.getTiempoMinutos()) : "");
        TextField fPrecio = txf(tramo.getPrecioTiempo() > 0 ? String.valueOf(tramo.getPrecioTiempo()) : "");
        grid.addRow(0, lbl(t("tarifas.tramos.dialogo.lbl.tiempo")), fTiempo);
        grid.addRow(1, lbl(t("tarifas.tramos.dialogo.lbl.precio")), fPrecio);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            int tiempo = parseInt(fTiempo.getText(), 0);
            if (tiempo <= 0 || tiempo % 5 != 0) {
                alerta(t("tarifas.tramos.validacion.tiempo")); return null;
            }
            double precio = parseDouble(fPrecio.getText());
            if (precio <= 0) {
                alerta(t("tarifas.tramos.validacion.precio")); return null;
            }
            tramo.setTarifaId(tarifa.getId());
            tramo.setTiempoMinutos(tiempo);
            tramo.setPrecioTiempo(precio);
            return tramo;
        });
        return dlg.showAndWait();
    }

    private void importar() {
        List<String> css = getScene() != null
            ? new ArrayList<>(getScene().getStylesheets())
            : List.of();
        ModuloWindowManager.abrirEnVentana(
            t("tarifas.importar.titulo"),
            () -> new ImportView(ImportService.TipoEntidad.TARIFAS, this::cargar),
            css
        );
    }

    private void mostrarResultadoImportacion(org.gipsybuho.service.importer.ImportResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Importación completada en %.1f s.%n", r.duracion().toMillis() / 1000.0));
        sb.append(String.format("✓ %d filas importadas%n", r.filasImportadas()));
        sb.append(String.format("✓ %d filas actualizadas%n", r.filasActualizadas()));
        sb.append(String.format("✗ %d filas descartadas", r.filasDescartadas()));
        if (!r.errores().isEmpty()) {
            sb.append("\n\nErrores (primeros 10):");
            r.errores().stream().limit(10).forEach(e ->
                sb.append(String.format("%n  Fila %d — %s: %s",
                    e.numeroFila(), e.campo() != null ? e.campo() : "—", e.mensaje())));
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setTitle(t("tarifas.importar.resultado.titulo"));
        a.setHeaderText(null);
        a.getDialogPane().setPrefWidth(480);
        if (getScene() != null) a.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        a.showAndWait();
    }

    private void exportar() {
        String[][] formatos = {
            {"sqlite", t("export.fmt.sqlite.label"),
                t("export.fmt.sqlite.desc"), "db"},
            {"csv",    t("export.fmt.csv.label"),
                t("tarifas.export.csv.desc"), "csv"},
            {"sql",    t("export.fmt.sql.label"),
                t("tarifas.export.sql.desc"), "sql"},
            {"json",   t("export.fmt.json.label"),
                t("tarifas.export.json.desc"), "json"},
            {"pdf",    t("export.fmt.pdf.label"),
                t("tarifas.export.pdf.desc"), "pdf"},
            {"word",   t("export.fmt.word.label"),
                t("tarifas.export.word.desc"), "docx"},
            {"excel",  t("export.fmt.excel.label"),
                t("tarifas.export.excel.desc"), "xlsx"}
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

        Label lblSelecciona = new Label(t("export.dialog.instruccion"));
        lblSelecciona.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox contenido = new VBox(12, lblSelecciona, opBox);
        contenido.setPadding(new Insets(16));

        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle(t("tarifas.export.titulo"));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (getScene() != null) dlg.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dlg.getDialogPane().setPrefWidth(460);
        dlg.getDialogPane().setContent(contenido);
        ((Button) dlg.getDialogPane().lookupButton(ButtonType.OK)).setText(t("export.dialog.btn"));

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && grupo.getSelectedToggle() != null)
                return (String[]) grupo.getSelectedToggle().getUserData();
            return null;
        });

        dlg.showAndWait().ifPresent(this::lanzarExportacion);
    }

    private void lanzarExportacion(String[] fmt) {
        FileChooser fc = new FileChooser();
        fc.setTitle(tf("export.dialog.guardar", fmt[1]));
        fc.setInitialFileName(t("nav.tarifas") + "_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + fmt[3]);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(tf("tarifas.export.filtro", fmt[3].toUpperCase()), "*." + fmt[3]));
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
                            Tarifa tarifa = selExp.get(0);
                            Path pdf = new PDFService().generarFichaTarifa(tarifa);
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
                    case "excel"  -> ExportService.exportarTarifasExcel(destino, dao.findAll());
                }
                Platform.runLater(() -> {
                    SoundService.play(SoundService.Sound.COMPLETE);
                    setDisable(false);
                    Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        tf("export.exito.mensaje", destino), ButtonType.OK);
                    ok.setTitle(t("export.exito.titulo"));
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
        if (lista.isEmpty()) { alerta(t("tarifas.preview.alerta_vacio")); return; }
        setDisable(true);
        SoundService.play(SoundService.Sound.START);
        Thread.ofVirtual().start(() -> {
            try {
                byte[] pdfBytes; String tituloVentana;
                if (lista.size() == 1) {
                    Tarifa tarifa = lista.get(0);
                    Path pdfPath = new PDFService().generarFichaTarifa(tarifa);
                    pdfBytes = Files.readAllBytes(pdfPath);
                    tituloVentana = tf("tarifas.preview.titulo_una", tarifa.getNombre());
                    Files.deleteIfExists(pdfPath);
                } else {
                    pdfBytes = PdfPreviewService.previsualizarTarifas(lista);
                    tituloVentana = tf("tarifas.preview.titulo_varias", lista.size());
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

    private Button btn(String t, Runnable r) {
        String label = t.replaceFirst("^\\P{L}+", "").strip();
        Button b = new Button(label);
        b.getStyleClass().add("btn-toolbar");
        b.setOnAction(e -> r.run()); return b;
    }

    private TextField txf(String v) { return new TextField(v != null ? v : ""); }
    private Label lbl(String t) { return new Label(t); }
    private double parseDouble(String s) { try { return Double.parseDouble(s.replace(",",".")); } catch(Exception e){return 0;} }
    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch(Exception e){return def;} }
    private boolean contiene(String texto, String q) { return texto != null && texto.toLowerCase().contains(q); }
    private void alerta(String m) { new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK).showAndWait(); }
    private void mostrarError(Exception e) { new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage(), ButtonType.OK).showAndWait(); }
}
