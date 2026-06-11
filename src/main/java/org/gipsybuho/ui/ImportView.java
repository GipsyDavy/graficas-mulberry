package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Tarifa;
import org.gipsybuho.service.EntityImportService;
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.ImportService.ImportResult;
import org.gipsybuho.service.ImportService.TipoEntidad;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.ValidationIssue;
import org.gipsybuho.service.importer.EntityImportSpec;
import org.gipsybuho.service.importer.FieldSpec;

import java.io.File;
import java.util.*;

public class ImportView extends VBox {

    private final ImportService svc = new ImportService();
    private final TipoEntidad tipoInicial;

    // State
    private File archivoSeleccionado;
    private ImportResult resultadoParseo;
    private TipoEntidad tipoSeleccionado;
    private Map<String, String> mappingActual = new LinkedHashMap<>();
    private ImportService.ImportConfig importConfig = null;
    private final Runnable onImportComplete;

    // UI panels (one per step)
    private final VBox panelPasos = new VBox(0);
    private final ScrollPane panelScroll = new ScrollPane(panelPasos);
    private final TextArea logArea = new TextArea();
    private GroupingConfig groupingConfig = null;

    // Step indicators
    private final Label[] stepLabels = new Label[4];

    private record GroupingConfig(String field, String fixedValue) {}

    public ImportView() {
        this(null, null);
    }

    public ImportView(Runnable onImportComplete) {
        this(null, onImportComplete);
    }

    public ImportView(TipoEntidad tipoInicial, Runnable onImportComplete) {
        this.tipoInicial = tipoInicial;
        this.onImportComplete = onImportComplete;
        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(14);

        Label titulo = new Label("Importación de datos con IA");
        titulo.getStyleClass().add("view-title");

        Label subtitulo = new Label(
            "Importa clientes, materiales, empleados o tarifas desde CSV, Excel (.xlsx/.xls) o JSON. " +
            "La IA local detecta el tipo de datos y mapea los campos automáticamente.");
        subtitulo.setWrapText(true);
        subtitulo.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:13px;");

        panelScroll.setFitToWidth(true);
        panelScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        panelScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        panelScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        getChildren().addAll(titulo, subtitulo, buildIndicadorPasos(), new Separator(), panelScroll, buildLog());
        VBox.setVgrow(panelScroll, Priority.ALWAYS);

        mostrarPaso1();
    }

    // ── Step indicator ────────────────────────────────────────────────────────

    private HBox buildIndicadorPasos() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        String[] nombres = {"1. Seleccionar archivo", "2. Detectar tipo", "3. Mapear campos", "4. Importar"};
        for (int i = 0; i < 4; i++) {
            Label l = new Label(nombres[i]);
            l.setPadding(new Insets(6, 16, 6, 16));
            l.setStyle("-fx-background-color:-c-tab-bg;-fx-text-fill:-c-text-muted;-fx-font-size:12px;");
            stepLabels[i] = l;
            bar.getChildren().add(l);
            if (i < 3) {
                Label arr = new Label("›");
                arr.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:18px;-fx-padding:0 4;");
                bar.getChildren().add(arr);
            }
        }
        activarPaso(0);
        return bar;
    }

    private void activarPaso(int idx) {
        for (int i = 0; i < 4; i++) {
            if (i == idx) {
                stepLabels[i].setStyle(
                    "-fx-background-color:-c-primary;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:12px;-fx-background-radius:4;-fx-padding:6 16;");
            } else if (i < idx) {
                stepLabels[i].setStyle(
                    "-fx-background-color:#27AE60;-fx-text-fill:white;" +
                    "-fx-font-size:12px;-fx-background-radius:4;-fx-padding:6 16;");
            } else {
                stepLabels[i].setStyle(
                    "-fx-background-color:-c-tab-bg;-fx-text-fill:-c-text-muted;" +
                    "-fx-font-size:12px;-fx-padding:6 16;");
            }
        }
    }

    // ── Log area ──────────────────────────────────────────────────────────────

    private VBox buildLog() {
        Label lbl = new Label("Registro");
        lbl.setStyle("-fx-font-weight:bold;");
        Button btnClear = new Button("Limpiar");
        btnClear.setOnAction(e -> logArea.clear());
        btnClear.setStyle("-fx-padding:3 8;-fx-background-radius:4;");
        HBox header = new HBox(lbl, new Region(), btnClear);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(5);
        logArea.setStyle("-fx-font-family:monospace;-fx-font-size:11px;");
        logArea.setPromptText("El progreso de la importación aparecerá aquí…");
        return new VBox(5, header, logArea);
    }

    private void log(String msg) {
        String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            logArea.appendText("[" + hora + "] " + msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // ── PASO 1: Select file ───────────────────────────────────────────────────

    private void mostrarPaso1() {
        activarPaso(0);
        panelPasos.getChildren().setAll(buildPaso1());
    }

    private VBox buildPaso1() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Selecciona el archivo a importar");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        Label info = new Label("Formatos admitidos: CSV (.csv, .txt), Excel (.xlsx, .xls), JSON (.json)");
        info.setStyle("-fx-text-fill:-c-text-muted;");

        Label lblArchivo = new Label("Ningún archivo seleccionado");
        lblArchivo.setStyle("-fx-text-fill:-c-text-muted;-fx-font-style:italic;");

        Button btnElegir = btnColor("📂  Elegir archivo…", "#4C9BE8");
        btnElegir.setPrefWidth(220);
        btnElegir.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar archivo de datos");
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos de datos", "*.csv", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm", "*.json", "*.txt"),
                new FileChooser.ExtensionFilter("CSV", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls", "*.xlsb", "*.xlsm"),
                new FileChooser.ExtensionFilter("JSON", "*.json"),
                new FileChooser.ExtensionFilter("Todos", "*.*")
            );
            File f = fc.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (f != null) {
                archivoSeleccionado = f;
                lblArchivo.setText("✅  " + f.getName() + "  (" + f.length() / 1024 + " KB)");
                lblArchivo.setStyle("-fx-text-fill:-c-text;");
            }
        });

        Button btnSiguiente = btnColor("Siguiente →", "#27AE60");
        btnSiguiente.setPrefWidth(160);
        btnSiguiente.setOnAction(e -> {
            if (archivoSeleccionado == null) { alerta("Selecciona un archivo primero."); return; }
            try {
                log("📂 Leyendo " + archivoSeleccionado.getName() + "…");
                resultadoParseo = svc.parseFile(archivoSeleccionado);
                log("✅ " + resultadoParseo.rows.size() + " filas, " + resultadoParseo.headers.size() +
                    " columnas detectadas. Formato: " + resultadoParseo.formato);
                mostrarPaso2();
            } catch (Exception ex) {
                log("❌ Error al leer el archivo: " + ex.getMessage());
                alerta("No se pudo leer el archivo:\n" + ex.getMessage());
            }
        });

        // Format tips
        VBox tips = new VBox(4,
            tipLabel("CSV: La primera fila debe contener los nombres de columnas. Separador: coma, punto y coma o tabulador."),
            tipLabel("Excel: Se leerá la primera hoja. La primera fila debe tener los encabezados."),
            tipLabel("JSON: Array de objetos o un objeto con un campo que contiene el array.")
        );

        HBox botonesRow = new HBox(12, btnElegir, btnSiguiente);
        botonesRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(titulo, info, botonesRow, lblArchivo, new Separator(), tips);
        return box;
    }

    private Label tipLabel(String t) {
        Label l = new Label("💡  " + t);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");
        return l;
    }

    // ── PASO 2: Detect entity type with AI ────────────────────────────────────

    private void mostrarPaso2() {
        activarPaso(1);
        panelPasos.getChildren().setAll(buildPaso2());
    }

    private VBox buildPaso2() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Detectar tipo de datos");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        // Columns detected
        Label lblCols = new Label("Columnas encontradas: " + String.join(", ", resultadoParseo.headers));
        lblCols.setWrapText(true);
        lblCols.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        ComboBox<TipoEntidad> cbTipo = new ComboBox<>(
            FXCollections.observableArrayList(TipoEntidad.values()));
        cbTipo.setPrefWidth(200);
        if (tipoInicial != null) {
            cbTipo.setValue(tipoInicial);
            cbTipo.setDisable(true);
        }

        Label lblIAEstado = new Label("");
        lblIAEstado.setStyle("-fx-text-fill:-c-text-muted;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(24, 24);
        spinner.setVisible(false);

        HBox statusRow = new HBox(8, spinner, lblIAEstado);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button btnIA = btnColor("🤖  Preguntar a la IA", "#9B59B6");
        btnIA.setDisable(tipoInicial != null);
        btnIA.setOnAction(e -> {
            if (!svc.isOllamaDisponible()) {
                lblIAEstado.setText("❌ Ollama no está disponible. Selecciona el tipo manualmente.");
                lblIAEstado.setStyle("-fx-text-fill:#E74C3C;");
                return;
            }
            btnIA.setDisable(true);
            spinner.setVisible(true);
            lblIAEstado.setText("Analizando columnas…");
            lblIAEstado.setStyle("-fx-text-fill:-c-text-muted;");
            log("🤖 Consultando a la IA para detectar tipo de entidad…");

            Thread.ofVirtual().start(() -> {
                TipoEntidad sugerido = svc.sugerirTipoEntidad(resultadoParseo.headers);
                Platform.runLater(() -> {
                    cbTipo.setValue(sugerido);
                    lblIAEstado.setText("✅ La IA sugiere: " + sugerido.label);
                    lblIAEstado.setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;");
                    spinner.setVisible(false);
                    btnIA.setDisable(false);
                    log("🤖 IA detectó: " + sugerido.label);
                });
            });
        });

        Button btnVolver = btnColor("← Volver", "#95A5A6");
        btnVolver.setOnAction(e -> mostrarPaso1());

        Button btnSiguiente = btnColor("Siguiente →", "#27AE60");
        btnSiguiente.setOnAction(e -> {
            if (cbTipo.getValue() == null) { alerta("Selecciona o detecta el tipo de datos."); return; }
            tipoSeleccionado = cbTipo.getValue();
            mostrarPaso3();
        });

        // Manual type selection hint
        Label hint = new Label("Puedes dejar que la IA lo detecte automáticamente o seleccionar manualmente:");
        hint.setStyle("-fx-text-fill:-c-text-muted;");

        HBox tipoRow = new HBox(12, cbTipo, btnIA);
        tipoRow.setAlignment(Pos.CENTER_LEFT);

        HBox botonesRow = new HBox(12, btnVolver, btnSiguiente);
        botonesRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(titulo, lblCols, new Separator(), hint, tipoRow, statusRow, new Separator(), botonesRow);

        // Auto-trigger IA if Ollama is available and the module did not fix the entity type.
        if (tipoInicial != null) {
            lblIAEstado.setText("Tipo fijado desde el módulo: " + tipoInicial.label + ".");
            lblIAEstado.setStyle("-fx-text-fill:-c-text-muted;");
        } else if (svc.isOllamaDisponible()) {
            btnIA.fire();
        } else {
            lblIAEstado.setText("⚠ Ollama no detectado. Selecciona el tipo manualmente.");
            lblIAEstado.setStyle("-fx-text-fill:#F39C12;");
            cbTipo.setValue(TipoEntidad.CLIENTES);
        }

        return box;
    }

    // ── PASO 3: Field mapping ─────────────────────────────────────────────────

    private void mostrarPaso3() {
        activarPaso(2);
        importConfig = null;
        panelPasos.getChildren().setAll(buildPaso3Cargando());
        log("🤖 Mapeando campos con IA para " + tipoSeleccionado.label + "…");

        Thread.ofVirtual().start(() -> {
            Map<String, String> mapping = svc.mapearCampos(tipoSeleccionado, resultadoParseo.headers);
            Platform.runLater(() -> {
                mappingActual = mapping;
                int mapeados = (int) mapping.values().stream().filter(Objects::nonNull).count();
                log("✅ IA mapeó " + mapeados + "/" + resultadoParseo.headers.size() + " columnas.");
                panelPasos.getChildren().setAll(buildPaso3(mapping));
            });
        });
    }

    private VBox buildPaso3Cargando() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30, 0, 0, 0));
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        Label lbl = new Label("La IA está analizando y mapeando los campos…");
        lbl.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:14px;");
        box.getChildren().addAll(spinner, lbl);
        return box;
    }

    private VBox buildPaso3(Map<String, String> mapping) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Revisión y ajuste del mapeo de campos");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        Label desc = new Label(
            "Revisa cómo la IA ha mapeado las columnas del archivo a los campos de " + tipoSeleccionado.label +
            ". Puedes ajustar manualmente cualquier asignación.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:-c-text-muted;");

        // Mapping table
        TableView<MappingRow> tablaMappeo = buildTablaMappeo(mapping);

        // ── Pivot section ─────────────────────────────────────────────────────
        List<CheckBox> pivotChecks = new ArrayList<>();
        VBox colsBox = new VBox(4);
        for (MappingRow mr : tablaMappeo.getItems()) {
            CheckBox cb = new CheckBox(mr.columna);
            cb.setStyle("-fx-text-fill:-c-text;");
            cb.setUserData(mr.columna);
            cb.setSelected(mr.getCampoDestino() == null); // pre-select unmapped columns
            pivotChecks.add(cb);
            colsBox.getChildren().add(cb);
        }
        ScrollPane scrollCols = new ScrollPane(colsBox);
        scrollCols.setFitToWidth(true);
        scrollCols.setPrefHeight(130);
        scrollCols.setStyle("-fx-background-color:-c-card-bg;");

        List<String> camposDisponibles = new ArrayList<>(tipoSeleccionado.campos);
        ComboBox<String> cbPivotLabel = new ComboBox<>(FXCollections.observableArrayList(camposDisponibles));
        cbPivotLabel.setPrefWidth(200);
        cbPivotLabel.setPromptText("campo para nombre de columna");
        if (tipoSeleccionado.campos.contains("tecnica")) cbPivotLabel.setValue("tecnica");

        ComboBox<String> cbPivotValor = new ComboBox<>(FXCollections.observableArrayList(camposDisponibles));
        cbPivotValor.setPrefWidth(200);
        cbPivotValor.setPromptText("campo para valor de celda");
        tipoSeleccionado.campos.stream()
            .filter(c -> c.startsWith("precio"))
            .findFirst().ifPresent(cbPivotValor::setValue);

        HBox rowLabelPivot = new HBox(12, labelMin("El nombre de columna va al campo:", 230), cbPivotLabel);
        rowLabelPivot.setAlignment(Pos.CENTER_LEFT);
        HBox rowValorPivot = new HBox(12, labelMin("El valor de la celda va al campo:", 230), cbPivotValor);
        rowValorPivot.setAlignment(Pos.CENTER_LEFT);

        Label pivotHelp = new Label("Columnas que generarán un registro cada una (marca las de precio/valor):");
        pivotHelp.setStyle("-fx-text-fill:-c-text;");
        VBox pivotContent = new VBox(8, pivotHelp, scrollCols, rowLabelPivot, rowValorPivot);
        pivotContent.setStyle("-fx-text-fill:-c-text;");
        pivotContent.setPadding(new Insets(6, 0, 0, 18));
        pivotContent.setVisible(false);
        pivotContent.setManaged(false);

        CheckBox cbExpandido = new CheckBox("Modo expandido: generar múltiples registros por fila (ej: columnas de precio)");
        cbExpandido.setStyle("-fx-font-weight:bold;-fx-text-fill:-c-text;");
        cbExpandido.setOnAction(e -> {
            pivotContent.setVisible(cbExpandido.isSelected());
            pivotContent.setManaged(cbExpandido.isSelected());
        });

        VBox sectionPivot = new VBox(8, new Separator(), cbExpandido, pivotContent);
        sectionPivot.setPadding(new Insets(4, 0, 0, 0));
        // ── End pivot section ─────────────────────────────────────────────────

        CheckBox cbGrupoFijo = new CheckBox();
        TextField tfGrupoFijo = new TextField();
        VBox sectionGrouping = buildGroupingSection(cbGrupoFijo, tfGrupoFijo);

        // Summary
        long mapeados = mapping.values().stream().filter(Objects::nonNull).count();
        Label resumen = new Label("✅ " + mapeados + " campos mapeados de " + mapping.size() + " columnas. " +
            "Las columnas sin mapeo se ignorarán.");
        resumen.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        Button btnNuevoCampo = btnColor("Nuevo campo…", "#27AE60");
        String tableName = specFor(tipoSeleccionado).tableName();
        btnNuevoCampo.setVisible(tableName != null);
        btnNuevoCampo.setManaged(tableName != null);
        btnNuevoCampo.setOnAction(e -> {
            for (MappingRow row : tablaMappeo.getItems()) {
                mappingActual.put(row.columna, row.getCampoDestino());
            }
            crearCampoDinamico(tableName);
        });

        Button btnVolver = btnColor("← Volver", "#95A5A6");
        btnVolver.setOnAction(e -> mostrarPaso2());

        Button btnSiguiente = btnColor("Siguiente →", "#27AE60");
        btnSiguiente.setOnAction(e -> {
            for (MappingRow row : tablaMappeo.getItems()) {
                mappingActual.put(row.columna, row.getCampoDestino());
            }
            groupingConfig = null;
            if (cbGrupoFijo.isSelected()) {
                String groupField = groupingField(tipoSeleccionado);
                String groupValue = tfGrupoFijo.getText() == null ? "" : tfGrupoFijo.getText().trim();
                if (groupField == null) {
                    alerta("La agrupación fija solo está disponible para Materiales y Tarifas.");
                    return;
                }
                if (groupValue.isBlank()) {
                    alerta("Introduce el nombre del grupo para aplicarlo a la importación.");
                    return;
                }
                groupingConfig = new GroupingConfig(groupField, groupValue);
            }
            if (cbExpandido.isSelected()) {
                List<String> pivotCols = pivotChecks.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (String) cb.getUserData())
                    .toList();
                String labelField = cbPivotLabel.getValue();
                String valorField = cbPivotValor.getValue();
                if (pivotCols.isEmpty() || labelField == null || valorField == null) {
                    alerta("Selecciona al menos una columna de valor y los dos campos destino para el modo expandido.");
                    return;
                }
                if (labelField.equals(valorField)) {
                    alerta("El campo para el nombre y el campo para el valor no pueden ser el mismo.");
                    return;
                }
                List<String> mappedPivotCols = tablaMappeo.getItems().stream()
                    .filter(row -> pivotCols.contains(row.columna) && row.getCampoDestino() != null)
                    .map(row -> row.columna)
                    .toList();
                if (!mappedPivotCols.isEmpty()) {
                    alerta("Estas columnas ya están mapeadas como campos fijos y no pueden usarse como columnas de valor: "
                        + String.join(", ", mappedPivotCols)
                        + ". Déjalas sin seleccionar en el modo expandido o cambia su campo destino a '(ignorar)'.");
                    return;
                }
                if (groupingConfig != null &&
                        (groupingConfig.field().equals(labelField) || groupingConfig.field().equals(valorField))) {
                    alerta("El grupo fijo no puede usar el mismo campo que el modo expandido.");
                    return;
                }
                Map<String, String> fixedMapping = new LinkedHashMap<>(mappingActual);
                pivotCols.forEach(fixedMapping::remove);
                fixedMapping.entrySet().removeIf(entry -> entry.getValue() == null);
                importConfig = new ImportService.ImportConfig(fixedMapping, pivotCols, labelField, valorField);
            } else {
                importConfig = new ImportService.ImportConfig(mappingActual);
            }
            mostrarPaso35();
        });

        HBox botonesRow = new HBox(12, btnVolver, btnNuevoCampo, btnSiguiente);
        botonesRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(titulo, desc, tablaMappeo, sectionPivot, sectionGrouping, resumen, new Separator(), botonesRow);
        return box;
    }

    private VBox buildGroupingSection(CheckBox cbGrupoFijo, TextField tfGrupoFijo) {
        String field = groupingField(tipoSeleccionado);
        VBox section = new VBox(8);
        section.setPadding(new Insets(4, 0, 0, 0));
        if (field == null) {
            section.setVisible(false);
            section.setManaged(false);
            return section;
        }

        String label = tipoSeleccionado == TipoEntidad.MATERIALES ? "categoría" : "técnica";
        Label title = new Label("Agrupación");
        title.setStyle("-fx-font-weight:bold;-fx-text-fill:-c-text;");
        Label help = new Label(
            "Puedes mapear una columna del archivo al campo '" + field + "' o aplicar una " +
            label + " fija a todos los registros de esta importación.");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        cbGrupoFijo.setText("Aplicar " + label + " fija a toda la importación");
        cbGrupoFijo.setStyle("-fx-text-fill:-c-text;");
        tfGrupoFijo.setPromptText(tipoSeleccionado == TipoEntidad.MATERIALES ? "Ej: Papeles" : "Ej: Tarjetas de visita");
        tfGrupoFijo.setPrefWidth(260);
        tfGrupoFijo.setDisable(true);
        cbGrupoFijo.setOnAction(e -> tfGrupoFijo.setDisable(!cbGrupoFijo.isSelected()));

        HBox fixedRow = new HBox(10, cbGrupoFijo, tfGrupoFijo);
        fixedRow.setAlignment(Pos.CENTER_LEFT);
        section.getChildren().addAll(new Separator(), title, help, fixedRow);
        return section;
    }

    private TableView<MappingRow> buildTablaMappeo(Map<String, String> mapping) {
        List<String> opcionesDestino = new ArrayList<>();
        opcionesDestino.add("(ignorar)");
        opcionesDestino.addAll(tipoSeleccionado.campos);
        opcionesDestino.addAll(dynamicFieldKeys(specFor(tipoSeleccionado)));

        ObservableList<MappingRow> items = FXCollections.observableArrayList();
        for (var entry : mapping.entrySet()) {
            items.add(new MappingRow(entry.getKey(), entry.getValue(), opcionesDestino));
        }

        TableView<MappingRow> tabla = new TableView<>(items);
        tabla.setPrefHeight(280);
        tabla.setMinHeight(220);
        tabla.setMaxHeight(320);
        tabla.setEditable(true);
        tabla.getStyleClass().add("data-table");
        TableColumnSizing.enableHorizontalScroll(tabla);

        // Column: file column name
        TableColumn<MappingRow, String> colArchivo = new TableColumn<>("Columna del archivo");
        colArchivo.setCellValueFactory(new PropertyValueFactory<>("columna"));
        colArchivo.setEditable(false);
        colArchivo.setStyle("-fx-font-weight:bold;");
        colArchivo.setPrefWidth(220);

        // Column: destination field (editable ComboBox)
        TableColumn<MappingRow, String> colDestino = new TableColumn<>("Campo destino (editable)");
        colDestino.setCellValueFactory(new PropertyValueFactory<>("campoDestino"));
        colDestino.setCellFactory(tc -> new ComboBoxTableCell(opcionesDestino));
        colDestino.setOnEditCommit(event -> event.getRowValue().setCampoDestino(event.getNewValue()));
        colDestino.setPrefWidth(240);

        // Column: preview value
        TableColumn<MappingRow, String> colEjemplo = new TableColumn<>("Valor de ejemplo");
        colEjemplo.setCellValueFactory(cd -> {
            String col = cd.getValue().columna;
            String ejemplo = resultadoParseo.rows.isEmpty() ? "" :
                resultadoParseo.rows.get(0).getOrDefault(col, "");
            return new javafx.beans.property.SimpleStringProperty(ejemplo);
        });
        colEjemplo.setPrefWidth(240);

        // Status column
        TableColumn<MappingRow, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd -> {
            String dest = cd.getValue().getCampoDestino();
            boolean mapeado = dest != null && !dest.equals("(ignorar)");
            return new javafx.beans.property.SimpleStringProperty(mapeado ? "✅" : "⬜ ignorado");
        });
        colEstado.setPrefWidth(90);

        tabla.getColumns().addAll(colArchivo, colDestino, colEjemplo, colEstado);
        TableColumnSizing.autoSizeLater(tabla, 90, 520, 60);
        return tabla;
    }

    private List<String> dynamicFieldKeys(EntityImportSpec spec) {
        if (spec.tableName() == null) return List.of();
        try {
            Set<String> baseKeys = spec.campos().stream()
                .map(FieldSpec::clave)
                .collect(java.util.stream.Collectors.toSet());
            return new ColumnConfigDAO().findVisibleDynamic(spec.tableName(), baseKeys).stream()
                .map(ColumnConfigDAO.ColumnConfig::columnName)
                .toList();
        } catch (Exception ex) {
            log("⚠ No se pudieron cargar campos dinámicos: " + ex.getMessage());
            return List.of();
        }
    }

    private void crearCampoDinamico(String tableName) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crear campo nuevo");
        dialog.setHeaderText("Nombre visible del nuevo campo");
        dialog.setContentText("Etiqueta:");
        if (getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        dialog.showAndWait().ifPresent(label -> {
            if (label == null || label.isBlank()) return;
            try {
                Set<String> baseKeys = specFor(tipoSeleccionado).campos().stream()
                    .map(FieldSpec::clave)
                    .collect(java.util.stream.Collectors.toSet());
                ColumnConfigDAO.ColumnConfig config =
                    new ColumnConfigDAO().addDynamicColumn(tableName, label, baseKeys);
                log("✅ Campo dinámico creado: " + config.label() + " (" + config.columnName() + ")");
                panelPasos.getChildren().setAll(buildPaso3(new LinkedHashMap<>(mappingActual)));
            } catch (Exception ex) {
                alerta("No se pudo crear el campo:\n" + ex.getMessage());
            }
        });
    }

    // MappingRow model for the mapping table
    public static class MappingRow {
        public final String columna;
        private String campoDestino;
        private final List<String> opciones;

        public MappingRow(String columna, String campoDestino, List<String> opciones) {
            this.columna = columna;
            this.campoDestino = campoDestino != null ? campoDestino : "(ignorar)";
            this.opciones = opciones;
        }

        public String getColumna() { return columna; }
        public String getCampoDestino() { return "(ignorar)".equals(campoDestino) ? null : campoDestino; }
        public void setCampoDestino(String v) { this.campoDestino = v != null ? v : "(ignorar)"; }
    }

    // Custom ComboBox cell for the mapping table
    private static class ComboBoxTableCell extends TableCell<MappingRow, String> {
        private final ComboBox<String> combo;
        private boolean syncing = false;

        ComboBoxTableCell(List<String> options) {
            combo = new ComboBox<>(FXCollections.observableArrayList(options));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setOnAction(e -> {
                if (syncing) return;
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().setCampoDestino(combo.getValue());
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                return;
            }
            // Leer del modelo para evitar estado stale al reciclar celdas
            MappingRow row = getTableRow() != null ? getTableRow().getItem() : null;
            String dest = row != null
                ? (row.getCampoDestino() != null ? row.getCampoDestino() : "(ignorar)")
                : (item != null ? item : "(ignorar)");
            syncing = true;
            combo.setValue(dest);
            syncing = false;
            setGraphic(combo);
            setText(null);
        }
    }

    // ── PASO 3.5: AI validation ───────────────────────────────────────────────

    private void mostrarPaso35() {
        activarPaso(2);
        panelPasos.getChildren().setAll(buildPaso35Cargando());

        Map<String, String> mappingParaValidar = importConfig != null
            ? importConfig.mapping
            : new LinkedHashMap<>(mappingActual);

        Thread.ofVirtual().start(() -> {
            List<ValidationIssue> issues;
            try {
                issues = svc.validateImportData(resultadoParseo.rows, mappingParaValidar, tipoSeleccionado);
            } catch (Exception ex) {
                issues = List.of();
                System.err.println("Validation step failed: " + ex.getMessage());
            }
            final List<ValidationIssue> finalIssues = issues;
            Platform.runLater(() -> panelPasos.getChildren().setAll(buildPaso35(finalIssues)));
        });
    }

    private VBox buildPaso35Cargando() {
        VBox box = new VBox(16);
        box.setPadding(new Insets(32, 0, 0, 0));
        box.setAlignment(Pos.CENTER);

        Label titulo = new Label("Validación de datos con IA");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);

        Label msg = new Label("Analizando los datos con IA local…");
        msg.setStyle("-fx-text-fill:-c-text-muted;");

        box.getChildren().addAll(titulo, spinner, msg);
        return box;
    }

    private VBox buildPaso35(List<ValidationIssue> issues) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Validación de datos con IA");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        ObservableList<ValidationIssue> issueList = FXCollections.observableArrayList(issues);

        Map<String, String> mappingParaValidar = importConfig != null
            ? importConfig.mapping
            : new LinkedHashMap<>(mappingActual);

        // Declare buttons here so cell factory can reference btnContinuar
        Button btnVolver = btnColor("← Volver", "#95A5A6");
        btnVolver.setOnAction(e -> mostrarPaso3());

        Button btnContinuar = btnColor("Continuar →", "#27AE60");
        btnContinuar.setDefaultButton(true);
        updateContinuarButton(btnContinuar, issueList);
        btnContinuar.setOnAction(e -> mostrarPaso4());

        if (issues.isEmpty()) {
            Label ok = new Label("✅  No se detectaron problemas en los datos.");
            ok.setStyle("-fx-text-fill:#27AE60;-fx-font-size:13px;");
            box.getChildren().addAll(titulo, ok);
        } else {
            long errors = issues.stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count();
            Label resumen = new Label(
                errors + " error(es) · " + (issues.size() - errors) + " advertencia(s) detectadas");
            resumen.setStyle("-fx-font-size:13px;-fx-text-fill:" + (errors > 0 ? "#E74C3C" : "#E67E22") + ";");

            TableView<ValidationIssue> tabla = new TableView<>(issueList);
            tabla.getStyleClass().add("data-table");
            tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tabla.setPrefHeight(220);

            TableColumn<ValidationIssue, String> colFila = new TableColumn<>("Fila");
            colFila.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().rowIndex() + 1)));
            colFila.setPrefWidth(50);

            TableColumn<ValidationIssue, String> colCampo = new TableColumn<>("Campo");
            colCampo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().columnName()));
            colCampo.setPrefWidth(130);

            TableColumn<ValidationIssue, String> colProblema = new TableColumn<>("Problema");
            colProblema.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().issue()));

            TableColumn<ValidationIssue, String> colSeveridad = new TableColumn<>("Nivel");
            colSeveridad.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().severity() == ValidationIssue.Severity.ERROR ? "ERROR" : "Aviso"));
            colSeveridad.setPrefWidth(65);

            TableColumn<ValidationIssue, String> colSugerencia = new TableColumn<>("Sugerencia IA");
            colSugerencia.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().suggestedFix().orElse("—")));
            colSugerencia.setPrefWidth(160);

            TableColumn<ValidationIssue, Void> colAcciones = new TableColumn<>("Acciones");
            colAcciones.setPrefWidth(180);
            colAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnCorregirCell = btnColor("🤖 Corregir con IA", "#6C63FF");
                private final Button btnIgnorarCell  = btnColor("Ignorar", "#95A5A6");
                private final HBox pane = new HBox(6, btnCorregirCell, btnIgnorarCell);
                {
                    btnCorregirCell.setOnAction(e -> {
                        ValidationIssue iss = getTableRow().getItem();
                        if (iss == null) return;
                        if (iss.rowIndex() < 0 || iss.rowIndex() >= resultadoParseo.rows.size()) return;
                        btnCorregirCell.setDisable(true);
                        Thread.ofVirtual().start(() -> {
                            Optional<String> fix = svc.corregirValor(
                                resultadoParseo.rows.get(iss.rowIndex()),
                                iss.columnName(), iss.issue(), tipoSeleccionado, mappingParaValidar);
                            Platform.runLater(() -> {
                                fix.ifPresent(val ->
                                    resultadoParseo.rows.get(iss.rowIndex()).put(iss.columnName(), val));
                                issueList.remove(iss);
                                updateContinuarButton(btnContinuar, issueList);
                            });
                        });
                    });
                    btnIgnorarCell.setOnAction(e -> {
                        ValidationIssue iss = getTableRow().getItem();
                        if (iss != null) {
                            issueList.remove(iss);
                            updateContinuarButton(btnContinuar, issueList);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                    ValidationIssue iss = getTableRow().getItem();
                    btnCorregirCell.setVisible(iss.suggestedFix().isPresent() || svc.isOllamaDisponible());
                    btnCorregirCell.setDisable(false);
                    setGraphic(pane);
                }
            });

            tabla.getColumns().addAll(colFila, colCampo, colProblema, colSeveridad, colSugerencia, colAcciones);
            box.getChildren().addAll(titulo, resumen, tabla);
        }

        HBox botones = new HBox(12, btnVolver, btnContinuar);
        botones.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(new Separator(), botones);
        return box;
    }

    private void updateContinuarButton(Button btn, ObservableList<ValidationIssue> issues) {
        long errors = issues.stream()
            .filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count();
        btn.setDisable(errors > 0);
        btn.setText(errors > 0
            ? "Continuar → (" + errors + " error(es) pendientes)"
            : "Continuar →");
    }

    // ── PASO 4: Preview and import ────────────────────────────────────────────

    private void mostrarPaso4() {
        activarPaso(3);
        panelPasos.getChildren().setAll(buildPaso4());
    }

    private VBox buildPaso4() {
        ImportService.ImportConfig config = importConfig != null
            ? importConfig
            : new ImportService.ImportConfig(mappingActual);
        PreparedImport prepared = prepareImport(config);
        int estimatedRecords = prepared.rows().size();

        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Vista previa e importar");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:-c-text;");

        long camposMapeados = config.mapping.values().stream().filter(Objects::nonNull).count()
            + (config.hasPivot() ? 2L : 0L);
        String pivotInfo = config.hasPivot()
            ? " · modo expandido: " + config.pivotColumns.size() + " columnas × " + resultadoParseo.rows.size() + " filas"
            : "";
        String groupInfo = groupingConfig != null
            ? " · grupo fijo: " + groupingConfig.fixedValue()
            : "";
        Label resumen = new Label(
            "📋 ~" + estimatedRecords + " registros listos · " +
            camposMapeados + " campos mapeados → " + tipoSeleccionado.label + pivotInfo + groupInfo);
        resumen.setStyle("-fx-font-size:13px;");
        resumen.setWrapText(true);

        // Preview table (first 8 rows)
        TableView<Map<String, String>> preview = buildTablaPreview(prepared, config);

        // Warn if required field is not mapped
        List<String> warnings = buildWarnings(config);
        VBox warningBox = new VBox(3);
        for (String w : warnings) {
            Label lw = new Label("⚠ " + w);
            lw.setStyle("-fx-text-fill:#E67E22;-fx-font-size:12px;");
            warningBox.getChildren().add(lw);
        }

        // Progress
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);
        Label lblProgreso = new Label("");

        Button btnVolver = btnColor("← Volver", "#95A5A6");
        btnVolver.setOnAction(e -> mostrarPaso3());

        Button btnImportar = btnColor("⬇  Importar ~" + estimatedRecords + " registros", "#27AE60");
        btnImportar.setPrefWidth(260);
        btnImportar.setDefaultButton(true);
        btnImportar.setOnAction(e -> {
            btnImportar.setDisable(true);
            btnVolver.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            lblProgreso.setText("Importando…");
            SoundService.play(SoundService.Sound.START);
            log("⬇ Iniciando importación" + (config.hasPivot() ? " (modo expandido)" : "") +
                " en " + tipoSeleccionado.label + "…");

            Thread.ofVirtual().start(() -> {
                try {
                    org.gipsybuho.service.importer.ImportResult result =
                        new EntityImportService().importar(
                            specFor(tipoSeleccionado),
                            prepared.rows(),
                            prepared.mapping(),
                            specFor(tipoSeleccionado).politicaDefecto()
                        );
                    Platform.runLater(() -> {
                        SoundService.play(SoundService.Sound.COMPLETE);
                        progressBar.setProgress(1.0);
                        lblProgreso.setText("✅ Importación completada");
                        log("✅ " + result.filasImportadas() + " registros importados y " +
                            result.filasActualizadas() + " actualizados en " + tipoSeleccionado.label + ".");
                        btnImportar.setDisable(false);
                        btnVolver.setDisable(false);

                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Importación completada");
                        ok.setHeaderText(null);
                        ok.setContentText(importResultText(result));
                        ok.showAndWait();
                        if (onImportComplete != null) {
                            onImportComplete.run();
                        }
                        mostrarPaso1();  // Reset to start
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        SoundService.play(SoundService.Sound.ERROR);
                        progressBar.setProgress(0);
                        lblProgreso.setText("❌ Error: " + ex.getMessage());
                        log("❌ Error durante la importación: " + ex.getMessage());
                        btnImportar.setDisable(false);
                        btnVolver.setDisable(false);
                        alerta("Error al importar:\n" + ex.getMessage());
                    });
                }
            });
        });

        HBox botonesRow = new HBox(12, btnVolver, btnImportar, progressBar, lblProgreso);
        botonesRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(titulo, resumen, preview);
        if (!warnings.isEmpty()) box.getChildren().add(warningBox);
        box.getChildren().addAll(new Separator(), botonesRow);
        return box;
    }

    private record PreparedImport(List<Map<String, String>> rows, Map<String, String> mapping) {}

    private PreparedImport prepareImport(ImportService.ImportConfig config) {
        List<Map<String, String>> rows = config.hasPivot()
            ? expandPivotRows(resultadoParseo.rows, config)
            : copyRows(resultadoParseo.rows);
        Map<String, String> mapping = new LinkedHashMap<>(config.mapping);
        mapping.entrySet().removeIf(entry -> entry.getValue() == null);
        if (false && config.hasPivot()) {
            mapping.put("__pivot_label__", config.pivotLabelField);
            mapping.put("__pivot_value__", config.pivotValueField);
        }
        if (groupingConfig != null) {
            mapping.entrySet().removeIf(entry -> groupingConfig.field().equals(entry.getValue()));
            for (Map<String, String> row : rows) {
                row.put("__fixed_group__", groupingConfig.fixedValue());
            }
            mapping.put("__fixed_group__", groupingConfig.field());
        }
        return new PreparedImport(rows, mapping);
    }

    private List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : rows) result.add(new LinkedHashMap<>(row));
        return result;
    }

    private List<Map<String, String>> expandPivotRows(List<Map<String, String>> rows, ImportService.ImportConfig config) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            for (String pivotCol : config.pivotColumns) {
                String value = row.getOrDefault(pivotCol, "");
                if (value.isBlank()) continue;
                Map<String, String> newRow = new LinkedHashMap<>(row);
                newRow.put("__pivot_label__", pivotCol);
                newRow.put("__pivot_value__", value);
                result.add(newRow);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private TableView<Map<String, String>> buildTablaPreview(PreparedImport prepared, ImportService.ImportConfig config) {
        ObservableList<Map<String, String>> items = FXCollections.observableArrayList();
        int limit = Math.min(8, prepared.rows().size());
        for (int i = 0; i < limit; i++) items.add(prepared.rows().get(i));

        TableView<Map<String, String>> tabla = new TableView<>(items);
        tabla.setPrefHeight(300);
        tabla.setMinHeight(240);
        tabla.setMaxHeight(360);
        tabla.getStyleClass().add("data-table");
        TableColumnSizing.enableHorizontalScroll(tabla);

        for (var entry : prepared.mapping().entrySet()) {
            if (entry.getValue() == null) continue;
            String col = entry.getKey();
            String dest = entry.getValue();
            TableColumn<Map<String, String>, String> tc = new TableColumn<>(dest + "\n← " + col);
            tc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getOrDefault(col, "")));
            tc.setPrefWidth(140);
            tabla.getColumns().add(tc);
        }

        if (prepared.rows().size() > 8) {
            tabla.setPlaceholder(new Label("… y " + (prepared.rows().size() - 8) + " registros más"));
        }

        TableColumnSizing.autoSizeLater(tabla, 90, 520, 80);
        return tabla;
    }

    private List<String> buildWarnings(ImportService.ImportConfig config) {
        List<String> warns = new ArrayList<>();
        boolean nombreMapeado = config.mapping.values().contains("nombre") ||
            (config.hasPivot() && ("nombre".equals(config.pivotLabelField) || "nombre".equals(config.pivotValueField)));
        if (!nombreMapeado) {
            warns.add("El campo 'nombre' no está mapeado. Los registros sin este campo serán ignorados.");
        }
        if (config.hasPivot()) {
            warns.add("Modo expandido activo: " + config.pivotColumns.size() +
                " columnas × filas generarán múltiples registros.");
        } else {
            long sinMapear = config.mapping.values().stream().filter(Objects::isNull).count();
            if (sinMapear > 0) {
                warns.add(sinMapear + " columna(s) sin mapear serán ignoradas durante la importación.");
            }
        }
        return warns;
    }

    private int calcularRegistrosPivot(List<Map<String, String>> rows, ImportService.ImportConfig config) {
        int count = 0;
        for (Map<String, String> row : rows) {
            for (String col : config.pivotColumns) {
                if (!row.getOrDefault(col, "").isBlank()) count++;
            }
        }
        return count;
    }

    private EntityImportSpec specFor(TipoEntidad tipo) {
        return switch (tipo) {
            case CLIENTES   -> Cliente.IMPORT_SPEC;
            case MATERIALES -> Material.IMPORT_SPEC;
            case EMPLEADOS  -> Empleado.IMPORT_SPEC;
            case TARIFAS    -> Tarifa.IMPORT_SPEC;
        };
    }

    private String groupingField(TipoEntidad tipo) {
        return switch (tipo) {
            case MATERIALES -> "categoria";
            case TARIFAS    -> "tecnica";
            default         -> null;
        };
    }

    private String importResultText(org.gipsybuho.service.importer.ImportResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Importación completada en %.1f s.%n%n",
            result.duracion().toMillis() / 1000.0));
        sb.append(String.format("✓ %d filas importadas%n", result.filasImportadas()));
        sb.append(String.format("✓ %d filas actualizadas%n", result.filasActualizadas()));
        sb.append(String.format("✗ %d filas descartadas", result.filasDescartadas()));
        if (!result.errores().isEmpty()) {
            sb.append("\n\nErrores (primeros 10):");
            result.errores().stream().limit(10).forEach(error ->
                sb.append(String.format("%n  Fila %d — %s: %s",
                    error.numeroFila(),
                    error.campo() != null ? error.campo() : "—",
                    error.mensaje())));
        }
        return sb.toString();
    }

    private Label labelMin(String text, double minWidth) {
        Label lbl = new Label(text);
        lbl.setMinWidth(minWidth);
        lbl.setStyle("-fx-text-fill:-c-text;");
        return lbl;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private Button btnColor(String texto, String color) {
        Button b = new Button(texto);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:8 18;-fx-background-radius:4;");
        return b;
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
