package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClienteDAOTest {

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
    void saveInsertaDefaultsDDLEnTipoYCiudad() throws Exception {
        Cliente c = new Cliente();
        c.setNombre("Test Empresa");
        // tipo y ciudad se dejan a null para verificar que setBase aplica el fallback Java-side.

        new ClienteDAO().save(c);

        Cliente recargado = new ClienteDAO().findById(c.getId());
        assertEquals("empresa", recargado.getTipo(),
            "tipo null al insertar debe preservar DEFAULT DDL 'empresa'");
        assertEquals("Almería", recargado.getCiudad(),
            "ciudad null al insertar debe preservar DEFAULT DDL 'Almería'");
    }

    @Test
    void mapMantieneApellidosCuandoApellidoLegacyEstaVacio() throws Exception {
        Cliente cliente = mapCliente(
            List.of("id", "nombre", "apellidos", "apellido"),
            Map.of(
                "id", 1,
                "nombre", "Ana",
                "apellidos", "Garcia Lopez",
                "apellido", ""
            )
        );

        assertEquals("Garcia Lopez", cliente.getApellidos());
    }

    @Test
    void mapUsaApellidoLegacySiApellidosEstaVacio() throws Exception {
        Cliente cliente = mapCliente(
            List.of("id", "nombre", "apellidos", "apellido"),
            Map.of(
                "id", 1,
                "nombre", "Ana",
                "apellidos", "",
                "apellido", "Garcia Lopez"
            )
        );

        assertEquals("Garcia Lopez", cliente.getApellidos());
    }

    @Test
    void nifUnicoRechazaDuplicado() throws Exception {
        ClienteDAO dao = new ClienteDAO();
        Cliente c1 = new Cliente();
        c1.setNombre("Empresa A");
        c1.setNif("B12345678");
        dao.save(c1);

        Cliente c2 = new Cliente();
        c2.setNombre("Empresa B");
        c2.setNif("B12345678");
        assertThrows(Exception.class, () -> dao.save(c2),
            "Insertar dos clientes con el mismo NIF no-nulo debe lanzar excepción");
    }

    @Test
    void updateMantieneColumnasExtraValidasConQuotingCentralizado() throws Exception {
        DatabaseManager.addColumn("clientes", "campo_extra");
        ClienteDAO dao = new ClienteDAO();

        Cliente c = new Cliente();
        c.setNombre("Empresa Extra");
        c.setExtra("campo_extra", "uno");
        dao.save(c);

        c.setExtra("campo_extra", "dos");
        dao.save(c);

        Cliente recargado = dao.findById(c.getId());
        assertEquals("dos", recargado.getExtra("campo_extra"));
    }

    private Cliente mapCliente(List<String> columns, Map<String, Object> values) throws Exception {
        ClienteDAO dao = new ClienteDAO();
        Method map = ClienteDAO.class.getDeclaredMethod("map", ResultSet.class);
        map.setAccessible(true);
        return (Cliente) map.invoke(dao, resultSet(columns, values));
    }

    private ResultSet resultSet(List<String> columns, Map<String, Object> values) {
        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ResultSetMetaData.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getColumnCount" -> columns.size();
                case "getColumnName" -> columns.get(((Integer) args[0]) - 1);
                default -> defaultValue(method.getReturnType());
            }
        );

        return (ResultSet) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ResultSet.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getMetaData" -> metaData;
                case "getString" -> stringValue(columns.get(((Integer) args[0]) - 1), values);
                case "getInt" -> intValue(columns.get(((Integer) args[0]) - 1), values);
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private String stringValue(String column, Map<String, Object> values) {
        Object value = values.get(column);
        return value != null ? value.toString() : null;
    }

    private int intValue(String column, Map<String, Object> values) {
        Object value = values.get(column);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
