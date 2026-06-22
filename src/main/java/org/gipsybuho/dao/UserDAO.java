package org.gipsybuho.dao;

import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final String SELECT_COLS =
        "id, username, password_hash, role, permissions, created_at, last_login, security_question, security_answer_hash";

    private final Connection conn;

    public UserDAO(Connection conn) {
        this.conn = conn;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLS + " FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT " + SELECT_COLS + " FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario por ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO usuarios (username, password_hash, role, permissions, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getPermissions());
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
            return user.getId() > 0;
        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE usuarios SET username = ?, password_hash = ?, role = ?, permissions = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getPermissions());
            ps.setInt(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLS + " FROM usuarios ORDER BY username";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
        }
        return users;
    }

    public boolean updateSecurityQuestion(int userId, String question, String answerHash) {
        String sql = "UPDATE usuarios SET security_question = ?, security_answer_hash = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, question);
            ps.setString(2, answerHash);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar pregunta de seguridad: " + e.getMessage());
            return false;
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE usuarios SET last_login = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar último acceso: " + e.getMessage());
        }
    }

    // ── Lockout persistente ───────────────────────────────────────────────────

    private record LockoutState(int count, Instant lockedUntil) {}

    private LockoutState readLockout(int userId, String colFailed, String colUntil) {
        String sql = "SELECT " + colFailed + ", " + colUntil + " FROM usuarios WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int cnt = rs.getInt(1);
                String until = rs.getString(2);
                return new LockoutState(cnt, until != null ? Instant.parse(until) : null);
            }
        } catch (SQLException e) {
            System.err.println("Error leyendo lockout: " + e.getMessage());
        }
        return new LockoutState(0, null);
    }

    private void writeLockout(int userId, String colFailed, String colUntil, int count, Instant lockedUntil) {
        String sql = "UPDATE usuarios SET " + colFailed + " = ?, " + colUntil + " = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, count);
            ps.setString(2, lockedUntil != null ? lockedUntil.toString() : null);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error escribiendo lockout: " + e.getMessage());
        }
    }

    private static final String COL_LOGIN_FAILED  = "login_failed";
    private static final String COL_LOGIN_UNTIL   = "login_locked_until";
    private static final String COL_REC_FAILED    = "recovery_failed";
    private static final String COL_REC_UNTIL     = "recovery_locked_until";

    public boolean isLoginLocked(int userId) {
        LockoutState s = readLockout(userId, COL_LOGIN_FAILED, COL_LOGIN_UNTIL);
        return s.lockedUntil() != null && Instant.now().isBefore(s.lockedUntil());
    }

    public long getLoginSecondsRemaining(int userId) {
        LockoutState s = readLockout(userId, COL_LOGIN_FAILED, COL_LOGIN_UNTIL);
        if (s.lockedUntil() == null) return 0;
        long secs = Duration.between(Instant.now(), s.lockedUntil()).toSeconds();
        return Math.max(0, secs);
    }

    public void clearLoginLockout(int userId) {
        writeLockout(userId, COL_LOGIN_FAILED, COL_LOGIN_UNTIL, 0, null);
    }

    public void recordLoginFailure(int userId, int maxAttempts, Duration lockoutDuration) {
        LockoutState s = readLockout(userId, COL_LOGIN_FAILED, COL_LOGIN_UNTIL);
        int newCount = s.count() + 1;
        Instant until = newCount >= maxAttempts ? Instant.now().plus(lockoutDuration) : null;
        writeLockout(userId, COL_LOGIN_FAILED, COL_LOGIN_UNTIL, newCount, until);
    }

    public boolean isRecoveryLocked(int userId) {
        LockoutState s = readLockout(userId, COL_REC_FAILED, COL_REC_UNTIL);
        return s.lockedUntil() != null && Instant.now().isBefore(s.lockedUntil());
    }

    public long getRecoverySecondsRemaining(int userId) {
        LockoutState s = readLockout(userId, COL_REC_FAILED, COL_REC_UNTIL);
        if (s.lockedUntil() == null) return 0;
        long secs = Duration.between(Instant.now(), s.lockedUntil()).toSeconds();
        return Math.max(0, secs);
    }

    public void clearRecoveryLockout(int userId) {
        writeLockout(userId, COL_REC_FAILED, COL_REC_UNTIL, 0, null);
    }

    public void recordRecoveryFailure(int userId, int maxAttempts, Duration lockoutDuration) {
        LockoutState s = readLockout(userId, COL_REC_FAILED, COL_REC_UNTIL);
        int newCount = s.count() + 1;
        Instant until = newCount >= maxAttempts ? Instant.now().plus(lockoutDuration) : null;
        writeLockout(userId, COL_REC_FAILED, COL_REC_UNTIL, newCount, until);
    }

    public boolean hasAdmin() {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE role = 'ADMINISTRADOR'";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        UserRole role;
        try {
            role = UserRole.valueOf(rs.getString("role"));
        } catch (IllegalArgumentException e) {
            // Rol desconocido en BD — fallar cerrado, no otorgar permisos por defecto (SEC-6).
            throw new SQLException("Rol de usuario desconocido en base de datos: " + rs.getString("role"));
        }
        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = createdAtStr != null ? LocalDateTime.parse(createdAtStr) : null;
        String lastLoginStr = rs.getString("last_login");
        LocalDateTime lastLogin = lastLoginStr != null ? LocalDateTime.parse(lastLoginStr) : null;
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            role,
            rs.getString("permissions"),
            createdAt,
            lastLogin,
            rs.getString("security_question"),
            rs.getString("security_answer_hash")
        );
    }
}
