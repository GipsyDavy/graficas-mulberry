package org.gipsybuho.service.importer;

import org.gipsybuho.dao.TarifaDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Tarifa;
import org.gipsybuho.service.EntityImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityImportServiceTarifaTest {

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
    void importaTramosConMismaTecnicaYNombrePeroDistintoMinimo() throws Exception {
        int inicial = new TarifaDAO().findAll().size();

        ImportResult result = importar(List.of(
            fila("Tarjetas de visita", "Tarjetas 300g", "100", "12"),
            fila("Tarjetas de visita", "Tarjetas 300g", "200", "15")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(2, result.filasImportadas());
        assertEquals(0, result.filasDescartadas());
        assertEquals(inicial + 2, new TarifaDAO().findAll().size());
    }

    @Test
    void omiteSoloTramoDuplicadoConMismaTecnicaNombreYMinimo() throws Exception {
        int inicial = new TarifaDAO().findAll().size();

        importar(List.of(
            fila("Tarjetas de visita", "Tarjetas 300g", "100", "12")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        ImportResult result = importar(List.of(
            fila("Tarjetas de visita", "Tarjetas 300g", "100", "15")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.filasDescartadas());
        assertEquals(inicial + 1, new TarifaDAO().findAll().size());
    }

    private ImportResult importar(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Tarifa.IMPORT_SPEC, filas, mapping(), policy);
    }

    private Map<String, String> mapping() {
        return Map.of(
            "tecnica", "tecnica",
            "nombre", "nombre",
            "minimo_unidades", "minimo_unidades",
            "precio_unit", "precio_unit"
        );
    }

    private Map<String, String> fila(String tecnica, String nombre, String minimoUnidades, String precioUnit) {
        return Map.of(
            "tecnica", tecnica,
            "nombre", nombre,
            "minimo_unidades", minimoUnidades,
            "precio_unit", precioUnit
        );
    }
}
