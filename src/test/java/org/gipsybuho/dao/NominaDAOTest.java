package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NominaDAOTest {

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
    void saveInsertaYRecuperaConNombreEmpleadoJoin() throws Exception {
        Empleado emp = crearEmpleado("Carmen", "Vidal");
        Nomina n = new Nomina();
        n.setEmpleadoId(emp.getId());
        n.setMes(5);
        n.setAnio(2026);
        n.setSalarioBase(1800.0);
        new NominaDAO(DatabaseManager.getConnection()).save(n);

        Nomina recargada = new NominaDAO(DatabaseManager.getConnection()).findById(n.getId());
        assertNotNull(recargada);
        assertEquals(5, recargada.getMes());
        assertEquals(2026, recargada.getAnio());
        assertEquals(1800.0, recargada.getSalarioBase(), 0.001);
        assertTrue(recargada.getEmpleadoNombre().contains("Carmen"), "findById debe resolver nombre del empleado via JOIN");
    }

    @Test
    void findByPeriodoFiltraPorMesYAnio() throws Exception {
        Empleado emp = crearEmpleado("Pedro", "Gomez");
        NominaDAO dao = new NominaDAO(DatabaseManager.getConnection());
        Nomina n1 = nomina(emp.getId(), 1, 2026);
        Nomina n2 = nomina(emp.getId(), 2, 2026);
        dao.save(n1);
        dao.save(n2);

        List<Nomina> enero = dao.findByPeriodo(1, 2026);
        assertEquals(1, enero.size(), "findByPeriodo debe devolver solo las nominas del periodo");
        assertEquals(1, enero.get(0).getMes());
    }

    @Test
    void deleteEliminaRegistro() throws Exception {
        Empleado emp = crearEmpleado("Elena", "Mora");
        NominaDAO dao = new NominaDAO(DatabaseManager.getConnection());
        Nomina n = nomina(emp.getId(), 3, 2026);
        dao.save(n);
        assertEquals(1, dao.findAll().size());

        dao.delete(n.getId());
        assertNull(dao.findById(n.getId()), "tras delete, findById debe devolver null");
        assertEquals(0, dao.findAll().size());
    }

    private Empleado crearEmpleado(String nombre, String apellidos) throws Exception {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e.setApellidos(apellidos);
        e.setActivo(true);
        new EmpleadoDAO(DatabaseManager.getConnection()).save(e);
        return e;
    }

    private Nomina nomina(int empleadoId, int mes, int anio) {
        Nomina n = new Nomina();
        n.setEmpleadoId(empleadoId);
        n.setMes(mes);
        n.setAnio(anio);
        n.setSalarioBase(1500.0);
        return n;
    }
}
