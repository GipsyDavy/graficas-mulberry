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

import static org.gipsybuho.service.LanguageManager.t;
import static org.gipsybuho.service.LanguageManager.tf;

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

        Label title = new Label(t("admin.titulo"));
        title.getStyleClass().add("view-title");

        Label subtitle = new Label(t("admin.subtitulo"));
        subtitle.getStyleClass().add("view-subtitle");

        usernameField.setPromptText(t("admin.prompt.usuario"));
        usernameField.setMaxWidth(260);
        passwordField.setPromptText(tf("login.prompt.contrasena_min", AuthService.MIN_PASSWORD_LENGTH));
        passwordField.setMaxWidth(260);
        confirmField.setPromptText(t("common.prompt.confirmar_contrasena"));
        confirmField.setMaxWidth(260);
        confirmField.setOnAction(e -> handleCreate());

        CheckBox showPw = new CheckBox(t("common.chk.mostrar_contrasena"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.add(new Label(t("common.label.usuario")),    0, 0);
        grid.add(usernameField,                            1, 0);
        grid.add(new Label(t("common.label.contrasena")), 0, 1);
        grid.add(wrapPasswordField(passwordField, showPw), 1, 1);
        grid.add(new Label(t("common.label.confirmar")),  0, 2);
        grid.add(wrapPasswordField(confirmField,  showPw), 1, 2);

        questionCombo.setItems(FXCollections.observableArrayList(AuthService.SECURITY_QUESTIONS));
        questionCombo.getSelectionModel().selectFirst();
        questionCombo.setMaxWidth(Double.MAX_VALUE);
        answerField.setPromptText(t("common.prompt.respuesta"));
        answerField.setMaxWidth(260);

        grid.add(new Label(t("common.label.pregunta_seg")), 0, 3);
        grid.add(questionCombo, 1, 3);
        grid.add(new Label(t("common.label.respuesta")), 0, 4);
        grid.add(answerField, 1, 4);

        Button btn = new Button(t("admin.btn.crear"));
        btn.setDefaultButton(true);
        btn.setPrefWidth(220);
        btn.setOnAction(e -> handleCreate());

        msgLabel.setStyle("-fx-text-fill: #c0392b;");

        getChildren().addAll(title, subtitle, grid, showPw, btn, msgLabel);
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

    private void handleCreate() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        String question = questionCombo.getValue();
        String answer   = answerField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            msgLabel.setText(t("admin.error.campos_obligatorios"));
            TableColumnSizing.shake(username.isEmpty() ? usernameField : password.isEmpty() ? passwordField : confirmField);
            return;
        }
        if (!password.equals(confirm)) {
            msgLabel.setText(t("common.error.contrasenas_no_coinciden"));
            TableColumnSizing.shake(confirmField);
            return;
        }
        if (!AuthService.isPasswordValid(password)) {
            msgLabel.setText(tf("common.error.contrasena_min", AuthService.MIN_PASSWORD_LENGTH));
            TableColumnSizing.shake(passwordField);
            return;
        }
        if (question == null || answer.isEmpty()) {
            msgLabel.setText(t("admin.error.pregunta_respuesta"));
            TableColumnSizing.shake(answerField);
            return;
        }
        if (!authService.registerUser(username, password,
                UserRole.ADMINISTRADOR, UserRole.ADMINISTRADOR.getPermissionsString())) {
            msgLabel.setText(t("admin.error.usuario_existe"));
            TableColumnSizing.shake(usernameField);
            return;
        }
        authService.login(username, password).ifPresent(user -> {
            authService.setSecurityQuestion(user.getId(), question, answer);
            onSetupComplete.accept(user);
        });
    }
}
