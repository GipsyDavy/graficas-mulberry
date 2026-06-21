package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.LineaFactura;
import org.gipsybuho.model.LineaPresupuesto;
import org.gipsybuho.model.Presupuesto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests cross-DAO que verifican el comportamiento del patron "tx solo si no
 * hay externa" cuando el caller (simulando EntityImportService) ya tiene una
 * tx abierta. Los DAOs deben detectar la tx externa, NO commitear, NO
 * rollbackear, NO tocar autoCommit. La tx externa controla el ciclo.
 */
class TxAnidadaTest {

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
    void txExternaCommiteaPersistePresupuestoYFactura() throws Exception {
        Cliente c = crearCliente();
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(lineaPresupuesto("Camiseta", 10, 5.0)));
        Factura f = nuevaFactura(c.getId(), "F-1");
        f.setLineas(List.of(lineaFactura("Bolsa", 5, 2.0)));

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            new PresupuestoDAO().save(p);
            new FacturaDAO().save(f);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        assertEquals(1, new PresupuestoDAO().findAll().size(),
            "Tras commit del caller, el presupuesto debe persistir");
        assertEquals(1, new FacturaDAO().findAll().size(),
            "Tras commit del caller, la factura debe persistir");
    }

    @Test
    void txExternaRollbackDeshaceAmbosSaves() throws Exception {
        Cliente c = crearCliente();
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(lineaPresupuesto("Camiseta", 10, 5.0)));
        Factura f = nuevaFactura(c.getId(), "F-1");
        f.setLineas(List.of(lineaFactura("Bolsa", 5, 2.0)));

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            new PresupuestoDAO().save(p);
            new FacturaDAO().save(f);
            // Simulamos un error de negocio detectado DESPUES de los saves:
            // el caller decide rollbackear todo el grupo.
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }

        assertEquals(0, new PresupuestoDAO().findAll().size(),
            "Tras rollback del caller, el presupuesto NO debe persistir");
        assertEquals(0, new FacturaDAO().findAll().size(),
            "Tras rollback del caller, la factura NO debe persistir");
    }

    @Test
    void txExternaRollbackPorSqlExceptionEnSegundoSave() throws Exception {
        Cliente c = crearCliente();
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(lineaPresupuesto("Camiseta", 10, 5.0)));
        // Segunda entidad con linea invalida: el save fallara con SQLException.
        Factura fMala = nuevaFactura(c.getId(), "F-1");
        fMala.setLineas(List.of(lineaFactura("Bolsa", 5, 2.0), lineaFacturaInvalida()));

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            assertThrows(SQLException.class, () -> {
                new PresupuestoDAO().save(p);
                new FacturaDAO().save(fMala);
            });
            // El caller, como hace EntityImportService, rollbackea ante la excepcion.
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }

        assertEquals(0, new PresupuestoDAO().findAll().size(),
            "Tras rollback del caller por SQLException, el primer save NO debe persistir");
        assertEquals(0, new FacturaDAO().findAll().size(),
            "Tras rollback del caller por SQLException, el segundo save (fallido) tampoco debe persistir");
    }

    private Cliente crearCliente() throws SQLException {
        Cliente c = new Cliente();
        c.setNombre("Ana");
        c.setApellidos("Garcia");
        c.setNif("111A");
        c.setTipo("empresa");
        new ClienteDAO(DatabaseManager.getConnection()).save(c);
        return c;
    }

    private Presupuesto nuevoPresupuesto(int clienteId, String numero) {
        Presupuesto p = new Presupuesto();
        p.setClienteId(clienteId);
        p.setNumero(numero);
        p.setFecha("2026-05-22");
        p.setEstado("borrador");
        p.setIvaPorcentaje(21.0);
        return p;
    }

    private Factura nuevaFactura(int clienteId, String numero) {
        Factura f = new Factura();
        f.setClienteId(clienteId);
        f.setNumero(numero);
        f.setFecha("2026-05-22");
        f.setFechaVencimiento("2026-06-22");
        f.setEstado("pendiente");
        f.setFormaPago("Transferencia");
        f.setIvaPorcentaje(21.0);
        return f;
    }

    private LineaPresupuesto lineaPresupuesto(String desc, int cant, double precio) {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setDescripcion(desc);
        l.setCantidad(cant);
        l.setPrecioUnit(precio);
        l.setDescuento(0);
        l.calcularTotal();
        return l;
    }

    private LineaFactura lineaFactura(String desc, int cant, double precio) {
        LineaFactura l = new LineaFactura();
        l.setDescripcion(desc);
        l.setCantidad(cant);
        l.setPrecioUnit(precio);
        l.setDescuento(0);
        l.setTotal(cant * precio);
        return l;
    }

    private LineaFactura lineaFacturaInvalida() {
        LineaFactura l = new LineaFactura();
        l.setDescripcion(null);
        l.setCantidad(1);
        l.setPrecioUnit(0);
        l.setDescuento(0);
        l.setTotal(0);
        return l;
    }
}
