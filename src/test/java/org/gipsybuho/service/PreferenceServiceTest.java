package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PreferenceServiceTest {

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
        Field f = PreferenceService.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    @Test
    void beginnerModeDefaulteaFalso() {
        assertFalse(PreferenceService.getInstance().isBeginnerMode());
    }

    @Test
    void setBeginnerModePersiste() throws Exception {
        PreferenceService.getInstance().setBeginnerMode(true);
        resetSingleton();
        assertTrue(PreferenceService.getInstance().isBeginnerMode());
    }

    @Test
    void isFirstRunEsTrueEnPrimerArranque() {
        assertTrue(PreferenceService.getInstance().isFirstRun());
    }

    @Test
    void markFirstRunCompletedMarcaComoCompletado() {
        PreferenceService ps = PreferenceService.getInstance();
        assertTrue(ps.isFirstRun());
        ps.markFirstRunCompleted();
        assertFalse(ps.isFirstRun());
    }
}
