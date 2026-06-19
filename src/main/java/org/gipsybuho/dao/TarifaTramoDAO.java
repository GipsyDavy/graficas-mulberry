package org.gipsybuho.dao;

import org.gipsybuho.model.TarifaTramo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TarifaTramoDAO {

    private final Connection conn;

    public TarifaTramoDAO(Connection conn) {
        this.conn = conn;
    }

    public List<TarifaTramo> findByTarifaId(int tarifaId) throws SQLException {
        List<TarifaTramo> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM tarifa_tramos WHERE tarifa_id=? ORDER BY tiempo_minutos ASC")) {
            ps.setInt(1, tarifaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void save(TarifaTramo t) throws SQLException {
        if (t.getId() == 0) insert(t); else update(t);
    }

    private void insert(TarifaTramo t) throws SQLException {
        String sql = "INSERT INTO tarifa_tramos (tarifa_id, tiempo_minutos, precio_tiempo) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getTarifaId());
            ps.setInt(2, t.getTiempoMinutos());
            ps.setDouble(3, t.getPrecioTiempo());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) t.setId(keys.getInt(1));
        }
    }

    private void update(TarifaTramo t) throws SQLException {
        String sql = "UPDATE tarifa_tramos SET tiempo_minutos=?, precio_tiempo=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getTiempoMinutos());
            ps.setDouble(2, t.getPrecioTiempo());
            ps.setInt(3, t.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM tarifa_tramos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private TarifaTramo map(ResultSet rs) throws SQLException {
        TarifaTramo t = new TarifaTramo();
        t.setId(rs.getInt("id"));
        t.setTarifaId(rs.getInt("tarifa_id"));
        t.setTiempoMinutos(rs.getInt("tiempo_minutos"));
        t.setPrecioTiempo(rs.getDouble("precio_tiempo"));
        return t;
    }
}
