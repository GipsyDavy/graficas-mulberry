package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Tarifa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TarifaDAO {

    public List<Tarifa> findAll() throws SQLException {
        List<Tarifa> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tarifas ORDER BY tecnica, nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Tarifa> findByTecnica(String tecnica) throws SQLException {
        List<Tarifa> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM tarifas WHERE tecnica=? AND activa=1 ORDER BY nombre")) {
            ps.setString(1, tecnica);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Tarifa findById(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM tarifas WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Tarifa t) throws SQLException {
        if (t.getId() == 0) insert(t); else update(t);
    }

    private void insert(Tarifa t) throws SQLException {
        String sql = "INSERT INTO tarifas (tecnica,nombre,descripcion,precio_unit,precio_setup,minimo_unidades,activa) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, t);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) t.setId(keys.getInt(1));
        }
    }

    private void update(Tarifa t) throws SQLException {
        String sql = "UPDATE tarifas SET tecnica=?,nombre=?,descripcion=?,precio_unit=?,precio_setup=?,minimo_unidades=?,activa=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, t);
            ps.setInt(8, t.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM tarifas WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void set(PreparedStatement ps, Tarifa t) throws SQLException {
        ps.setString(1, t.getTecnica());
        ps.setString(2, t.getNombre());
        ps.setString(3, t.getDescripcion());
        ps.setDouble(4, t.getPrecioUnit());
        ps.setDouble(5, t.getPrecioSetup());
        ps.setInt(6, t.getMinimoUnidades());
        ps.setInt(7, t.isActiva() ? 1 : 0);
    }

    private Tarifa map(ResultSet rs) throws SQLException {
        Tarifa t = new Tarifa();
        t.setId(rs.getInt("id"));
        t.setTecnica(rs.getString("tecnica"));
        t.setNombre(rs.getString("nombre"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setPrecioUnit(rs.getDouble("precio_unit"));
        t.setPrecioSetup(rs.getDouble("precio_setup"));
        t.setMinimoUnidades(rs.getInt("minimo_unidades"));
        t.setActiva(rs.getInt("activa") == 1);
        t.setUpdatedAt(rs.getString("updated_at"));
        return t;
    }
}
