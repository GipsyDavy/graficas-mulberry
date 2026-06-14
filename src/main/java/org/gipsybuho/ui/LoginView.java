package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

import java.util.List;
import java.util.function.Consumer;

public class LoginView extends VBox {

    private final AuthService authService;
    private final Consumer<User> onLoginSuccess;
    private final ComboBox<User> userCombo   = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final Label msgLabel = new Label();

    public LoginView(AuthService authService, Consumer<User> onLoginSuccess) {
        this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
        build();
        loadUsers();
    }

    private void build() {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(50));

        Label title = new Label("Iniciar sesión");
        title.getStyleClass().add("view-title");

        userCombo.setPromptText("Selecciona usuario");
        userCombo.setMaxWidth(260);
        userCombo.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u == null ? "" : u.getUsername(); }
            @Override public User fromString(String s) { return null; }
        });

        passwordField.setPromptText("Contraseña");
        passwordField.setMaxWidth(260);
        passwordField.setOnAction(e -> handleLogin());

        CheckBox showPw = new CheckBox("Mostrar contraseña");

        Label forgotLabel = new Label("¿Olvidaste tu contraseña?");
        forgotLabel.setStyle("-fx-cursor: hand; -fx-text-fill: #3498db; -fx-font-size: 11px;");
        forgotLabel.setOnMouseClicked(e -> showRecoveryDialog());

        Button btn = new Button("Entrar");
        btn.setDefaultButton(true);
        btn.setPrefWidth(260);
        btn.setOnAction(e -> handleLogin());

        msgLabel.setStyle("-fx-text-fill: #c0392b;");

        getChildren().addAll(title, userCombo, wrapPasswordField(passwordField, showPw), showPw,
                             forgotLabel, btn, msgLabel);
    }

    private void showRecoveryDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Recuperar contraseña");
        dialog.setHeaderText("Responde tu pregunta de seguridad para restablecer la contraseña");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<User> userComboRec = new ComboBox<>();
        userComboRec.setMaxWidth(Double.MAX_VALUE);
        userComboRec.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u == null ? "" : u.getUsername(); }
            @Override public User fromString(String s) { return null; }
        });
        userComboRec.setItems(FXCollections.observableArrayList(authService.getAllUsers()));

        Label questionLabel = new Label("— Selecciona un usuario —");
        questionLabel.setStyle("-fx-font-style: italic;");
        questionLabel.setWrapText(true);

        TextField answerField = new TextField();
        answerField.setPromptText("Tu respuesta");

        PasswordField newPwField     = new PasswordField();
        PasswordField confirmPwField = new PasswordField();
        newPwField.setPromptText("Nueva contraseña (mín. " + AuthService.MIN_PASSWORD_LENGTH + " caracteres)");
        confirmPwField.setPromptText("Confirmar contraseña");

        CheckBox showPwRec = new CheckBox("Mostrar contraseña");

        Label msgRec = new Label();
        msgRec.setStyle("-fx-text-fill: #c0392b;");
        msgRec.setWrapText(true);

        userComboRec.valueProperty().addListener((obs, old, user) -> {
            if (user == null) {
                questionLabel.setText("— Selecciona un usuario —");
            } else {
                authService.getSecurityQuestion(user.getUsername()).ifPresentOrElse(
                    q -> { questionLabel.setText(q); questionLabel.setStyle("-fx-font-style: italic;"); },
                    () -> questionLabel.setText("⚠ Sin pregunta configurada. Contacta con el administrador.")
                );
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setMinWidth(380);
        grid.add(new Label("Usuario:"),          0, 0);
        grid.add(userComboRec,                   1, 0);
        grid.add(new Label("Pregunta:"),         0, 1);
        grid.add(questionLabel,                  1, 1);
        grid.add(new Label("Respuesta:"),        0, 2);
        grid.add(answerField,                    1, 2);
        grid.add(new Label("Nueva contraseña:"), 0, 3);
        grid.add(wrapPasswordField(newPwField, showPwRec),     1, 3);
        grid.add(new Label("Confirmar:"),        0, 4);
        grid.add(wrapPasswordField(confirmPwField, showPwRec), 1, 4);
        grid.add(showPwRec,                      1, 5);

        dialog.getDialogPane().setContent(new VBox(10, grid, msgRec));

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            User selected = userComboRec.getValue();
            String answer = answerField.getText().trim();
            String newPw  = newPwField.getText();
            String confirm = confirmPwField.getText();

            if (selected == null) {
                msgRec.setText("Selecciona un usuario."); event.consume(); return;
            }
            if (authService.getSecurityQuestion(selected.getUsername()).isEmpty()) {
                msgRec.setText("Sin pregunta configurada. Contacta con el administrador."); event.consume(); return;
            }
            if (answer.isEmpty()) {
                msgRec.setText("Introduce la respuesta."); TableColumnSizing.shake(answerField); event.consume(); return;
            }
            if (newPw.isEmpty() || confirm.isEmpty()) {
                msgRec.setText("Introduce y confirma la nueva contraseña."); TableColumnSizing.shake(newPw.isEmpty() ? newPwField : confirmPwField); event.consume(); return;
            }
            if (!newPw.equals(confirm)) {
                msgRec.setText("Las contraseñas no coinciden."); TableColumnSizing.shake(confirmPwField); event.consume(); return;
            }
            if (!AuthService.isPasswordValid(newPw)) {
                msgRec.setText("La contraseña debe tener al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres."); TableColumnSizing.shake(newPwField); event.consume(); return;
            }
            if (!authService.resetPasswordWithAnswer(selected.getUsername(), answer, newPw)) {
                if (authService.isRecoveryTemporarilyBlocked(selected.getUsername())) {
                    msgRec.setText("Demasiados intentos. Espera " +
                        formatearEspera(authService.getRecoveryLockoutSecondsRemaining(selected.getUsername())) + ".");
                } else {
                    msgRec.setText("Respuesta incorrecta. Inténtalo de nuevo."); TableColumnSizing.shake(answerField);
                }
                event.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? Boolean.TRUE : null);
        dialog.showAndWait()
            .filter(Boolean.TRUE::equals)
            .ifPresent(v -> {
                msgLabel.setStyle("-fx-text-fill: #27ae60;");
                msgLabel.setText("Contraseña restablecida. Ya puedes iniciar sesión.");
            });
    }

    private static StackPane wrapPasswordField(PasswordField pf, CheckBox showToggle) {
        TextField tf = new TextField();
        tf.setPromptText(pf.getPromptText());
        tf.setMaxWidth(pf.getMaxWidth());
        tf.setOnAction(pf.getOnAction());
        tf.disableProperty().bind(pf.disableProperty());
        tf.setVisible(false);
        tf.setManaged(false);
        tf.textProperty().bindBidirectional(pf.textProperty());
        tf.translateXProperty().bind(pf.translateXProperty());
        showToggle.selectedProperty().addListener((obs, old, show) -> {
            pf.setVisible(!show);
            pf.setManaged(!show);
            tf.setVisible(show);
            tf.setManaged(show);
        });
        StackPane stack = new StackPane(pf, tf);
        stack.setMaxWidth(pf.getMaxWidth());
        return stack;
    }

    private void loadUsers() {
        List<User> users = authService.getAllUsers();
        userCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) userCombo.getSelectionModel().selectFirst();
    }

    private void handleLogin() {
        User selected = userCombo.getValue();
        String password = passwordField.getText();

        if (selected == null || password.isEmpty()) {
            msgLabel.setText("Selecciona usuario e introduce contraseña.");
            TableColumnSizing.shake(passwordField);
            return;
        }
        authService.login(selected.getUsername(), password).ifPresentOrElse(
            onLoginSuccess,
            () -> {
                if (authService.isLoginTemporarilyBlocked(selected.getUsername())) {
                    msgLabel.setText("Demasiados intentos. Espera " +
                        formatearEspera(authService.getLoginLockoutSecondsRemaining(selected.getUsername())) + ".");
                } else {
                    msgLabel.setText("Contraseña incorrecta.");
                }
                TableColumnSizing.shake(passwordField);
                passwordField.clear();
                passwordField.requestFocus();
            }
        );
    }

    private static String formatearEspera(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);
        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }
}
