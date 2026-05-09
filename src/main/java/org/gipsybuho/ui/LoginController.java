package org.gipsybuho.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.gipsybuho.dao.UserDAO; // Importar UserDAO
import org.gipsybuho.db.DatabaseManager; // Importar DatabaseManager para getConfig
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // Importar ChronoUnit
import java.util.Optional;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Label errorMessageLabel;

    private AuthService authService;
    private UserDAO userDAO; // Añadir UserDAO para verificar el estado de bloqueo
    private LoginCallback loginCallback;

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setUserDAO(UserDAO userDAO) { // Setter para UserDAO
        this.userDAO = userDAO;
    }

    public void setLoginCallback(LoginCallback loginCallback) {
        this.loginCallback = loginCallback;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorMessageLabel.setText("Por favor, introduce usuario y contraseña.");
            return;
        }

        if (authService == null || userDAO == null) { // Verificar UserDAO también
            errorMessageLabel.setText("Error interno: Servicio de autenticación no inicializado.");
            System.err.println("AuthService o UserDAO no están inyectados en LoginController.");
            return;
        }

        Optional<User> loggedInUser = authService.login(username, password);

        if (loggedInUser.isPresent()) {
            errorMessageLabel.setText(""); // Limpiar cualquier mensaje de error
            if (loginCallback != null) {
                loginCallback.onLoginSuccess(loggedInUser.get());
            }
        } else {
            // El login falló, verificar la razón específica
            Optional<User> userInDb = userDAO.findByUsername(username);
            if (userInDb.isPresent()) {
                User user = userInDb.get();
                if (authService.isAccountLocked(user)) {
                    int lockoutMinutes = Integer.parseInt(DatabaseManager.getConfig("login_lockout_minutes"));
                    LocalDateTime unlockTime = user.getLastFailedLogin().plusMinutes(lockoutMinutes);
                    long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), unlockTime);
                    errorMessageLabel.setText(String.format("Cuenta bloqueada. Inténtelo de nuevo en %d minuto(s).", minutesRemaining + 1));
                } else {
                    errorMessageLabel.setText("Usuario o contraseña incorrectos.");
                }
            } else {
                errorMessageLabel.setText("Usuario o contraseña incorrectos.");
            }
        }
    }

    // Interfaz para comunicar el éxito del login
    public interface LoginCallback {
        void onLoginSuccess(User user);
    }
}
