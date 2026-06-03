package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Pedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PedidoDAOTest {

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
    void saveInsertaYRecuperaConClienteNombreJoin() throws Exception {
        Cliente c = crearCliente("Imprenta Sol", "Garcia");
        Pedido p = nuevoPedido(c.getId(), "PED-001");
        new PedidoDAO().save(p);

        Pedido recargado = new PedidoDAO().findById(p.getId());
        assertNotNull(recargado);
        assertEquals("PED-001", recargado.getNumero());
        assertTrue(recargado.getClienteNombre().contains("Imprenta Sol"), "findById debe resolver clienteNombre via JOIN");
    }

    @Test
    void saveAplicaDefaultsDDLEstadoEIva() throws Exception {
        Cliente c = crearCliente("Graficas Sur", "");
        Pedido p = new Pedido();
        p.setClienteId(c.getId());
        p.setNumero("PED-DEF-1");
        p.setFecha(LocalDate.now());
        // estado e iva_porcentaje NO se setean: bind aplica defaults Java-side

        new PedidoDAO().save(p);

        Pedido recargado = new PedidoDAO().findById(p.getId());
        assertNotNull(recargado);
        assertEquals("pendiente", recargado.getEstado(), "estado null debe quedar como 'pendiente'");
        assertEquals(21.0, recargado.getIvaPorcentaje(), 0.001, "iva_porcentaje<=0 debe quedar como 21.0");
    }

    @Test
    void deleteEliminaRegistro() throws Exception {
        Cliente c = crearCliente("Litografia Norte", "");
        PedidoDAO dao = new PedidoDAO();
        Pedido p = nuevoPedido(c.getId(), "PED-DEL-1");
        dao.save(p);
        assertEquals(1, dao.findAll().size());

        dao.delete(p.getId());
        assertNull(dao.findById(p.getId()), "tras delete, findById debe devolver null");
        assertEquals(0, dao.findAll().size());
    }

    private Cliente crearCliente(String nombre, String apellidos) throws Exception {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setApellidos(apellidos);
        c.setTipo("empresa");
        new ClienteDAO().save(c);
        return c;
    }

    private Pedido nuevoPedido(int clienteId, String numero) {
        Pedido p = new Pedido();
        p.setClienteId(clienteId);
        p.setNumero(numero);
        p.setFecha(LocalDate.now());
        p.setEstado("pendiente");
        p.setIvaPorcentaje(21.0);
        return p;
    }
}
