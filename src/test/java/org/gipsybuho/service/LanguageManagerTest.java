package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LanguageManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.closeConnection();
        System.setProperty("graficas.mulberry.db.url", "jdbc:sqlite:" + tempDir.resolve("test.db"));
        DatabaseManager.initialize();
        resetSingleton();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetSingleton();
        DatabaseManager.closeConnection();
        System.clearProperty("graficas.mulberry.db.url");
    }

    private void resetSingleton() throws Exception {
        Field f = LanguageManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void defaultIdiomaEsEspanol() {
        assertEquals("es", LanguageManager.getInstance().getIdioma());
    }

    @Test
    void bundleEsCargaCorrectamente() {
        assertEquals("Español", LanguageManager.t("lang.es"));
    }

    @Test
    void claveInexistenteRetornaCorchetes() {
        assertEquals("[clave.inexistente.xyz]", LanguageManager.t("clave.inexistente.xyz"));
    }

    @Test
    void setIdiomaGuardaEnConfig() {
        LanguageManager.getInstance().setIdioma("en");
        assertEquals("en", DatabaseManager.getConfig("idioma"));
    }

    @Test
    void bundleEnCargaSiIdiomaEstablecijoEnBD() throws Exception {
        DatabaseManager.setConfig("idioma", "en");
        resetSingleton();
        assertEquals("en", LanguageManager.getInstance().getIdioma());
        assertEquals("English", LanguageManager.t("lang.en"));
    }
}
