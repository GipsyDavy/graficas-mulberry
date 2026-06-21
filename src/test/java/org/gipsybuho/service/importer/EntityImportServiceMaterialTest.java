package org.gipsybuho.service.importer;

import org.gipsybuho.dao.MaterialDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Material;
import org.gipsybuho.service.EntityImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntityImportServiceMaterialTest {

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
    void importaMaterialesSinReferenciaUsandoNombreComoClave() throws Exception {
        int inicial = new MaterialDAO(DatabaseManager.getConnection()).findAll().size();

        ImportResult result = importar(List.of(
            Map.of("nombre", "Plástico brillo 250m", "proveedor", "CODIAL", "precio_unidad", "0.35"),
            Map.of("nombre", "Plástico mate 250m", "proveedor", "CODIAL", "precio_unidad", "0.40")
        ));

        assertEquals(2, result.filasImportadas());
        assertEquals(0, result.filasDescartadas());
        assertEquals(inicial + 2, new MaterialDAO(DatabaseManager.getConnection()).findAll().size());
    }

    @Test
    void omiteMaterialDuplicadoPorNombreCuandoNoHayReferencia() throws Exception {
        int inicial = new MaterialDAO(DatabaseManager.getConnection()).findAll().size();

        importar(List.of(Map.of("nombre", "Tinta cyan", "precio_unidad", "12")));
        ImportResult result = importar(List.of(Map.of("nombre", "Tinta cyan", "precio_unidad", "15")));

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.filasDescartadas());
        assertEquals(inicial + 1, new MaterialDAO(DatabaseManager.getConnection()).findAll().size());
    }

    @Test
    void sigueUsandoReferenciaComoClaveCuandoExiste() throws Exception {
        int inicial = new MaterialDAO(DatabaseManager.getConnection()).findAll().size();

        importar(List.of(Map.of("nombre", "Papel estucado", "referencia", "REF-1")));
        // Mismo nombre pero referencia distinta: NO es duplicado.
        ImportResult result = importar(List.of(Map.of("nombre", "Papel estucado", "referencia", "REF-2")));

        assertEquals(1, result.filasImportadas());
        assertEquals(inicial + 2, new MaterialDAO(DatabaseManager.getConnection()).findAll().size());
    }

    @Test
    void ignoraUnidadNumericaImportadaYConservaUdPorDefecto() throws Exception {
        Map<String, String> mapping = Map.of("nombre", "nombre", "unidad", "unidad");

        ImportResult result = new EntityImportService().importar(
            Material.IMPORT_SPEC,
            List.of(Map.of("nombre", "Papel con unidad numerica", "unidad", "0.08")),
            mapping,
            DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(1, result.filasImportadas());
        Material guardado = new MaterialDAO(DatabaseManager.getConnection()).findAll().stream()
            .filter(m -> "Papel con unidad numerica".equals(m.getNombre()))
            .findFirst().orElse(null);
        assertNotNull(guardado);
        assertEquals("ud", guardado.getUnidad());
    }

    private ImportResult importar(List<Map<String, String>> filas) throws Exception {
        Map<String, String> mapping = Map.of(
            "nombre", "nombre",
            "referencia", "referencia",
            "proveedor", "proveedor",
            "precio_unidad", "precio_unidad"
        );
        return new EntityImportService().importar(Material.IMPORT_SPEC, filas, mapping, DuplicatePolicy.SKIP_IF_EXISTS);
    }
}
