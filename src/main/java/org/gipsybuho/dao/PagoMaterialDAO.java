package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.PagoMaterial;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PagoMaterialDAO {

    private static final String SELECT_BASE = """
        SELECT pm.*, m.nombre AS mat_nombre, m.unidad AS mat_unidad
        FROM pagos_material pm
        LEFT JOIN materiales m ON pm.material_id = m.id
        """;

    public List<PagoMaterial> findAll() throws SQLException {
        List<PagoMaterial> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE +
                 "ORDER BY CASE pm.estado WHEN 'pendiente' THEN 0 ELSE 1 END, pm.fecha_vencimiento ASC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<PagoMaterial> findPendientes() throws SQLException {
        List<PagoMaterial> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE +
                 "WHERE pm.estado = 'pendiente' ORDER BY pm.fecha_vencimiento ASC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<PagoMaterial> findVencidos() throws SQLException {
        List<PagoMaterial> list = new ArrayList<>();
        String hoy = LocalDate.now().toString();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                SELECT_BASE + "WHERE pm.estado='pendiente' AND pm.fecha_vencimiento < ? ORDER BY pm.fecha_vencimiento")) {
            ps.setString(1, hoy);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<PagoMaterial> findProximosVencimientos(int dias) throws SQLException {
        List<PagoMaterial> list = new ArrayList<>();
        String hoy   = LocalDate.now().toString();
        String hasta = LocalDate.now().plusDays(dias).toString();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                SELECT_BASE + "WHERE pm.estado='pendiente' AND pm.fecha_vencimiento >= ? AND pm.fecha_vencimiento <= ? ORDER BY pm.fecha_vencimiento")) {
            ps.setString(1, hoy);
            ps.setString(2, hasta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countVencidos() throws SQLException {
        String hoy = LocalDate.now().toString();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM pagos_material WHERE estado='pendiente' AND fecha_vencimiento < ?")) {
            ps.setString(1, hoy);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public double totalPendiente() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COALESCE(SUM(importe_total),0) FROM pagos_material WHERE estado='pendiente'")) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public void save(PagoMaterial p) throws SQLException {
        if (p.getId() == 0) insert(p); else update(p);
    }

    private void insert(PagoMaterial p) throws SQLException {
        String sql = """
            INSERT INTO pagos_material
              (material_id, proveedor, numero_factura, fecha_compra,
               cantidad_comprada, unidad, importe_total, forma_pago,
               fecha_vencimiento, estado, fecha_pago, notas)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        }
    }

    private void update(PagoMaterial p) throws SQLException {
        String sql = """
            UPDATE pagos_material SET
              material_id=?, proveedor=?, numero_factura=?, fecha_compra=?,
              cantidad_comprada=?, unidad=?, importe_total=?, forma_pago=?,
              fecha_vencimiento=?, estado=?, fecha_pago=?, notas=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            bind(ps, p);
            ps.setInt(13, p.getId());
            ps.executeUpdate();
        }
    }

    public void marcarPagado(int id, LocalDate fechaPago) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE pagos_material SET estado='pagado', fecha_pago=? WHERE id=?")) {
            ps.setString(1, fechaPago != null ? fechaPago.toString() : LocalDate.now().toString());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM pagos_material WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, PagoMaterial p) throws SQLException {
        ps.setInt(1, p.getMaterialId());
        ps.setString(2, p.getProveedor());
        ps.setString(3, p.getNumeroFactura());
        ps.setString(4, p.getFechaCompra() != null ? p.getFechaCompra().toString() : null);
        ps.setDouble(5, p.getCantidadComprada());
        ps.setString(6, p.getUnidad());
        ps.setDouble(7, p.getImporteTotal());
        ps.setString(8, p.getFormaPago());
        ps.setString(9, p.getFechaVencimiento() != null ? p.getFechaVencimiento().toString() : null);
        ps.setString(10, p.getEstado() != null ? p.getEstado() : "pendiente");
        ps.setString(11, p.getFechaPago() != null ? p.getFechaPago().toString() : null);
        ps.setString(12, p.getNotas());
    }

    private PagoMaterial map(ResultSet rs) throws SQLException {
        PagoMaterial p = new PagoMaterial();
        p.setId(rs.getInt("id"));
        p.setMaterialId(rs.getInt("material_id"));
        p.setMaterialNombre(rs.getString("mat_nombre"));
        p.setProveedor(rs.getString("proveedor"));
        p.setNumeroFactura(rs.getString("numero_factura"));
        String fc = rs.getString("fecha_compra");
        if (fc != null) p.setFechaCompra(LocalDate.parse(fc));
        p.setCantidadComprada(rs.getDouble("cantidad_comprada"));
        p.setUnidad(rs.getString("mat_unidad"));
        p.setImporteTotal(rs.getDouble("importe_total"));
        p.setFormaPago(rs.getString("forma_pago"));
        String fv = rs.getString("fecha_vencimiento");
        if (fv != null) p.setFechaVencimiento(LocalDate.parse(fv));
        p.setEstado(rs.getString("estado"));
        String fp = rs.getString("fecha_pago");
        if (fp != null) p.setFechaPago(LocalDate.parse(fp));
        p.setNotas(rs.getString("notas"));
        return p;
    }
}
