package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextToSpeechService {

    public static final String KEY_VOZ_TTS = "tts_voz";
    public static final String VOZ_WINDOWS_FEMENINA = "Windows femenina";
    public static final String OPCION_INSTALAR_VOCES_NATURALES = "Instalar voces naturales es-ES de Windows...";

    private static Process currentProcess;
    private static Thread currentThread;
    private static long playbackGeneration;

    private TextToSpeechService() {}

    public static List<String> nombresVoces() {
        Set<String> voces = new LinkedHashSet<>(detectarVocesWindowsEsEs());
        voces.add(VOZ_WINDOWS_FEMENINA);
        if (voces.stream().noneMatch(TextToSpeechService::pareceVozNatural)) {
            voces.add(OPCION_INSTALAR_VOCES_NATURALES);
        }
        return new ArrayList<>(voces);
    }

    public static String getVozSeleccionada() {
        String voz = DatabaseManager.getConfig(KEY_VOZ_TTS);
        return voz == null || voz.isBlank() || OPCION_INSTALAR_VOCES_NATURALES.equals(voz)
            ? VOZ_WINDOWS_FEMENINA
            : voz;
    }

    public static void setVozSeleccionada(String voz) {
        if (voz == null || voz.isBlank() || OPCION_INSTALAR_VOCES_NATURALES.equals(voz)) {
            return;
        }
        DatabaseManager.setConfig(KEY_VOZ_TTS, voz);
    }

    public static boolean esOpcionInstalacion(String voz) {
        return OPCION_INSTALAR_VOCES_NATURALES.equals(voz);
    }

    public static String mensajeInstalacionVocesNaturales() {
        return "No se han detectado voces naturales es-ES compatibles. "
            + "Instálalas desde Windows en Configuración > Hora e idioma > Voz o Idioma y región, "
            + "añadiendo Español (España) y sus opciones de voz. Después reinicia la aplicación.";
    }

    public static boolean isDisponible() {
        return isWindows() && hayVocesWindowsInstaladas();
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
                + "$selected = $env:GM_TTS_VOICE; "
                + "$voices = $speaker.GetInstalledVoices() "
                + "| Where-Object { $_.Enabled } "
                + "| Sort-Object "
                + "@{ Expression = { if ($_.VoiceInfo.Culture.Name -eq 'es-ES') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Name -match 'Natural|Neural|Online') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Gender -eq 'Female') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Name -like '*Helena*') { 0 } elseif ($_.VoiceInfo.Name -like '*Laura*') { 1 } else { 2 } } }, "
                + "@{ Expression = { $_.VoiceInfo.Name } }; "
                + "$voice = $voices | Where-Object { $_.VoiceInfo.Name -eq $selected } | Select-Object -First 1; "
                + "if (-not $voice) { $voice = $voices "
                + "| Sort-Object "
                + "@{ Expression = { if ($_.VoiceInfo.Culture.Name -eq 'es-ES') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Name -match 'Natural|Neural|Online') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Gender -eq 'Female') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Name -like '*Helena*') { 0 } elseif ($_.VoiceInfo.Name -like '*Laura*') { 1 } else { 2 } } }, "
                + "@{ Expression = { $_.VoiceInfo.Name } } "
                + "| Select-Object -First 1; } "
                + "if ($voice) { $speaker.SelectVoice($voice.VoiceInfo.Name); } "
                + "$speaker.Rate = -1; "
                + "$speaker.Volume = 100; "
                + "$speaker.Speak($env:GM_TTS_TEXT); "
                + "$speaker.Dispose();"
        );
        pb.environment().put("GM_TTS_TEXT", normalizarTexto(text));
        pb.environment().put("GM_TTS_VOICE", getVozSeleccionada());
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
            String salida = new String(process.getInputStream().readAllBytes(), charsetConsolaWindows());
            process.waitFor();
            if (process.exitValue() != 0 && !salida.isBlank()) {
                System.err.println("No se pudo reproducir la voz de Windows: " + salida.trim());
            }
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

    private static boolean hayVocesWindowsInstaladas() {
        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "$ErrorActionPreference = 'Stop'; "
                + "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$voice = $speaker.GetInstalledVoices() | Where-Object { $_.Enabled } | Select-Object -First 1; "
                + "$speaker.Dispose(); "
                + "if ($voice) { exit 0 } else { exit 1 }"
        );
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            process.getInputStream().readAllBytes();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static List<String> detectarVocesWindowsEsEs() {
        if (!isWindows()) return List.of();

        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "$ErrorActionPreference = 'Stop'; "
                + "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$speaker.GetInstalledVoices() "
                + "| Where-Object { $_.Enabled -and $_.VoiceInfo.Culture.Name -eq 'es-ES' } "
                + "| Sort-Object "
                + "@{ Expression = { if ($_.VoiceInfo.Name -match 'Natural|Neural|Online') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Gender -eq 'Female') { 0 } else { 1 } } }, "
                + "@{ Expression = { $_.VoiceInfo.Name } } "
                + "| ForEach-Object { $_.VoiceInfo.Name }; "
                + "$speaker.Dispose();"
        );
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            String salida = new String(process.getInputStream().readAllBytes(), charsetConsolaWindows());
            process.waitFor();
            if (process.exitValue() != 0) return List.of();
            return salida.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        } catch (IOException e) {
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private static boolean pareceVozNatural(String voz) {
        String lower = voz.toLowerCase(Locale.ROOT);
        return lower.contains("natural") || lower.contains("neural") || lower.contains("online");
    }

    private static Charset charsetConsolaWindows() {
        return isWindows() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
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
