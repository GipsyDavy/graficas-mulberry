package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EmpleadoDAOTest {

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
    void saveInsertaYRecuperaPorId() throws Exception {
        Empleado e = new Empleado();
        e.setNombre("Marta");
        e.setApellidos("Fernandez");
        e.setActivo(true);
        new EmpleadoDAO().save(e);

        Empleado recargado = new EmpleadoDAO().findById(e.getId());
        assertNotNull(recargado);
        assertEquals("Marta", recargado.getNombre());
        assertEquals("Fernandez", recargado.getApellidos());
        assertTrue(recargado.isActivo());
        assertEquals("Operario", recargado.getCategoria(), "categoria null debe aplicar DEFAULT DDL 'Operario'");
    }

    @Test
    void deleteHaceSoftDeleteYReactivarLoRestaurea() throws Exception {
        EmpleadoDAO dao = new EmpleadoDAO();
        Empleado e = new Empleado();
        e.setNombre("Jose");
        e.setApellidos("Ruiz");
        e.setActivo(true);
        dao.save(e);

        dao.delete(e.getId());
        assertFalse(dao.findById(e.getId()).isActivo(), "delete debe marcar activo=0");
        assertEquals(0, dao.findAll().size(), "findAll no debe mostrar empleados inactivos");

        dao.reactivar(e.getId());
        assertTrue(dao.findById(e.getId()).isActivo(), "reactivar debe restaurar activo=1");
        assertEquals(1, dao.findAll().size(), "findAll debe mostrar el empleado reactivado");
    }

    @Test
    void countCuentaSoloActivos() throws Exception {
        EmpleadoDAO dao = new EmpleadoDAO();
        Empleado activo = new Empleado();
        activo.setNombre("Ana");
        activo.setActivo(true);
        dao.save(activo);

        Empleado inactivo = new Empleado();
        inactivo.setNombre("Luis");
        inactivo.setActivo(true);
        dao.save(inactivo);
        dao.delete(inactivo.getId());

        assertEquals(1, dao.count(), "count solo debe contar empleados activos");
        assertEquals(2, dao.findAllIncluirBajas().size(), "findAllIncluirBajas debe devolver todos");
    }

    @Test
    void nifUnicoRechazaDuplicado() throws Exception {
        EmpleadoDAO dao = new EmpleadoDAO();
        Empleado e1 = new Empleado();
        e1.setNombre("Ana");
        e1.setNif("12345678A");
        e1.setActivo(true);
        dao.save(e1);

        Empleado e2 = new Empleado();
        e2.setNombre("Carlos");
        e2.setNif("12345678A");
        e2.setActivo(true);
        assertThrows(Exception.class, () -> dao.save(e2),
            "Insertar dos empleados con el mismo NIF no-nulo debe lanzar excepción");
    }
}
