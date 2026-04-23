package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterialDAO {

    public List<Material> findAll() throws SQLException {
        List<Material> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM materiales ORDER BY categoria, nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Material> findBajoStock() throws SQLException {
        List<Material> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM materiales WHERE stock_actual <= stock_minimo ORDER BY nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countBajoStock() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM materiales WHERE stock_actual <= stock_minimo")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Material findById(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM materiales WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Material m) throws SQLException {
        if (m.getId() == 0) insert(m); else update(m);
    }

    private void insert(Material m) throws SQLException {
        String sql = "INSERT INTO materiales (nombre,referencia,categoria,stock_actual,stock_minimo,unidad,precio_unidad,proveedor) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, m);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) m.setId(keys.getInt(1));
        }
    }

    private void update(Material m) throws SQLException {
        String sql = "UPDATE materiales SET nombre=?,referencia=?,categoria=?,stock_actual=?,stock_minimo=?,unidad=?,precio_unidad=?,proveedor=?,updated_at=datetime('now') WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, m);
            ps.setInt(9, m.getId());
            ps.executeUpdate();
        }
    }

    public void ajustarStock(int id, double cantidad, String tipo, String descripcion) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        Material m = findById(id);
        if (m == null) return;
        double nuevoStock = tipo.equals("entrada") ? m.getStockActual() + cantidad : m.getStockActual() - cantidad;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE materiales SET stock_actual=?, updated_at=datetime('now') WHERE id=?")) {
            ps.setDouble(1, nuevoStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO movimientos_material (material_id,tipo,cantidad,fecha,descripcion) VALUES (?,?,?,date('now'),?)")) {
            ps.setInt(1, id);
            ps.setString(2, tipo);
            ps.setDouble(3, cantidad);
            ps.setString(4, descripcion);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM materiales WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void set(PreparedStatement ps, Material m) throws SQLException {
        ps.setString(1, m.getNombre());
        ps.setString(2, m.getReferencia());
        ps.setString(3, m.getCategoria());
        ps.setDouble(4, m.getStockActual());
        ps.setDouble(5, m.getStockMinimo());
        ps.setString(6, m.getUnidad());
        ps.setDouble(7, m.getPrecioUnidad());
        ps.setString(8, m.getProveedor());
    }

    private Material map(ResultSet rs) throws SQLException {
        Material m = new Material();
        m.setId(rs.getInt("id"));
        m.setNombre(rs.getString("nombre"));
        m.setReferencia(rs.getString("referencia"));
        m.setCategoria(rs.getString("categoria"));
        m.setStockActual(rs.getDouble("stock_actual"));
        m.setStockMinimo(rs.getDouble("stock_minimo"));
        m.setUnidad(rs.getString("unidad"));
        m.setPrecioUnidad(rs.getDouble("precio_unidad"));
        m.setProveedor(rs.getString("proveedor"));
        m.setUpdatedAt(rs.getString("updated_at"));
        return m;
    }
}
