package org.gipsybuho.service.importer;

import org.gipsybuho.dao.AlbaranDAO;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.FacturaDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.service.EntityImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityImportServiceAlbaranTest {

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
    void importaAlbaranConDosLineas() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "", "Camiseta blanca", 10, "ud"),
                filaLinea("111A", "", "", "A-1", "", "", "Bolsa kraft",     5, "")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas(), "una entidad insertada");

        List<Albaran> persistidos = new AlbaranDAO().findAll();
        assertEquals(1, persistidos.size());
        Albaran a = new AlbaranDAO().findById(persistidos.get(0).getId());
        assertNotNull(a);
        assertEquals("A-1", a.getNumero());
        assertEquals(2, a.getLineas().size());
        // Estado default lo aplica la BD: 'pendiente'.
        assertEquals("pendiente", a.getEstado());
        // FKs opcionales sin informar: 0.
        assertEquals(0, a.getFacturaId());
        assertEquals(0, a.getPedidoId());
        // Unidad por defecto del DAO: 'ud' aunque la 2a fila la dejó vacía.
        assertEquals("Camiseta blanca", a.getLineas().get(0).getDescripcion());
        assertEquals(10, a.getLineas().get(0).getCantidad());
        assertEquals("ud", a.getLineas().get(0).getUnidad());
        assertEquals("Bolsa kraft", a.getLineas().get(1).getDescripcion());
        assertEquals("ud", a.getLineas().get(1).getUnidad());
    }

    @Test
    void descartaGrupoConCabeceraInconsistente() throws Exception {
        crearCliente("Ana",  "Garcia", "111A");
        crearCliente("Luis", "Lopez",  "222B");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "", "Camiseta", 10, "ud"),
                filaLinea("222B", "", "", "A-1", "", "", "Bolsa",     5, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(2, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Inconsistencia"));
        assertEquals(0, new AlbaranDAO().findAll().size());
    }

    @Test
    void informaErrorCuandoClienteNifNoExiste() throws Exception {
        ImportResult result = importar(List.of(
                filaLinea("NOPE", "", "", "A-1", "", "", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Cliente con nif"));
        assertEquals(0, new AlbaranDAO().findAll().size());
    }

    @Test
    void importaPorNombreCuandoNifEstaVacio() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("", "Ana", "Garcia", "A-1", "", "", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas());
        assertEquals(1, new AlbaranDAO().findAll().size());
    }

    @Test
    void omiteAlbaranDuplicadoConSkipIfExists() throws Exception {
        Cliente c = crearCliente("Ana", "Garcia", "111A");
        Albaran existente = new Albaran();
        existente.setClienteId(c.getId());
        existente.setNumero("A-1");
        existente.setFecha("2025-01-15");
        existente.setEstado("pendiente");
        new AlbaranDAO().save(existente);

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(0, result.filasActualizadas());
        assertEquals(0, result.errores().size());
        assertEquals(1, new AlbaranDAO().findAll().size());
    }

    @Test
    void rechazaUpdateExistingDesdeElInicioParaParentChild() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "", "Camiseta", 10, "ud")
            ), DuplicatePolicy.UPDATE_EXISTING)
        );
        assertTrue(ex.getMessage().contains("UPDATE_EXISTING"));
        assertTrue(ex.getMessage().contains("Albaranes"));
    }

    @Test
    void rechazaCreateNewDesdeElInicioParaParentChild() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "", "Camiseta", 10, "ud")
            ), DuplicatePolicy.CREATE_NEW)
        );
        assertTrue(ex.getMessage().contains("CREATE_NEW"));
        assertTrue(ex.getMessage().contains("Albaranes"));
    }

    @Test
    void resuelveFacturaNumeroAFkCuandoExiste() throws Exception {
        Cliente c = crearCliente("Ana", "Garcia", "111A");
        Factura f = new Factura();
        f.setClienteId(c.getId());
        f.setNumero("F-99");
        f.setFecha("2025-01-15");
        f.setEstado("pendiente");
        f.setIvaPorcentaje(21.0);
        new FacturaDAO().save(f);

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "F-99", "", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas());
        List<Albaran> persistidos = new AlbaranDAO().findAll();
        assertEquals(1, persistidos.size());
        Albaran a = new AlbaranDAO().findById(persistidos.get(0).getId());
        assertEquals(f.getId(), a.getFacturaId());
        assertEquals(0, a.getPedidoId());
    }

    @Test
    void informaErrorCuandoFacturaNumeroNoExiste() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "F-DESCONOCIDA", "", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Factura con numero"));
        assertEquals(0, new AlbaranDAO().findAll().size());
    }

    @Test
    void resuelvePedidoNumeroAFkCuandoExiste() throws Exception {
        Cliente c = crearCliente("Ana", "Garcia", "111A");
        Pedido p = new Pedido();
        p.setClienteId(c.getId());
        p.setNumero("P-77");
        p.setFecha(LocalDate.parse("2025-01-15"));
        p.setEstado("pendiente");
        p.setIvaPorcentaje(21.0);
        new PedidoDAO(DatabaseManager.getConnection()).save(p);

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "P-77", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.errores().size(), "no debe haber errores: " + result.errores());
        assertEquals(1, result.filasImportadas());
        List<Albaran> persistidos = new AlbaranDAO().findAll();
        assertEquals(1, persistidos.size());
        Albaran a = new AlbaranDAO().findById(persistidos.get(0).getId());
        assertEquals(p.getId(), a.getPedidoId());
        assertEquals(0, a.getFacturaId());
    }

    @Test
    void informaErrorCuandoPedidoNumeroNoExiste() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                filaLinea("111A", "", "", "A-1", "", "P-DESCONOCIDO", "Camiseta", 10, "ud")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Pedido con numero"));
        assertEquals(0, new AlbaranDAO().findAll().size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ImportResult importar(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Albaran.IMPORT_SPEC, filas, mapping(), policy);
    }

    private Map<String, String> mapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("cliente_nif",       "cliente_nif");
        mapping.put("cliente_nombre",    "cliente_nombre");
        mapping.put("cliente_apellidos", "cliente_apellidos");
        mapping.put("numero",            "numero");
        mapping.put("factura_numero",    "factura_numero");
        mapping.put("pedido_numero",     "pedido_numero");
        mapping.put("descripcion",       "descripcion");
        mapping.put("cantidad",          "cantidad");
        mapping.put("unidad",            "unidad");
        return mapping;
    }

    private Map<String, String> filaLinea(String clienteNif, String clienteNombre, String clienteApellidos,
                                          String numero, String facturaNumero, String pedidoNumero,
                                          String descripcion, int cantidad, String unidad) {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("cliente_nif",       clienteNif);
        fila.put("cliente_nombre",    clienteNombre);
        fila.put("cliente_apellidos", clienteApellidos);
        fila.put("numero",            numero);
        fila.put("factura_numero",    facturaNumero);
        fila.put("pedido_numero",     pedidoNumero);
        fila.put("descripcion",       descripcion);
        fila.put("cantidad",          String.valueOf(cantidad));
        fila.put("unidad",            unidad);
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
