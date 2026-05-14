package org.gipsybuho.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportBackupServiceTest {

    @Test
    void aceptaSentenciasQueEmiteExportService() throws Exception {
        assertTrue(esSeguro("BEGIN TRANSACTION"));
        assertTrue(esSeguro("COMMIT"));
        assertTrue(esSeguro("PRAGMA foreign_keys = OFF"));
        assertTrue(esSeguro("PRAGMA foreign_keys = ON"));
        assertTrue(esSeguro("DROP TABLE IF EXISTS clientes"));
        assertTrue(esSeguro("CREATE TABLE clientes (id INTEGER PRIMARY KEY, nombre TEXT)"));
        assertTrue(esSeguro("INSERT INTO clientes (id, nombre) VALUES (1, 'Ana')"));
    }

    @Test
    void rechazaSentenciasSobreTablasFueraDelBackup() throws Exception {
        assertFalse(esSeguro("DROP TABLE IF EXISTS usuarios"));
        assertFalse(esSeguro("CREATE TABLE usuarios (id INTEGER PRIMARY KEY)"));
        assertFalse(esSeguro("INSERT INTO usuarios (id) VALUES (1)"));
    }

    @Test
    void aceptaIdentificadoresEntreComillasDobles() throws Exception {
        assertTrue(esSeguro("DROP TABLE IF EXISTS \"clientes\""));
        assertTrue(esSeguro("INSERT INTO \"clientes\" (id) VALUES (1)"));
    }

    @Test
    void rechazaAttachYDetach() throws Exception {
        assertFalse(esSeguro("ATTACH DATABASE 'malicioso.db' AS evil"));
        assertFalse(esSeguro("DETACH DATABASE evil"));
    }

    @Test
    void rechazaPragmaSensibles() throws Exception {
        assertFalse(esSeguro("PRAGMA key = 'secreto'"));
        assertFalse(esSeguro("PRAGMA cipher_compatibility = 4"));
        assertFalse(esSeguro("PRAGMA journal_mode = WAL"));
    }

    @Test
    void rechazaDmlDestructivo() throws Exception {
        assertFalse(esSeguro("UPDATE usuarios SET password_hash = 'x' WHERE id = 1"));
        assertFalse(esSeguro("DELETE FROM usuarios"));
        assertFalse(esSeguro("TRUNCATE TABLE clientes"));
    }

    @Test
    void rechazaDropTableSinIfExists() throws Exception {
        assertFalse(esSeguro("DROP TABLE clientes"));
        assertFalse(esSeguro("DROP INDEX idx_clientes"));
        assertFalse(esSeguro("DROP TRIGGER trg_clientes"));
    }

    @Test
    void rechazaIdentificadoresInvalidos() throws Exception {
        assertFalse(esSeguro("INSERT INTO 1tabla (id) VALUES (1)"));
        assertFalse(esSeguro("CREATE TABLE -- comentario raro"));
        assertFalse(esSeguro("INSERT INTO \"tabla rara\" (id) VALUES (1)"));
    }

    @Test
    void rechazaComandosShellSqlite() throws Exception {
        assertFalse(esSeguro(".dump"));
        assertFalse(esSeguro(".load extension.so"));
        assertFalse(esSeguro("SELECT load_extension('mal.so')"));
    }

    @Test
    void aceptaInsertOrReplaceEIgnore() throws Exception {
        assertTrue(esSeguro("INSERT OR REPLACE INTO clientes (id) VALUES (1)"));
        assertTrue(esSeguro("INSERT OR IGNORE INTO clientes (id) VALUES (1)"));
    }

    @Test
    void aceptaCreateTableMultilineaConSaltosDeLinea() throws Exception {
        String stmt = "CREATE TABLE clientes (\n  id INTEGER PRIMARY KEY,\n  nombre TEXT NOT NULL\n)";
        assertTrue(esSeguro(stmt));
    }

    @Test
    void divideSqlRespetandoPuntoYComaDentroDeLiterales() throws Exception {
        List<String> sentencias = dividir(
            "INSERT INTO clientes (id, notas) VALUES (1, 'línea 1;\nlínea 2');\n" +
            "COMMIT;"
        );

        assertEquals(2, sentencias.size());
        assertTrue(sentencias.get(0).contains("línea 1;"));
        assertTrue(sentencias.get(0).contains("línea 2"));
        assertEquals("COMMIT", sentencias.get(1));
    }

    private static boolean esSeguro(String stmt) throws Exception {
        Method m = ImportBackupService.class.getDeclaredMethod("esStatementSeguro", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, stmt);
    }

    @SuppressWarnings("unchecked")
    private static List<String> dividir(String sql) throws Exception {
        Method m = ImportBackupService.class.getDeclaredMethod("dividirSentenciasSql", String.class);
        m.setAccessible(true);
        return (List<String>) m.invoke(null, sql);
    }
}
