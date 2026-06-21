package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialDAOTest {

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
    void ajustarStockRespetaTxExternaYNoCommitea() throws Exception {
        Material m = crearMaterial("Tinta test", 100.0);
        int idMaterial = m.getId();
        int movimientosPrevios = contarMovimientos(idMaterial);

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            new MaterialDAO(DatabaseManager.getConnection()).ajustarStock(idMaterial, 10.0, "salida", "test rollback");
            // Stock DENTRO de la tx: ya descontado en memoria de la tx.
            assertEquals(90.0, new MaterialDAO(DatabaseManager.getConnection()).findById(idMaterial).getStockActual(), 0.001,
                "Dentro de la tx, el stock debe verse descontado");
            // Forzamos rollback del caller. ajustarStock NO debe haber commiteado.
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }

        // Despues del rollback: el stock vuelve al original y no hay movimiento nuevo.
        assertEquals(100.0, new MaterialDAO(DatabaseManager.getConnection()).findById(idMaterial).getStockActual(), 0.001,
            "Tras rollback del caller, el stock vuelve al estado previo");
        assertEquals(movimientosPrevios, contarMovimientos(idMaterial),
            "Tras rollback del caller, no hay movimiento de stock registrado");
    }

    @Test
    void ajustarStockSinTxExternaCommiteaNormalmente() throws Exception {
        Material m = crearMaterial("Tinta test 2", 50.0);
        int idMaterial = m.getId();

        new MaterialDAO(DatabaseManager.getConnection()).ajustarStock(idMaterial, 5.0, "salida", "test sin tx externa");

        assertEquals(45.0, new MaterialDAO(DatabaseManager.getConnection()).findById(idMaterial).getStockActual(), 0.001,
            "Sin tx externa, ajustarStock commitea y el stock queda persistido");
        assertTrue(contarMovimientos(idMaterial) >= 1,
            "Sin tx externa, el movimiento queda registrado");
    }

    @Test
    void saveInsertaDefaultsDDLEnCategoriaYUnidad() throws Exception {
        // Material construido inline SIN setear categoria ni unidad.
        // El helper crearMaterial(...) los setea explicitamente y no sirve para este caso.
        Material m = new Material();
        m.setNombre("Material sin categoria ni unidad");
        m.setReferencia("REF-DEF-DDL");
        m.setStockActual(0.0);
        m.setStockMinimo(0.0);
        m.setPrecioUnidad(0.0);

        new MaterialDAO(DatabaseManager.getConnection()).save(m);

        Material recargado = new MaterialDAO(DatabaseManager.getConnection()).findById(m.getId());
        assertEquals("consumibles", recargado.getCategoria(),
            "categoria null al insertar debe preservar DEFAULT DDL 'consumibles'");
        assertEquals("ud", recargado.getUnidad(),
            "unidad null al insertar debe preservar DEFAULT DDL 'ud'");
    }

    @Test
    void referenciaUnicaRechazaDuplicado() throws Exception {
        MaterialDAO dao = new MaterialDAO(DatabaseManager.getConnection());
        Material m1 = new Material();
        m1.setNombre("Material A");
        m1.setReferencia("REF-UNICA-001");
        m1.setStockActual(0.0);
        m1.setStockMinimo(0.0);
        m1.setPrecioUnidad(0.0);
        dao.save(m1);

        Material m2 = new Material();
        m2.setNombre("Material B");
        m2.setReferencia("REF-UNICA-001");
        m2.setStockActual(0.0);
        m2.setStockMinimo(0.0);
        m2.setPrecioUnidad(0.0);
        assertThrows(Exception.class, () -> dao.save(m2),
            "Insertar dos materiales con la misma referencia no-nula debe lanzar excepción");
    }

    private Material crearMaterial(String nombre, double stock) throws SQLException {
        Material m = new Material();
        m.setNombre(nombre);
        m.setReferencia("REF-" + nombre.hashCode());
        m.setCategoria("test");
        m.setStockActual(stock);
        m.setStockMinimo(0.0);
        m.setUnidad("ud");
        m.setPrecioUnidad(1.0);
        new MaterialDAO(DatabaseManager.getConnection()).save(m);
        return m;
    }

    private int contarMovimientos(int materialId) throws SQLException {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM movimientos_material WHERE material_id = ?")) {
            ps.setInt(1, materialId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
