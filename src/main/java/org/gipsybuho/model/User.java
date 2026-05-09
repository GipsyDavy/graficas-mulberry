package org.gipsybuho.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private int failedLoginAttempts; // Nuevo campo
    private LocalDateTime lastFailedLogin; // Nuevo campo

    public User() {
    }

    // Constructor completo
    public User(int id, String username, String passwordHash, LocalDateTime lastLogin, LocalDateTime createdAt,
                int failedLoginAttempts, LocalDateTime lastFailedLogin) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lastFailedLogin = lastFailedLogin;
    }

    // Constructor sin ID para nuevos usuarios (y con valores por defecto para los nuevos campos)
    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now(); // Establecer la fecha de creación automáticamente
        this.failedLoginAttempts = 0; // Por defecto 0 intentos fallidos
        this.lastFailedLogin = null; // Por defecto sin fecha de último intento fallido
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Getters y Setters para los nuevos campos
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLastFailedLogin() {
        return lastFailedLogin;
    }

    public void setLastFailedLogin(LocalDateTime lastFailedLogin) {
        this.lastFailedLogin = lastFailedLogin;
    }

    @Override
    public String toString() {
        return "User{" +
               "id=" + id +
               ", username='" + username + '\'' +
               ", lastLogin=" + lastLogin +
               ", createdAt=" + createdAt +
               ", failedLoginAttempts=" + failedLoginAttempts +
               ", lastFailedLogin=" + lastFailedLogin +
               '}';
    }
}
