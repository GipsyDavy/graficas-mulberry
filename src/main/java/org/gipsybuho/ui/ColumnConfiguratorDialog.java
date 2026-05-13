package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.dao.ColumnConfigDAO.ColumnConfig;

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
                String tipo = item.baseColumn() ? "base" : "usuario";
                String estado = item.visible() ? "" : " (oculta)";
                setText(item.label() + "  [" + tipo + "]" + estado);
            }
        });

        Button add = new Button("+ Añadir");
        Button rename = new Button("Renombrar");
        Button show = new Button("Mostrar");
        Button hide = new Button("Ocultar");
        add.setMaxWidth(Double.MAX_VALUE);
        rename.setMaxWidth(Double.MAX_VALUE);
        show.setMaxWidth(Double.MAX_VALUE);
        hide.setMaxWidth(Double.MAX_VALUE);

        add.setOnAction(e -> addColumn());
        rename.setOnAction(e -> renameColumn(listView.getSelectionModel().getSelectedItem()));
        show.setOnAction(e -> showColumn(listView.getSelectionModel().getSelectedItem()));
        hide.setOnAction(e -> hideColumn(listView.getSelectionModel().getSelectedItem()));

        VBox actions = new VBox(8, add, rename, show, hide);
        HBox.setHgrow(listView, Priority.ALWAYS);
        HBox content = new HBox(12, listView, actions);
        content.setPadding(new Insets(12));

        Label note = new Label("Las columnas base solo se renombran. Las columnas de usuario se ocultan para no perder datos.");
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
        TextInputDialog input = new TextInputDialog();
        input.setTitle("Nueva columna");
        input.setHeaderText(null);
        input.setContentText("Nombre de la columna:");
        input.showAndWait().ifPresent(label -> {
            try {
                Set<String> reserved = new java.util.HashSet<>(baseColumns.keySet());
                reserved.addAll(ignoredColumns);
                dao.addDynamicColumn(tableName, label, reserved);
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
        if (selected.baseColumn()) {
            new Alert(Alert.AlertType.INFORMATION, "Las columnas base no se pueden eliminar ni ocultar.", ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "La columna se ocultará, pero sus datos permanecerán en SQLite.\n\n¿Continuar?",
            ButtonType.YES,
            ButtonType.NO
        );
        confirm.setTitle("Ocultar columna");
        confirm.setHeaderText(null);
        confirm.showAndWait().filter(ButtonType.YES::equals).ifPresent(button -> {
            try {
                dao.hideDynamic(tableName, selected.columnName());
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
            dao.showDynamic(tableName, selected.columnName());
            changed = true;
            load();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}
