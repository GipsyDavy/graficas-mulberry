package org.gipsybuho.dao;

import org.gipsybuho.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class LogAccessDAO {

    public void logAccess(Integer userId, String username, String eventType) {
        String sql = "INSERT INTO log_accesos (user_id, username, event_type, timestamp) VALUES (?, ?, ?, datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setString(2, username);
            pstmt.setString(3, eventType);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar acceso: " + e.getMessage());
        }
    }

    public Optional<String> getLastSuccessfulLogin(String username) {
        String sql = "SELECT timestamp FROM log_accesos WHERE username = ? AND event_type = 'login_success' ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("timestamp"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el último acceso exitoso: " + e.getMessage());
        }
        return Optional.empty();
    }
}
