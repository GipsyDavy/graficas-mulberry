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
import org.gipsybuho.service.ImportService;
import org.gipsybuho.service.ImportService.ImportResult;
import org.gipsybuho.service.ImportService.TipoEntidad;
import org.gipsybuho.service.SoundService;

import java.io.File;
import java.util.*;

public class ImportView extends VBox {

    private final ImportService svc = new ImportService();

    // State
    private File archivoSeleccionado;
    private ImportResult resultadoParseo;
    private TipoEntidad tipoSeleccionado;
    private Map<String, String> mappingActual = new LinkedHashMap<>();

    // UI panels (one per step)
    private final VBox panelPasos = new VBox(0);
    private final TextArea logArea = new TextArea();

    // Step indicators
    private final Label[] stepLabels = new Label[4];

    public ImportView() {
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

        getChildren().addAll(titulo, subtitulo, buildIndicadorPasos(), new Separator(), panelPasos, buildLog());
        VBox.setVgrow(panelPasos, Priority.ALWAYS);

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
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

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
                new FileChooser.ExtensionFilter("Archivos de datos", "*.csv", "*.xlsx", "*.xls", "*.json", "*.txt"),
                new FileChooser.ExtensionFilter("CSV", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"),
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
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

        // Columns detected
        Label lblCols = new Label("Columnas encontradas: " + String.join(", ", resultadoParseo.headers));
        lblCols.setWrapText(true);
        lblCols.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        ComboBox<TipoEntidad> cbTipo = new ComboBox<>(
            FXCollections.observableArrayList(TipoEntidad.values()));
        cbTipo.setPrefWidth(200);

        Label lblIAEstado = new Label("");
        lblIAEstado.setStyle("-fx-text-fill:-c-text-muted;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(24, 24);
        spinner.setVisible(false);

        HBox statusRow = new HBox(8, spinner, lblIAEstado);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button btnIA = btnColor("🤖  Preguntar a la IA", "#9B59B6");
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

        // Auto-trigger IA if Ollama is available
        if (svc.isOllamaDisponible()) {
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
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

        Label desc = new Label(
            "Revisa cómo la IA ha mapeado las columnas del archivo a los campos de " + tipoSeleccionado.label +
            ". Puedes ajustar manualmente cualquier asignación.");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:-c-text-muted;");

        // Mapping table
        TableView<MappingRow> tablaMappeo = buildTablaMappeo(mapping);
        VBox.setVgrow(tablaMappeo, Priority.ALWAYS);

        // Summary
        long mapeados = mapping.values().stream().filter(Objects::nonNull).count();
        Label resumen = new Label("✅ " + mapeados + " campos mapeados de " + mapping.size() + " columnas. " +
            "Las columnas sin mapeo se ignorarán.");
        resumen.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");

        Button btnVolver = btnColor("← Volver", "#95A5A6");
        btnVolver.setOnAction(e -> mostrarPaso2());

        Button btnSiguiente = btnColor("Siguiente →", "#27AE60");
        btnSiguiente.setOnAction(e -> {
            // Collect current mapping from table items
            for (MappingRow row : tablaMappeo.getItems()) {
                mappingActual.put(row.columna, row.getCampoDestino());
            }
            mostrarPaso4();
        });

        HBox botonesRow = new HBox(12, btnVolver, btnSiguiente);
        botonesRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(titulo, desc, tablaMappeo, resumen, new Separator(), botonesRow);
        return box;
    }

    private TableView<MappingRow> buildTablaMappeo(Map<String, String> mapping) {
        List<String> opcionesDestino = new ArrayList<>();
        opcionesDestino.add("(ignorar)");
        opcionesDestino.addAll(tipoSeleccionado.campos);

        ObservableList<MappingRow> items = FXCollections.observableArrayList();
        for (var entry : mapping.entrySet()) {
            items.add(new MappingRow(entry.getKey(), entry.getValue(), opcionesDestino));
        }

        TableView<MappingRow> tabla = new TableView<>(items);
        tabla.setPrefHeight(280);
        tabla.setEditable(true);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Column: file column name
        TableColumn<MappingRow, String> colArchivo = new TableColumn<>("Columna del archivo");
        colArchivo.setCellValueFactory(new PropertyValueFactory<>("columna"));
        colArchivo.setEditable(false);
        colArchivo.setStyle("-fx-font-weight:bold;");

        // Column: destination field (editable ComboBox)
        TableColumn<MappingRow, String> colDestino = new TableColumn<>("Campo destino (editable)");
        colDestino.setCellValueFactory(new PropertyValueFactory<>("campoDestino"));
        colDestino.setCellFactory(tc -> new ComboBoxTableCell(opcionesDestino));
        colDestino.setOnEditCommit(event -> event.getRowValue().setCampoDestino(event.getNewValue()));

        // Column: preview value
        TableColumn<MappingRow, String> colEjemplo = new TableColumn<>("Valor de ejemplo");
        colEjemplo.setCellValueFactory(cd -> {
            String col = cd.getValue().columna;
            String ejemplo = resultadoParseo.rows.isEmpty() ? "" :
                resultadoParseo.rows.get(0).getOrDefault(col, "");
            return new javafx.beans.property.SimpleStringProperty(ejemplo);
        });
        colEjemplo.setStyle("-fx-text-fill:-c-text-muted;");

        // Status column
        TableColumn<MappingRow, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd -> {
            String dest = cd.getValue().getCampoDestino();
            boolean mapeado = dest != null && !dest.equals("(ignorar)");
            return new javafx.beans.property.SimpleStringProperty(mapeado ? "✅" : "⬜ ignorado");
        });
        colEstado.setPrefWidth(90);

        tabla.getColumns().addAll(colArchivo, colDestino, colEjemplo, colEstado);
        return tabla;
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

        ComboBoxTableCell(List<String> options) {
            combo = new ComboBox<>(FXCollections.observableArrayList(options));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setOnAction(e -> {
                if (isEditing()) {
                    commitEdit(combo.getValue());
                } else if (getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().setCampoDestino(combo.getValue());
                    updateItem(combo.getValue(), false);
                }
            });
        }

        @Override
        public void startEdit() {
            super.startEdit();
            setGraphic(combo);
            setText(null);
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
                return;
            }
            // Always show combo
            combo.setValue(item != null ? item : "(ignorar)");
            setGraphic(combo);
            setText(null);
            if (getTableRow() != null && getTableRow().getItem() != null) {
                boolean mapeado = item != null && !item.equals("(ignorar)");
                setStyle(mapeado ? "-fx-text-fill:-c-text;" : "-fx-text-fill:-c-text-muted;");
            }
        }
    }

    // ── PASO 4: Preview and import ────────────────────────────────────────────

    private void mostrarPaso4() {
        activarPaso(3);
        panelPasos.getChildren().setAll(buildPaso4());
    }

    private VBox buildPaso4() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        Label titulo = new Label("Vista previa e importar");
        titulo.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

        long camposMapeados = mappingActual.values().stream().filter(Objects::nonNull).count();
        Label resumen = new Label(
            "📋 " + resultadoParseo.rows.size() + " registros listos · " +
            camposMapeados + " campos mapeados → " + tipoSeleccionado.label);
        resumen.setStyle("-fx-font-size:13px;");

        // Preview table (first 8 rows)
        TableView<Map<String, String>> preview = buildTablaPreview();
        VBox.setVgrow(preview, Priority.ALWAYS);

        // Warn if required field is not mapped
        List<String> warnings = buildWarnings();
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

        Button btnImportar = btnColor("⬇  Importar " + resultadoParseo.rows.size() + " registros", "#27AE60");
        btnImportar.setPrefWidth(260);
        btnImportar.setDefaultButton(true);
        btnImportar.setOnAction(e -> {
            btnImportar.setDisable(true);
            btnVolver.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            lblProgreso.setText("Importando…");
            SoundService.play(SoundService.Sound.START);
            log("⬇ Iniciando importación de " + resultadoParseo.rows.size() + " registros en " + tipoSeleccionado.label + "…");

            // Resolve "(ignorar)" entries before import
            Map<String, String> mappingFinal = new LinkedHashMap<>(mappingActual);

            Thread.ofVirtual().start(() -> {
                try {
                    int n = svc.importar(tipoSeleccionado, resultadoParseo.rows, mappingFinal);
                    Platform.runLater(() -> {
                        SoundService.play(SoundService.Sound.COMPLETE);
                        progressBar.setProgress(1.0);
                        lblProgreso.setText("✅ Importación completada");
                        log("✅ " + n + " registros importados correctamente en " + tipoSeleccionado.label + ".");
                        btnImportar.setDisable(false);
                        btnVolver.setDisable(false);

                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Importación completada");
                        ok.setHeaderText(null);
                        ok.setContentText("✅  Se han importado " + n + " registros en " + tipoSeleccionado.label + ".\n\nPuedes verlos en la sección correspondiente.");
                        ok.showAndWait();
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

    @SuppressWarnings("unchecked")
    private TableView<Map<String, String>> buildTablaPreview() {
        ObservableList<Map<String, String>> items = FXCollections.observableArrayList();
        int limit = Math.min(8, resultadoParseo.rows.size());
        for (int i = 0; i < limit; i++) items.add(resultadoParseo.rows.get(i));

        TableView<Map<String, String>> tabla = new TableView<>(items);
        tabla.setPrefHeight(220);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        for (var entry : mappingActual.entrySet()) {
            if (entry.getValue() == null) continue;
            String col = entry.getKey();
            String dest = entry.getValue();
            TableColumn<Map<String, String>, String> tc = new TableColumn<>(dest + "\n← " + col);
            tc.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getOrDefault(col, "")));
            tc.setPrefWidth(140);
            tabla.getColumns().add(tc);
        }

        if (resultadoParseo.rows.size() > 8) {
            tabla.setPlaceholder(new Label("... y " + (resultadoParseo.rows.size() - 8) + " registros más"));
        }

        return tabla;
    }

    private List<String> buildWarnings() {
        List<String> warns = new ArrayList<>();
        // Check required fields
        String requiredField = switch (tipoSeleccionado) {
            case CLIENTES, MATERIALES, EMPLEADOS, TARIFAS -> "nombre";
        };
        boolean nombreMapeado = mappingActual.values().contains(requiredField);
        if (!nombreMapeado) {
            warns.add("El campo '" + requiredField + "' no está mapeado. Los registros sin este campo serán ignorados.");
        }
        long sinMapear = mappingActual.values().stream().filter(Objects::isNull).count();
        if (sinMapear > 0) {
            warns.add(sinMapear + " columna(s) sin mapear serán ignoradas durante la importación.");
        }
        return warns;
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
