package org.gipsybuho.model;

import java.time.LocalDateTime;

public class User {
    public static final String ROLE_INITIAL_ADMIN = "ADMIN_INICIAL";
    public static final String ROLE_USER = "USUARIO";
    public static final String ALL_PERMISSIONS = "*";

    private int id;
    private String username;
    private String passwordHash;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private int failedLoginAttempts; // Nuevo campo
    private LocalDateTime lastFailedLogin; // Nuevo campo
    private boolean initialAdmin;
    private String role;
    private String permissions;

    public User() {
    }

    // Constructor completo
    public User(int id, String username, String passwordHash, LocalDateTime lastLogin, LocalDateTime createdAt,
                int failedLoginAttempts, LocalDateTime lastFailedLogin) {
        this(id, username, passwordHash, lastLogin, createdAt, failedLoginAttempts, lastFailedLogin, false, ROLE_USER, "");
    }

    public User(int id, String username, String passwordHash, LocalDateTime lastLogin, LocalDateTime createdAt,
                int failedLoginAttempts, LocalDateTime lastFailedLogin, boolean initialAdmin, String role, String permissions) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.lastLogin = lastLogin;
        this.createdAt = createdAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lastFailedLogin = lastFailedLogin;
        this.initialAdmin = initialAdmin;
        this.role = role == null || role.isBlank() ? ROLE_USER : role;
        this.permissions = permissions == null ? "" : permissions;
    }

    // Constructor sin ID para nuevos usuarios (y con valores por defecto para los nuevos campos)
    public User(String username, String passwordHash) {
        this(username, passwordHash, false, ROLE_USER, "");
    }

    public User(String username, String passwordHash, boolean initialAdmin, String role, String permissions) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now(); // Establecer la fecha de creación automáticamente
        this.failedLoginAttempts = 0; // Por defecto 0 intentos fallidos
        this.lastFailedLogin = null; // Por defecto sin fecha de último intento fallido
        this.initialAdmin = initialAdmin;
        this.role = role == null || role.isBlank() ? ROLE_USER : role;
        this.permissions = permissions == null ? "" : permissions;
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

    public boolean isInitialAdmin() {
        return initialAdmin;
    }

    public void setInitialAdmin(boolean initialAdmin) {
        this.initialAdmin = initialAdmin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null || role.isBlank() ? ROLE_USER : role;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions == null ? "" : permissions;
    }

    public boolean hasPermission(String permission) {
        if (initialAdmin || ALL_PERMISSIONS.equals(permissions)) {
            return true;
        }
        if (permission == null || permission.isBlank()) {
            return false;
        }
        for (String item : permissions.split(",")) {
            if (permission.equals(item.trim())) {
                return true;
            }
        }
        return false;
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
               ", initialAdmin=" + initialAdmin +
               ", role='" + role + '\'' +
               ", permissions='" + permissions + '\'' +
               '}';
    }
}
