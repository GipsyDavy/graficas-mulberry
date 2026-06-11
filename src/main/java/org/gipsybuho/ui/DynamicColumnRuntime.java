package org.gipsybuho.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import org.gipsybuho.dao.ColumnConfigDAO;
import org.gipsybuho.dao.ColumnConfigDAO.ColumnConfig;
import org.gipsybuho.dao.DynamicColumnValueDAO;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class DynamicColumnRuntime<T> {

    private final ColumnConfigDAO configDAO = new ColumnConfigDAO();
    private final DynamicColumnValueDAO valueDAO = new DynamicColumnValueDAO();
    private final String tableName;
    private final String moduleName;
    private final Map<String, String> baseColumns;
    private final TableView<T> table;
    private final ObservableList<T> data;
    private final Function<T, Integer> idProvider;
    private Map<Integer, Map<String, String>> cache = new LinkedHashMap<>();
    private final Map<String, Control> extraControls = new LinkedHashMap<>();

    public DynamicColumnRuntime(
            String tableName,
            String moduleName,
            Map<String, String> baseColumns,
            TableView<T> table,
            ObservableList<T> data,
            Function<T, Integer> idProvider) {
        this.tableName = tableName;
        this.moduleName = moduleName;
        this.baseColumns = baseColumns;
        this.table = table;
        this.data = data;
        this.idProvider = idProvider;
    }

    public void apply() {
        try {
            TableColumnSizing.enableHorizontalScroll(table);
            configDAO.syncTable(tableName, baseColumns);
            Map<String, String> labels = configDAO.visibleLabels(tableName);
            Set<String> baseIds = baseColumns.keySet();
            for (TableColumn<T, ?> column : table.getColumns()) {
                Object key = column.getUserData();
                if (key instanceof String colName && baseIds.contains(colName)) {
                    column.setText(labels.getOrDefault(colName, baseColumns.get(colName)));
                }
            }
            table.getColumns().removeIf(column -> {
                Object key = column.getUserData();
                return key instanceof String colName && !baseIds.contains(colName);
            });

            List<ColumnConfig> extras = configDAO.findVisibleDynamic(tableName, baseIds);
            List<String> extraNames = extras.stream().map(ColumnConfig::columnName).toList();
            cache = valueDAO.findValues(tableName, extraNames);
            table.setEditable(true);
            for (ColumnConfig config : extras) addColumn(config);
            TableColumnSizing.autoSizeLater(table);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    public void configure() {
        try {
            List<String> stylesheets = table.getScene() != null ? table.getScene().getStylesheets() : List.of();
            boolean changed = new ColumnConfiguratorDialog(tableName, moduleName, baseColumns, stylesheets).show();
            if (changed) apply();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    public int addFormFields(GridPane grid, int row, T item, Map<String, TextField> fields) {
        extraControls.clear();
        try {
            List<ColumnConfig> extras = configDAO.findVisibleDynamic(tableName, baseColumns.keySet());
            if (extras.isEmpty()) return row;
            List<String> extraNames = extras.stream().map(ColumnConfig::columnName).toList();
            Map<String, String> values = valueDAO.findValues(tableName, idProvider.apply(item), extraNames);

            grid.add(new Separator(), 0, row++, 4, 1);
            grid.add(new javafx.scene.control.Label("Datos adicionales"), 0, row++, 4, 1);
            for (ColumnConfig config : extras) {
                String colName = config.columnName();
                String stored = values.getOrDefault(colName, "");
                grid.add(new javafx.scene.control.Label(config.label()), 0, row);

                String tipo = config.dataType() != null ? config.dataType() : "TEXTO";
                if ("FECHA".equals(tipo)) {
                    DatePicker dp = new DatePicker();
                    if (!stored.isBlank()) {
                        try { dp.setValue(java.time.LocalDate.parse(stored)); }
                        catch (Exception ignored) {}
                    }
                    extraControls.put(colName, dp);
                    grid.add(dp, 1, row, 3, 1);
                } else {
                    TextField field = new TextField(stored);
                    if ("NUMERICO".equals(tipo)) {
                        applyNumericFilter(field, false);
                    } else if ("PRECIO".equals(tipo)) {
                        applyNumericFilter(field, true);
                        field.setPromptText("0,00");
                    }
                    fields.put(colName, field);
                    extraControls.put(colName, field);
                    grid.add(field, 1, row, 3, 1);
                }
                row++;
            }
        } catch (Exception ex) {
            showError(ex);
        }
        return row;
    }

    public void saveFormFields(T item, Map<String, TextField> fields) throws SQLException {
        Map<String, String> values = new LinkedHashMap<>();
        fields.forEach((key, field) -> values.put(key, field.getText().trim()));
        extraControls.forEach((key, control) -> {
            if (control instanceof DatePicker dp && !fields.containsKey(key)) {
                values.put(key, dp.getValue() != null ? dp.getValue().toString() : "");
            }
        });
        int id = idProvider.apply(item);
        valueDAO.updateValues(tableName, id, values);
        cache.put(id, values);
    }

    private void applyNumericFilter(TextField field, boolean allowDecimal) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll(allowDecimal ? "[^0-9.,\\-]" : "[^0-9\\-]", "");
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void addColumn(ColumnConfig config) {
        String columnName = config.columnName();
        TableColumn<T, String> column = new TableColumn<>(config.label());
        column.setUserData(columnName);
        column.setCellValueFactory(data -> {
            int id = idProvider.apply(data.getValue());
            String value = cache.getOrDefault(id, Map.of()).get(columnName);
            return new SimpleStringProperty(value != null ? value : "");
        });
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            T item = event.getRowValue();
            int id = idProvider.apply(item);
            try {
                valueDAO.updateValue(tableName, id, columnName, event.getNewValue());
                cache.computeIfAbsent(id, ignored -> new LinkedHashMap<>()).put(columnName, event.getNewValue());
            } catch (Exception ex) {
                showError(ex);
                apply();
            }
        });
        column.setMinWidth(90);
        column.setPrefWidth(Math.min(420, Math.max(130, config.label().length() * 7.4 + 38)));
        table.getColumns().add(column);
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
    }
}
