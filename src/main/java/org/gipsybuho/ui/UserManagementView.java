package org.gipsybuho.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserManagementView extends VBox {

    private final AuthService authService;
    private final TableView<User> userTable = new TableView<>();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label messageLabel = new Label();

    public UserManagementView(AuthService authService) {
        this.authService = authService;
        initializeUI();
        loadUsers();
    }

    private void initializeUI() {
        this.setPadding(new Insets(20));
        this.setSpacing(15);
        this.getStyleClass().add("user-management-view");

        Label title = new Label("Gestión de Usuarios");
        title.getStyleClass().add("view-title");

        // --- Tabla de Usuarios ---
        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<User, String> usernameCol = new TableColumn<>("Usuario");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);

        TableColumn<User, String> lastLoginCol = new TableColumn<>("Último Acceso");
        lastLoginCol.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new ReadOnlyStringWrapper(user.getLastLogin() != null ?
                    user.getLastLogin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Nunca");
        });
        lastLoginCol.setPrefWidth(180);

        TableColumn<User, String> createdAtCol = new TableColumn<>("Creado El");
        createdAtCol.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new ReadOnlyStringWrapper(user.getCreatedAt() != null ?
                    user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });
        createdAtCol.setPrefWidth(180);

        userTable.getColumns().addAll(idCol, usernameCol, lastLoginCol, createdAtCol);
        userTable.setItems(userList);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setPlaceholder(new Label("No hay usuarios registrados."));

        // --- Formulario para Crear Usuario ---
        GridPane createUserPane = new GridPane();
        createUserPane.setHgap(10);
        createUserPane.setVgap(10);
        createUserPane.setPadding(new Insets(10));
        createUserPane.getStyleClass().add("form-pane");

        createUserPane.add(new Label("Nuevo Usuario:"), 0, 0);
        createUserPane.add(new Label("Usuario:"), 0, 1);
        createUserPane.add(usernameField, 1, 1);
        createUserPane.add(new Label("Contraseña:"), 0, 2);
        createUserPane.add(passwordField, 1, 2);
        createUserPane.add(new Label("Confirmar Contraseña:"), 0, 3);
        createUserPane.add(confirmPasswordField, 1, 3);

        Button createUserButton = new Button("Crear Usuario");
        createUserButton.setOnAction(e -> handleCreateUser());
        createUserPane.add(createUserButton, 1, 4);

        // --- Botones de Acción para Usuarios Seleccionados ---
        HBox actionButtons = new HBox(10);
        actionButtons.setPadding(new Insets(10, 0, 0, 0));

        Button deleteUserButton = new Button("Eliminar Usuario");
        deleteUserButton.setOnAction(e -> handleDeleteUser());

        Button changePasswordButton = new Button("Cambiar Contraseña");
        changePasswordButton.setOnAction(e -> handleChangePassword());

        actionButtons.getChildren().addAll(deleteUserButton, changePasswordButton);

        messageLabel.getStyleClass().add("message-label");

        this.getChildren().addAll(title, userTable, createUserPane, actionButtons, messageLabel);
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(authService.getAllUsers());
    }

    private void handleCreateUser() {
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
        if (password.length() < 6) { // Ejemplo de política de contraseña
            messageLabel.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        if (authService.registerUser(username, password)) {
            messageLabel.setText("Usuario '" + username + "' creado exitosamente.");
            usernameField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            loadUsers(); // Recargar la tabla de usuarios
        } else {
            messageLabel.setText("Error: El nombre de usuario '" + username + "' ya existe.");
        }
    }

    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            messageLabel.setText("Por favor, selecciona un usuario para eliminar.");
            return;
        }

        if (selectedUser.getUsername().equals("admin")) {
            messageLabel.setText("No se puede eliminar el usuario 'admin'.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Estás seguro de que quieres eliminar al usuario '" + selectedUser.getUsername() + "'?");
        alert.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (authService.deleteUser(selectedUser.getId())) {
                messageLabel.setText("Usuario '" + selectedUser.getUsername() + "' eliminado exitosamente.");
                loadUsers();
            } else {
                messageLabel.setText("Error al eliminar el usuario.");
            }
        }
    }

    private void handleChangePassword() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            messageLabel.setText("Por favor, selecciona un usuario para cambiar su contraseña.");
            return;
        }

        // Crear un diálogo para pedir la nueva contraseña
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Contraseña para " + selectedUser.getUsername());
        dialog.setHeaderText("Introduce la nueva contraseña:");

        ButtonType okButtonType = new ButtonType("Cambiar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nueva Contraseña");
        PasswordField confirmNewPasswordField = new PasswordField();
        confirmNewPasswordField.setPromptText("Confirmar Nueva Contraseña");

        VBox content = new VBox(10, newPasswordField, confirmNewPasswordField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        // Habilitar/deshabilitar el botón OK basado en la validación de las contraseñas
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setDisable(true);

        newPasswordField.textProperty().addListener((obs, oldVal, newVal) ->
                okButton.setDisable(newVal.isEmpty() || !newVal.equals(confirmNewPasswordField.getText())));
        confirmNewPasswordField.textProperty().addListener((obs, oldVal, newVal) ->
                okButton.setDisable(newVal.isEmpty() || !newVal.equals(newPasswordField.getText())));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return newPasswordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newPassword -> {
            if (newPassword.length() < 6) {
                messageLabel.setText("La nueva contraseña debe tener al menos 6 caracteres.");
                return;
            }
            if (authService.changePassword(selectedUser.getId(), newPassword)) {
                messageLabel.setText("Contraseña de '" + selectedUser.getUsername() + "' cambiada exitosamente.");
            } else {
                messageLabel.setText("Error al cambiar la contraseña.");
            }
        });
    }
}
