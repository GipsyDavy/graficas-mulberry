package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
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

class PresupuestoDAOTest {

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
        Presupuesto p = nuevoPresupuesto(c.getId(), "P-1");
        p.setLineas(List.of(linea("Camiseta", 10, 5.0), lineaInvalida()));

        assertThrows(SQLException.class, () -> new PresupuestoDAO().save(p));

        assertEquals(0, new PresupuestoDAO().findAll().size(),
            "Si una linea falla, la cabecera no debe quedar persistida");
    }

    @Test
    void saveRollbackDejaPresupuestoPrevioIntacto() throws Exception {
        Cliente c = crearCliente();
        Presupuesto previo = nuevoPresupuesto(c.getId(), "P-1");
        previo.setLineas(List.of(linea("Camiseta", 10, 5.0)));
        new PresupuestoDAO().save(previo);
        int idPrevio = previo.getId();

        Presupuesto nuevo = nuevoPresupuesto(c.getId(), "P-2");
        nuevo.setLineas(List.of(linea("Bolsa", 5, 2.0), lineaInvalida()));
        assertThrows(SQLException.class, () -> new PresupuestoDAO().save(nuevo));

        Presupuesto recargado = new PresupuestoDAO().findById(idPrevio);
        assertNotNull(recargado, "El presupuesto previo no debe haberse perdido");
        assertEquals(1, recargado.getLineas().size(), "Las lineas previas siguen intactas");
        assertEquals(1, new PresupuestoDAO().findAll().size(), "Solo el previo persiste");
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

    private LineaPresupuesto linea(String descripcion, int cantidad, double precio) {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setDescripcion(descripcion);
        l.setCantidad(cantidad);
        l.setPrecioUnit(precio);
        l.setDescuento(0);
        l.calcularTotal();
        return l;
    }

    private LineaPresupuesto lineaInvalida() {
        LineaPresupuesto l = new LineaPresupuesto();
        l.setDescripcion(null);
        l.setCantidad(1);
        l.setPrecioUnit(0);
        l.setDescuento(0);
        l.calcularTotal();
        return l;
    }
}
