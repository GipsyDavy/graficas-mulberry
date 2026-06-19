package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class DynamicColumnValueDAO {

    private final Connection conn;

    public DynamicColumnValueDAO(Connection conn) {
        this.conn = conn;
    }

    public Map<Integer, Map<String, String>> findValues(String tableName, List<String> columns) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        Map<Integer, Map<String, String>> values = new LinkedHashMap<>();
        if (columns.isEmpty()) return values;

        StringBuilder sql = new StringBuilder("SELECT id");
        for (String column : columns) {
            sql.append(", ").append(DatabaseManager.quoteIdentifier(column));
        }
        sql.append(" FROM ").append(DatabaseManager.quoteIdentifier(tableName));

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql.toString())) {
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : columns) row.put(column, rs.getString(column));
                values.put(rs.getInt("id"), row);
            }
        }
        return values;
    }

    public Map<String, String> findValues(String tableName, int id, List<String> columns) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        Map<String, String> values = new LinkedHashMap<>();
        if (id <= 0 || columns.isEmpty()) return values;

        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(DatabaseManager.quoteIdentifier(columns.get(i)));
        }
        sql.append(" FROM ").append(DatabaseManager.quoteIdentifier(tableName)).append(" WHERE id=?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                for (String column : columns) values.put(column, rs.getString(column));
            }
        }
        return values;
    }

    public void updateValue(String tableName, int id, String columnName, String value) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        DatabaseManager.requireSqlIdentifier(columnName);
        String sql = "UPDATE " + DatabaseManager.quoteIdentifier(tableName)
            + " SET " + DatabaseManager.quoteIdentifier(columnName) + "=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateValues(String tableName, int id, Map<String, String> values) throws SQLException {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            updateValue(tableName, id, entry.getKey(), entry.getValue());
        }
    }

    public Map<Integer, String> findUnconvertibleValues(
            String tableName,
            String columnName,
            Function<String, Optional<String>> normalizer) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        DatabaseManager.requireSqlIdentifier(columnName);

        Map<Integer, String> invalid = new LinkedHashMap<>();
        String sql = "SELECT id, " + DatabaseManager.quoteIdentifier(columnName)
            + " FROM " + DatabaseManager.quoteIdentifier(tableName);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String current = rs.getString(columnName);
                if (current != null && !current.isBlank() && normalizer.apply(current).isEmpty()) {
                    invalid.put(rs.getInt("id"), current);
                }
            }
        }
        return invalid;
    }

    public int normalizeColumnValues(String tableName, String columnName, Function<String, String> normalizer)
            throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        DatabaseManager.requireSqlIdentifier(columnName);

        Map<Integer, String> updates = new LinkedHashMap<>();
        String sql = "SELECT id, " + DatabaseManager.quoteIdentifier(columnName)
            + " FROM " + DatabaseManager.quoteIdentifier(tableName);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String current = rs.getString(columnName);
                String normalized = normalizer.apply(current);
                if (normalized == null) normalized = "";
                String currentSafe = current != null ? current : "";
                if (!currentSafe.equals(normalized)) {
                    updates.put(rs.getInt("id"), normalized);
                }
            }
        }

        boolean previousAutoCommit = conn.getAutoCommit();
        Savepoint savepoint = null;
        if (previousAutoCommit) {
            conn.setAutoCommit(false);
        } else {
            savepoint = conn.setSavepoint();
        }
        try {
            for (Map.Entry<Integer, String> entry : updates.entrySet()) {
                updateValue(tableName, entry.getKey(), columnName, entry.getValue());
            }
            if (previousAutoCommit) conn.commit();
        } catch (SQLException ex) {
            if (savepoint != null) conn.rollback(savepoint);
            else conn.rollback();
            throw ex;
        } finally {
            if (savepoint != null) {
                try { conn.releaseSavepoint(savepoint); } catch (SQLException ignored) {}
            }
            if (previousAutoCommit) conn.setAutoCommit(true);
        }
        return updates.size();
    }
}
