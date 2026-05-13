package org.gipsybuho.service;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundService {

    public enum Sound {
        HOVER,        // Paso del cursor por controles principales
        CLICK,        // Click de botón (corto, sutil)
        NAVIGATE,     // Navegación por módulos
        WINDOW_OPEN,  // Apertura de ventana o módulo
        CHAT_SEND,    // Mensaje enviado al asistente
        CHAT_RECEIVE, // Respuesta recibida del asistente
        SUCCESS,      // Operación exitosa (arpeggio ascendente 2 notas)
        ERROR,        // Error o advertencia (tono descendente)
        START,        // Inicio de proceso (sweep ascendente)
        COMPLETE,     // Proceso completado (arpeggio ascendente 3 notas)
        NOTIFICATION  // Notificación general (campana suave)
    }

    private static float volume = 0.65f;   // 0.0 – 1.0
    private static boolean muted  = false;

    private static final AtomicBoolean clickPlaying = new AtomicBoolean(false);
    private static long lastHoverMs;

    private static final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sound");
        t.setDaemon(true);
        return t;
    });

    // ── Public API ────────────────────────────────────────────────────────────

    public static void play(Sound sound) {
        if (muted || volume < 0.01f) return;
        if (sound == Sound.HOVER) {
            long now = System.currentTimeMillis();
            if (now - lastHoverMs < 90) return;
            lastHoverMs = now;
        }
        // Para clicks rápidos: descartar si ya hay uno sonando
        if (sound == Sound.CLICK) {
            if (!clickPlaying.compareAndSet(false, true)) return;
            pool.submit(() -> {
                try { playPCM(genClick()); }
                catch (Exception ignored) {}
                finally { clickPlaying.set(false); }
            });
        } else {
            pool.submit(() -> {
                try { playPCM(generatePCM(sound)); }
                catch (Exception ignored) {}
            });
        }
    }

    public static void setVolume(float v) { volume = Math.max(0f, Math.min(1f, v)); }
    public static float getVolume()        { return volume; }
    public static void setMuted(boolean m) { muted = m; }
    public static boolean isMuted()        { return muted; }

    // ── PCM generation ────────────────────────────────────────────────────────

    private static final int SR = 44100; // sample rate

    private static byte[] generatePCM(Sound sound) {
        return switch (sound) {
            case HOVER        -> tone(1180, 24, 0.13f, 0f, 0f);
            case CLICK        -> genClick();
            case NAVIGATE     -> arpeggio(new int[]{640, 820}, 42, 0.25f);
            case WINDOW_OPEN  -> arpeggio(new int[]{420, 620, 840}, 54, 0.26f);
            case CHAT_SEND    -> arpeggio(new int[]{740, 980}, 48, 0.24f);
            case CHAT_RECEIVE -> arpeggio(new int[]{620, 780, 930}, 58, 0.22f);
            case SUCCESS      -> arpeggio(new int[]{523, 659}, 110, 0.38f);
            case ERROR        -> tone(320, 280, 0.40f, 12f, 0.18f);
            case START        -> sweep(380, 720, 160, 0.35f);
            case COMPLETE     -> arpeggio(new int[]{523, 659, 784}, 75, 0.40f);
            case NOTIFICATION -> tone(880, 220, 0.34f, 0f, 0f);
        };
    }

    private static byte[] genClick() {
        // Breve pulso a 850 Hz, 30 ms, ataque y caída muy rápidos
        return tone(850, 30, 0.28f, 0f, 0f);
    }

    /**
     * Sine wave with linear attack/release envelope.
     * @param freq       frequency in Hz
     * @param durationMs duration in milliseconds
     * @param amplitude  base amplitude (0.0–1.0, will be scaled by volume)
     * @param tremFreq   tremolo rate in Hz (0 = no tremolo)
     * @param tremDepth  tremolo depth (0.0–1.0)
     */
    private static byte[] tone(int freq, int durationMs, float amplitude,
                                float tremFreq, float tremDepth) {
        int n   = SR * durationMs / 1000;
        int att = Math.min(n / 5, SR / 100);  // attack  ≤ 10 ms
        int rel = Math.min(n / 3, SR / 40);   // release ≤ 25 ms
        byte[] buf = new byte[2 * n];

        for (int i = 0; i < n; i++) {
            double t   = (double) i / SR;
            double env = i < att ? (double) i / att
                       : i > n - rel ? (double)(n - i) / rel
                       : 1.0;
            double trem = tremDepth > 0
                ? (1.0 - tremDepth * (0.5 + 0.5 * Math.sin(2 * Math.PI * tremFreq * t)))
                : 1.0;
            double s   = amplitude * volume * env * trem * Math.sin(2 * Math.PI * freq * t);
            short  pcm = (short)(s * 32767);
            buf[2 * i]     = (byte)(pcm & 0xFF);
            buf[2 * i + 1] = (byte)((pcm >> 8) & 0xFF);
        }
        return buf;
    }

    /** Concatenate several tones back-to-back. */
    private static byte[] arpeggio(int[] freqs, int noteMs, float amplitude) {
        int noteBytes = 2 * SR * noteMs / 1000;
        byte[] out = new byte[noteBytes * freqs.length];
        for (int f = 0; f < freqs.length; f++) {
            byte[] note = tone(freqs[f], noteMs, amplitude, 0f, 0f);
            System.arraycopy(note, 0, out, f * noteBytes, Math.min(note.length, noteBytes));
        }
        return out;
    }

    /** Linear frequency sweep. */
    private static byte[] sweep(int startHz, int endHz, int durationMs, float amplitude) {
        int n   = SR * durationMs / 1000;
        int rel = n / 4;
        byte[] buf = new byte[2 * n];
        double phase = 0;

        for (int i = 0; i < n; i++) {
            double progress = (double) i / n;
            double freq = startHz + (endHz - startHz) * progress;
            phase += 2 * Math.PI * freq / SR;
            double env = i > n - rel ? (double)(n - i) / rel : 1.0;
            double s   = amplitude * volume * env * Math.sin(phase);
            short  pcm = (short)(s * 32767);
            buf[2 * i]     = (byte)(pcm & 0xFF);
            buf[2 * i + 1] = (byte)((pcm >> 8) & 0xFF);
        }
        return buf;
    }

    // ── Playback ──────────────────────────────────────────────────────────────

    private static void playPCM(byte[] pcm) throws Exception {
        AudioFormat fmt  = new AudioFormat(SR, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        if (!AudioSystem.isLineSupported(info)) return;

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(fmt, pcm.length);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        }
    }
}
