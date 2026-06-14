package org.gipsybuho.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.gipsybuho.model.User;
import org.gipsybuho.model.UserPermissions;
import org.gipsybuho.model.UserRole;
import org.gipsybuho.service.AuthService;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserManagementView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthService authService;
    private final User currentUser;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final TableView<User> userTable = new TableView<>(users);
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final ComboBox<UserRole> roleCombo = new ComboBox<>();
    private final Map<String, CheckBox> permissionChecks = new LinkedHashMap<>();
    private final Button saveButton = new Button("Crear usuario");
    private final CheckBox showPw = new CheckBox("Mostrar contraseña");
    private final ComboBox<String> questionCombo = new ComboBox<>();
    private final TextField answerField = new TextField();
    private final Label msgLabel = new Label();

    private User editingUser;

    public UserManagementView(AuthService authService, User currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;

        getStyleClass().add("content-view");
        setPadding(new Insets(24));
        setSpacing(12);

        Label title = new Label("Gestión de usuarios");
        title.getStyleClass().add("view-title");

        getChildren().addAll(title, buildTable(), buildActionBar(), buildForm(), msgLabel);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        loadUsers();
        resetForm();
    }

    private TableView<User> buildTable() {
        userTable.getStyleClass().add("data-table");
        userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        userTable.setPlaceholder(new Label("No hay usuarios registrados"));

        TableColumn<User, String> usernameCol = new TableColumn<>("Usuario");
        usernameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        usernameCol.setPrefWidth(220);

        TableColumn<User, String> roleCol = new TableColumn<>("Rol");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().getLabel()));
        roleCol.setPrefWidth(180);

        TableColumn<User, String> createdCol = new TableColumn<>("Fecha creación");
        createdCol.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue())));
        createdCol.setPrefWidth(160);

        TableColumn<User, String> lastLoginCol = new TableColumn<>("Última sesión");
        lastLoginCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getLastLogin() == null ? "—" : data.getValue().getLastLogin().format(DATE_FORMAT)));
        lastLoginCol.setPrefWidth(160);

        userTable.getColumns().addAll(usernameCol, roleCol, createdCol, lastLoginCol);
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, selected) -> {
            if (selected != null) fillForm(selected);
        });

        return userTable;
    }

    private HBox buildActionBar() {
        Button changePasswordButton = new Button("Cambiar contraseña");
        changePasswordButton.getStyleClass().add("btn-toolbar");
        changePasswordButton.setOnAction(e -> changeSelectedPassword());

        Button editButton = new Button("Editar rol y permisos");
        editButton.getStyleClass().add("btn-toolbar");
        editButton.setOnAction(e -> startEditSelectedUser());

        Button deleteButton = new Button("Eliminar usuario");
        deleteButton.getStyleClass().add("btn-toolbar");
        deleteButton.setOnAction(e -> deleteSelectedUser());

        Button newButton = new Button("Nuevo usuario");
        newButton.getStyleClass().addAll("btn-toolbar", "btn-toolbar-active");
        newButton.setOnAction(e -> resetForm());

        HBox bar = new HBox(8, changePasswordButton, editButton, deleteButton, newButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("command-bar");
        return bar;
    }

    private VBox buildForm() {
        Label formTitle = new Label("Crear / editar usuario");

        usernameField.setPromptText("Nombre de usuario");
        passwordField.setPromptText("Contraseña");
        confirmField.setPromptText("Confirmar contraseña");

        roleCombo.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleCombo.valueProperty().addListener((obs, oldRole, newRole) -> {
            if (newRole != null) applyPermissions(newRole.getDefaultPermissions());
        });

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Usuario:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Contraseña:"), 0, 1);
        grid.add(wrapPasswordField(passwordField, showPw), 1, 1);
        grid.add(new Label("Confirmar:"), 0, 2);
        grid.add(wrapPasswordField(confirmField,  showPw), 1, 2);
        grid.add(new Label("Rol:"), 0, 3);
        grid.add(roleCombo, 1, 3);

        showPw.visibleProperty().bind(passwordField.disabledProperty().not());
        showPw.managedProperty().bind(passwordField.disabledProperty().not());

        questionCombo.setItems(FXCollections.observableArrayList(AuthService.SECURITY_QUESTIONS));
        questionCombo.getSelectionModel().selectFirst();
        questionCombo.setMaxWidth(Double.MAX_VALUE);
        answerField.setPromptText("Respuesta de seguridad");

        GridPane secGrid = new GridPane();
        secGrid.setHgap(12);
        secGrid.setVgap(10);
        secGrid.add(new Label("Pregunta seg.:"), 0, 0);
        secGrid.add(questionCombo, 1, 0);
        secGrid.add(new Label("Respuesta:"), 0, 1);
        secGrid.add(answerField, 1, 1);

        VBox securitySection = new VBox(0, secGrid);
        securitySection.visibleProperty().bind(passwordField.disabledProperty().not());
        securitySection.managedProperty().bind(passwordField.disabledProperty().not());

        VBox checksBox = new VBox(6);
        UserPermissions.AVAILABLE.forEach((key, label) -> {
            CheckBox checkBox = new CheckBox(label);
            permissionChecks.put(key, checkBox);
            checksBox.getChildren().add(checkBox);
        });

        ScrollPane permissionsPane = new ScrollPane(checksBox);
        permissionsPane.setFitToWidth(true);
        permissionsPane.setPrefViewportHeight(170);

        saveButton.setDefaultButton(true);
        saveButton.setOnAction(e -> saveForm());

        VBox form = new VBox(10, formTitle, grid, showPw, securitySection, permissionsPane, saveButton);
        form.setPadding(new Insets(12, 0, 0, 0));
        return form;
    }

    private void loadUsers() {
        users.setAll(authService.getAllUsers());
    }

    private void resetForm() {
        editingUser = null;
        userTable.getSelectionModel().clearSelection();
        usernameField.setDisable(false);
        usernameField.clear();
        passwordField.setDisable(false);
        confirmField.setDisable(false);
        passwordField.clear();
        confirmField.clear();
        roleCombo.setValue(UserRole.COMERCIAL);
        showPw.setSelected(false);
        questionCombo.getSelectionModel().selectFirst();
        answerField.clear();
        saveButton.setText("Crear usuario");
        clearMessage();
    }

    private void fillForm(User user) {
        editingUser = null;
        usernameField.setDisable(false);
        usernameField.setText(user.getUsername());
        passwordField.setDisable(false);
        confirmField.setDisable(false);
        passwordField.clear();
        confirmField.clear();
        roleCombo.setValue(user.getRole());
        applyPermissions(parsePermissions(user.getPermissions()));
        saveButton.setText("Crear usuario");
        clearMessage();
    }

    private void startEditSelectedUser() {
        User selected = selectedUser();
        if (selected == null) {
            showError("Selecciona un usuario.");
            return;
        }
        editingUser = selected;
        usernameField.setText(selected.getUsername());
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        confirmField.setDisable(true);
        passwordField.clear();
        confirmField.clear();
        roleCombo.setValue(selected.getRole());
        applyPermissions(parsePermissions(selected.getPermissions()));
        saveButton.setText("Guardar cambios");
        clearMessage();
    }

    private void saveForm() {
        if (editingUser == null) {
            createUser();
        } else {
            updateUser();
        }
    }

    private void createUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        UserRole role = roleCombo.getValue();

        String question = questionCombo.getValue();
        String answer   = answerField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || role == null) {
            showError("Todos los campos son obligatorios.");
            TableColumnSizing.shake(username.isEmpty() ? usernameField : password.isEmpty() ? passwordField : confirmField);
            return;
        }
        if (question == null || answer.isEmpty()) {
            showError("Selecciona una pregunta de seguridad y escribe la respuesta.");
            TableColumnSizing.shake(answerField);
            return;
        }
        if (!password.equals(confirm)) {
            showError("Las contraseñas no coinciden.");
            TableColumnSizing.shake(confirmField);
            return;
        }
        if (!AuthService.isPasswordValid(password)) {
            showError("La contraseña debe tener al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres.");
            TableColumnSizing.shake(passwordField);
            return;
        }
        if (!authService.registerUser(username, password, role, selectedPermissions())) {
            showError("El nombre de usuario ya existe o no se pudo crear.");
            TableColumnSizing.shake(usernameField);
            return;
        }
        authService.setSecurityQuestion(username, question, answer);

        loadUsers();
        resetForm();
        showSuccess("Usuario creado correctamente.");
    }

    private void updateUser() {
        UserRole role = roleCombo.getValue();
        if (role == null) {
            showError("Selecciona un rol.");
            return;
        }
        if (!authService.updateRoleAndPermissions(editingUser.getId(), role, selectedPermissions())) {
            showError("No se pudieron guardar los cambios.");
            return;
        }

        loadUsers();
        resetForm();
        showSuccess("Cambios guardados correctamente.");
    }

    private void changeSelectedPassword() {
        User selected = selectedUser();
        if (selected == null) {
            showError("Selecciona un usuario.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar contraseña");
        dialog.setHeaderText("Nueva contraseña para " + selected.getUsername());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField newPasswordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nueva contraseña");
        confirmPasswordField.setPromptText("Confirmar contraseña");

        CheckBox showPwDialog = new CheckBox("Mostrar contraseña");
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Contraseña:"), 0, 0);
        grid.add(wrapPasswordField(newPasswordField, showPwDialog), 1, 0);
        grid.add(new Label("Confirmar:"), 0, 1);
        grid.add(wrapPasswordField(confirmPasswordField, showPwDialog), 1, 1);
        grid.add(showPwDialog, 1, 2);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(grid);
        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String password = newPasswordField.getText();
            String confirm = confirmPasswordField.getText();
            if (password.isEmpty() || confirm.isEmpty()) {
                showError("Introduce y confirma la nueva contraseña.");
                TableColumnSizing.shake(password.isEmpty() ? newPasswordField : confirmPasswordField);
                event.consume();
            } else if (!password.equals(confirm)) {
                showError("Las contraseñas no coinciden.");
                TableColumnSizing.shake(confirmPasswordField);
                event.consume();
            } else if (!AuthService.isPasswordValid(password)) {
                showError("La contraseña debe tener al menos " + AuthService.MIN_PASSWORD_LENGTH + " caracteres.");
                TableColumnSizing.shake(newPasswordField);
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == ButtonType.OK ? newPasswordField.getText() : null);
        dialog.showAndWait().ifPresent(password -> {
            if (authService.resetPasswordAdmin(selected.getId(), password)) {
                loadUsers();
                showSuccess("Contraseña actualizada correctamente.");
            } else {
                showError("No se pudo cambiar la contraseña.");
            }
        });
    }

    private void deleteSelectedUser() {
        User selected = selectedUser();
        if (selected == null) {
            showError("Selecciona un usuario.");
            return;
        }
        if (selected.getId() == currentUser.getId()) {
            showError("No puedes eliminar tu propio usuario.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar usuario");
        alert.setHeaderText("Eliminar usuario " + selected.getUsername());
        alert.setContentText("Esta acción no se puede deshacer.");

        alert.showAndWait()
            .filter(button -> button == ButtonType.OK)
            .ifPresent(button -> {
                if (authService.deleteUser(selected.getId())) {
                    loadUsers();
                    resetForm();
                    showSuccess("Usuario eliminado correctamente.");
                } else {
                    showError("No se pudo eliminar el usuario.");
                }
            });
    }

    private User selectedUser() {
        return userTable.getSelectionModel().getSelectedItem();
    }

    private String selectedPermissions() {
        return permissionChecks.entrySet().stream()
            .filter(entry -> entry.getValue().isSelected())
            .map(Map.Entry::getKey)
            .collect(Collectors.joining(","));
    }

    private void applyPermissions(Set<String> permissions) {
        permissionChecks.forEach((key, checkBox) -> checkBox.setSelected(permissions.contains(key)));
    }

    private Set<String> parsePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) return Set.of();
        return Arrays.stream(permissions.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toSet());
    }

    private String formatDate(User user) {
        return user.getCreatedAt() == null ? "" : user.getCreatedAt().format(DATE_FORMAT);
    }

    private void showSuccess(String message) {
        msgLabel.getStyleClass().removeAll("field-error-msg");
        if (!msgLabel.getStyleClass().contains("field-success-msg"))
            msgLabel.getStyleClass().add("field-success-msg");
        msgLabel.setText(message);
    }

    private void showError(String message) {
        msgLabel.getStyleClass().removeAll("field-success-msg");
        if (!msgLabel.getStyleClass().contains("field-error-msg"))
            msgLabel.getStyleClass().add("field-error-msg");
        msgLabel.setText(message);
    }

    private void clearMessage() {
        msgLabel.setText("");
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
}
