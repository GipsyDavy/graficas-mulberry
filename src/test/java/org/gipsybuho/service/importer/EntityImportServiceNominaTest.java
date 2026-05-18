package org.gipsybuho.service.importer;

import org.gipsybuho.dao.EmpleadoDAO;
import org.gipsybuho.dao.NominaDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.service.EntityImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityImportServiceNominaTest {

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
    void importaDosNominasValidasConEmpleadosExistentes() throws Exception {
        crearEmpleado("Ana", "Garcia");
        crearEmpleado("Luis", "Lopez");

        ImportResult result = importar(List.of(
            fila("Ana", "Garcia", "1", "2026", "1200"),
            fila("Luis", "Lopez", "2", "2026", "1300")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(2, result.filasImportadas());
        assertEquals(0, result.errores().size());
        assertEquals(2, new NominaDAO().findAll().size());
    }

    @Test
    void informaErrorCuandoEmpleadoNoExiste() throws Exception {
        ImportResult result = importar(List.of(
            fila("No Existe", "", "1", "2026", "1200")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Empleado no encontrado"));
    }

    @Test
    void informaErrorCuandoEmpleadoEsAmbiguo() throws Exception {
        crearEmpleado("Ana", "Garcia");
        crearEmpleado("Ana", "Garcia");

        ImportResult result = importar(List.of(
            fila("Ana", "Garcia", "1", "2026", "1200")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Empleado ambiguo"));
    }

    @Test
    void omiteNominaDuplicadaConSkipIfExists() throws Exception {
        Empleado empleado = crearEmpleado("Ana", "Garcia");
        Nomina existente = new Nomina();
        existente.setEmpleadoId(empleado.getId());
        existente.setMes(1);
        existente.setAnio(2026);
        new NominaDAO().save(existente);

        ImportResult result = importar(List.of(
            fila("Ana", "Garcia", "1", "2026", "1200")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(0, result.filasActualizadas());
        assertEquals(0, result.errores().size());
        assertEquals(1, new NominaDAO().findAll().size());
    }

    @Test
    void rechazaCampoNumericoMalFormado() throws Exception {
        crearEmpleado("Ana", "Garcia");

        ImportResult result = importar(List.of(
            fila("Ana", "Garcia", "1", "2026", "abc")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertEquals(ErrorTipo.TIPO_INVALIDO, result.errores().get(0).tipo());
    }

    private ImportResult importar(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Nomina.IMPORT_SPEC, filas, mapping(), policy);
    }

    private Map<String, String> mapping() {
        return Map.of(
            "empleado_nombre", "empleado_nombre",
            "empleado_apellidos", "empleado_apellidos",
            "mes", "mes",
            "anio", "anio",
            "salario_base", "salario_base"
        );
    }

    private Map<String, String> fila(String empleadoNombre, String empleadoApellidos,
                                      String mes, String anio, String salarioBase) {
        return Map.of(
            "empleado_nombre", empleadoNombre,
            "empleado_apellidos", empleadoApellidos,
            "mes", mes,
            "anio", anio,
            "salario_base", salarioBase
        );
    }

    private Empleado crearEmpleado(String nombre, String apellidos) throws Exception {
        Empleado empleado = new Empleado();
        empleado.setNombre(nombre);
        empleado.setApellidos(apellidos);
        empleado.setActivo(true);
        new EmpleadoDAO().save(empleado);
        return empleado;
    }
}
