package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.model.LineaPresupuesto;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PedidoDAO {

    private final Connection conn;

    public PedidoDAO(Connection conn) {
        this.conn = conn;
    }

    private static final String SELECT_BASE = """
        SELECT p.*,
            c.nombre || COALESCE(' ' || NULLIF(c.apellidos,''), '') AS cliente_nombre,
            COALESCE((SELECT SUM(pp.importe) FROM pagos_pedido pp
                      WHERE pp.pedido_id = p.id AND pp.estado = 'pagado'), 0.0) AS importe_pagado,
            COALESCE((SELECT COUNT(*) FROM pagos_pedido pp
                      WHERE pp.pedido_id = p.id AND pp.estado = 'pendiente'
                      AND pp.fecha_vencimiento < date('now')), 0) AS pagos_vencidos
        FROM pedidos p
        LEFT JOIN clientes c ON p.cliente_id = c.id
        """;

    public List<Pedido> findAll() throws SQLException {
        List<Pedido> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY p.fecha DESC, p.id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Pedido findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT_BASE + "WHERE p.id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void save(Pedido p) throws SQLException {
        if (p.getId() == 0) insert(p); else update(p);
    }

    private void insert(Pedido p) throws SQLException {
        String sql = """
            INSERT INTO pedidos
              (numero, cliente_id, fecha, fecha_entrega_prevista, fecha_entrega_real,
               estado, descripcion, importe_total, iva_porcentaje, notas)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        }
    }

    private void update(Pedido p) throws SQLException {
        String sql = """
            UPDATE pedidos SET
              numero=?, cliente_id=?, fecha=?, fecha_entrega_prevista=?, fecha_entrega_real=?,
              estado=?, descripcion=?, importe_total=?, iva_porcentaje=?, notas=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, p);
            ps.setInt(11, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM pedidos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Pedido p) throws SQLException {
        ps.setString(1, p.getNumero());
        ps.setInt(2, p.getClienteId());
        ps.setString(3, p.getFecha() != null ? p.getFecha().toString() : LocalDate.now().toString());
        ps.setString(4, p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().toString() : null);
        ps.setString(5, p.getFechaEntregaReal() != null ? p.getFechaEntregaReal().toString() : null);
        ps.setString(6, p.getEstado() != null ? p.getEstado() : "pendiente");
        ps.setString(7, p.getDescripcion());
        ps.setDouble(8, p.getImporteTotal());
        ps.setDouble(9, p.getIvaPorcentaje() > 0 ? p.getIvaPorcentaje() : 21.0);
        ps.setString(10, p.getNotas());
    }

    public Pedido crearDesdePresupuesto(int presupuestoId) throws SQLException {
        PresupuestoDAO pDao = new PresupuestoDAO(this.conn);
        var presupuesto = pDao.findById(presupuestoId);
        if (presupuesto == null) throw new SQLException("Presupuesto no encontrado");

        Pedido p = new Pedido();
        p.setClienteId(presupuesto.getClienteId());
        p.setFecha(LocalDate.now());
        p.setEstado("pendiente");
        p.setImporteTotal(presupuesto.getTotal());
        p.setIvaPorcentaje(presupuesto.getIvaPorcentaje() > 0 ? presupuesto.getIvaPorcentaje() : 21.0);
        p.setNotas(presupuesto.getNotas());
        String desc = presupuesto.getLineas().stream()
            .map(LineaPresupuesto::getDescripcion)
            .filter(d -> d != null && !d.isBlank())
            .collect(Collectors.joining(", "));
        p.setDescripcion(desc.isBlank() ? null : desc);

        boolean externalTx = !conn.getAutoCommit();
        if (!externalTx) conn.setAutoCommit(false);
        try {
            p.setNumero(DatabaseManager.generarNumeroPedido());
            save(p);
            if (!externalTx) conn.commit();
        } catch (SQLException e) {
            if (!externalTx) conn.rollback();
            throw e;
        } finally {
            if (!externalTx) conn.setAutoCommit(true);
        }
        return p;
    }

    private Pedido map(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id"));
        p.setNumero(rs.getString("numero"));
        p.setClienteId(rs.getInt("cliente_id"));
        p.setClienteNombre(rs.getString("cliente_nombre"));
        String f = rs.getString("fecha");
        if (f != null) p.setFecha(LocalDate.parse(f));
        String fep = rs.getString("fecha_entrega_prevista");
        if (fep != null) p.setFechaEntregaPrevista(LocalDate.parse(fep));
        String fer = rs.getString("fecha_entrega_real");
        if (fer != null) p.setFechaEntregaReal(LocalDate.parse(fer));
        p.setEstado(rs.getString("estado"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setImporteTotal(rs.getDouble("importe_total"));
        p.setIvaPorcentaje(rs.getDouble("iva_porcentaje"));
        p.setNotas(rs.getString("notas"));
        p.setImportePagado(rs.getDouble("importe_pagado"));
        p.setPagosVencidos(rs.getInt("pagos_vencidos"));
        return p;
    }
}
