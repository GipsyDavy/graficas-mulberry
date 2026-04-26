package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.LineaAlbaran;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlbaranDAO {

    public List<Albaran> findAll() throws SQLException {
        List<Albaran> list = new ArrayList<>();
        String sql = """
            SELECT a.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellido, ''), '')) as cliente_nombre,
                   f.numero as factura_numero, p.numero as pedido_numero
            FROM albaranes a
            LEFT JOIN clientes c ON a.cliente_id = c.id
            LEFT JOIN facturas f ON a.factura_id = f.id
            LEFT JOIN pedidos p ON a.pedido_id = p.id
            ORDER BY a.created_at DESC
            """;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Albaran findById(int id) throws SQLException {
        String sql = """
            SELECT a.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellido, ''), '')) as cliente_nombre,
                   f.numero as factura_numero, p.numero as pedido_numero
            FROM albaranes a
            LEFT JOIN clientes c ON a.cliente_id = c.id
            LEFT JOIN facturas f ON a.factura_id = f.id
            LEFT JOIN pedidos p ON a.pedido_id = p.id
            WHERE a.id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Albaran a = map(rs);
            a.setLineas(findLineas(id));
            return a;
        }
    }

    public List<LineaAlbaran> findLineas(int albaranId) throws SQLException {
        List<LineaAlbaran> lineas = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM lineas_albaran WHERE albaran_id=? ORDER BY orden")) {
            ps.setInt(1, albaranId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lineas.add(mapLinea(rs));
        }
        return lineas;
    }

    public Albaran crearDesdeFactura(int facturaId) throws SQLException {
        FacturaDAO fDao = new FacturaDAO();
        var factura = fDao.findById(facturaId);
        if (factura == null) throw new SQLException("Factura no encontrada");

        Albaran a = new Albaran();
        a.setNumero(DatabaseManager.generarNumeroAlbaran());
        a.setClienteId(factura.getClienteId());
        a.setFecha(java.time.LocalDate.now().toString());
        a.setFacturaId(facturaId);
        a.setEstado("pendiente");

        for (var lf : factura.getLineas()) {
            LineaAlbaran la = new LineaAlbaran();
            la.setDescripcion(lf.getDescripcion());
            la.setCantidad(lf.getCantidad());
            la.setUnidad("ud");
            a.getLineas().add(la);
        }
        save(a);
        return a;
    }

    public void save(Albaran a) throws SQLException {
        if (a.getId() == 0) insert(a); else update(a);
        saveLineas(a);
    }

    private void insert(Albaran a) throws SQLException {
        String sql = "INSERT INTO albaranes (numero,cliente_id,fecha,factura_id,pedido_id,estado,observaciones) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, a);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) a.setId(keys.getInt(1));
        }
    }

    private void update(Albaran a) throws SQLException {
        String sql = "UPDATE albaranes SET numero=?,cliente_id=?,fecha=?,factura_id=?,pedido_id=?,estado=?,observaciones=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, a);
            ps.setInt(8, a.getId());
            ps.executeUpdate();
        }
    }

    private void saveLineas(Albaran a) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM lineas_albaran WHERE albaran_id=?")) {
            ps.setInt(1, a.getId());
            ps.executeUpdate();
        }
        if (a.getLineas().isEmpty()) return;
        String sql = "INSERT INTO lineas_albaran (albaran_id,descripcion,cantidad,unidad,orden) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            int orden = 0;
            for (LineaAlbaran l : a.getLineas()) {
                ps.setInt(1, a.getId());
                ps.setString(2, l.getDescripcion());
                ps.setInt(3, l.getCantidad());
                ps.setString(4, l.getUnidad() != null ? l.getUnidad() : "ud");
                ps.setInt(5, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void updateEstado(int id, String estado) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE albaranes SET estado=? WHERE id=?")) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM albaranes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void set(PreparedStatement ps, Albaran a) throws SQLException {
        ps.setString(1, a.getNumero());
        ps.setInt(2, a.getClienteId());
        ps.setString(3, a.getFecha());
        if (a.getFacturaId() > 0) ps.setInt(4, a.getFacturaId()); else ps.setNull(4, Types.INTEGER);
        if (a.getPedidoId() > 0) ps.setInt(5, a.getPedidoId()); else ps.setNull(5, Types.INTEGER);
        ps.setString(6, a.getEstado());
        ps.setString(7, a.getObservaciones());
    }

    private Albaran map(ResultSet rs) throws SQLException {
        Albaran a = new Albaran();
        a.setId(rs.getInt("id"));
        a.setNumero(rs.getString("numero"));
        a.setClienteId(rs.getInt("cliente_id"));
        a.setClienteNombre(rs.getString("cliente_nombre"));
        a.setFecha(rs.getString("fecha"));
        a.setFacturaId(rs.getInt("factura_id"));
        a.setFacturaNumero(rs.getString("factura_numero"));
        a.setPedidoId(rs.getInt("pedido_id"));
        a.setPedidoNumero(rs.getString("pedido_numero"));
        a.setEstado(rs.getString("estado"));
        a.setObservaciones(rs.getString("observaciones"));
        a.setCreatedAt(rs.getString("created_at"));
        return a;
    }

    private LineaAlbaran mapLinea(ResultSet rs) throws SQLException {
        LineaAlbaran l = new LineaAlbaran();
        l.setId(rs.getInt("id"));
        l.setAlbaranId(rs.getInt("albaran_id"));
        l.setDescripcion(rs.getString("descripcion"));
        l.setCantidad(rs.getInt("cantidad"));
        l.setUnidad(rs.getString("unidad"));
        l.setOrden(rs.getInt("orden"));
        return l;
    }
}
