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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacturaDAOTest {

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
        Factura f = nuevaFactura(c.getId(), "F-1");
        f.setLineas(List.of(lineaFactura("Camiseta", 10, 5.0), lineaFacturaInvalida()));

        assertThrows(SQLException.class, () -> new FacturaDAO().save(f));

        assertEquals(0, new FacturaDAO().findAll().size(),
            "Si una linea falla, la cabecera no debe quedar persistida");
    }

    @Test
    void crearDesdePresupuestoEsAtomicoEnFalloDeLineas() throws Exception {
        Cliente c = crearCliente();
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(lineaPresupuesto("Camiseta", 10, 5.0)));
        new PresupuestoDAO().save(p);

        // Estrategia para forzar fallo a mitad de crearDesdePresupuesto:
        // 1. Reservamos un numero de factura (incrementa siguiente_factura).
        // 2. Pre-creamos una factura con ese numero exacto.
        // 3. Reseteamos siguiente_factura al valor previo.
        // 4. crearDesdePresupuesto volvera a generar el mismo numero y el INSERT
        //    en facturas fallara por UNIQUE constraint en facturas.numero.
        String siguienteFacturaPrevio = DatabaseManager.getConfig("siguiente_factura");
        String numeroColision = DatabaseManager.generarNumeroFactura();
        Factura preExistente = nuevaFactura(c.getId(), numeroColision);
        preExistente.setLineas(List.of(lineaFactura("Bolsa", 1, 10.0)));
        new FacturaDAO().save(preExistente);
        DatabaseManager.setConfig("siguiente_factura", siguienteFacturaPrevio);

        assertThrows(SQLException.class, () -> new FacturaDAO().crearDesdePresupuesto(p.getId()));

        // Verificacion de atomicidad:
        // Tras el fallo, sigue habiendo solo la factura pre-existente (no la nueva).
        assertEquals(1, new FacturaDAO().findAll().size(),
            "Tras el fallo, solo debe existir la factura pre-creada");
        Presupuesto recargado = new PresupuestoDAO().findById(p.getId());
        assertNotNull(recargado);
        assertNotEquals("facturado", recargado.getEstado(),
            "El estado del presupuesto NO debe haber cambiado a facturado");
    }

    private Cliente crearCliente() throws SQLException {
        Cliente c = new Cliente();
        c.setNombre("Ana");
        c.setApellidos("Garcia");
        c.setNif("111A");
        c.setTipo("empresa");
        new ClienteDAO().save(c);
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

    @Test
    void saveAplicaDefaultsDDLCuandoCamposNulos() throws Exception {
        Cliente c = crearCliente();
        Factura f = new Factura();
        f.setClienteId(c.getId());
        f.setNumero("F-DEF-1");
        f.setFecha("2026-05-23");
        // estado y formaPago NO se setean: deben quedar como DEFAULT DDL
        f.setIvaPorcentaje(21.0);
        f.setLineas(List.of(lineaFactura("Item", 1, 1.0)));

        new FacturaDAO().save(f);

        Factura recargado = new FacturaDAO().findById(f.getId());
        assertNotNull(recargado, "La factura debe persistirse");
        assertEquals("pendiente", recargado.getEstado(),
            "estado=null en el modelo debe quedar como DEFAULT DDL 'pendiente'");
        assertEquals("Transferencia bancaria", recargado.getFormaPago(),
            "formaPago=null en el modelo debe quedar como DEFAULT DDL 'Transferencia bancaria'");
    }
}
