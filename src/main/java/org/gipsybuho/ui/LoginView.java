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

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

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

        Label title = new Label(t("login.titulo"));
        title.getStyleClass().add("view-title");

        userCombo.setPromptText(t("login.prompt.usuario"));
        userCombo.setMaxWidth(260);
        userCombo.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u == null ? "" : u.getUsername(); }
            @Override public User fromString(String s) { return null; }
        });

        passwordField.setPromptText(t("login.prompt.contrasena"));
        passwordField.setMaxWidth(260);
        passwordField.setOnAction(e -> handleLogin());

        CheckBox showPw = new CheckBox(t("common.chk.mostrar_contrasena"));

        Label forgotLabel = new Label(t("login.enlace.olvidaste"));
        forgotLabel.setStyle("-fx-cursor: hand; -fx-text-fill: #3498db; -fx-font-size: 11px;");
        forgotLabel.setOnMouseClicked(e -> showRecoveryDialog());

        Button btn = new Button(t("login.btn.entrar"));
        btn.setDefaultButton(true);
        btn.setPrefWidth(260);
        btn.setOnAction(e -> handleLogin());

        msgLabel.setStyle("-fx-text-fill: #c0392b;");

        getChildren().addAll(title, userCombo, wrapPasswordField(passwordField, showPw), showPw,
                             forgotLabel, btn, msgLabel);
    }

    private void showRecoveryDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(t("login.recovery.titulo"));
        dialog.setHeaderText(t("login.recovery.header"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<User> userComboRec = new ComboBox<>();
        userComboRec.setMaxWidth(Double.MAX_VALUE);
        userComboRec.setConverter(new StringConverter<>() {
            @Override public String toString(User u) { return u == null ? "" : u.getUsername(); }
            @Override public User fromString(String s) { return null; }
        });
        userComboRec.setItems(FXCollections.observableArrayList(authService.getAllUsers()));

        Label questionLabel = new Label(t("login.recovery.selecciona_usuario"));
        questionLabel.setStyle("-fx-font-style: italic;");
        questionLabel.setWrapText(true);

        TextField answerField = new TextField();
        answerField.setPromptText(t("common.prompt.respuesta"));

        PasswordField newPwField     = new PasswordField();
        PasswordField confirmPwField = new PasswordField();
        newPwField.setPromptText(tf("login.recovery.prompt.nueva_contrasena", AuthService.MIN_PASSWORD_LENGTH));
        confirmPwField.setPromptText(t("common.prompt.confirmar_contrasena"));

        CheckBox showPwRec = new CheckBox(t("common.chk.mostrar_contrasena"));

        Label msgRec = new Label();
        msgRec.setStyle("-fx-text-fill: #c0392b;");
        msgRec.setWrapText(true);

        userComboRec.valueProperty().addListener((obs, old, user) -> {
            if (user == null) {
                questionLabel.setText(t("login.recovery.selecciona_usuario"));
            } else {
                authService.getSecurityQuestion(user.getUsername()).ifPresentOrElse(
                    q -> { questionLabel.setText(q); questionLabel.setStyle("-fx-font-style: italic;"); },
                    () -> questionLabel.setText(t("login.recovery.sin_pregunta_aviso"))
                );
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setMinWidth(380);
        grid.add(new Label(t("common.label.usuario")),                  0, 0);
        grid.add(userComboRec,                                           1, 0);
        grid.add(new Label(t("common.label.pregunta")),                  0, 1);
        grid.add(questionLabel,                                          1, 1);
        grid.add(new Label(t("common.label.respuesta")),                 0, 2);
        grid.add(answerField,                                            1, 2);
        grid.add(new Label(t("login.recovery.label.nueva_contrasena")),  0, 3);
        grid.add(wrapPasswordField(newPwField, showPwRec),               1, 3);
        grid.add(new Label(t("common.label.confirmar")),                 0, 4);
        grid.add(wrapPasswordField(confirmPwField, showPwRec),           1, 4);
        grid.add(showPwRec,                      1, 5);

        dialog.getDialogPane().setContent(new VBox(10, grid, msgRec));

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            User selected = userComboRec.getValue();
            String answer = answerField.getText().trim();
            String newPw  = newPwField.getText();
            String confirm = confirmPwField.getText();

            if (selected == null) {
                msgRec.setText(t("login.recovery.error.selecciona_usuario")); event.consume(); return;
            }
            if (authService.getSecurityQuestion(selected.getUsername()).isEmpty()) {
                msgRec.setText(t("login.recovery.error.sin_pregunta")); event.consume(); return;
            }
            if (answer.isEmpty()) {
                msgRec.setText(t("login.recovery.error.introduce_respuesta")); TableColumnSizing.shake(answerField); event.consume(); return;
            }
            if (newPw.isEmpty() || confirm.isEmpty()) {
                msgRec.setText(t("login.recovery.error.introduce_confirma")); TableColumnSizing.shake(newPw.isEmpty() ? newPwField : confirmPwField); event.consume(); return;
            }
            if (!newPw.equals(confirm)) {
                msgRec.setText(t("common.error.contrasenas_no_coinciden")); TableColumnSizing.shake(confirmPwField); event.consume(); return;
            }
            if (!AuthService.isPasswordValid(newPw)) {
                msgRec.setText(tf("common.error.contrasena_min", AuthService.MIN_PASSWORD_LENGTH)); TableColumnSizing.shake(newPwField); event.consume(); return;
            }
            if (!authService.resetPasswordWithAnswer(selected.getUsername(), answer, newPw)) {
                if (authService.isRecoveryTemporarilyBlocked(selected.getUsername())) {
                    msgRec.setText(tf("common.error.demasiados_intentos",
                        formatearEspera(authService.getRecoveryLockoutSecondsRemaining(selected.getUsername()))));
                } else {
                    msgRec.setText(t("login.recovery.error.respuesta_incorrecta")); TableColumnSizing.shake(answerField);
                }
                event.consume();
            }
        });

        dialog.setResultConverter(bt -> bt == ButtonType.OK ? Boolean.TRUE : null);
        dialog.showAndWait()
            .filter(Boolean.TRUE::equals)
            .ifPresent(v -> {
                msgLabel.setStyle("-fx-text-fill: #27ae60;");
                msgLabel.setText(t("login.exito.contrasena_restablecida"));
            });
    }

    private static StackPane wrapPasswordField(PasswordField pf, CheckBox showToggle) {
        TextField tfVisible = new TextField();
        tfVisible.setPromptText(pf.getPromptText());
        tfVisible.setMaxWidth(pf.getMaxWidth());
        tfVisible.setOnAction(pf.getOnAction());
        tfVisible.disableProperty().bind(pf.disableProperty());
        tfVisible.setVisible(false);
        tfVisible.setManaged(false);
        tfVisible.textProperty().bindBidirectional(pf.textProperty());
        tfVisible.translateXProperty().bind(pf.translateXProperty());
        showToggle.selectedProperty().addListener((obs, old, show) -> {
            pf.setVisible(!show);
            pf.setManaged(!show);
            tfVisible.setVisible(show);
            tfVisible.setManaged(show);
        });
        StackPane stack = new StackPane(pf, tfVisible);
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
            msgLabel.setText(t("login.error.campos_vacios"));
            TableColumnSizing.shake(passwordField);
            return;
        }
        authService.login(selected.getUsername(), password).ifPresentOrElse(
            onLoginSuccess,
            () -> {
                if (authService.isLoginTemporarilyBlocked(selected.getUsername())) {
                    msgLabel.setText(tf("common.error.demasiados_intentos",
                        formatearEspera(authService.getLoginLockoutSecondsRemaining(selected.getUsername()))));
                } else {
                    msgLabel.setText(t("login.error.contrasena_incorrecta"));
                }
                TableColumnSizing.shake(passwordField);
                passwordField.clear();
                passwordField.requestFocus();
            }
        );
    }

    private static String formatearEspera(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);
        return minutes == 1 ? t("common.tiempo.minuto") : tf("common.tiempo.minutos", minutes);
    }
}
