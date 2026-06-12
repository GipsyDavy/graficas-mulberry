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
        auth = new AuthService(new UserDAO());
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
}
