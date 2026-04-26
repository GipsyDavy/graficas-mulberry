package org.gipsybuho.service;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reproductor de música de fondo (MP3). Singleton estático — la música
 * continúa reproduciéndose aunque el usuario cambie de vista.
 * Todos los métodos públicos deben llamarse desde el hilo FX.
 */
public class MusicService {

    public enum Estado { PARADO, REPRODUCIENDO, PAUSADO }

    // ── Estado global ─────────────────────────────────────────────────────────

    private static final List<String> playlist = new ArrayList<>();
    private static int     indice   = 0;
    private static float   volumen  = 0.50f;
    private static boolean loop     = true;
    private static Estado  estado   = Estado.PARADO;
    private static MediaPlayer player;

    // Callbacks para actualizar la UI desde cualquier vista
    private static Consumer<String> onTrackChange;
    private static Consumer<Estado> onEstadoChange;

    // ── Playlist ──────────────────────────────────────────────────────────────

    public static void setPlaylist(List<String> paths) {
        playlist.clear();
        playlist.addAll(paths);
    }

    public static List<String> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public static void addTrack(String path) {
        playlist.add(path);
    }

    public static void removeTrack(int i) {
        if (i >= 0 && i < playlist.size()) {
            // Si estamos borrando la pista actual, parar
            if (i == indice && estado != Estado.PARADO) stop();
            playlist.remove(i);
            if (indice >= playlist.size()) indice = 0;
        }
    }

    // ── Controles ─────────────────────────────────────────────────────────────

    public static void play() {
        if (playlist.isEmpty()) return;
        if (estado == Estado.PAUSADO && player != null) {
            player.play();
            cambiarEstado(Estado.REPRODUCIENDO);
            return;
        }
        if (indice >= playlist.size()) indice = 0;
        cargar(indice);
    }

    public static void play(int trackIndex) {
        if (trackIndex < 0 || trackIndex >= playlist.size()) return;
        indice = trackIndex;
        cargar(indice);
    }

    public static void pause() {
        if (player != null && estado == Estado.REPRODUCIENDO) {
            player.pause();
            cambiarEstado(Estado.PAUSADO);
        }
    }

    public static void stop() {
        destruirPlayer();
        cambiarEstado(Estado.PARADO);
    }

    public static void siguiente() {
        if (playlist.isEmpty()) return;
        indice = (indice + 1) % playlist.size();
        cargar(indice);
    }

    public static void anterior() {
        if (playlist.isEmpty()) return;
        indice = (indice - 1 + playlist.size()) % playlist.size();
        cargar(indice);
    }

    // ── Volumen ───────────────────────────────────────────────────────────────

    public static void setVolumen(float v) {
        volumen = Math.max(0f, Math.min(1f, v));
        if (player != null) player.setVolume(volumen);
    }

    public static float getVolumen() { return volumen; }

    // ── Estado y callbacks ────────────────────────────────────────────────────

    public static Estado getEstado()        { return estado; }
    public static boolean isReproduciendo() { return estado == Estado.REPRODUCIENDO; }
    public static int getIndice()           { return indice; }
    public static boolean isLoop()          { return loop; }
    public static void setLoop(boolean l)   { loop = l; }

    public static String getNombreActual() {
        if (playlist.isEmpty() || indice >= playlist.size()) return "";
        return new File(playlist.get(indice)).getName();
    }

    public static void setOnTrackChange(Consumer<String> cb)  { onTrackChange  = cb; }
    public static void setOnEstadoChange(Consumer<Estado> cb) { onEstadoChange = cb; }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    public static void dispose() {
        destruirPlayer();
    }

    // ── Interno ───────────────────────────────────────────────────────────────

    private static void cargar(int idx) {
        destruirPlayer();
        if (idx >= playlist.size()) return;

        File f = new File(playlist.get(idx));
        if (!f.exists()) {
            // Pista no encontrada → saltar a la siguiente
            if (playlist.size() > 1) { indice = (idx + 1) % playlist.size(); cargar(indice); }
            return;
        }

        try {
            Media media = new Media(f.toURI().toString());
            player = new MediaPlayer(media);
            player.setVolume(volumen);

            player.setOnReady(() -> {
                player.play();
                cambiarEstado(Estado.REPRODUCIENDO);
                notificarCambioTrack(f.getName());
            });

            player.setOnEndOfMedia(() -> {
                if (loop) {
                    siguiente();
                } else {
                    cambiarEstado(Estado.PARADO);
                }
            });

            player.setOnError(() -> {
                System.err.println("Music error on track: " + f.getName());
                if (playlist.size() > 1) siguiente();
                else cambiarEstado(Estado.PARADO);
            });

        } catch (Exception e) {
            System.err.println("MusicService error: " + e.getMessage());
        }
    }

    private static void destruirPlayer() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    private static void cambiarEstado(Estado e) {
        estado = e;
        if (onEstadoChange != null)
            Platform.runLater(() -> onEstadoChange.accept(e));
    }

    private static void notificarCambioTrack(String nombre) {
        if (onTrackChange != null)
            Platform.runLater(() -> onTrackChange.accept(nombre));
    }
}
