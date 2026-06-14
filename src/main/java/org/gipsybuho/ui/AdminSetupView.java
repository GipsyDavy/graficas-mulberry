package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserRole;
import org.gipsybuho.service.AuthService;

import java.util.function.Consumer;

public class AdminSetupView extends VBox {

    private final AuthService authService;
    private final Consumer<User> onSetupComplete;
    private final TextField usernameField     = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField  = new PasswordField();
    private final ComboBox<String> questionCombo = new ComboBox<>();
    private final TextField answerField       = new TextField();
    private final Label msgLabel = new Label();

    public AdminSetupView(AuthService authService, Consumer<User> onSetupComplete) {
        this.authService = authService;
        this.onSetupComplete = onSetupComplete;
        build();
    }

    private void build() {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(48));

        Label title = new Label("Bienvenido a Gráficas Mulberry");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Crea el usuario administrador para comenzar.");
        subtitle.getStyleClass().add("view-subtitle");

        usernameField.setPromptText("Nombre de usuario");
        usernameField.setMaxWidth(260);
        passwordField.setPromptText("Contraseña (mín. " + AuthService.MIN_PASSWORD_LENGTH + " caracteres)");
        passwordField.setMaxWidth(260);
        confirmField.setPromptText("Confirmar contraseña");
        confirmField.setMaxWidth(260);
        confirmField.setOnAction(e -> handleCreate());

        CheckBox showPw = new CheckBox("Mostrar contraseña");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.add(new Label("Usuario:"),    0, 0);
        grid.add(usernameField,            1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(wrapPasswordField(passwordField, showPw), 1, 1);
        grid.add(new Label("Confirmar:"),  0, 2);
        grid.add(wrapPasswordField(confirmField,  showPw), 1, 2);

        questionCombo.setItems(FXCollections.observableArrayList(AuthService.SECURITY_QUESTIONS));
        questionCombo.getSelectionModel().selectFirst();
        questionCombo.setMaxWidth(Double.MAX_VALUE);
        answerField.setPromptText("Tu respuesta");
        answerField.setMaxWidth(260);

        grid.add(new Label("Pregunta seg.:"), 0, 3);
        grid.add(questionCombo, 1, 3);
        grid.add(new Label("Respuesta:"), 0, 4);
        grid.add(answerField, 1, 4);

        Button btn = new Button("Crear administrador");
        btn.setDefaultButton(true);
        btn.setPrefWidth(220);
        btn.setOnAction(e -> handleCreate());

        msgLabel.setStyle("-fx-text-fill: #c0392b;");

        getChildren().addAll(title, subtitle, grid, showPw, btn, msgLabel);
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

    private void handleCreate() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        String question = questionCombo.getValue();
        String answer   = answerField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            msgLabel.setText("Todos los campos son obligatorios.");
            TableColumnSizing.shake(username.isEmpty() ? usernameField : password.isEmpty() ? passwordField : confirmField);
            return;
        }
        if (!password.equals(confirm)) {
            msgLabel.setText("Las contraseñas no coinciden.");
            TableColumnSizing.shake(confirmField);
            return;
        }
        if (!AuthService.isPasswordValid(password)) {
            msgLabel.setText("La contraseña debe tener al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres.");
            TableColumnSizing.shake(passwordField);
            return;
        }
        if (question == null || answer.isEmpty()) {
            msgLabel.setText("Selecciona una pregunta de seguridad y escribe la respuesta.");
            TableColumnSizing.shake(answerField);
            return;
        }
        if (!authService.registerUser(username, password,
                UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString())) {
            msgLabel.setText("El nombre de usuario ya existe o no se pudo crear.");
            TableColumnSizing.shake(usernameField);
            return;
        }
        authService.login(username, password).ifPresent(user -> {
            authService.setSecurityQuestion(user.getId(), question, answer);
            onSetupComplete.accept(user);
        });
    }
}
