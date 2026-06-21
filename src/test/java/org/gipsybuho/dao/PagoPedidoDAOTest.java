package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.PagoPedido;
import org.gipsybuho.model.Pedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagoPedidoDAOTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.closeConnection();
        System.setProperty("graficas.mulberry.db.url", "jdbc:sqlite:" + tempDir.resolve("test.db"));
        DatabaseManager.initialize();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.closeConnection();
        System.clearProperty("graficas.mulberry.db.url");
    }

    @Test
    void saveInsertaYMarcarPagadoCambiaEstado() throws Exception {
        int pedidoId = crearPedido("PED-PP-001");
        PagoPedido pp = pagoPendiente(pedidoId, 500.0);
        PagoPedidoDAO dao = new PagoPedidoDAO();
        dao.save(pp);
        assertEquals("pendiente", dao.findByPedido(pedidoId).get(0).getEstado());

        dao.marcarPagado(pp.getId(), LocalDate.now());
        assertEquals("pagado", dao.findByPedido(pedidoId).get(0).getEstado(), "marcarPagado debe cambiar estado a 'pagado'");
    }

    @Test
    void findByPedidoFiltraPorPedidoId() throws Exception {
        int pedido1 = crearPedido("PED-PP-002");
        int pedido2 = crearPedido("PED-PP-003");
        PagoPedidoDAO dao = new PagoPedidoDAO();
        dao.save(pagoPendiente(pedido1, 100.0));
        dao.save(pagoPendiente(pedido1, 200.0));
        dao.save(pagoPendiente(pedido2, 300.0));

        List<PagoPedido> pagos1 = dao.findByPedido(pedido1);
        assertEquals(2, pagos1.size(), "findByPedido debe devolver solo los pagos del pedido indicado");
        assertEquals(1, dao.findByPedido(pedido2).size());
    }

    @Test
    void totalPendienteAgregaCorrectamente() throws Exception {
        int pedidoId = crearPedido("PED-PP-004");
        PagoPedidoDAO dao = new PagoPedidoDAO();
        PagoPedido p1 = pagoPendiente(pedidoId, 400.0);
        PagoPedido p2 = pagoPendiente(pedidoId, 600.0);
        dao.save(p1);
        dao.save(p2);
        dao.marcarPagado(p2.getId(), LocalDate.now());

        assertEquals(400.0, dao.totalPendiente(), 0.001, "totalPendiente no debe incluir pagos ya pagados");
    }

    private int crearPedido(String numero) throws Exception {
        Cliente c = new Cliente();
        c.setNombre("Cliente Test");
        c.setTipo("empresa");
        new ClienteDAO().save(c);

        Pedido p = new Pedido();
        p.setClienteId(c.getId());
        p.setNumero(numero);
        p.setFecha(LocalDate.now());
        p.setEstado("pendiente");
        p.setIvaPorcentaje(21.0);
        new PedidoDAO(DatabaseManager.getConnection()).save(p);
        return p.getId();
    }

    private PagoPedido pagoPendiente(int pedidoId, double importe) {
        PagoPedido pp = new PagoPedido();
        pp.setPedidoId(pedidoId);
        pp.setImporte(importe);
        pp.setEstado("pendiente");
        pp.setFechaVencimiento(LocalDate.now().plusDays(30));
        return pp;
    }
}
