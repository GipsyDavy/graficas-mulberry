package org.gipsybuho.dao;

import org.gipsybuho.model.NotaCalendario;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotaCalendarioDAO {

    private final Connection conn;

    public NotaCalendarioDAO(Connection conn) {
        this.conn = conn;
    }

    public void guardar(NotaCalendario nota) throws SQLException {
        if (nota.getId() == 0) insertar(nota);
        else actualizar(nota);
    }

    private void insertar(NotaCalendario nota) throws SQLException {
        String sql = "INSERT INTO notas_calendario (fecha, titulo, nota) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nota.getFecha().toString());
            ps.setString(2, nota.getTitulo());
            ps.setString(3, nota.getNota());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) nota.setId(rs.getInt(1));
            }
        }
    }

    private void actualizar(NotaCalendario nota) throws SQLException {
        String sql = "UPDATE notas_calendario SET titulo=?, nota=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nota.getTitulo());
            ps.setString(2, nota.getNota());
            ps.setInt(3, nota.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM notas_calendario WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<NotaCalendario> findByFecha(LocalDate fecha) throws SQLException {
        List<NotaCalendario> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM notas_calendario WHERE fecha=? ORDER BY id")) {
            ps.setString(1, fecha.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<LocalDate> fechasConNotas(int anio, int mes) throws SQLException {
        List<LocalDate> list = new ArrayList<>();
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin    = inicio.withDayOfMonth(inicio.lengthOfMonth());
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT fecha FROM notas_calendario WHERE fecha >= ? AND fecha <= ?")) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fin.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(LocalDate.parse(rs.getString("fecha")));
        }
        return list;
    }

    public List<NotaCalendario> findProximas(int dias) throws SQLException {
        List<NotaCalendario> list = new ArrayList<>();
        LocalDate hoy   = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM notas_calendario WHERE fecha >= ? AND fecha <= ? ORDER BY fecha, id")) {
            ps.setString(1, hoy.toString());
            ps.setString(2, limite.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private NotaCalendario map(ResultSet rs) throws SQLException {
        NotaCalendario n = new NotaCalendario();
        n.setId(rs.getInt("id"));
        n.setFecha(LocalDate.parse(rs.getString("fecha")));
        n.setTitulo(rs.getString("titulo"));
        n.setNota(rs.getString("nota"));
        return n;
    }
}
