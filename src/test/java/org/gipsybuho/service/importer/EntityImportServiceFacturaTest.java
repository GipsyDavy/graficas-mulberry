package org.gipsybuho.service.importer;

import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.PresupuestoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.Presupuesto;
import org.gipsybuho.service.EntityImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityImportServiceFacturaTest {

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
    void importaFacturaConDosLineasYRecalculaTotales() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "F-1", "", "Camiseta", 10, "5.00", "0"),
                filaLinea("111A", "", "", "F-1", "", "Bolsa",     5, "2.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas(), "una entidad insertada");

        List<Factura> persistidas = new FacturaDAO().findAll();
        assertEquals(1, persistidas.size());
        Factura f = new FacturaDAO().findById(persistidas.get(0).getId());
        assertNotNull(f);
        assertEquals("F-1", f.getNumero());
        assertEquals(2, f.getLineas().size());
        // Totales: 10*5 + 5*2 = 60 base; IVA default 21% = 12.6; total = 72.6
        assertEquals(60.0, f.getBaseImponible(), 0.001);
        assertEquals(21.0, f.getIvaPorcentaje(), 0.001);
        assertEquals(12.6, f.getIvaImporte(), 0.001);
        assertEquals(72.6, f.getTotal(), 0.001);
        // Defaults aplicados: estado='pendiente', presupuesto_id=0 (sin FK).
        assertEquals("pendiente", f.getEstado());
        assertEquals(0, f.getPresupuestoId());
    }

    @Test
    void descartaGrupoConCabeceraInconsistente() throws Exception {
        crearCliente("Ana",  "Garcia", "111A");
        crearCliente("Luis", "Lopez",  "222B");

        // Mismo numero "F-1" pero cliente_nif distinto en cada fila -> inconsistencia.
        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "F-1", "", "Camiseta", 10, "5.00", "0"),
                filaLinea("222B", "", "", "F-1", "", "Bolsa",     5, "2.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        // detectarInconsistenciaGrupo anade un RowError por cada fila del grupo discrepante.
        assertEquals(2, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Inconsistencia"));
        assertEquals(0, new FacturaDAO().findAll().size());
    }

    @Test
    void informaErrorCuandoClienteNifNoExiste() throws Exception {
        ImportResult result = importar(List.of(
                filaLinea("NOPE", "", "", "F-1", "", "Camiseta", 10, "5.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Cliente con nif"));
        assertEquals(0, new FacturaDAO().findAll().size());
    }

    @Test
    void importaPorNombreCuandoNifEstaVacio() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("", "Ana", "Garcia", "F-1", "", "Camiseta", 10, "5.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas());
        assertEquals(1, new FacturaDAO().findAll().size());
    }

    @Test
    void omiteFacturaDuplicadaConSkipIfExists() throws Exception {
        Cliente c = crearCliente("Ana", "Garcia", "111A");
        Factura existente = new Factura();
        existente.setClienteId(c.getId());
        existente.setNumero("F-1");
        existente.setFecha("2025-01-15");
        existente.setEstado("pendiente");
        existente.setIvaPorcentaje(21.0);
        new FacturaDAO().save(existente);

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "F-1", "", "Camiseta", 10, "5.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(0, result.filasActualizadas());
        assertEquals(0, result.errores().size());
        assertEquals(1, new FacturaDAO().findAll().size());
    }

    @Test
    void rechazaUpdateExistingDesdeElInicioParaParentChild() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            importar(List.of(
                filaLinea("111A", "", "", "F-1", "", "Camiseta", 10, "5.00", "0")
            ), DuplicatePolicy.UPDATE_EXISTING)
        );
        assertTrue(ex.getMessage().contains("UPDATE_EXISTING"));
        assertTrue(ex.getMessage().contains("Facturas"));
    }

    @Test
    void rechazaCreateNewDesdeElInicioParaParentChild() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            importar(List.of(
                filaLinea("111A", "", "", "F-1", "", "Camiseta", 10, "5.00", "0")
            ), DuplicatePolicy.CREATE_NEW)
        );
        assertTrue(ex.getMessage().contains("CREATE_NEW"));
        assertTrue(ex.getMessage().contains("Facturas"));
    }

    @Test
    void resuelvePresupuestoNumeroAFkCuandoExiste() throws Exception {
        Cliente c = crearCliente("Ana", "Garcia", "111A");
        Presupuesto p = new Presupuesto();
        p.setClienteId(c.getId());
        p.setNumero("P-99");
        p.setFecha("2025-01-15");
        p.setEstado("aceptado");
        p.setIvaPorcentaje(21.0);
        new PresupuestoDAO(DatabaseManager.getConnection()).save(p);

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "F-1", "P-99", "Camiseta", 10, "5.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas());
        List<Factura> persistidas = new FacturaDAO().findAll();
        assertEquals(1, persistidas.size());
        Factura f = new FacturaDAO().findById(persistidas.get(0).getId());
        assertEquals(p.getId(), f.getPresupuestoId());
    }

    @Test
    void informaErrorCuandoPresupuestoNumeroNoExiste() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "F-1", "P-DESCONOCIDO", "Camiseta", 10, "5.00", "0")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Presupuesto con numero"));
        assertEquals(0, new FacturaDAO().findAll().size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ImportResult importar(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Factura.IMPORT_SPEC, filas, mapping(), policy);
    }

    private Map<String, String> mapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("cliente_nif",        "cliente_nif");
        mapping.put("cliente_nombre",     "cliente_nombre");
        mapping.put("cliente_apellidos",  "cliente_apellidos");
        mapping.put("numero",             "numero");
        mapping.put("presupuesto_numero", "presupuesto_numero");
        mapping.put("descripcion",        "descripcion");
        mapping.put("cantidad",           "cantidad");
        mapping.put("precio_unit",        "precio_unit");
        mapping.put("descuento",          "descuento");
        return mapping;
    }

    private Map<String, String> filaLinea(String clienteNif, String clienteNombre, String clienteApellidos,
                                          String numero, String presupuestoNumero,
                                          String descripcion, int cantidad,
                                          String precioUnit, String descuento) {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("cliente_nif",        clienteNif);
        fila.put("cliente_nombre",     clienteNombre);
        fila.put("cliente_apellidos",  clienteApellidos);
        fila.put("numero",             numero);
        fila.put("presupuesto_numero", presupuestoNumero);
        fila.put("descripcion",        descripcion);
        fila.put("cantidad",           String.valueOf(cantidad));
        fila.put("precio_unit",        precioUnit);
        fila.put("descuento",          descuento);
        return fila;
    }

    private Cliente crearCliente(String nombre, String apellidos, String nif) throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellidos(apellidos);
        cliente.setNif(nif);
        cliente.setTipo("empresa");
        new ClienteDAO(DatabaseManager.getConnection()).save(cliente);
        return cliente;
    }
}
