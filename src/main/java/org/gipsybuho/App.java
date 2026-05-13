package org.gipsybuho;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.gipsybuho.dao.LogAccessDAO;
import org.gipsybuho.dao.NotaCalendarioDAO;
import org.gipsybuho.dao.UserDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.User;
import org.gipsybuho.service.AuthService;
import org.gipsybuho.service.MusicService;
import org.gipsybuho.service.OllamaManager;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.TemaManager;
import org.gipsybuho.ui.LockScreenController;
import org.gipsybuho.ui.LoginController;
import org.gipsybuho.ui.MainView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class App extends Application implements LoginController.LoginCallback, LockScreenController.LockScreenCallback {

    private AuthService authService;
    private UserDAO userDAO; // Mantener una referencia a UserDAO
    private Stage primaryStage;
    private User currentUser;
    private Scene mainAppScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!SingleInstanceLock.acquireLock()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de Aplicación");
            alert.setHeaderText("La aplicación ya está en ejecución.");
            alert.setContentText("Solo se permite una instancia de Gráficas Mulberry a la vez.");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        this.primaryStage = primaryStage;

        OllamaManager.startAsync();
        DatabaseManager.initialize();

        userDAO = new UserDAO(); // Instanciar UserDAO aquí
        LogAccessDAO logAccessDAO = new LogAccessDAO();
        this.authService = new AuthService(userDAO, logAccessDAO);

        String volStr = DatabaseManager.getConfig("audio_volumen");
        if (!volStr.isBlank()) {
            try { SoundService.setVolume(Integer.parseInt(volStr) / 100f); }
            catch (NumberFormatException ignored) {}
        }
        String mutedStr = DatabaseManager.getConfig("audio_muted");
        SoundService.setMuted("1".equals(mutedStr));

        showLoginScreen();
    }

    public void showLoginScreen() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/gipsybuho/ui/LoginView.fxml"));
        Parent loginRoot = fxmlLoader.load();
        LoginController loginController = fxmlLoader.getController();
        loginController.setAuthService(authService);
        loginController.setUserDAO(userDAO); // Inyectar UserDAO
        loginController.setLoginCallback(this);

        Scene scene = new Scene(loginRoot, 450, 400);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());
        TemaManager.aplicarTodo(scene);

        primaryStage.setTitle("Gráficas Mulberry — Iniciar Sesión");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(450);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }

    @Override
    public void onLoginSuccess(User loggedInUser) {
        this.currentUser = loggedInUser;
        String musicaPlaylist = DatabaseManager.getConfig("musica_playlist");
        if (!musicaPlaylist.isBlank()) {
            MusicService.setPlaylist(java.util.Arrays.asList(musicaPlaylist.split("\\|")));
        }
        String musicaVolStr = DatabaseManager.getConfig("musica_volumen");
        if (!musicaVolStr.isBlank()) {
            try { MusicService.setVolumen(Integer.parseInt(musicaVolStr) / 100f); }
            catch (NumberFormatException ignored) {}
        }
        String musicaLoop = DatabaseManager.getConfig("musica_loop");
        MusicService.setLoop(!"0".equals(musicaLoop));
        boolean musicaAutoplay = "1".equals(DatabaseManager.getConfig("musica_autoplay"));

        MainView mainView = new MainView(primaryStage, authService, currentUser, this::showLockScreen, this::showLoginScreenSafely);
        mainAppScene = new Scene(mainView, 1280, 800);
        mainAppScene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());

        TemaManager.aplicarTodo(mainAppScene);

        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setScene(mainAppScene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);

        try {
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo cargar el icono de la aplicación: " + e.getMessage());
        }

        mainAppScene.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (ev.getSource() instanceof javafx.scene.control.Button) {
                SoundService.play(SoundService.Sound.CLICK);
            }
        });

        primaryStage.show();
        SoundService.play(SoundService.Sound.NOTIFICATION);
        Platform.runLater(this::notificarRecordatoriosProximos);
        if (musicaAutoplay && !MusicService.getPlaylist().isEmpty()) {
            Platform.runLater(MusicService::play);
        }
    }

    public void showLockScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/gipsybuho/ui/LockScreenView.fxml"));
            Parent lockRoot = fxmlLoader.load();
            LockScreenController lockController = fxmlLoader.getController();
            lockController.setAuthService(authService);
            lockController.setLockedUser(currentUser);
            lockController.setCallback(this);

            Scene lockScene = new Scene(lockRoot, 450, 400);
            lockScene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());
            TemaManager.aplicarTodo(lockScene);

            primaryStage.setTitle("Gráficas Mulberry — Bloqueado");
            primaryStage.setScene(lockScene);
            primaryStage.setMinWidth(450);
            primaryStage.setMinHeight(400);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error al cargar la pantalla de bloqueo: " + e.getMessage());
            try {
                showLoginScreen();
            } catch (IOException ex) {
                System.err.println("Error fatal: no se pudo cargar ni la pantalla de bloqueo ni la de login.");
                Platform.exit();
            }
        }
    }

    private void showLoginScreenSafely() {
        try {
            showLoginScreen();
        } catch (IOException e) {
            System.err.println("Error al volver a la pantalla de login: " + e.getMessage());
            Platform.exit();
        }
    }

    @Override
    public void onUnlockSuccess() {
        primaryStage.setScene(mainAppScene);
        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        primaryStage.show();
    }

    private void notificarRecordatoriosProximos() {
        try {
            String rawDias = DatabaseManager.getConfig("cal_dias_aviso");
            int diasAviso;
            try { diasAviso = rawDias.isBlank() ? 3 : Integer.parseInt(rawDias); }
            catch (NumberFormatException ignored) { diasAviso = 3; }
            List<NotaCalendario> proximas = new NotaCalendarioDAO().findProximas(diasAviso);
            if (proximas.isEmpty()) return;

            StringBuilder sb = new StringBuilder();
            LocalDate hoy = LocalDate.now();
            for (NotaCalendario n : proximas) {
                long dias = ChronoUnit.DAYS.between(hoy, n.getFecha());
                String cuando = dias == 0 ? "HOY" : dias == 1 ? "mañana" : "en " + dias + " días";
                sb.append("• ").append(n.getTitulo()).append("  (").append(cuando).append(")\n");
            }

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Recordatorios próximos");
            alert.setHeaderText("Tienes " + proximas.size()
                + " recordatorio(s) en los próximos 3 días");
            alert.setContentText(sb.toString().stripTrailing());
            alert.show();
        } catch (Exception e) {
            System.err.println("Error al comprobar recordatorios: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        MusicService.dispose();
        DatabaseManager.closeConnection();
        OllamaManager.stop();
        SingleInstanceLock.releaseLock();
    }
}
