package org.gipsybuho.service;

import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);

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

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    public Optional<User> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();

        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        if (userDAO.isLoginLocked(user.getId())) return Optional.empty();

        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            userDAO.clearLoginLockout(user.getId());
            userDAO.updateLastLogin(user.getId());
            return Optional.of(user);
        } else {
            userDAO.recordLoginFailure(user.getId(), MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION);
            return Optional.empty();
        }
    }

    public boolean isLoginTemporarilyBlocked(String username) {
        if (username == null) return false;
        return userDAO.findByUsername(username)
            .map(u -> userDAO.isLoginLocked(u.getId()))
            .orElse(false);
    }

    public long getLoginLockoutSecondsRemaining(String username) {
        if (username == null) return 0;
        return userDAO.findByUsername(username)
            .map(u -> userDAO.getLoginSecondsRemaining(u.getId()))
            .orElse(0L);
    }

    public boolean isRecoveryTemporarilyBlocked(String username) {
        if (username == null) return false;
        return userDAO.findByUsername(username)
            .map(u -> userDAO.isRecoveryLocked(u.getId()))
            .orElse(false);
    }

    public long getRecoveryLockoutSecondsRemaining(String username) {
        if (username == null) return 0;
        return userDAO.findByUsername(username)
            .map(u -> userDAO.getRecoverySecondsRemaining(u.getId()))
            .orElse(0L);
    }

    public boolean registerUser(String username, String password, UserRole role, String permissions) {
        if (!isPasswordValid(password)) return false;
        if (userDAO.findByUsername(username).isPresent()) return false;
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRole(role);
        user.setPermissions(permissions);
        return userDAO.createUser(user);
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (!isPasswordValid(newPassword)) return false;
        return userDAO.findById(userId)
            .filter(u -> BCrypt.checkpw(oldPassword, u.getPasswordHash()))
            .map(u -> {
                u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                return userDAO.updateUser(u);
            }).orElse(false);
    }

    // Reset forzado por ADMINISTRADOR — no requiere contraseña actual. Solo llamar desde flujos autorizados de admin.
    public boolean resetPasswordAdmin(int userId, String newPassword) {
        if (!isPasswordValid(newPassword)) return false;
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
        if (!isPasswordValid(newPassword)) return false;
        if (username == null || answer == null) return false;

        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();
        if (userDAO.isRecoveryLocked(user.getId())) return false;

        if (user.getSecurityAnswerHash() != null
                && BCrypt.checkpw(answer.trim().toLowerCase(), user.getSecurityAnswerHash())) {
            userDAO.clearRecoveryLockout(user.getId());
            user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            return userDAO.updateUser(user);
        } else {
            userDAO.recordRecoveryFailure(user.getId(), MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION);
            return false;
        }
    }
}
