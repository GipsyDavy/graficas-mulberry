package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public List<Cliente> findAll() throws SQLException {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM clientes ORDER BY nombre";
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Cliente> search(String texto) throws SQLException {
        List<Cliente> list = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE nombre LIKE ? OR apellido LIKE ? OR nif LIKE ? OR email LIKE ? ORDER BY nombre";
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
        String sql = "INSERT INTO clientes (nombre,apellido,tipo,nif,direccion,ciudad,cp,telefono,email,notas) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, c);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        }
    }

    private void update(Cliente c) throws SQLException {
        String sql = "UPDATE clientes SET nombre=?,apellido=?,tipo=?,nif=?,direccion=?,ciudad=?,cp=?,telefono=?,email=?,notas=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, c);
            ps.setInt(11, c.getId());
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

    private void set(PreparedStatement ps, Cliente c) throws SQLException {
        ps.setString(1, c.getNombre());
        ps.setString(2, c.getApellido());
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
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setTipo(rs.getString("tipo"));
        c.setNif(rs.getString("nif"));
        c.setDireccion(rs.getString("direccion"));
        c.setCiudad(rs.getString("ciudad"));
        c.setCp(rs.getString("cp"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setNotas(rs.getString("notas"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    }
}
