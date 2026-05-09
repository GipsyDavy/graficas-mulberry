package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, last_login, created_at, failed_login_attempts, last_failed_login FROM usuarios WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario por nombre: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, username, password_hash, last_login, created_at, failed_login_attempts, last_failed_login FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario por ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void createUser(User user) {
        String sql = "INSERT INTO usuarios (username, password_hash, created_at, failed_login_attempts) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getCreatedAt().toString());
            pstmt.setInt(4, user.getFailedLoginAttempts()); // Se inserta el valor inicial (0)
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
        }
    }

    public void updateUser(User user) {
        String sql = "UPDATE usuarios SET username = ?, password_hash = ?, last_login = ?, failed_login_attempts = ?, last_failed_login = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getLastLogin() != null ? user.getLastLogin().toString() : null);
            pstmt.setInt(4, user.getFailedLoginAttempts());
            pstmt.setString(5, user.getLastFailedLogin() != null ? user.getLastFailedLogin().toString() : null);
            pstmt.setInt(6, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE usuarios SET last_login = ?, failed_login_attempts = 0, last_failed_login = NULL WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDateTime.now().toString());
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar último login: " + e.getMessage());
        }
    }

    public void updateFailedLoginAttempts(int userId, int attempts, LocalDateTime lastFailedLogin) {
        String sql = "UPDATE usuarios SET failed_login_attempts = ?, last_failed_login = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, attempts);
            pstmt.setString(2, lastFailedLogin != null ? lastFailedLogin.toString() : null);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar intentos de login fallidos: " + e.getMessage());
        }
    }

    public void deleteUser(int userId) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() {
        String sql = "SELECT id, username, password_hash, last_login, created_at, failed_login_attempts, last_failed_login FROM usuarios";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener todos los usuarios: " + e.getMessage());
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String lastLoginStr = rs.getString("last_login");
        String createdAtStr = rs.getString("created_at");
        int failedLoginAttempts = rs.getInt("failed_login_attempts"); // Nuevo campo
        String lastFailedLoginStr = rs.getString("last_failed_login"); // Nuevo campo

        LocalDateTime lastLogin = (lastLoginStr != null) ? LocalDateTime.parse(lastLoginStr) : null;
        LocalDateTime createdAt = (createdAtStr != null) ? LocalDateTime.parse(createdAtStr) : null;
        LocalDateTime lastFailedLogin = (lastFailedLoginStr != null) ? LocalDateTime.parse(lastFailedLoginStr) : null; // Parsear nuevo campo

        return new User(id, username, passwordHash, lastLogin, createdAt, failedLoginAttempts, lastFailedLogin);
    }
}
