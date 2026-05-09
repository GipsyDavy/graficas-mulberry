package org.gipsybuho.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginScreen extends VBox {

    private final AuthService authService;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label messageLabel;
    private final Consumer<User> onLoginSuccess;

    public LoginScreen(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;

        // Configuración del layout principal
        setAlignment(Pos.CENTER);
        setSpacing(15);
        setPadding(new Insets(50));
        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5;");
        setMaxWidth(400);
        setMaxHeight(350);

        Label titleLabel = new Label("Iniciar Sesión");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#333333"));

        usernameField = new TextField();
        usernameField.setPromptText("Usuario");
        usernameField.setMaxWidth(250);
        usernameField.setPrefHeight(35);
        usernameField.setStyle("-fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 3;");

        passwordField = new PasswordField();
        passwordField.setPromptText("Contraseña");
        passwordField.setMaxWidth(250);
        passwordField.setPrefHeight(35);
        passwordField.setStyle("-fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 3;");

        Button loginButton = new Button("Entrar");
        loginButton.setPrefWidth(150);
        loginButton.setPrefHeight(40);
        loginButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5;");
        loginButton.setOnAction(e -> handleLogin());

        messageLabel = new Label("");
        messageLabel.setTextFill(Color.RED);
        messageLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        getChildren().addAll(titleLabel, usernameField, passwordField, loginButton, messageLabel);
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Por favor, introduce usuario y contraseña.");
            return;
        }

        try {
            User loggedInUser = authService.login(username, password);
            if (loggedInUser != null) {
                messageLabel.setTextFill(Color.GREEN);
                messageLabel.setText("Inicio de sesión exitoso.");
                onLoginSuccess.accept(loggedInUser); // Notificar al App.java
            } else {
                messageLabel.setTextFill(Color.RED);
                messageLabel.setText("Usuario o contraseña incorrectos.");
            }
        } catch (SQLException e) {
            messageLabel.setTextFill(Color.RED);
            messageLabel.setText("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
