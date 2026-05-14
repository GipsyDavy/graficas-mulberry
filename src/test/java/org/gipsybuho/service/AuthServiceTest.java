package org.gipsybuho.service;

import org.gipsybuho.dao.LogAccessDAO;
import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private Connection keeper;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        String dbUrl = "jdbc:sqlite:file:auth-" + UUID.randomUUID() + "?mode=memory&cache=shared";
        System.setProperty("graficas.mulberry.db.url", dbUrl);
        keeper = DriverManager.getConnection(dbUrl);
        DatabaseManager.closeConnection();
        DatabaseManager.initialize();
        authService = new AuthService(new UserDAO(), new LogAccessDAO());
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseManager.closeConnection();
        if (keeper != null && !keeper.isClosed()) {
            keeper.close();
        }
        System.clearProperty("graficas.mulberry.db.url");
    }

    @Test
    void loginCorrectoDevuelveUsuarioYReseteaIntentosFallidos() throws Exception {
        assertTrue(authService.registerUser("ana", "secreta"));
        assertTrue(authService.login("ana", "mal").isEmpty());

        Optional<User> login = authService.login("ana", "secreta");

        assertTrue(login.isPresent());
        assertEquals(0, login.get().getFailedLoginAttempts());
        assertNull(login.get().getLastFailedLogin());
        assertNotNull(login.get().getLastLogin());
    }

    @Test
    void loginFallidoIncrementaIntentosYBloqueaEnElSiguienteIntento() {
        assertTrue(authService.registerUser("ana", "secreta"));

        for (int i = 0; i < 5; i++) {
            assertTrue(authService.login("ana", "mal").isEmpty());
        }

        User bloqueada = authService.getAllUsers().stream()
            .filter(u -> "ana".equals(u.getUsername()))
            .findFirst()
            .orElseThrow();
        assertEquals(5, bloqueada.getFailedLoginAttempts());
        assertTrue(authService.isAccountLocked(bloqueada));
        assertTrue(authService.login("ana", "secreta").isEmpty());
    }

    @Test
    void bloqueoExpiradoPermiteLoginCorrecto() throws Exception {
        assertTrue(authService.registerUser("ana", "secreta"));
        int userId = authService.getAllUsers().stream()
            .filter(u -> "ana".equals(u.getUsername()))
            .findFirst()
            .orElseThrow()
            .getId();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE usuarios SET failed_login_attempts = 5, last_failed_login = ? WHERE id = ?")) {
            ps.setString(1, LocalDateTime.now().minusMinutes(10).toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        }

        Optional<User> login = authService.login("ana", "secreta");

        assertTrue(login.isPresent());
        assertEquals(0, login.get().getFailedLoginAttempts());
    }

    @Test
    void createInitialAdminCreaUnoSoloConPermisosCompletos() {
        Optional<User> admin = authService.createInitialAdmin("admin", "secreta");
        Optional<User> segundo = authService.createInitialAdmin("otro", "secreta");

        assertTrue(admin.isPresent());
        assertEquals(User.ROLE_INITIAL_ADMIN, admin.get().getRole());
        assertEquals(User.ALL_PERMISSIONS, admin.get().getPermissions());
        assertTrue(admin.get().isInitialAdmin());
        assertTrue(segundo.isEmpty());
    }

    @Test
    void createInitialAdminPromocionaUsuarioExistente() {
        assertTrue(authService.registerUser("ana", "vieja"));

        Optional<User> admin = authService.createInitialAdmin("ana", "nueva");

        assertTrue(admin.isPresent());
        assertTrue(admin.get().isInitialAdmin());
        assertTrue(authService.login("ana", "vieja").isEmpty());
        assertTrue(authService.login("ana", "nueva").isPresent());
    }

    @Test
    void changePasswordInvalidaPasswordAnterior() {
        assertTrue(authService.registerUser("ana", "vieja"));
        int userId = authService.login("ana", "vieja").orElseThrow().getId();

        assertTrue(authService.changePassword(userId, "nueva"));

        assertTrue(authService.login("ana", "vieja").isEmpty());
        assertTrue(authService.login("ana", "nueva").isPresent());
    }
}
