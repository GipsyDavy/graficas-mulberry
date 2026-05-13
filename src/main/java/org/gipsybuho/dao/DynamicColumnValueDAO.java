package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicColumnValueDAO {

    public Map<Integer, Map<String, String>> findValues(String tableName, List<String> columns) throws SQLException {
        DatabaseManager.requireSqlIdentifier(tableName);
        Map<Integer, Map<String, String>> values = new LinkedHashMap<>();
        if (columns.isEmpty()) return values;

        StringBuilder sql = new StringBuilder("SELECT id");
        for (String column : columns) {
            sql.append(", ").append(DatabaseManager.quoteIdentifier(column));
        }
        sql.append(" FROM ").append(DatabaseManager.quoteIdentifier(tableName));

        try (Statement st = DatabaseManager.getConnection().createStatement();
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

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString())) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
}
