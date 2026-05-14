package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class TextToSpeechService {

    public static final String KEY_VOZ_TTS = "tts_voz";
    public static final String VOZ_WINDOWS_FEMENINA = "Windows femenina";

    private static Process currentProcess;
    private static Thread currentThread;
    private static long playbackGeneration;

    private TextToSpeechService() {}

    public static List<String> nombresVoces() {
        return List.of(VOZ_WINDOWS_FEMENINA);
    }

    public static String getVozSeleccionada() {
        return VOZ_WINDOWS_FEMENINA;
    }

    public static void setVozSeleccionada(String voz) {
        DatabaseManager.setConfig(KEY_VOZ_TTS, VOZ_WINDOWS_FEMENINA);
    }

    public static synchronized void speak(String text) {
        if (text == null || text.isBlank()) return;

        stop();
        long generation = ++playbackGeneration;
        currentThread = Thread.ofVirtual().start(() -> speakWithWindows(text, generation));
    }

    public static synchronized void stop() {
        playbackGeneration++;
        stopCurrentProcess();
        currentProcess = null;

        if (currentThread != null) {
            currentThread.interrupt();
            currentThread = null;
        }
    }

    private static void speakWithWindows(String text, long generation) {
        if (!isWindows() || !isActive(generation)) return;

        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "$ErrorActionPreference = 'Stop'; "
                + "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$voice = $speaker.GetInstalledVoices() "
                + "| Where-Object { $_.Enabled } "
                + "| Sort-Object "
                + "@{ Expression = { if ($_.VoiceInfo.Culture.Name -eq 'es-ES') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Gender -eq 'Female') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Name -like '*Helena*') { 0 } elseif ($_.VoiceInfo.Name -like '*Laura*') { 1 } else { 2 } } }, "
                + "@{ Expression = { $_.VoiceInfo.Name } } "
                + "| Select-Object -First 1; "
                + "if ($voice) { $speaker.SelectVoice($voice.VoiceInfo.Name); } "
                + "$speaker.Rate = -1; "
                + "$speaker.Volume = 100; "
                + "$speaker.Speak($env:GM_TTS_TEXT); "
                + "$speaker.Dispose();"
        );
        pb.environment().put("GM_TTS_TEXT", normalizarTexto(text));
        pb.redirectErrorStream(true);

        Process process = null;
        try {
            process = pb.start();
            synchronized (TextToSpeechService.class) {
                if (!isActive(generation)) {
                    process.destroyForcibly();
                    return;
                }
                currentProcess = process;
            }
            process.waitFor();
        } catch (IOException e) {
            System.err.println("No se pudo iniciar la voz femenina de Windows: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        } finally {
            synchronized (TextToSpeechService.class) {
                if (currentProcess == process) {
                    currentProcess = null;
                }
            }
        }
    }

    private static void stopCurrentProcess() {
        if (currentProcess == null || !currentProcess.isAlive()) return;

        currentProcess.descendants().forEach(ProcessHandle::destroyForcibly);
        currentProcess.destroyForcibly();
    }

    private static synchronized boolean isActive(long generation) {
        return generation == playbackGeneration && !Thread.currentThread().isInterrupted();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
    }

    private static String normalizarTexto(String text) {
        return text
            .replaceAll("(?s)```.*?```", " bloque de codigo ")
            .replaceAll("[#*_`>\\[\\](){}|]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
