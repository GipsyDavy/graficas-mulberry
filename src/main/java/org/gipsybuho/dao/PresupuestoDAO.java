package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.LineaPresupuesto;
import org.gipsybuho.model.Presupuesto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoDAO {

    public List<Presupuesto> findAll() throws SQLException {
        List<Presupuesto> list = new ArrayList<>();
        String sql = """
            SELECT p.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellidos, ''), '')) as cliente_nombre
            FROM presupuestos p
            LEFT JOIN clientes c ON p.cliente_id = c.id
            ORDER BY p.created_at DESC
            """;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Presupuesto findById(int id) throws SQLException {
        String sql = """
            SELECT p.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellidos, ''), '')) as cliente_nombre
            FROM presupuestos p
            LEFT JOIN clientes c ON p.cliente_id = c.id
            WHERE p.id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Presupuesto p = map(rs);
            p.setLineas(findLineas(id));
            return p;
        }
    }

    public List<LineaPresupuesto> findLineas(int presupuestoId) throws SQLException {
        List<LineaPresupuesto> lineas = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM lineas_presupuesto WHERE presupuesto_id=? ORDER BY orden")) {
            ps.setInt(1, presupuestoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lineas.add(mapLinea(rs));
        }
        return lineas;
    }

    public void save(Presupuesto p) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        boolean externalTx = !conn.getAutoCommit();
        if (!externalTx) conn.setAutoCommit(false);
        try {
            if (p.getId() == 0) insert(p); else update(p);
            saveLineas(p);
            if (!externalTx) conn.commit();
        } catch (SQLException e) {
            if (!externalTx) conn.rollback();
            throw e;
        } finally {
            if (!externalTx) conn.setAutoCommit(true);
        }
    }

    private void insert(Presupuesto p) throws SQLException {
        String sql = "INSERT INTO presupuestos (numero,cliente_id,fecha,fecha_validez,estado,base_imponible,iva_porcentaje,iva_importe,total,notas,condiciones) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, p);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        }
    }

    private void update(Presupuesto p) throws SQLException {
        String sql = "UPDATE presupuestos SET numero=?,cliente_id=?,fecha=?,fecha_validez=?,estado=?,base_imponible=?,iva_porcentaje=?,iva_importe=?,total=?,notas=?,condiciones=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, p);
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        }
    }

    private void saveLineas(Presupuesto p) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM lineas_presupuesto WHERE presupuesto_id=?")) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        }
        String sql = "INSERT INTO lineas_presupuesto (presupuesto_id,descripcion,tecnica,cantidad,precio_unit,descuento,total,orden) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            int orden = 0;
            for (LineaPresupuesto l : p.getLineas()) {
                ps.setInt(1, p.getId());
                ps.setString(2, l.getDescripcion());
                ps.setString(3, l.getTecnica());
                ps.setInt(4, l.getCantidad());
                ps.setDouble(5, l.getPrecioUnit());
                ps.setDouble(6, l.getDescuento());
                ps.setDouble(7, l.getTotal());
                ps.setInt(8, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void updateEstado(int id, String estado) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE presupuestos SET estado=? WHERE id=?")) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM presupuestos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countByEstado(String estado) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM presupuestos WHERE estado=?")) {
            ps.setString(1, estado);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void set(PreparedStatement ps, Presupuesto p) throws SQLException {
        ps.setString(1, p.getNumero());
        ps.setInt(2, p.getClienteId());
        ps.setString(3, p.getFecha());
        ps.setString(4, p.getFechaValidez());
        ps.setString(5, p.getEstado());
        ps.setDouble(6, p.getBaseImponible());
        ps.setDouble(7, p.getIvaPorcentaje());
        ps.setDouble(8, p.getIvaImporte());
        ps.setDouble(9, p.getTotal());
        ps.setString(10, p.getNotas());
        ps.setString(11, p.getCondiciones());
    }

    private Presupuesto map(ResultSet rs) throws SQLException {
        Presupuesto p = new Presupuesto();
        p.setId(rs.getInt("id"));
        p.setNumero(rs.getString("numero"));
        p.setClienteId(rs.getInt("cliente_id"));
        p.setClienteNombre(rs.getString("cliente_nombre"));
        p.setFecha(rs.getString("fecha"));
        p.setFechaValidez(rs.getString("fecha_validez"));
        p.setEstado(rs.getString("estado"));
        p.setBaseImponible(rs.getDouble("base_imponible"));
        p.setIvaPorcentaje(rs.getDouble("iva_porcentaje"));
        p.setIvaImporte(rs.getDouble("iva_importe"));
        p.setTotal(rs.getDouble("total"));
        p.setNotas(rs.getString("notas"));
        p.setCondiciones(rs.getString("condiciones"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
    }

    private LineaPresupuesto mapLinea(ResultSet rs) throws SQLException {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setId(rs.getInt("id"));
        l.setPresupuestoId(rs.getInt("presupuesto_id"));
        l.setDescripcion(rs.getString("descripcion"));
        l.setTecnica(rs.getString("tecnica"));
        l.setCantidad(rs.getInt("cantidad"));
        l.setPrecioUnit(rs.getDouble("precio_unit"));
        l.setDescuento(rs.getDouble("descuento"));
        l.setTotal(rs.getDouble("total"));
        l.setOrden(rs.getInt("orden"));
        return l;
    }
}
