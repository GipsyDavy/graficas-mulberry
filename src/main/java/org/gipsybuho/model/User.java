package org.gipsybuho.model;

import java.time.LocalDateTime;
import java.util.Arrays;

public class User {

    private int id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private String permissions;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private String securityQuestion;
    private String securityAnswerHash;

    public User() {}

    public User(int id, String username, String passwordHash,
                UserRole role, String permissions, LocalDateTime createdAt, LocalDateTime lastLogin,
                String securityQuestion, String securityAnswerHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.COMERCIAL;
        this.permissions = permissions != null ? permissions : "";
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.securityQuestion = securityQuestion;
        this.securityAnswerHash = securityAnswerHash;
    }

    public boolean hasPermission(String module) {
        if (module == null || module.isBlank()) return false;
        if (permissions == null || permissions.isBlank()) return false;
        return Arrays.asList(permissions.split(",")).contains(module.trim());
    }

    public boolean isAdmin() {
        return UserRole.ADMINISTRADOR == role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role != null ? role : UserRole.COMERCIAL; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions != null ? permissions : ""; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswerHash() { return securityAnswerHash; }
    public void setSecurityAnswerHash(String securityAnswerHash) { this.securityAnswerHash = securityAnswerHash; }

    @Override
    public String toString() { return username; }
}
