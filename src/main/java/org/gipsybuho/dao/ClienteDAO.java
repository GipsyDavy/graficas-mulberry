package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;

import java.sql.*;
import java.util.*;

public class ClienteDAO {

    private static final Set<String> CAMPOS_FIJOS = Set.of(
        "id", "nombre", "apellidos", "apellido", "tipo", "nif",
        "direccion", "ciudad", "cp", "telefono", "email", "notas", "created_at"
    );

    public List<Cliente> findAll() throws SQLException {
        List<Cliente> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM clientes ORDER BY nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Cliente> search(String texto) throws SQLException {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE nombre LIKE ? OR apellidos LIKE ? OR nif LIKE ? OR email LIKE ? ORDER BY nombre";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            String q = "%" + texto + "%";
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q); ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Cliente findById(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM clientes WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Cliente c) throws SQLException {
        if (c.getId() == 0) insert(c); else update(c);
    }

    private void insert(Cliente c) throws SQLException {
        List<String> cols = new ArrayList<>(List.of(
            "nombre", "apellidos", "tipo", "nif", "direccion", "ciudad", "cp", "telefono", "email", "notas"
        ));
        List<String> extraKeys = new ArrayList<>(c.getExtras().keySet());
        cols.addAll(extraKeys);

        String sql = "INSERT INTO clientes (" + quotedColumns(cols) + ") VALUES (" +
            String.join(",", Collections.nCopies(cols.size(), "?")) + ")";

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setBase(ps, c);
            int idx = 11;
            for (String key : extraKeys) ps.setString(idx++, c.getExtra(key));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        }
    }

    private void update(Cliente c) throws SQLException {
        List<String> baseFields = List.of(
            "nombre", "apellidos", "tipo", "nif", "direccion", "ciudad", "cp", "telefono", "email", "notas"
        );
        List<String> extraKeys = new ArrayList<>(c.getExtras().keySet());

        StringBuilder sql = new StringBuilder("UPDATE clientes SET ");
        List<String> sets = new ArrayList<>();
        baseFields.forEach(f -> sets.add(f + "=?"));
        extraKeys.forEach(k -> sets.add("\"" + k + "\"=?"));
        sql.append(String.join(",", sets)).append(" WHERE id=?");

        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString())) {
            setBase(ps, c);
            int idx = 11;
            for (String key : extraKeys) ps.setString(idx++, c.getExtra(key));
            ps.setInt(idx, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM clientes WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM clientes")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Devuelve los nombres de columnas extra (fuera de los campos fijos) presentes en la tabla. */
    public List<String> obtenerColumnasExtra() throws SQLException {
        List<String> extras = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(clientes)")) {
            while (rs.next()) {
                String col = rs.getString("name").toLowerCase();
                if (!CAMPOS_FIJOS.contains(col)) extras.add(col);
            }
        }
        return extras;
    }

    private String quotedColumns(List<String> columns) {
        return columns.stream()
            .map(DatabaseManager::quoteIdentifier)
            .collect(java.util.stream.Collectors.joining(","));
    }

    private void setBase(PreparedStatement ps, Cliente c) throws SQLException {
        ps.setString(1, c.getNombre());
        ps.setString(2, c.getApellidos());
        ps.setString(3, c.getTipo());
        ps.setString(4, c.getNif());
        ps.setString(5, c.getDireccion());
        ps.setString(6, c.getCiudad());
        ps.setString(7, c.getCp());
        ps.setString(8, c.getTelefono());
        ps.setString(9, c.getEmail());
        ps.setString(10, c.getNotas());
    }

    private Cliente map(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            String col = meta.getColumnName(i).toLowerCase();
            switch (col) {
                case "id"         -> c.setId(rs.getInt(i));
                case "nombre"              -> c.setNombre(rs.getString(i));
                case "apellidos", "apellido" -> c.setApellidos(rs.getString(i));
                case "tipo"       -> c.setTipo(rs.getString(i));
                case "nif"        -> c.setNif(rs.getString(i));
                case "direccion"  -> c.setDireccion(rs.getString(i));
                case "ciudad"     -> c.setCiudad(rs.getString(i));
                case "cp"         -> c.setCp(rs.getString(i));
                case "telefono"   -> c.setTelefono(rs.getString(i));
                case "email"      -> c.setEmail(rs.getString(i));
                case "notas"      -> c.setNotas(rs.getString(i));
                case "created_at" -> c.setCreatedAt(rs.getString(i));
                default           -> { String v = rs.getString(i); if (v != null) c.setExtra(col, v); }
            }
        }
        return c;
    }
}
