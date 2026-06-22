package org.gipsybuho.service;

import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.closeConnection();
        System.setProperty("graficas.mulberry.db.url", "jdbc:sqlite:" + tempDir.resolve("test.db"));
        DatabaseManager.initialize();
        auth = new AuthService(new UserDAO(DatabaseManager.getConnection()));
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.closeConnection();
        System.clearProperty("graficas.mulberry.db.url");
    }

    @Test
    void registerUserRechazaPasswordCorta() {
        assertFalse(auth.registerUser("admin", "1234567",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));
        assertFalse(auth.login("admin", "1234567").isPresent());
    }

    @Test
    void resetPasswordAdminRechazaPasswordCorta() {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));
        User user = auth.login("admin", "12345678").orElseThrow();

        assertFalse(auth.resetPasswordAdmin(user.getId(), "1234567"));
        assertFalse(auth.login("admin", "1234567").isPresent());
        assertTrue(auth.login("admin", "12345678").isPresent());
    }

    @Test
    void changePasswordRechazaPasswordCorta() {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));
        User user = auth.login("admin", "12345678").orElseThrow();

        assertFalse(auth.changePassword(user.getId(), "12345678", "1234567"));
        assertFalse(auth.login("admin", "1234567").isPresent());
        assertTrue(auth.login("admin", "12345678").isPresent());
    }

    @Test
    void loginBloqueaTemporalmenteTrasCincoFallos() {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));

        for (int i = 0; i < 5; i++) {
            assertFalse(auth.login("admin", "incorrecta").isPresent());
        }

        assertTrue(auth.isLoginTemporarilyBlocked("admin"));
        assertFalse(auth.login("admin", "12345678").isPresent());
    }

    @Test
    void recuperacionBloqueaTemporalmenteTrasCincoFallos() {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));
        assertTrue(auth.setSecurityQuestion("admin", AuthService.SECURITY_QUESTIONS.get(0), "respuesta"));

        for (int i = 0; i < 5; i++) {
            assertFalse(auth.resetPasswordWithAnswer("admin", "mal", "87654321"));
        }

        assertTrue(auth.isRecoveryTemporarilyBlocked("admin"));
        assertFalse(auth.resetPasswordWithAnswer("admin", "respuesta", "87654321"));
    }

    @Test
    void lockoutLoginPersisteAlReiniciarAuthService() throws SQLException {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));

        for (int i = 0; i < 5; i++) {
            assertFalse(auth.login("admin", "incorrecta").isPresent());
        }
        assertTrue(auth.isLoginTemporarilyBlocked("admin"));

        // Simula reinicio: nueva instancia con la misma BD
        AuthService auth2 = new AuthService(new UserDAO(DatabaseManager.getConnection()));
        assertTrue(auth2.isLoginTemporarilyBlocked("admin"));
        assertFalse(auth2.login("admin", "12345678").isPresent());
    }

    @Test
    void lockoutRecuperacionPersisteAlReiniciarAuthService() throws SQLException {
        assertTrue(auth.registerUser("admin", "12345678",
            UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString()));
        assertTrue(auth.setSecurityQuestion("admin", AuthService.SECURITY_QUESTIONS.get(0), "respuesta"));

        for (int i = 0; i < 5; i++) {
            assertFalse(auth.resetPasswordWithAnswer("admin", "mal", "87654321"));
        }
        assertTrue(auth.isRecoveryTemporarilyBlocked("admin"));

        AuthService auth2 = new AuthService(new UserDAO(DatabaseManager.getConnection()));
        assertTrue(auth2.isRecoveryTemporarilyBlocked("admin"));
        assertFalse(auth2.resetPasswordWithAnswer("admin", "respuesta", "87654321"));
    }
}
