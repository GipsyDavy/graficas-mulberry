package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.PagoPedido;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PagoPedidoDAO {

    private static final String SELECT_BASE = """
        SELECT pp.*,
            p.numero AS pedido_numero,
            c.nombre || COALESCE(' ' || NULLIF(c.apellidos,''), '') AS cliente_nombre
        FROM pagos_pedido pp
        JOIN pedidos p ON pp.pedido_id = p.id
        LEFT JOIN clientes c ON p.cliente_id = c.id
        """;

    public List<PagoPedido> findByPedido(int pedidoId) throws SQLException {
        List<PagoPedido> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                SELECT_BASE + "WHERE pp.pedido_id = ? ORDER BY pp.fecha_vencimiento ASC, pp.id ASC")) {
            ps.setInt(1, pedidoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<PagoPedido> findAll() throws SQLException {
        List<PagoPedido> list = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE +
                 "ORDER BY CASE pp.estado WHEN 'pendiente' THEN 0 ELSE 1 END, pp.fecha_vencimiento ASC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void save(PagoPedido p) throws SQLException {
        if (p.getId() == 0) insert(p); else update(p);
    }

    private void insert(PagoPedido p) throws SQLException {
        String sql = """
            INSERT INTO pagos_pedido
              (pedido_id, concepto, importe, fecha_emision, fecha_vencimiento,
               forma_pago, estado, fecha_pago, notas)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        }
    }

    private void update(PagoPedido p) throws SQLException {
        String sql = """
            UPDATE pagos_pedido SET
              pedido_id=?, concepto=?, importe=?, fecha_emision=?, fecha_vencimiento=?,
              forma_pago=?, estado=?, fecha_pago=?, notas=?
            WHERE id=?
            """;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            bind(ps, p);
            ps.setInt(10, p.getId());
            ps.executeUpdate();
        }
    }

    public void marcarPagado(int id, LocalDate fecha) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE pagos_pedido SET estado='pagado', fecha_pago=? WHERE id=?")) {
            ps.setString(1, fecha != null ? fecha.toString() : LocalDate.now().toString());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM pagos_pedido WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countVencidos() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM pagos_pedido WHERE estado='pendiente' AND fecha_vencimiento < date('now')")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public double totalPendiente() throws SQLException {
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COALESCE(SUM(importe),0) FROM pagos_pedido WHERE estado='pendiente'")) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    private void bind(PreparedStatement ps, PagoPedido p) throws SQLException {
        ps.setInt(1, p.getPedidoId());
        ps.setString(2, p.getConcepto() != null ? p.getConcepto() : "Pago");
        ps.setDouble(3, p.getImporte());
        ps.setString(4, p.getFechaEmision() != null ? p.getFechaEmision().toString() : null);
        ps.setString(5, p.getFechaVencimiento() != null ? p.getFechaVencimiento().toString() : null);
        ps.setString(6, p.getFormaPago() != null ? p.getFormaPago() : "Transferencia bancaria");
        ps.setString(7, p.getEstado() != null ? p.getEstado() : "pendiente");
        ps.setString(8, p.getFechaPago() != null ? p.getFechaPago().toString() : null);
        ps.setString(9, p.getNotas());
    }

    private PagoPedido map(ResultSet rs) throws SQLException {
        PagoPedido p = new PagoPedido();
        p.setId(rs.getInt("id"));
        p.setPedidoId(rs.getInt("pedido_id"));
        p.setPedidoNumero(rs.getString("pedido_numero"));
        p.setClienteNombre(rs.getString("cliente_nombre"));
        p.setConcepto(rs.getString("concepto"));
        p.setImporte(rs.getDouble("importe"));
        String fe = rs.getString("fecha_emision");
        if (fe != null) p.setFechaEmision(LocalDate.parse(fe));
        String fv = rs.getString("fecha_vencimiento");
        if (fv != null) p.setFechaVencimiento(LocalDate.parse(fv));
        p.setFormaPago(rs.getString("forma_pago"));
        p.setEstado(rs.getString("estado"));
        String fp = rs.getString("fecha_pago");
        if (fp != null) p.setFechaPago(LocalDate.parse(fp));
        p.setNotas(rs.getString("notas"));
        return p;
    }
}
