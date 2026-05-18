package org.gipsybuho.service.importer;

import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.dao.PedidoDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Pedido;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityImportServicePedidoTest {

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
    void importaDosPedidosValidosConClientePorNif() throws Exception {
        crearCliente("Ana", "Garcia", "111A");
        crearCliente("Luis", "Lopez", "222B");

        ImportResult result = importar(List.of(
                fila("111A", "", "", "PED-1", "120.50"),
                fila("222B", "", "", "PED-2", "230.75")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(2, result.filasImportadas());
        assertEquals(0, result.errores().size());
        assertEquals(2, new PedidoDAO().findAll().size());
    }

    @Test
    void informaErrorCuandoClienteNifNoExiste() throws Exception {
        ImportResult result = importar(List.of(
                fila("NOPE", "", "", "PED-1", "120.50")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Cliente con nif"));
        assertTrue(result.errores().get(0).mensaje().contains("no encontrado"));
    }

    @Test
    void informaErrorCuandoClienteNifNoExisteAunqueNombreCoincida() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                fila("NOPE", "Ana", "Garcia", "PED-1", "120.50")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Cliente con nif"));
        assertTrue(result.errores().get(0).mensaje().contains("no encontrado"));
        assertEquals(0, new PedidoDAO().findAll().size());
    }

    @Test
    void importaPorNombreCuandoNifEstaVacio() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                fila("", "Ana", "Garcia", "PED-1", "120.50")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(1, result.filasImportadas());
        assertEquals(0, result.errores().size());
        assertEquals(1, new PedidoDAO().findAll().size());
    }

    @Test
    void informaErrorCuandoClienteAmbiguoPorNombre() throws Exception {
        crearCliente("Ana", "Garcia", "111A");
        crearCliente("Ana", "Garcia", "222B");

        ImportResult result = importar(List.of(
                fila("", "Ana", "Garcia", "PED-1", "120.50")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertTrue(result.errores().get(0).mensaje().contains("Cliente ambiguo"));
    }

    @Test
    void omitePedidoDuplicadoConSkipIfExists() throws Exception {
        Cliente cliente = crearCliente("Ana", "Garcia", "111A");
        Pedido existente = new Pedido();
        existente.setClienteId(cliente.getId());
        existente.setNumero("PED-1");
        new PedidoDAO().save(existente);

        ImportResult result = importar(List.of(
                fila("111A", "", "", "PED-1", "120.50")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(0, result.filasActualizadas());
        assertEquals(0, result.errores().size());
        assertEquals(1, new PedidoDAO().findAll().size());
    }

    @Test
    void rechazaImporteTotalMalFormado() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importar(List.of(
                fila("111A", "", "", "PED-1", "abc")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        assertEquals(ErrorTipo.TIPO_INVALIDO, result.errores().get(0).tipo());
    }

    @Test
    void rechazaFechaMalFormada() throws Exception {
        crearCliente("Ana", "Garcia", "111A");

        ImportResult result = importarConFecha(List.of(
                fila("111A", "", "", "PED-FECHA-1", "120.50", "15/03/2024")
        ), DuplicatePolicy.SKIP_IF_EXISTS);

        assertEquals(0, result.filasImportadas());
        assertEquals(1, result.errores().size());
        RowError err = result.errores().get(0);
        assertEquals(ErrorTipo.TIPO_INVALIDO, err.tipo());
        assertEquals("fecha", err.campo());
        assertTrue(err.mensaje().contains("Fecha no válida"));
    }

    @Test
    void rechazaActualizacionConFechaMalFormada() throws Exception {
        Cliente cliente = crearCliente("Ana", "Garcia", "111A");
        Pedido existente = new Pedido();
        existente.setClienteId(cliente.getId());
        existente.setNumero("P-001");
        existente.setFecha(java.time.LocalDate.of(2024, 1, 15));
        new PedidoDAO().save(existente);

        ImportResult result = importarConFecha(List.of(
                fila("111A", "", "", "P-001", "120.50", "15/03/2024")
        ), DuplicatePolicy.UPDATE_EXISTING);

        assertEquals(0, result.filasImportadas());
        assertEquals(0, result.filasActualizadas());
        assertEquals(1, result.errores().size());
        RowError err = result.errores().get(0);
        assertEquals(ErrorTipo.TIPO_INVALIDO, err.tipo());
        assertEquals("fecha", err.campo());
        assertTrue(err.mensaje().contains("Fecha no válida"));
        Pedido guardado = new PedidoDAO().findAll().stream()
                .filter(pedido -> "P-001".equals(pedido.getNumero()))
                .findFirst()
                .orElseThrow();
        assertEquals(java.time.LocalDate.of(2024, 1, 15), guardado.getFecha());
    }

    private ImportResult importar(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Pedido.IMPORT_SPEC, filas, mapping(), policy);
    }

    private ImportResult importarConFecha(List<Map<String, String>> filas, DuplicatePolicy policy) throws Exception {
        return new EntityImportService().importar(Pedido.IMPORT_SPEC, filas, mapping(true), policy);
    }

    private Map<String, String> mapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("cliente_nif", "cliente_nif");
        mapping.put("cliente_nombre", "cliente_nombre");
        mapping.put("cliente_apellidos", "cliente_apellidos");
        mapping.put("numero", "numero");
        mapping.put("importe_total", "importe_total");
        return mapping;
    }

    private Map<String, String> mapping(boolean conFecha) {
        Map<String, String> mapping = mapping();
        if (conFecha) mapping.put("fecha", "fecha");
        return mapping;
    }

    private Map<String, String> fila(String clienteNif, String clienteNombre, String clienteApellidos,
                                     String numero, String importeTotal) {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("cliente_nif", clienteNif);
        fila.put("cliente_nombre", clienteNombre);
        fila.put("cliente_apellidos", clienteApellidos);
        fila.put("numero", numero);
        fila.put("importe_total", importeTotal);
        return fila;
    }

    private Map<String, String> fila(String clienteNif, String clienteNombre, String clienteApellidos,
                                     String numero, String importeTotal, String fecha) {
        Map<String, String> fila = fila(clienteNif, clienteNombre, clienteApellidos, numero, importeTotal);
        fila.put("fecha", fecha);
        return fila;
    }

    private Cliente crearCliente(String nombre, String apellidos, String nif) throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setApellidos(apellidos);
        cliente.setNif(nif);
        cliente.setTipo("empresa");
        new ClienteDAO().save(cliente);
        return cliente;
    }
}
