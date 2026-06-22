package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.LineaAlbaran;
import org.gipsybuho.model.LineaFactura;
import org.gipsybuho.model.LineaPresupuesto;
import org.gipsybuho.model.Presupuesto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlbaranDAOTest {

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
    void saveHaceRollbackCuandoFallaLineaInvalida() throws Exception {
        Cliente c = crearCliente();
        Albaran a = nuevoAlbaran(c.getId(), "A-1");
        a.setLineas(List.of(lineaAlbaran("Producto", 5), lineaAlbaranInvalida()));

        assertThrows(SQLException.class, () -> new AlbaranDAO().save(a));

        assertEquals(0, new AlbaranDAO().findAll().size(),
            "Si una linea falla, la cabecera no debe quedar persistida");
    }

    @Test
    void crearDesdeFacturaEsAtomicoEnColisionUnique() throws Exception {
        Cliente c = crearCliente();
        Factura f = nuevaFactura(c.getId(), "F-1");
        f.setLineas(List.of(lineaFactura("Camiseta", 10, 5.0)));
        new FacturaDAO(DatabaseManager.getConnection()).save(f);

        String siguienteAlbaranPrevio = DatabaseManager.getConfig("siguiente_albaran");
        String numeroColision = DatabaseManager.generarNumeroAlbaran();
        Albaran preExistente = nuevoAlbaran(c.getId(), numeroColision);
        preExistente.setLineas(List.of(lineaAlbaran("Otro", 1)));
        new AlbaranDAO().save(preExistente);
        DatabaseManager.setConfig("siguiente_albaran", siguienteAlbaranPrevio);

        assertThrows(SQLException.class, () -> new AlbaranDAO().crearDesdeFactura(f.getId()));

        assertEquals(1, new AlbaranDAO().findAll().size(),
            "Tras el fallo, solo debe existir el albaran pre-creado");
    }

    @Test
    void crearDesdePresupuestoEsAtomicoEnColisionUnique() throws Exception {
        Cliente c = crearCliente();
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(lineaPresupuesto("Camiseta", 10, 5.0)));
        new PresupuestoDAO(DatabaseManager.getConnection()).save(p);

        String siguienteAlbaranPrevio = DatabaseManager.getConfig("siguiente_albaran");
        String numeroColision = DatabaseManager.generarNumeroAlbaran();
        Albaran preExistente = nuevoAlbaran(c.getId(), numeroColision);
        preExistente.setLineas(List.of(lineaAlbaran("Otro", 1)));
        new AlbaranDAO().save(preExistente);
        DatabaseManager.setConfig("siguiente_albaran", siguienteAlbaranPrevio);

        assertThrows(SQLException.class, () -> new AlbaranDAO().crearDesdePresupuesto(p.getId()));

        assertEquals(1, new AlbaranDAO().findAll().size(),
            "Tras el fallo, solo debe existir el albaran pre-creado");
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

    private Albaran nuevoAlbaran(int clienteId, String numero) {
        Albaran a = new Albaran();
        a.setClienteId(clienteId);
        a.setNumero(numero);
        a.setFecha("2026-05-22");
        a.setEstado("pendiente");
        return a;
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

    private Presupuesto nuevoPresupuesto(int clienteId, String numero) {
        Presupuesto p = new Presupuesto();
        p.setClienteId(clienteId);
        p.setNumero(numero);
        p.setFecha("2026-05-22");
        p.setEstado("borrador");
        p.setIvaPorcentaje(21.0);
        return p;
    }

    private LineaAlbaran lineaAlbaran(String desc, int cant) {
        LineaAlbaran l = new LineaAlbaran();
        l.setDescripcion(desc);
        l.setCantidad(cant);
        l.setUnidad("ud");
        return l;
    }

    private LineaAlbaran lineaAlbaranInvalida() {
        LineaAlbaran l = new LineaAlbaran();
        l.setDescripcion(null);
        l.setCantidad(1);
        l.setUnidad("ud");
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

    private LineaPresupuesto lineaPresupuesto(String desc, int cant, double precio) {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setDescripcion(desc);
        l.setCantidad(cant);
        l.setPrecioUnit(precio);
        l.setDescuento(0);
        l.calcularTotal();
        return l;
    }

    @Test
    void saveAplicaDefaultsDDLCuandoCamposNulos() throws Exception {
        Cliente c = crearCliente();
        Albaran a = new Albaran();
        a.setClienteId(c.getId());
        a.setNumero("A-DEF-1");
        a.setFecha("2026-05-23");
        // estado NO se setea: debe quedar como DEFAULT DDL 'pendiente'
        a.setLineas(List.of(lineaAlbaran("Item", 1)));

        new AlbaranDAO().save(a);

        Albaran recargado = new AlbaranDAO().findById(a.getId());
        assertNotNull(recargado, "El albaran debe persistirse");
        assertEquals("pendiente", recargado.getEstado(),
            "estado=null en el modelo debe quedar como DEFAULT DDL 'pendiente'");
    }
}
