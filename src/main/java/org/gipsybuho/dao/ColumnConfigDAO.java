package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ColumnConfigDAO {

    public record ColumnConfig(String tableName, String columnName, String label, boolean baseColumn, boolean visible) {}

    public void syncTable(String tableName, Map<String, String> baseColumns) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        ensureConfigTable();
        for (Map.Entry<String, String> entry : baseColumns.entrySet()) {
            upsertConfig(tableName, entry.getKey(), entry.getValue(), true, true);
        }
        for (String column : physicalColumns(tableName)) {
            if ("id".equalsIgnoreCase(column) || baseColumns.containsKey(column)) continue;
            upsertConfig(tableName, column, defaultLabel(column), false, true);
        }
    }

    public List<ColumnConfig> findAll(String tableName) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        ensureConfigTable();
        List<ColumnConfig> configs = new ArrayList<>();
        String sql = """
            SELECT table_name, column_name, label, base_column, visible
            FROM column_configs
            WHERE table_name=?
            ORDER BY base_column DESC, created_at, column_name
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, tableName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) configs.add(map(rs));
        }
        return configs;
    }

    public List<ColumnConfig> findVisibleDynamic(String tableName, Set<String> baseColumnNames) throws SQLException {
        List<ColumnConfig> configs = new ArrayList<>();
        for (ColumnConfig config : findAll(tableName)) {
            if (config.visible() && !config.baseColumn() && !baseColumnNames.contains(config.columnName())) {
                configs.add(config);
            }
        }
        return configs;
    }

    public Map<String, String> visibleLabels(String tableName) throws SQLException {
        Map<String, String> labels = new LinkedHashMap<>();
        for (ColumnConfig config : findAll(tableName)) {
            if (config.visible()) labels.put(config.columnName(), config.label());
        }
        return labels;
    }

    public ColumnConfig addDynamicColumn(String tableName, String label, Set<String> baseColumnNames) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        String cleanLabel = cleanLabel(label);
        if (cleanLabel.isBlank()) throw new IllegalArgumentException("El nombre de la columna no puede estar vacío.");

        Set<String> existing = new java.util.HashSet<>(physicalColumns(tableName));
        existing.addAll(baseColumnNames);
        String baseName = normalizeColumnName(cleanLabel);
        String columnName = uniqueColumnName(baseName, existing);

        DatabaseManager.addColumn(tableName, columnName);
        upsertConfig(tableName, columnName, cleanLabel, false, true);
        return new ColumnConfig(tableName, columnName, cleanLabel, false, true);
    }

    public void rename(String tableName, String columnName, String label) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        DatabaseManager.requireSqlIdentifier(columnName);
        String cleanLabel = cleanLabel(label);
        if (cleanLabel.isBlank()) throw new IllegalArgumentException("La etiqueta no puede estar vacía.");

        ensureConfigTable();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE column_configs SET label=? WHERE table_name=? AND column_name=?")) {
            ps.setString(1, cleanLabel);
            ps.setString(2, tableName);
            ps.setString(3, columnName);
            ps.executeUpdate();
        }
    }

    public void hideDynamic(String tableName, String columnName) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        DatabaseManager.requireSqlIdentifier(columnName);
        ensureConfigTable();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE column_configs SET visible=0 WHERE table_name=? AND column_name=? AND base_column=0")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ps.executeUpdate();
        }
    }

    private void ensureConfigTable() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS column_configs (
                    table_name TEXT NOT NULL,
                    column_name TEXT NOT NULL,
                    label TEXT NOT NULL,
                    base_column INTEGER DEFAULT 0,
                    visible INTEGER DEFAULT 1,
                    created_at TEXT DEFAULT (datetime('now')),
                    PRIMARY KEY (table_name, column_name)
                )""");
        }
    }

    private void upsertConfig(String tableName, String columnName, String label, boolean baseColumn, boolean visible)
            throws SQLException {
        DatabaseManager.requireSqlIdentifier(columnName);
        String sql = """
            INSERT INTO column_configs (table_name, column_name, label, base_column, visible)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(table_name, column_name) DO UPDATE SET
                base_column=excluded.base_column,
                visible=CASE WHEN column_configs.visible=0 AND excluded.base_column=0 THEN 0 ELSE excluded.visible END
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ps.setString(3, label);
            ps.setInt(4, baseColumn ? 1 : 0);
            ps.setInt(5, visible ? 1 : 0);
            ps.executeUpdate();
        }
    }

    private List<String> physicalColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + DatabaseManager.quoteIdentifier(tableName) + ")")) {
            while (rs.next()) columns.add(rs.getString("name").toLowerCase(Locale.ROOT));
        }
        return columns;
    }

    private ColumnConfig map(ResultSet rs) throws SQLException {
        return new ColumnConfig(
            rs.getString("table_name"),
            rs.getString("column_name"),
            rs.getString("label"),
            rs.getInt("base_column") == 1,
            rs.getInt("visible") == 1
        );
    }

    private String uniqueColumnName(String baseName, Set<String> existing) {
        String candidate = baseName;
        int suffix = 2;
        while (existing.contains(candidate)) {
            candidate = baseName + "_" + suffix++;
        }
        return candidate;
    }

    private String normalizeColumnName(String label) {
        String normalized = Normalizer.normalize(label, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) normalized = "campo";
        if (!Character.isLetter(normalized.charAt(0))) normalized = "campo_" + normalized;
        return "ext_" + normalized;
    }

    private String defaultLabel(String columnName) {
        String label = columnName.startsWith("ext_") ? columnName.substring(4) : columnName;
        label = label.replace('_', ' ').trim();
        return label.isBlank() ? columnName : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private String cleanLabel(String label) {
        return label == null ? "" : label.trim().replaceAll("\\s+", " ");
    }
}
