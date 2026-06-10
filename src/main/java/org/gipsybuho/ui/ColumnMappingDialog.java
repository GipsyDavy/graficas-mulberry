package org.gipsybuho.ui;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.service.importer.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dialog modal para revisar y ajustar el mapeo de columnas del archivo importado a los campos del spec.
 * Devuelve Optional&lt;MappingResult&gt; via showAndWait().
 */
public class ColumnMappingDialog extends Dialog<MappingResult> {

    private static final String IGNORAR = "(ignorar)";

    /**
     * @param owner       ventana padre para centrado y bloqueo modal
     * @param spec        IMPORT_SPEC de la entidad destino
     * @param headers     lista de headers del archivo ya parseado
     * @param previewRows primeras filas del archivo (máximo 3) para la columna de muestra
     */
    public ColumnMappingDialog(Window owner, EntityImportSpec spec,
                                List<String> headers, List<Map<String, String>> previewRows) {
        if (owner != null) initOwner(owner);
        setTitle("Configurar importación de " + spec.nombre());
        setResizable(true);
        getDialogPane().setPrefSize(760, 560);

        // clave → etiqueta legible para el converter del ComboBox (mutable — se añaden campos nuevos)
        Map<String, String> claveToEtiqueta = spec.campos().stream()
            .collect(Collectors.toMap(FieldSpec::clave, FieldSpec::etiqueta,
                (a, b) -> a, LinkedHashMap::new));

        Set<String> obligClaves = spec.camposObligatorios().stream()
            .map(FieldSpec::clave).collect(Collectors.toSet());

        // Un ColumnRow por cada header del archivo
        ObservableList<ColumnRow> items = FXCollections.observableArrayList();
        for (String h : headers) {
            String sugerido = spec.matcher().sugerirCampo(h);
            boolean oblSugerido = sugerido != null && obligClaves.contains(sugerido);
            items.add(new ColumnRow(h, buildPreview(h, previewRows),
                sugerido != null ? sugerido : IGNORAR, oblSugerido));
        }

        // ObservableList compartida entre todas las CampoCelda: al añadir aquí se actualiza todo
        ObservableList<String> opciones = FXCollections.observableArrayList();
        opciones.add(IGNORAR);
        spec.campos().forEach(f -> opciones.add(f.clave()));

        // Tabla
        TableView<ColumnRow> tabla = buildTable(items, opciones, claveToEtiqueta, obligClaves);

        // ComboBox de política de duplicados
        ComboBox<DuplicatePolicy> cbPolicy = buildPolicyCombo(spec);

        // Botón "Nuevo campo" — solo activo para entidades planas con tableName conocido
        String tableName = spec.tableName();
        Button btnNuevoCampo = new Button("➕  Nuevo campo…");
        btnNuevoCampo.setStyle("-fx-background-color:#27AE60;-fx-text-fill:white;-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:4;");
        btnNuevoCampo.setVisible(tableName != null);
        btnNuevoCampo.setManaged(tableName != null);
        btnNuevoCampo.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog();
            tid.setTitle("Crear campo nuevo");
            tid.setHeaderText("Nombre visible del nuevo campo");
            tid.setContentText("Etiqueta:");
            if (getDialogPane().getScene() != null)
                tid.getDialogPane().getStylesheets().addAll(getDialogPane().getScene().getStylesheets());
            tid.showAndWait().ifPresent(label -> {
                if (label.isBlank()) return;
                try {
                    Set<String> especClaves = spec.campos().stream()
                        .map(FieldSpec::clave).collect(Collectors.toSet());
                    ColumnConfigDAO.ColumnConfig config =
                        new ColumnConfigDAO().addDynamicColumn(tableName, label, especClaves);
                    // Añadir a la lista compartida: todos los ComboBox se actualizan automáticamente
                    opciones.add(config.columnName());
                    claveToEtiqueta.put(config.columnName(), config.label());
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                        "No se pudo crear el campo:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
                }
            });
        });

        // Layout
        Label infoLabel = new Label(headers.size() + " columnas detectadas → " + spec.nombre());
        infoLabel.setStyle("-fx-text-fill:-c-text-muted;-fx-font-size:12px;");
        infoLabel.setPadding(new Insets(0, 0, 8, 0));
        Label validationLabel = new Label();
        validationLabel.setStyle("-fx-text-fill:#E74C3C;-fx-font-size:12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBox = new HBox(8, new Label("Si el registro ya existe:"), cbPolicy, spacer, btnNuevoCampo);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setPadding(new Insets(10, 0, 4, 0));

        VBox bottom = new VBox(6, validationLabel, bottomBox);

        BorderPane content = new BorderPane();
        content.setTop(infoLabel);
        content.setCenter(tabla);
        content.setBottom(bottom);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Botón OK deshabilitado hasta que todos los campos obligatorios estén mapeados
        Button btnOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        Observable[] observables = items.stream()
            .map(r -> (Observable) r.campoSeleccionado)
            .toArray(Observable[]::new);
        validationLabel.textProperty().bind(Bindings.createStringBinding(
            () -> validationMessage(items, obligClaves), observables));
        validationLabel.visibleProperty().bind(validationLabel.textProperty().isNotEmpty());
        validationLabel.managedProperty().bind(validationLabel.visibleProperty());
        btnOk.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            Set<String> mapeados = items.stream()
                .map(r -> r.campoSeleccionado.get())
                .collect(Collectors.toSet());
            boolean faltanObligatorios = !spec.camposObligatorios().stream()
                .allMatch(f -> mapeados.contains(f.clave()));
            return faltanObligatorios || hasDuplicateDestinations(items);
        }, observables));

        // Converter: devuelve null si el usuario cancela
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Map<String, String> mapping = new LinkedHashMap<>();
            for (ColumnRow r : items) {
                String clave = r.campoSeleccionado.get();
                if (clave != null && !IGNORAR.equals(clave)) {
                    mapping.put(r.header, clave);
                }
            }
            return new MappingResult(Collections.unmodifiableMap(mapping), cbPolicy.getValue());
        });
    }

    private String buildPreview(String header, List<Map<String, String>> rows) {
        List<String> vals = new ArrayList<>();
        for (int i = 0; i < Math.min(3, rows.size()); i++) {
            String v = rows.get(i).getOrDefault(header, "");
            if (v != null && !v.isBlank()) vals.add(v);
        }
        return String.join(" │ ", vals);
    }

    private TableView<ColumnRow> buildTable(ObservableList<ColumnRow> items,
                                             ObservableList<String> opciones,
                                             Map<String, String> claveToEtiqueta,
                                             Set<String> obligClaves) {
        TableView<ColumnRow> tabla = new TableView<>(items);
        tabla.setEditable(false);
        tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // Col 1: header del archivo (no editable)
        TableColumn<ColumnRow, String> colHeader = new TableColumn<>("Columna del archivo");
        colHeader.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().header));
        colHeader.setPrefWidth(160);
        colHeader.setMinWidth(120);
        colHeader.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(empty ? "" : "-fx-font-weight:bold;");
            }
        });

        // Col 2: muestra de datos (no editable)
        TableColumn<ColumnRow, String> colPreview = new TableColumn<>("Muestra");
        colPreview.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().preview));
        colPreview.setPrefWidth(200);
        colPreview.setMinWidth(100);
        colPreview.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(empty ? "" : "-fx-text-fill:-c-text-muted;");
            }
        });

        // Col 3: campo destino — ComboBox siempre visible, reacciona a cambios de la property
        TableColumn<ColumnRow, String> colCampo = new TableColumn<>("Campo destino");
        colCampo.setCellValueFactory(cd -> cd.getValue().campoSeleccionado);
        colCampo.setCellFactory(tc -> new CampoCelda(opciones, claveToEtiqueta));
        colCampo.setPrefWidth(220);
        colCampo.setMinWidth(160);

        // Col 4: indicador de obligatorio — reacciona a cambios de la misma property
        TableColumn<ColumnRow, String> colIndicador = new TableColumn<>("");
        colIndicador.setCellValueFactory(cd -> cd.getValue().campoSeleccionado);
        colIndicador.setCellFactory(tc -> new IndicadorCelda(obligClaves));
        colIndicador.setMinWidth(36);
        colIndicador.setMaxWidth(36);
        colIndicador.setPrefWidth(36);
        colIndicador.setResizable(false);

        tabla.getColumns().addAll(colHeader, colPreview, colCampo, colIndicador);
        return tabla;
    }

    private ComboBox<DuplicatePolicy> buildPolicyCombo(EntityImportSpec spec) {
        ComboBox<DuplicatePolicy> cb = new ComboBox<>(
            FXCollections.observableArrayList(DuplicatePolicy.values()));
        cb.setValue(spec.politicaDefecto());
        cb.setConverter(new StringConverter<>() {
            @Override public String toString(DuplicatePolicy p) {
                if (p == null) return "";
                return switch (p) {
                    case SKIP_IF_EXISTS  -> "Omitir (conservar existente)";
                    case UPDATE_EXISTING -> "Actualizar existente";
                    case CREATE_NEW      -> "Crear siempre";
                };
            }
            @Override public DuplicatePolicy fromString(String s) { return null; }
        });
        return cb;
    }

    private String validationMessage(ObservableList<ColumnRow> items, Set<String> obligClaves) {
        List<String> faltantes = obligClaves.stream()
            .filter(clave -> items.stream().noneMatch(r -> clave.equals(r.campoSeleccionado.get())))
            .toList();
        if (!faltantes.isEmpty()) {
            return "Faltan campos obligatorios: " + String.join(", ", faltantes);
        }
        List<String> duplicados = duplicateDestinations(items);
        if (!duplicados.isEmpty()) {
            return "Cada campo destino solo puede usarse una vez: " + String.join(", ", duplicados);
        }
        return "";
    }

    private boolean hasDuplicateDestinations(ObservableList<ColumnRow> items) {
        return !duplicateDestinations(items).isEmpty();
    }

    private List<String> duplicateDestinations(ObservableList<ColumnRow> items) {
        Set<String> vistos = new HashSet<>();
        Set<String> duplicados = new LinkedHashSet<>();
        for (ColumnRow row : items) {
            String clave = row.campoSeleccionado.get();
            if (clave == null || IGNORAR.equals(clave)) continue;
            if (!vistos.add(clave)) duplicados.add(clave);
        }
        return new ArrayList<>(duplicados);
    }

    // ── Modelo de fila ────────────────────────────────────────────────────────

    static final class ColumnRow {
        final String header;
        final String preview;
        final SimpleStringProperty campoSeleccionado;
        /** true si el matcher sugirió un campo obligatorio para este header. */
        final boolean obligatorioSugerido;

        ColumnRow(String header, String preview, String initial, boolean obligatorioSugerido) {
            this.header = header;
            this.preview = preview;
            this.campoSeleccionado = new SimpleStringProperty(initial);
            this.obligatorioSugerido = obligatorioSugerido;
        }
    }

    // ── Celda ComboBox (siempre visible, no modo edición) ─────────────────────

    private static final class CampoCelda extends TableCell<ColumnRow, String> {
        private final ComboBox<String> combo;
        private boolean actualizando;

        CampoCelda(ObservableList<String> opciones, Map<String, String> claveToEtiqueta) {
            combo = new ComboBox<>(opciones);  // lista compartida: nuevos campos aparecen automáticamente
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setConverter(new StringConverter<>() {
                @Override public String toString(String clave) {
                    if (clave == null || IGNORAR.equals(clave)) return IGNORAR;
                    return claveToEtiqueta.getOrDefault(clave, clave);
                }
                @Override public String fromString(String s) { return null; }
            });
            // Propaga la selección del usuario al modelo (evita loop con 'actualizando')
            combo.setOnAction(e -> {
                if (!actualizando && getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().campoSeleccionado.set(combo.getValue());
                }
            });
        }

        @Override
        protected void updateItem(String clave, boolean empty) {
            super.updateItem(clave, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            // Actualización programática: no debe disparar setOnAction
            actualizando = true;
            combo.setValue(clave != null ? clave : IGNORAR);
            actualizando = false;
            setGraphic(combo);
        }
    }

    // ── Celda indicador ───────────────────────────────────────────────────────

    private static final class IndicadorCelda extends TableCell<ColumnRow, String> {
        private final Set<String> obligClaves;

        IndicadorCelda(Set<String> obligClaves) {
            this.obligClaves = obligClaves;
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(String clave, boolean empty) {
            super.updateItem(clave, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setText("");
                setStyle("");
                return;
            }
            if (obligClaves.contains(clave)) {
                // Campo obligatorio cubierto
                setText("✓");
                setStyle("-fx-text-fill:#27AE60;-fx-font-weight:bold;");
            } else if (getTableRow().getItem().obligatorioSugerido && IGNORAR.equals(clave)) {
                // El matcher sugirió un obligatorio para este header pero el usuario lo ignoró
                setText("✱");
                setStyle("-fx-text-fill:#E74C3C;-fx-font-weight:bold;");
            } else {
                setText("");
                setStyle("");
            }
        }
    }
}
