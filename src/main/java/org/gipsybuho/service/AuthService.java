package org.gipsybuho.service;

import org.gipsybuho.dao.LogAccessDAO;
import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.db.DatabaseManager; // Importar DatabaseManager para getConfig
import org.gipsybuho.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;
    private final LogAccessDAO logAccessDAO;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int DEFAULT_MAX_LOGIN_ATTEMPTS = 5;
    private static final int DEFAULT_LOGIN_LOCKOUT_MINUTES = 5;

    public AuthService(UserDAO userDAO, LogAccessDAO logAccessDAO) {
        this.userDAO = userDAO;
        this.logAccessDAO = logAccessDAO;
    }

    /**
     * Intenta autenticar un usuario.
     * @param username El nombre de usuario.
     * @param password La contraseña en texto plano.
     * @return Un Optional que contiene el objeto User si la autenticación es exitosa, o vacío si falla.
     */
    public Optional<User> login(String username, String password) {
        Optional<User> userOptional = userDAO.findByUsername(username);

        if (userOptional.isEmpty()) {
            // Usuario no encontrado, registrar intento fallido sin user_id
            logAccessDAO.logAccess(null, username, "login_fail_user_not_found");
            return Optional.empty();
        }

        User user = userOptional.get();

        // Verificar si la cuenta está bloqueada
        if (isAccountLocked(user)) {
            logAccessDAO.logAccess(user.getId(), user.getUsername(), "login_fail_locked");
            return Optional.empty(); // Cuenta bloqueada
        }

        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            // Autenticación exitosa
            userDAO.updateLastLogin(user.getId()); // Actualizar last_login y resetear intentos fallidos
            user.setLastLogin(LocalDateTime.now()); // Actualizar el objeto User en memoria
            user.setFailedLoginAttempts(0); // Resetear en memoria
            user.setLastFailedLogin(null); // Resetear en memoria
            logAccessDAO.logAccess(user.getId(), user.getUsername(), "login_success");
            return Optional.of(user);
        } else {
            // Contraseña incorrecta
            int currentAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(currentAttempts);
            user.setLastFailedLogin(LocalDateTime.now());
            userDAO.updateFailedLoginAttempts(user.getId(), currentAttempts, user.getLastFailedLogin());
            logAccessDAO.logAccess(user.getId(), user.getUsername(), "login_fail_password");
            return Optional.empty();
        }
    }

    /**
     * Verifica si una cuenta de usuario está actualmente bloqueada debido a demasiados intentos fallidos.
     * @param user El objeto User a verificar.
     * @return true si la cuenta está bloqueada, false en caso contrario.
     */
    public boolean isAccountLocked(User user) {
        int maxAttempts = DEFAULT_MAX_LOGIN_ATTEMPTS;
        int lockoutMinutes = DEFAULT_LOGIN_LOCKOUT_MINUTES;

        try {
            String maxAttemptsStr = DatabaseManager.getConfig("max_login_attempts");
            if (!maxAttemptsStr.isBlank()) {
                maxAttempts = Integer.parseInt(maxAttemptsStr);
            }
            String lockoutMinutesStr = DatabaseManager.getConfig("login_lockout_minutes");
            if (!lockoutMinutesStr.isBlank()) {
                lockoutMinutes = Integer.parseInt(lockoutMinutesStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error al leer configuración de bloqueo de login. Usando valores por defecto.");
        }

        if (user.getFailedLoginAttempts() >= maxAttempts) {
            if (user.getLastFailedLogin() != null) {
                LocalDateTime unlockTime = user.getLastFailedLogin().plusMinutes(lockoutMinutes);
                return LocalDateTime.now().isBefore(unlockTime);
            }
            return true; // Si no hay last_failed_login pero los intentos exceden, se considera bloqueado
        }
        return false;
    }

    /**
     * Verifica la contraseña para un usuario específico por su ID.
     * Útil para la pantalla de bloqueo donde el usuario ya está identificado.
     * @param userId El ID del usuario.
     * @param password La contraseña en texto plano a verificar.
     * @return true si la contraseña es correcta, false en caso contrario.
     */
    public boolean checkPasswordForUser(int userId, String password) {
        Optional<User> userOptional = userDAO.findById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // No aplicamos bloqueo de cuenta aquí, ya que es para desbloquear una sesión activa.
            return BCrypt.checkpw(password, user.getPasswordHash());
        }
        return false;
    }

    /**
     * Registra un nuevo usuario.
     * @param username El nombre de usuario.
     * @param password La contraseña en texto plano.
     * @return true si el usuario fue registrado exitosamente, false si el nombre de usuario ya existe.
     */
    public boolean registerUser(String username, String password) {
        if (userDAO.findByUsername(username).isPresent()) {
            return false; // El nombre de usuario ya existe
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, hashedPassword);
        userDAO.createUser(newUser);
        logAccessDAO.logAccess(newUser.getId(), newUser.getUsername(), "user_registered");
        return true;
    }

    /**
     * Cambia la contraseña de un usuario.
     * @param userId El ID del usuario.
     * @param newPassword La nueva contraseña en texto plano.
     * @return true si la contraseña fue cambiada exitosamente, false en caso contrario.
     */
    public boolean changePassword(int userId, String newPassword) {
        Optional<User> userOptional = userDAO.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPasswordHash(newHashedPassword);
            userDAO.updateUser(user);
            logAccessDAO.logAccess(user.getId(), user.getUsername(), "password_changed");
            return true;
        }
        return false;
    }

    /**
     * Elimina un usuario.
     * @param userId El ID del usuario a eliminar.
     * @return true si el usuario fue eliminado, false en caso contrario.
     */
    public boolean deleteUser(int userId) {
        Optional<User> userOptional = userDAO.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userDAO.deleteUser(userId);
            logAccessDAO.logAccess(user.getId(), user.getUsername(), "user_deleted");
            return true;
        }
        return false;
    }

    /**
     * Obtiene todos los usuarios registrados.
     * @return Una lista de objetos User.
     */
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    /**
     * Obtiene la información del último acceso exitoso para un usuario.
     * @param username El nombre de usuario.
     * @return Una cadena formateada con la fecha y hora del último acceso, o un mensaje indicando que no hay registros.
     */
    public String getLastLoginInfo(String username) {
        Optional<String> lastLoginTimestamp = logAccessDAO.getLastSuccessfulLogin(username);
        if (lastLoginTimestamp.isPresent()) {
            LocalDateTime dateTime = LocalDateTime.parse(lastLoginTimestamp.get());
            return "Último acceso: " + dateTime.format(DATE_TIME_FORMATTER) + " — " + username;
        } else {
            return "Último acceso: No hay registros de acceso.";
        }
    }

    /**
     * Registra un evento de acceso en el log.
     * @param userId El ID del usuario (puede ser null si el usuario no se encontró).
     * @param username El nombre de usuario.
     * @param eventType El tipo de evento (ej. "logout").
     */
    public void logAccess(Integer userId, String username, String eventType) {
        logAccessDAO.logAccess(userId, username, eventType);
    }
}
