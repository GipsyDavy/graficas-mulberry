package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.dao.ColumnConfigDAO.ColumnConfig;
import org.gipsybuho.dao.DynamicColumnValueDAO;
import org.gipsybuho.util.TypedValueFormatter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ColumnConfiguratorDialog {

    private final ColumnConfigDAO dao = new ColumnConfigDAO();
    private final String tableName;
    private final String moduleName;
    private final Map<String, String> baseColumns;
    private final Set<String> ignoredColumns;
    private final List<String> stylesheets;
    private final ObservableList<ColumnConfig> configs = FXCollections.observableArrayList();
    private boolean changed;

    public ColumnConfiguratorDialog(String tableName, String moduleName, Map<String, String> baseColumns) {
        this(tableName, moduleName, baseColumns, List.of());
    }

    public ColumnConfiguratorDialog(String tableName, String moduleName, Map<String, String> baseColumns, List<String> stylesheets) {
        this(tableName, moduleName, baseColumns, Set.of(), stylesheets);
    }

    public ColumnConfiguratorDialog(String tableName, String moduleName, Map<String, String> baseColumns,
                                    Set<String> ignoredColumns, List<String> stylesheets) {
        this.tableName = tableName;
        this.moduleName = moduleName;
        this.baseColumns = baseColumns;
        this.ignoredColumns = Set.copyOf(ignoredColumns);
        this.stylesheets = new ArrayList<>(stylesheets);
    }

    public boolean show() throws SQLException {
        dao.syncTable(tableName, baseColumns, ignoredColumns);
        load();

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Configurar columnas - " + moduleName);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().getStylesheets().addAll(stylesheets);

        ListView<ColumnConfig> listView = new ListView<>(configs);
        listView.setPrefHeight(320);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(ColumnConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String tipo = item.baseColumn() ? "base"
                    : (item.dataType() != null ? item.dataType().toLowerCase() : "texto");
                String estado = item.visible() ? "" : " (oculta)";
                setText(item.label() + "  [" + tipo + "]" + estado);
            }
        });

        Button add = new Button("+ Añadir");
        Button rename = new Button("Renombrar");
        Button changeType = new Button("Tipo…");
        Button show = new Button("Mostrar");
        Button hide = new Button("Ocultar");
        Button delete = new Button("Eliminar");
        add.setMaxWidth(Double.MAX_VALUE);
        rename.setMaxWidth(Double.MAX_VALUE);
        changeType.setMaxWidth(Double.MAX_VALUE);
        show.setMaxWidth(Double.MAX_VALUE);
        hide.setMaxWidth(Double.MAX_VALUE);
        delete.setMaxWidth(Double.MAX_VALUE);
        delete.setStyle("-fx-text-fill:#E74C3C;");

        add.setOnAction(e -> addColumn());
        rename.setOnAction(e -> renameColumn(listView.getSelectionModel().getSelectedItem()));
        changeType.setOnAction(e -> changeColumnType(listView.getSelectionModel().getSelectedItem()));
        show.setOnAction(e -> showColumn(listView.getSelectionModel().getSelectedItem()));
        hide.setOnAction(e -> hideColumn(listView.getSelectionModel().getSelectedItem()));
        delete.setOnAction(e -> deleteColumn(listView.getSelectionModel().getSelectedItem()));

        VBox actions = new VBox(8, add, rename, changeType, show, hide, delete);
        HBox.setHgrow(listView, Priority.ALWAYS);
        HBox content = new HBox(12, listView, actions);
        content.setPadding(new Insets(12));

        Label note = new Label(
            "Columnas base: renombrar, ocultar/mostrar. " +
            "Columnas de usuario: renombrar, ocultar/mostrar, eliminar (borra los datos).");
        note.setWrapText(true);
        VBox root = new VBox(8, content, note);
        root.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(root);
        dialog.setResultConverter(button -> changed);
        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(changed);
    }

    private void load() throws SQLException {
        List<ColumnConfig> all = dao.findAll(tableName);
        configs.setAll(all.stream()
            .filter(config -> !ignoredColumns.contains(config.columnName()))
            .toList());
    }

    private void addColumn() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nueva columna");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(stylesheets);

        TextField tfLabel = new TextField();
        tfLabel.setPromptText("Nombre visible de la columna");
        tfLabel.setPrefWidth(220);

        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList(
            "TEXTO", "NUMÉRICO", "PRECIO", "FECHA"));
        cbTipo.setValue("TEXTO");
        cbTipo.setPrefWidth(220);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(tfLabel, 1, 0);
        grid.add(new Label("Tipo de dato:"), 0, 1);
        grid.add(cbTipo, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(bt -> {
            String label = tfLabel.getText().trim();
            if (label.isBlank()) return;
            String dataType = switch (cbTipo.getValue()) {
                case "NUMÉRICO" -> "NUMERICO";
                case "PRECIO"   -> "PRECIO";
                case "FECHA"    -> "FECHA";
                default         -> "TEXTO";
            };
            try {
                Set<String> reserved = new java.util.HashSet<>(baseColumns.keySet());
                reserved.addAll(ignoredColumns);
                dao.addDynamicColumn(tableName, label, dataType, reserved);
                changed = true;
                load();
            } catch (Exception ex) {
                showError(ex);
            }
        });
    }

    private void renameColumn(ColumnConfig selected) {
        if (selected == null) return;
        TextInputDialog input = new TextInputDialog(selected.label());
        input.setTitle("Renombrar columna");
        input.setHeaderText(null);
        input.setContentText("Etiqueta visible:");
        input.showAndWait().ifPresent(label -> {
            try {
                dao.rename(tableName, selected.columnName(), label);
                changed = true;
                load();
            } catch (Exception ex) {
                showError(ex);
            }
        });
    }

    private void hideColumn(ColumnConfig selected) {
        if (selected == null) return;
        String msg = selected.baseColumn()
            ? "La columna se ocultará en pantalla. Sus datos siguen disponibles internamente.\n\n¿Continuar?"
            : "La columna se ocultará, pero sus datos permanecerán en SQLite.\n\n¿Continuar?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Ocultar columna");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> {
            try {
                dao.setColumnVisible(tableName, selected.columnName(), false);
                changed = true;
                load();
            } catch (Exception ex) {
                showError(ex);
            }
        });
    }

    private void showColumn(ColumnConfig selected) {
        if (selected == null) return;
        if (selected.visible()) {
            new Alert(Alert.AlertType.INFORMATION, "La columna seleccionada ya está visible.", ButtonType.OK).showAndWait();
            return;
        }
        try {
            dao.setColumnVisible(tableName, selected.columnName(), true);
            changed = true;
            load();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteColumn(ColumnConfig selected) {
        if (selected == null) return;
        if (selected.baseColumn()) {
            new Alert(Alert.AlertType.INFORMATION,
                "Las columnas base no se pueden eliminar.\nPuedes ocultarlas para que no aparezcan en pantalla.",
                ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Se eliminarán la columna '" + selected.label() + "' y todos sus datos de forma permanente.\n\nEsta acción no se puede deshacer.\n\n¿Confirmar eliminación?",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setTitle("Eliminar columna");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> {
            try {
                dao.deleteDynamic(tableName, selected.columnName());
                changed = true;
                load();
            } catch (Exception ex) {
                showError(ex);
            }
        });
    }

    private void changeColumnType(ColumnConfig selected) {
        if (selected == null) return;

        String currentInternal = selected.dataType() != null ? selected.dataType() : "TEXTO";
        String currentDisplay = switch (currentInternal) {
            case "NUMERICO" -> "NUMÉRICO";
            case "PRECIO"   -> "PRECIO";
            case "FECHA"    -> "FECHA";
            default         -> "TEXTO";
        };

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cambiar tipo de dato");
        dialog.setHeaderText("Columna: " + selected.label());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(stylesheets);

        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList(
            "TEXTO", "NUMÉRICO", "PRECIO", "FECHA"));
        cbTipo.setValue(currentDisplay);
        cbTipo.setPrefWidth(200);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Tipo de dato:"), 0, 0);
        grid.add(cbTipo, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(bt -> {
            String newType = switch (cbTipo.getValue()) {
                case "NUMÉRICO" -> "NUMERICO";
                case "PRECIO"   -> "PRECIO";
                case "FECHA"    -> "FECHA";
                default         -> "TEXTO";
            };
            if (newType.equals(currentInternal)) return;
            try {
                dao.updateDataType(tableName, selected.columnName(), newType);
                normalizeExistingValuesIfRequested(selected, newType);
                changed = true;
                load();
            } catch (Exception ex) {
                showError(ex);
            }
        });
    }

    private void normalizeExistingValuesIfRequested(ColumnConfig selected, String newType) throws SQLException {
        if (selected.baseColumn() || "TEXTO".equals(newType)) return;

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "¿Quieres adaptar los valores existentes de '" + selected.label() + "' al tipo " + displayType(newType) + "?\n\n" +
            "Se normalizarán los valores que se puedan convertir. Los valores no reconocidos se conservarán para revisión manual.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setTitle("Adaptar valores existentes");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        DynamicColumnValueDAO valueDAO = new DynamicColumnValueDAO();
        Map<Integer, String> invalid = valueDAO.findUnconvertibleValues(
            tableName,
            selected.columnName(),
            value -> TypedValueFormatter.tryNormalizeForStorage(newType, value)
        );
        if (!invalid.isEmpty() && !confirmNormalizeWithInvalidValues(selected, newType, invalid)) return;

        int updated = valueDAO.normalizeColumnValues(
            tableName,
            selected.columnName(),
            value -> TypedValueFormatter.normalizeForStorage(newType, value)
        );
        new Alert(
            Alert.AlertType.INFORMATION,
            updated + " valor(es) adaptado(s).",
            ButtonType.OK
        ).showAndWait();
    }

    private boolean confirmNormalizeWithInvalidValues(
            ColumnConfig selected,
            String newType,
            Map<Integer, String> invalid) {
        String sample = invalid.entrySet().stream()
            .limit(8)
            .map(entry -> "ID " + entry.getKey() + ": " + entry.getValue())
            .reduce("", (acc, line) -> acc + (acc.isBlank() ? "" : "\n") + line);
        String extra = invalid.size() > 8 ? "\n…" : "";
        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            invalid.size() + " valor(es) de '" + selected.label() + "' no se pueden convertir a " + displayType(newType) + ".\n\n" +
            sample + extra + "\n\n" +
            "Si continúas, esos valores se conservarán sin cambios y solo se adaptarán los convertibles.",
            ButtonType.YES, ButtonType.NO
        );
        confirm.setTitle("Valores no convertibles");
        confirm.setHeaderText(null);
        return confirm.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private String displayType(String internalType) {
        return switch (internalType) {
            case "NUMERICO" -> "NUMÉRICO";
            case "PRECIO" -> "PRECIO";
            case "FECHA" -> "FECHA";
            default -> "TEXTO";
        };
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}
