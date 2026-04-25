package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    public List<Empleado> findAll() throws SQLException {
        List<Empleado> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM empleados WHERE activo=1 ORDER BY nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Empleado> findAllIncluirBajas() throws SQLException {
        List<Empleado> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM empleados ORDER BY nombre")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Empleado findById(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM empleados WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Empleado e) throws SQLException {
        if (e.getId() == 0) insert(e); else update(e);
    }

    private void insert(Empleado e) throws SQLException {
        String sql = "INSERT INTO empleados (nombre,apellido,nif,categoria,salario_base,fecha_alta,iban,irpf,activo,telefono,email,direccion) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, e);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) e.setId(keys.getInt(1));
        }
    }

    private void update(Empleado e) throws SQLException {
        String sql = "UPDATE empleados SET nombre=?,apellido=?,nif=?,categoria=?,salario_base=?,fecha_alta=?,iban=?,irpf=?,activo=?,telefono=?,email=?,direccion=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, e);
            ps.setInt(13, e.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE empleados SET activo=0, fecha_baja=date('now') WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void reactivar(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE empleados SET activo=1, fecha_baja=NULL WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM empleados WHERE activo=1")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void set(PreparedStatement ps, Empleado e) throws SQLException {
        ps.setString(1, e.getNombre());
        ps.setString(2, e.getApellido());
        ps.setString(3, e.getNif());
        ps.setString(4, e.getCategoria());
        ps.setDouble(5, e.getSalarioBase());
        ps.setString(6, e.getFechaAlta());
        ps.setString(7, e.getIban());
        ps.setDouble(8, e.getIrpf());
        ps.setInt(9, e.isActivo() ? 1 : 0);
        ps.setString(10, e.getTelefono());
        ps.setString(11, e.getEmail());
        ps.setString(12, e.getDireccion());
    }

    private Empleado map(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setId(rs.getInt("id"));
        e.setNombre(rs.getString("nombre"));
        e.setApellido(rs.getString("apellido"));
        e.setNif(rs.getString("nif"));
        e.setCategoria(rs.getString("categoria"));
        e.setSalarioBase(rs.getDouble("salario_base"));
        e.setFechaAlta(rs.getString("fecha_alta"));
        e.setFechaBaja(rs.getString("fecha_baja"));
        e.setIban(rs.getString("iban"));
        e.setIrpf(rs.getDouble("irpf"));
        e.setActivo(rs.getInt("activo") == 1);
        e.setTelefono(rs.getString("telefono"));
        e.setEmail(rs.getString("email"));
        e.setDireccion(rs.getString("direccion"));
        return e;
    }
}
