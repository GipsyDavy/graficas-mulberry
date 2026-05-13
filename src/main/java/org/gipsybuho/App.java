package org.gipsybuho;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.gipsybuho.dao.NotaCalendarioDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.NotaCalendario;
import org.gipsybuho.model.User;
import org.gipsybuho.service.MusicService;
import org.gipsybuho.service.OllamaManager;
import org.gipsybuho.service.SoundService;
import org.gipsybuho.service.TemaManager;
import org.gipsybuho.ui.MainView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class App extends Application {

    private Stage primaryStage;
    private User currentUser;

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
        String mutedStr = DatabaseManager.getConfig("audio_muted");
        SoundService.setMuted("1".equals(mutedStr));

        showMainApplication();
    }

    private void showMainApplication() {
        this.currentUser = new User(
            0,
            "Sistema",
            "",
            null,
            java.time.LocalDateTime.now(),
            0,
            null,
            true,
            User.ROLE_INITIAL_ADMIN,
            User.ALL_PERMISSIONS
        );

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

        MainView mainView = new MainView(primaryStage, currentUser);
        Scene mainAppScene = new Scene(mainView, 1280, 800);
        mainAppScene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());

        TemaManager.aplicarTodo(mainAppScene);

        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setScene(mainAppScene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        primaryStage.setOnCloseRequest(event -> {
            if (!mainView.confirmarSalida()) {
                event.consume();
            }
        });

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
