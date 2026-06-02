package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final String SELECT_COLS =
        "id, username, password_hash, role, permissions, created_at, last_login, security_question, security_answer_hash";

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLS + " FROM usuarios WHERE username = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(map(rs));
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
        }
        return users;
    }

    public boolean updateSecurityQuestion(int userId, String question, String answerHash) {
        String sql = "UPDATE usuarios SET security_question = ?, security_answer_hash = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
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
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar último acceso: " + e.getMessage());
        }
    }

    public boolean hasAdmin() {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE role = 'ADMINISTRADOR'";
        try (Statement st = DatabaseManager.getConnection().createStatement();
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
