package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

import java.util.Optional;
import java.util.function.Consumer;

public class InitialAdminSetupView extends VBox {

    private final AuthService authService;
    private final Consumer<User> onAdminCreated;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    private final TextField visibleConfirmPasswordField = new TextField();
    private final Label messageLabel = new Label();

    public InitialAdminSetupView(AuthService authService, Consumer<User> onAdminCreated) {
        this.authService = authService;
        this.onAdminCreated = onAdminCreated;
        initializeUI();
    }

    private void initializeUI() {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 1;");

        Label title = new Label("Crear usuario inicial");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#333333;");

        Label info = new Label("Este usuario tendrá todos los roles y permisos, y será el único que podrá gestionar usuarios.");
        info.setWrapText(true);
        info.setMaxWidth(380);
        info.setStyle("-fx-text-fill:#555555;");

        usernameField.setPromptText("Usuario inicial");
        passwordField.setPromptText("Contraseña");
        confirmPasswordField.setPromptText("Confirmar contraseña");
        visiblePasswordField.setPromptText("Contraseña");
        visibleConfirmPasswordField.setPromptText("Confirmar contraseña");

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleConfirmPasswordField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        visibleConfirmPasswordField.setVisible(false);
        visibleConfirmPasswordField.setManaged(false);

        CheckBox showPassword = new CheckBox("Mostrar contraseña");
        showPassword.setStyle("-fx-text-fill:#555555;");
        showPassword.selectedProperty().addListener((obs, oldValue, show) -> {
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
            confirmPasswordField.setVisible(!show);
            confirmPasswordField.setManaged(!show);
            visiblePasswordField.setVisible(show);
            visiblePasswordField.setManaged(show);
            visibleConfirmPasswordField.setVisible(show);
            visibleConfirmPasswordField.setManaged(show);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.add(new Label("Usuario:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(visiblePasswordField, 1, 1);
        grid.add(new Label("Confirmar:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        grid.add(visibleConfirmPasswordField, 1, 2);
        grid.add(showPassword, 1, 3);

        Button createButton = new Button("Crear usuario inicial");
        createButton.setMaxWidth(220);
        createButton.setOnAction(e -> handleCreate());

        messageLabel.setStyle("-fx-text-fill:#c0392b;");

        getChildren().addAll(title, info, grid, createButton, messageLabel);
    }

    private void handleCreate() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Todos los campos son obligatorios.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Las contraseñas no coinciden.");
            return;
        }
        if (password.length() < 6) {
            messageLabel.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        Optional<User> createdUser = authService.createInitialAdmin(username, password);
        if (createdUser.isEmpty()) {
            Optional<User> existingInitialAdmin = authService.getInitialAdmin();
            if (existingInitialAdmin.isPresent()) {
                openApplication(existingInitialAdmin.get());
                return;
            }
            messageLabel.setText("No se pudo crear el usuario inicial. Revisa que el nombre no exista ya.");
            return;
        }

        openApplication(createdUser.get());
    }

    private void openApplication(User user) {
        messageLabel.setText("");
        if (onAdminCreated != null) {
            onAdminCreated.accept(user);
        }
    }
}
