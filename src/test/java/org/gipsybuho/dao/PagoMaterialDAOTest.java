package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.PagoMaterial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagoMaterialDAOTest {

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
    void saveInsertaYMarcarPagadoCambiaEstado() throws Exception {
        Material mat = crearMaterial("Papel A4");
        PagoMaterial p = pagoPendiente(mat.getId(), 250.0, LocalDate.now().plusDays(30));
        PagoMaterialDAO dao = new PagoMaterialDAO();
        dao.save(p);
        assertEquals("pendiente", dao.findAll().get(0).getEstado());

        dao.marcarPagado(p.getId(), LocalDate.now());
        assertEquals("pagado", dao.findAll().get(0).getEstado(), "marcarPagado debe cambiar estado a 'pagado'");
    }

    @Test
    void findPendientesNoIncluyePagados() throws Exception {
        Material mat = crearMaterial("Tinta");
        PagoMaterialDAO dao = new PagoMaterialDAO();
        PagoMaterial pendiente = pagoPendiente(mat.getId(), 100.0, LocalDate.now().plusDays(10));
        PagoMaterial pagado = pagoPendiente(mat.getId(), 200.0, LocalDate.now().plusDays(5));
        dao.save(pendiente);
        dao.save(pagado);
        dao.marcarPagado(pagado.getId(), LocalDate.now());

        List<PagoMaterial> pendientes = dao.findPendientes();
        assertEquals(1, pendientes.size(), "findPendientes no debe incluir pagos ya pagados");
        assertEquals(pendiente.getId(), pendientes.get(0).getId());
    }

    @Test
    void totalPendienteAgregaCorrectamente() throws Exception {
        Material mat = crearMaterial("Cartón");
        PagoMaterialDAO dao = new PagoMaterialDAO();
        dao.save(pagoPendiente(mat.getId(), 300.0, LocalDate.now().plusDays(10)));
        dao.save(pagoPendiente(mat.getId(), 150.0, LocalDate.now().plusDays(20)));

        assertEquals(450.0, dao.totalPendiente(), 0.001, "totalPendiente debe sumar todos los importes pendientes");
    }

    private Material crearMaterial(String nombre) throws Exception {
        Material m = new Material();
        m.setNombre(nombre);
        m.setUnidad("ud");
        new MaterialDAO(DatabaseManager.getConnection()).save(m);
        return m;
    }

    private PagoMaterial pagoPendiente(int materialId, double importe, LocalDate vencimiento) {
        PagoMaterial p = new PagoMaterial();
        p.setMaterialId(materialId);
        p.setProveedor("Proveedor Test");
        p.setFechaCompra(LocalDate.now());
        p.setImporteTotal(importe);
        p.setFechaVencimiento(vencimiento);
        p.setEstado("pendiente");
        p.setCantidadComprada(1.0);
        return p;
    }
}
