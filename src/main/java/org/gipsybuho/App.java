package org.gipsybuho;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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
import org.gipsybuho.ui.AdminSetupView;
import org.gipsybuho.service.PreferenceService;
import org.gipsybuho.ui.LoginView;
import org.gipsybuho.ui.MainView;
import org.gipsybuho.ui.OnboardingDialog;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class App extends Application {

    private Stage primaryStage;
    private AuthService authService;

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

        String volStr = DatabaseManager.getConfig("audio_volumen");
        if (!volStr.isBlank()) {
            try { SoundService.setVolume(Integer.parseInt(volStr) / 100f); }
            catch (NumberFormatException ignored) {}
        }
        SoundService.setMuted("1".equals(DatabaseManager.getConfig("audio_muted")));

        authService = new AuthService(new UserDAO());

        try {
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo cargar el icono: " + e.getMessage());
        }

        if (!authService.hasAdmin()) {
            showAdminSetup();
        } else {
            showLogin();
        }
    }

    private void showAdminSetup() {
        AdminSetupView view = new AdminSetupView(authService, this::showMainApplication);
        Scene scene = buildAuthScene(view);
        primaryStage.setTitle("Gráficas Mulberry — Configuración inicial");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setWidth(500);
        primaryStage.setHeight(440);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> Platform.exit());
        primaryStage.show();
        SoundService.play(SoundService.Sound.NOTIFICATION);
    }

    private void showLogin() {
        LoginView view = new LoginView(authService, this::showMainApplication);
        Scene scene = buildAuthScene(view);
        primaryStage.setTitle("Gráficas Mulberry — Iniciar sesión");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setWidth(420);
        primaryStage.setHeight(360);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(e -> Platform.exit());
        primaryStage.show();
        SoundService.play(SoundService.Sound.NOTIFICATION);
    }

    private Scene buildAuthScene(javafx.scene.Parent view) {
        Scene scene = new Scene(view);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());
        TemaManager.aplicarTodo(scene);
        return scene;
    }

    private void showMainApplication(User user) {
        String musicaPlaylist = DatabaseManager.getConfig("musica_playlist");
        if (!musicaPlaylist.isBlank()) {
            MusicService.setPlaylist(java.util.Arrays.asList(musicaPlaylist.split("\\|")));
        }
        String musicaVolStr = DatabaseManager.getConfig("musica_volumen");
        if (!musicaVolStr.isBlank()) {
            try { MusicService.setVolumen(Integer.parseInt(musicaVolStr) / 100f); }
            catch (NumberFormatException ignored) {}
        }
        MusicService.setLoop(!"0".equals(DatabaseManager.getConfig("musica_loop")));
        boolean musicaAutoplay = "1".equals(DatabaseManager.getConfig("musica_autoplay"));

        MainView mainView = new MainView(primaryStage, user, authService, this::showLogin);
        Scene mainAppScene = new Scene(mainView, 1280, 800);
        mainAppScene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());
        TemaManager.aplicarTodo(mainAppScene);

        mainAppScene.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (ev.getSource() instanceof javafx.scene.control.Button) {
                SoundService.play(SoundService.Sound.CLICK);
            }
        });

        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setScene(mainAppScene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        Platform.runLater(() -> primaryStage.setMaximized(true));
        primaryStage.setOnCloseRequest(event -> {
            if (!mainView.confirmarSalida()) event.consume();
        });

        SoundService.play(SoundService.Sound.NOTIFICATION);
        Platform.runLater(() -> {
            if (PreferenceService.getInstance().isFirstRun()) {
                OnboardingDialog dlg = new OnboardingDialog();
                dlg.initOwner(primaryStage);
                dlg.showAndWait();
            }
            notificarRecordatoriosProximos();
        });
        if (musicaAutoplay && !MusicService.getPlaylist().isEmpty()) {
            Platform.runLater(MusicService::play);
        }
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
            alert.setHeaderText("Tienes " + proximas.size() + " recordatorio(s) en los próximos 3 días");
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
