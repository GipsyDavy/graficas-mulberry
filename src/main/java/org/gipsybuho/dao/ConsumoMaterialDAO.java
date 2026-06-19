package org.gipsybuho.dao;

import org.gipsybuho.model.ConsumoMaterial;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsumoMaterialDAO {

    private final Connection conn;

    public ConsumoMaterialDAO(Connection conn) {
        this.conn = conn;
    }

    public List<ConsumoMaterial> findAll() throws SQLException {
        List<ConsumoMaterial> list = new ArrayList<>();
        String sql = """
            SELECT cmt.*, m.nombre AS material_nombre, m.unidad
            FROM consumo_material_tecnica cmt
            JOIN materiales m ON cmt.material_id = m.id
            ORDER BY cmt.tecnica, m.nombre
            """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ConsumoMaterial> findByTecnica(String tecnica) throws SQLException {
        List<ConsumoMaterial> list = new ArrayList<>();
        String sql = """
            SELECT cmt.*, m.nombre AS material_nombre, m.unidad
            FROM consumo_material_tecnica cmt
            JOIN materiales m ON cmt.material_id = m.id
            WHERE cmt.tecnica = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tecnica);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void save(ConsumoMaterial c) throws SQLException {
        if (c.getId() == 0) insert(c); else update(c);
    }

    private void insert(ConsumoMaterial c) throws SQLException {
        String sql = "INSERT INTO consumo_material_tecnica (tecnica, material_id, cantidad_por_unidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTecnica());
            ps.setInt(2, c.getMaterialId());
            ps.setDouble(3, c.getCantidadPorUnidad());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        }
    }

    private void update(ConsumoMaterial c) throws SQLException {
        String sql = "UPDATE consumo_material_tecnica SET tecnica=?, material_id=?, cantidad_por_unidad=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getTecnica());
            ps.setInt(2, c.getMaterialId());
            ps.setDouble(3, c.getCantidadPorUnidad());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM consumo_material_tecnica WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private ConsumoMaterial map(ResultSet rs) throws SQLException {
        ConsumoMaterial c = new ConsumoMaterial();
        c.setId(rs.getInt("id"));
        c.setTecnica(rs.getString("tecnica"));
        c.setMaterialId(rs.getInt("material_id"));
        c.setMaterialNombre(rs.getString("material_nombre"));
        c.setUnidad(rs.getString("unidad"));
        c.setCantidadPorUnidad(rs.getDouble("cantidad_por_unidad"));
        return c;
    }
}