package org.gipsybuho.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

public class LockScreenController {

    @FXML
    private Label lockedUserLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button unlockButton;
    @FXML
    private Label errorMessageLabel;

    private AuthService authService;
    private User lockedUser;
    private LockScreenCallback callback;

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setLockedUser(User user) {
        this.lockedUser = user;
        lockedUserLabel.setText("Usuario: " + user.getUsername());
    }

    public void setCallback(LockScreenCallback callback) {
        this.callback = callback;
    }

    @FXML
    private void handleUnlock() {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            errorMessageLabel.setText("Por favor, introduce tu contraseña.");
            return;
        }

        if (authService == null || lockedUser == null) {
            errorMessageLabel.setText("Error interno: Servicio de autenticación o usuario no inicializado.");
            System.err.println("AuthService o lockedUser no están inyectados en LockScreenController.");
            return;
        }

        // Usar el nuevo método de AuthService para verificar solo la contraseña del usuario bloqueado
        if (authService.checkPasswordForUser(lockedUser.getId(), password)) {
            errorMessageLabel.setText(""); // Limpiar cualquier mensaje de error
            if (callback != null) {
                callback.onUnlockSuccess();
            }
        } else {
            errorMessageLabel.setText("Contraseña incorrecta.");
            // Aquí se podría añadir lógica para limitar intentos de desbloqueo
        }
    }

    public interface LockScreenCallback {
        void onUnlockSuccess();
    }
}
