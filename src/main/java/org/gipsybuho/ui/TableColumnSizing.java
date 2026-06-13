package org.gipsybuho.ui;

import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Duration;

final class TableColumnSizing {

    private static final double CHAR_WIDTH = 7.4;
    private static final double PADDING = 38.0;

    private TableColumnSizing() {
    }

    static <S> void enableHorizontalScroll(TableView<S> table) {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    static <S> void autoSizeLater(TableView<S> table) {
        autoSizeLater(table, 80, 420, 120);
    }

    static <S> void autoSizeLater(TableView<S> table, double minWidth, double maxWidth, int sampleRows) {
        Platform.runLater(() -> autoSize(table, minWidth, maxWidth, sampleRows));
    }

    static <S> void autoSize(TableView<S> table, double minWidth, double maxWidth, int sampleRows) {
        enableHorizontalScroll(table);
        for (TableColumn<S, ?> column : table.getColumns()) {
            autoSizeColumn(column, table, minWidth, maxWidth, sampleRows);
        }
    }

    private static <S, T> void autoSizeColumn(
            TableColumn<S, T> column,
            TableView<S> table,
            double minWidth,
            double maxWidth,
            int sampleRows) {
        double width = estimate(column.getText());
        int limit = Math.min(sampleRows, table.getItems().size());
        for (int i = 0; i < limit; i++) {
            ObservableValue<T> value = column.getCellObservableValue(table.getItems().get(i));
            Object raw = value != null ? value.getValue() : null;
            width = Math.max(width, estimate(raw != null ? raw.toString() : ""));
        }
        double target = Math.max(minWidth, Math.min(maxWidth, width));
        column.setMinWidth(Math.min(minWidth, target));
        column.setPrefWidth(target);
        column.setResizable(true);
    }

    static void animarFilas(TableView<?> tabla) {
        // Doble runLater: el VirtualFlow necesita dos ciclos de layout para crear las celdas
        Platform.runLater(() -> Platform.runLater(() -> {
            List<TableRow<?>> filas = tabla.lookupAll(".table-row-cell").stream()
                .filter(n -> n instanceof TableRow<?>)
                .<TableRow<?>>map(n -> (TableRow<?>) n)
                .filter(r -> !r.isEmpty())
                .sorted(java.util.Comparator.comparingDouble(
                    r -> r.localToScene(r.getBoundsInLocal()).getMinY()))
                .limit(10)
                .toList();
            for (int i = 0; i < filas.size(); i++) {
                TableRow<?> row = filas.get(i);
                int delay = i * 45;
                row.setOpacity(0);
                row.setTranslateY(22);
                FadeTransition ft = new FadeTransition(Duration.millis(260), row);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.setDelay(Duration.millis(delay));
                TranslateTransition tt = new TranslateTransition(Duration.millis(260), row);
                tt.setFromY(22);
                tt.setToY(0);
                tt.setDelay(Duration.millis(delay));
                ft.play();
                tt.play();
            }
        }));
    }

    private static double estimate(String text) {
        if (text == null || text.isBlank()) return PADDING;
        int longest = 0;
        for (String line : text.split("\\R")) {
            longest = Math.max(longest, line.length());
        }
        return longest * CHAR_WIDTH + PADDING;
    }
}
