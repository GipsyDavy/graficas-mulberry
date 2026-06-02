package org.gipsybuho.service;

import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

public class AuthService {

    public static final List<String> SECURITY_QUESTIONS = List.of(
        "¿Nombre de tu primera mascota?",
        "¿Ciudad donde naciste?",
        "¿Nombre de tu madre?",
        "¿Tu equipo de fútbol favorito?",
        "¿Nombre de tu mejor amigo de infancia?"
    );

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Optional<User> login(String username, String password) {
        return userDAO.findByUsername(username)
            .filter(u -> BCrypt.checkpw(password, u.getPasswordHash()))
            .map(u -> {
                userDAO.updateLastLogin(u.getId());
                return u;
            });
    }

    public boolean registerUser(String username, String password, UserRole role, String permissions) {
        if (userDAO.findByUsername(username).isPresent()) return false;
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(role);
        user.setPermissions(permissions);
        return userDAO.createUser(user);
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        return userDAO.findById(userId)
            .filter(u -> BCrypt.checkpw(oldPassword, u.getPasswordHash()))
            .map(u -> {
                u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                return userDAO.updateUser(u);
            }).orElse(false);
    }

    // Reset forzado por ADMINISTRADOR — no requiere contraseña actual. Solo llamar desde flujos autorizados de admin.
    public boolean resetPasswordAdmin(int userId, String newPassword) {
        return userDAO.findById(userId).map(u -> {
            u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            return userDAO.updateUser(u);
        }).orElse(false);
    }

    public boolean updateRoleAndPermissions(int userId, UserRole role, String permissions) {
        return userDAO.findById(userId).map(u -> {
            u.setRole(role);
            u.setPermissions(permissions);
            return userDAO.updateUser(u);
        }).orElse(false);
    }

    public boolean deleteUser(int userId) {
        return userDAO.deleteUser(userId);
    }

    public boolean hasAdmin() {
        return userDAO.hasAdmin();
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public boolean setSecurityQuestion(int userId, String question, String answer) {
        String hash = BCrypt.hashpw(answer.trim().toLowerCase(), BCrypt.gensalt());
        return userDAO.updateSecurityQuestion(userId, question, hash);
    }

    public boolean setSecurityQuestion(String username, String question, String answer) {
        return userDAO.findByUsername(username)
            .map(u -> setSecurityQuestion(u.getId(), question, answer))
            .orElse(false);
    }

    public Optional<String> getSecurityQuestion(String username) {
        return userDAO.findByUsername(username)
            .map(User::getSecurityQuestion)
            .filter(q -> q != null && !q.isBlank());
    }

    public boolean resetPasswordWithAnswer(String username, String answer, String newPassword) {
        return userDAO.findByUsername(username)
            .filter(u -> u.getSecurityAnswerHash() != null
                && BCrypt.checkpw(answer.trim().toLowerCase(), u.getSecurityAnswerHash()))
            .map(u -> {
                u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                return userDAO.updateUser(u);
            })
            .orElse(false);
    }
}
