package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.LineaFactura;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAO {

    public List<Factura> findAll() throws SQLException {
        List<Factura> list = new ArrayList<>();
        String sql = """
            SELECT f.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellidos, ''), '')) as cliente_nombre
            FROM facturas f
            LEFT JOIN clientes c ON f.cliente_id = c.id
            ORDER BY f.created_at DESC
            """;
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Factura findById(int id) throws SQLException {
        String sql = """
            SELECT f.*, (c.nombre || COALESCE(' ' || NULLIF(c.apellidos, ''), '')) as cliente_nombre
            FROM facturas f
            LEFT JOIN clientes c ON f.cliente_id = c.id
            WHERE f.id = ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Factura f = map(rs);
            f.setLineas(findLineas(id));
            return f;
        }
    }

    public List<LineaFactura> findLineas(int facturaId) throws SQLException {
        List<LineaFactura> lineas = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM lineas_factura WHERE factura_id=? ORDER BY orden")) {
            ps.setInt(1, facturaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lineas.add(mapLinea(rs));
        }
        return lineas;
    }

    public Factura crearDesdePresupuesto(int presupuestoId) throws SQLException {
        PresupuestoDAO pDao = new PresupuestoDAO();
        var presupuesto = pDao.findById(presupuestoId);
        if (presupuesto == null) throw new SQLException("Presupuesto no encontrado");

        Factura f = new Factura();
        f.setPresupuestoId(presupuestoId);
        f.setClienteId(presupuesto.getClienteId());
        f.setFecha(java.time.LocalDate.now().toString());
        f.setFechaVencimiento(java.time.LocalDate.now().plusDays(30).toString());
        f.setEstado("pendiente");
        f.setFormaPago("Transferencia bancaria");
        f.setBaseImponible(presupuesto.getBaseImponible());
        f.setIvaPorcentaje(presupuesto.getIvaPorcentaje());
        f.setIvaImporte(presupuesto.getIvaImporte());
        f.setTotal(presupuesto.getTotal());

        for (var lp : presupuesto.getLineas()) {
            LineaFactura lf = new LineaFactura();
            lf.setDescripcion(lp.getDescripcion());
            lf.setTecnica(lp.getTecnica());
            lf.setCantidad(lp.getCantidad());
            lf.setPrecioUnit(lp.getPrecioUnit());
            lf.setDescuento(lp.getDescuento());
            lf.setTotal(lp.getTotal());
            f.getLineas().add(lf);
        }

        Connection conn = DatabaseManager.getConnection();
        boolean externalTx = !conn.getAutoCommit();
        if (!externalTx) conn.setAutoCommit(false);
        try {
            f.setNumero(org.gipsybuho.db.DatabaseManager.generarNumeroFactura());
            save(f);
            descontarMateriales(f);
            pDao.updateEstado(presupuestoId, "facturado");
            if (!externalTx) conn.commit();
        } catch (SQLException e) {
            if (!externalTx) conn.rollback();
            throw e;
        } finally {
            if (!externalTx) conn.setAutoCommit(true);
        }
        return f;
    }

    private void descontarMateriales(Factura f) throws SQLException {
        ConsumoMaterialDAO consumoDao = new ConsumoMaterialDAO();
        MaterialDAO matDao = new MaterialDAO();
        for (LineaFactura linea : f.getLineas()) {
            if (linea.getTecnica() == null || linea.getTecnica().isBlank()) continue;
            List<org.gipsybuho.model.ConsumoMaterial> reglas = consumoDao.findByTecnica(linea.getTecnica());
            for (var regla : reglas) {
                double cantidad = regla.getCantidadPorUnidad() * linea.getCantidad();
                if (cantidad > 0)
                    matDao.ajustarStock(regla.getMaterialId(), cantidad, "salida",
                        "Factura " + f.getNumero() + " - " + linea.getDescripcion());
            }
        }
    }

    public void save(Factura f) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        boolean externalTx = !conn.getAutoCommit();
        if (!externalTx) conn.setAutoCommit(false);
        try {
            if (f.getId() == 0) insert(f); else update(f);
            saveLineas(f);
            if (!externalTx) conn.commit();
        } catch (SQLException e) {
            if (!externalTx) conn.rollback();
            throw e;
        } finally {
            if (!externalTx) conn.setAutoCommit(true);
        }
    }

    private void insert(Factura f) throws SQLException {
        String sql = "INSERT INTO facturas (numero,presupuesto_id,cliente_id,fecha,fecha_vencimiento,estado,forma_pago,base_imponible,iva_porcentaje,iva_importe,total,notas) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            set(ps, f);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) f.setId(keys.getInt(1));
        }
    }

    private void update(Factura f) throws SQLException {
        String sql = "UPDATE facturas SET numero=?,presupuesto_id=?,cliente_id=?,fecha=?,fecha_vencimiento=?,estado=?,forma_pago=?,base_imponible=?,iva_porcentaje=?,iva_importe=?,total=?,notas=? WHERE id=?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            set(ps, f);
            ps.setInt(13, f.getId());
            ps.executeUpdate();
        }
    }

    private void saveLineas(Factura f) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM lineas_factura WHERE factura_id=?")) {
            ps.setInt(1, f.getId());
            ps.executeUpdate();
        }
        String sql = "INSERT INTO lineas_factura (factura_id,descripcion,tecnica,cantidad,precio_unit,descuento,total,orden) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            int orden = 0;
            for (LineaFactura l : f.getLineas()) {
                ps.setInt(1, f.getId());
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
                "UPDATE facturas SET estado=? WHERE id=?")) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM facturas WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countByEstado(String estado) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM facturas WHERE estado=?")) {
            ps.setString(1, estado);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public double totalFacturadoAnio(int anio) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COALESCE(SUM(total),0) FROM facturas WHERE strftime('%Y',fecha)=? AND estado!='anulada'")) {
            ps.setString(1, String.valueOf(anio));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public double totalFacturadoMes(int anio, int mes) throws SQLException {
        String mesStr = String.format("%04d-%02d", anio, mes);
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COALESCE(SUM(total),0) FROM facturas WHERE strftime('%Y-%m',fecha)=? AND estado!='anulada'")) {
            ps.setString(1, mesStr);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public List<String[]> topClientesMes(int anio, int mes, int limit) throws SQLException {
        List<String[]> list = new ArrayList<>();
        String mesStr = String.format("%04d-%02d", anio, mes);
        String sql = """
            SELECT (c.nombre || COALESCE(' ' || NULLIF(c.apellidos, ''), '')) AS cliente_nombre,
                   COALESCE(SUM(f.total), 0) AS total_mes
            FROM facturas f
            LEFT JOIN clientes c ON f.cliente_id = c.id
            WHERE strftime('%Y-%m', f.fecha) = ? AND f.estado != 'anulada'
            GROUP BY f.cliente_id, c.nombre, c.apellidos
            ORDER BY total_mes DESC
            LIMIT ?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, mesStr);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("cliente_nombre"),
                    String.format("%.2f €", rs.getDouble("total_mes"))
                });
            }
        }
        return list;
    }

    private void set(PreparedStatement ps, Factura f) throws SQLException {
        ps.setString(1, f.getNumero());
        if (f.getPresupuestoId() > 0) {
            ps.setInt(2, f.getPresupuestoId());
        } else {
            ps.setNull(2, Types.INTEGER);
        }
        ps.setInt(3, f.getClienteId());
        ps.setString(4, f.getFecha());
        ps.setString(5, f.getFechaVencimiento());
        // Coincide con DEFAULT DDL: 'pendiente'. Ver DatabaseManager.createTables() tabla 'facturas'.
        // SQLite no aplica DEFAULT cuando se inserta NULL explícito, solo si la columna se omite del INSERT.
        ps.setString(6, f.getEstado() != null ? f.getEstado() : "pendiente");
        // Coincide con DEFAULT DDL: 'Transferencia bancaria'. Ver DatabaseManager.createTables() tabla 'facturas'.
        ps.setString(7, f.getFormaPago() != null ? f.getFormaPago() : "Transferencia bancaria");
        ps.setDouble(8, f.getBaseImponible());
        ps.setDouble(9, f.getIvaPorcentaje());
        ps.setDouble(10, f.getIvaImporte());
        ps.setDouble(11, f.getTotal());
        ps.setString(12, f.getNotas());
    }

    private Factura map(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.setId(rs.getInt("id"));
        f.setNumero(rs.getString("numero"));
        f.setPresupuestoId(rs.getInt("presupuesto_id"));
        f.setClienteId(rs.getInt("cliente_id"));
        f.setClienteNombre(rs.getString("cliente_nombre"));
        f.setFecha(rs.getString("fecha"));
        f.setFechaVencimiento(rs.getString("fecha_vencimiento"));
        f.setEstado(rs.getString("estado"));
        f.setFormaPago(rs.getString("forma_pago"));
        f.setBaseImponible(rs.getDouble("base_imponible"));
        f.setIvaPorcentaje(rs.getDouble("iva_porcentaje"));
        f.setIvaImporte(rs.getDouble("iva_importe"));
        f.setTotal(rs.getDouble("total"));
        f.setNotas(rs.getString("notas"));
        f.setCreatedAt(rs.getString("created_at"));
        return f;
    }

    private LineaFactura mapLinea(ResultSet rs) throws SQLException {
        LineaFactura l = new LineaFactura();
        l.setId(rs.getInt("id"));
        l.setFacturaId(rs.getInt("factura_id"));
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
