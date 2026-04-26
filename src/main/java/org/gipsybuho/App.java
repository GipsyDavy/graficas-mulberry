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

    @Override
    public void start(Stage primaryStage) throws Exception {
        OllamaManager.startAsync();   // arranca Ollama en background si está instalado
        DatabaseManager.initialize();

        // Restaurar volumen de efectos de sonido
        String volStr = DatabaseManager.getConfig("audio_volumen");
        if (!volStr.isBlank()) {
            try { SoundService.setVolume(Integer.parseInt(volStr) / 100f); }
            catch (NumberFormatException ignored) {}
        }
        String mutedStr = DatabaseManager.getConfig("audio_muted");
        SoundService.setMuted("1".equals(mutedStr));

        // Restaurar configuración de música de fondo
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

        MainView mainView = new MainView(primaryStage);
        Scene scene = new Scene(mainView, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/org/gipsybuho/styles.css")).toExternalForm());

        // Aplicar tema y tipografía guardados por el usuario
        TemaManager.aplicarTodo(scene);

        primaryStage.setTitle("Gráficas Mulberry — Sistema de Gestión");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);

        try {
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Advertencia: no se pudo cargar el icono de la aplicación: " + e.getMessage());
        }

        // Filtro global: cualquier Button reproduce CLICK
        scene.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (ev.getSource() instanceof javafx.scene.control.Button) {
                SoundService.play(SoundService.Sound.CLICK);
            }
        });

        primaryStage.show();
        SoundService.play(SoundService.Sound.NOTIFICATION); // sonido de bienvenida
        Platform.runLater(this::notificarRecordatoriosProximos);
        if (musicaAutoplay && !MusicService.getPlaylist().isEmpty()) {
            Platform.runLater(MusicService::play);
        }
    }

    private void notificarRecordatoriosProximos() {
        try {
            String rawDias = DatabaseManager.getConfig("cal_dias_aviso");
            int diasAviso = rawDias.isBlank() ? 3 : Integer.parseInt(rawDias);
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
    }
}
