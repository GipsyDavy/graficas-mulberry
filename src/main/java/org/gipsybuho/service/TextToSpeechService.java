package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class TextToSpeechService {

    public static final String KEY_VOZ_TTS = "tts_voz";
    public static final String VOZ_FEMENINA = "Español femenina";
    public static final String VOZ_MASCULINA = "Español masculina";
    public static final String VOZ_WINDOWS_FEMENINA = "Windows femenina (recomendada)";
    private static final String WINDOWS_PREFIX = "Windows: ";
    private static final Duration VOICE_LIST_CACHE_TTL = Duration.ofSeconds(30);

    private static Process currentProcess;
    private static Clip currentClip;
    private static Thread currentThread;
    private static List<WindowsVoice> cachedWindowsVoices;
    private static long cachedWindowsVoicesAt;

    private TextToSpeechService() {}

    public static List<String> nombresVoces() {
        List<String> voces = new ArrayList<>();
        if (vozWindowsFemeninaPreferida().isPresent()) {
            voces.add(VOZ_WINDOWS_FEMENINA);
        }
        for (WindowsVoice voice : windowsVoices()) {
            voces.add(WINDOWS_PREFIX + voice.name());
        }
        voces.add(VOZ_FEMENINA);
        voces.add(VOZ_MASCULINA);
        return List.copyOf(voces);
    }

    public static String getVozSeleccionada() {
        String voz = DatabaseManager.getConfig(KEY_VOZ_TTS);
        if (voz == null || voz.isBlank() || VOZ_FEMENINA.equals(voz)) {
            return vozWindowsFemeninaPreferida().map(v -> VOZ_WINDOWS_FEMENINA).orElse(VOZ_FEMENINA);
        }
        if (nombresVoces().contains(voz)) return voz;
        return vozWindowsFemeninaPreferida().map(v -> VOZ_WINDOWS_FEMENINA).orElse(VOZ_FEMENINA);
    }

    public static void setVozSeleccionada(String voz) {
        if (nombresVoces().contains(voz)) {
            DatabaseManager.setConfig(KEY_VOZ_TTS, voz);
        }
    }

    public static synchronized void speak(String text) {
        if (text == null || text.isBlank()) return;

        stop();

        currentThread = Thread.ofVirtual().start(() -> {
            String selectedVoice = getVozSeleccionada();
            boolean usarWindows = isWindowsVoice(selectedVoice);
            if (usarWindows || !speakWithPiper(normalizarTexto(text), selectedVoice)) {
                speakWithWindows(text, selectedVoice);
            }
        });
    }

    public static synchronized void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
        }
        currentProcess = null;

        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }

        if (currentThread != null) {
            currentThread.interrupt();
            currentThread = null;
        }
    }

    private static boolean speakWithPiper(String text, String selectedVoice) {
        Path piperDir = findPiperDir();
        if (piperDir == null) return false;

        VoiceFiles voice = voiceFiles(piperDir, selectedVoice);
        if (!Files.isRegularFile(voice.model()) || !Files.isRegularFile(voice.config())) {
            return false;
        }

        Path wav = null;
        try {
            wav = Files.createTempFile("gm-tts-", ".wav");
            ProcessBuilder pb = new ProcessBuilder(
                piperDir.resolve("piper.exe").toString(),
                "--model", voice.model().toString(),
                "--config", voice.config().toString(),
                "--espeak_data", piperDir.toString(),
                "--output_file", wav.toString()
            );
            pb.directory(piperDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            synchronized (TextToSpeechService.class) {
                currentProcess = process;
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(text);
                writer.write(System.lineSeparator());
            }

            int exitCode = process.waitFor();
            synchronized (TextToSpeechService.class) {
                if (currentProcess == process) currentProcess = null;
            }

            if (exitCode != 0 || Files.size(wav) == 0) return false;

            playWav(wav);
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo usar la voz Piper del asistente IA: " + e.getMessage());
            return false;
        } finally {
            if (wav != null) {
                try {
                    Files.deleteIfExists(wav);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void playWav(Path wav) throws Exception {
        Clip clip = AudioSystem.getClip();
        synchronized (TextToSpeechService.class) {
            currentClip = clip;
        }

        try (var audio = AudioSystem.getAudioInputStream(wav.toFile())) {
            clip.open(audio);
            clip.start();
            while (!Thread.currentThread().isInterrupted()
                && clip.isOpen()
                && clip.getFramePosition() < clip.getFrameLength()) {
                Thread.sleep(80);
            }
        } finally {
            clip.stop();
            clip.close();
            synchronized (TextToSpeechService.class) {
                if (currentClip == clip) currentClip = null;
            }
        }
    }

    private static Path findPiperDir() {
        for (Path candidate : piperCandidates()) {
            if (Files.isRegularFile(candidate.resolve("piper.exe"))) {
                return candidate;
            }
        }
        return null;
    }

    private static List<Path> piperCandidates() {
        Path userDir = Path.of(System.getProperty("user.dir", "."));
        Path codeDir = codeSourceDir();
        return List.of(
            userDir.resolve("installer").resolve("tts").resolve("piper"),
            userDir.resolve("tts").resolve("piper"),
            codeDir.resolve("tts").resolve("piper"),
            codeDir.getParent() != null ? codeDir.getParent().resolve("app").resolve("tts").resolve("piper") : codeDir
        );
    }

    private static Path codeSourceDir() {
        try {
            return Path.of(TextToSpeechService.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).getParent();
        } catch (URISyntaxException | NullPointerException e) {
            return Path.of(System.getProperty("user.dir", "."));
        }
    }

    private static VoiceFiles voiceFiles(Path piperDir, String selectedVoice) {
        String baseName = VOZ_MASCULINA.equals(selectedVoice)
            ? "es_ES-davefx-medium.onnx"
            : "es_ES-mls_10246-low.onnx";
        Path model = piperDir.resolve("voices").resolve(baseName);
        return new VoiceFiles(model, Path.of(model + ".json"));
    }

    private static void speakWithWindows(String text, String selectedVoice) {
        String preferredVoice = windowsVoiceName(selectedVoice)
            .orElseGet(() -> vozWindowsFemeninaPreferida()
                .flatMap(TextToSpeechService::windowsVoiceName)
                .orElse(VOZ_MASCULINA.equals(selectedVoice) ? "Pablo" : "Helena"));
        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$preferred = $env:GM_TTS_PREFERRED; "
                + "$voice = $speaker.GetInstalledVoices() "
                + "| Where-Object { $_.Enabled } "
                + "| Sort-Object @{ Expression = { if ($_.VoiceInfo.Name -eq $preferred) { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Culture.Name -eq 'es-ES') { 0 } else { 1 } } }, "
                + "@{ Expression = { if ($_.VoiceInfo.Gender -eq 'Female') { 0 } else { 1 } } }, "
                + "@{ Expression = { $_.VoiceInfo.Name } } "
                + "| Select-Object -First 1; "
                + "if ($voice) { $speaker.SelectVoice($voice.VoiceInfo.Name); } "
                + "$speaker.Rate = -1; "
                + "$speaker.Volume = 90; "
                + "$speaker.Speak($env:GM_TTS_TEXT);"
        );
        pb.environment().put("GM_TTS_TEXT", normalizarTexto(text));
        pb.environment().put("GM_TTS_PREFERRED", preferredVoice);
        pb.redirectErrorStream(true);

        try {
            currentProcess = pb.start();
            currentProcess.waitFor();
        } catch (IOException e) {
            currentProcess = null;
            System.err.println("No se pudo iniciar la voz del asistente IA: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (currentProcess != null && currentProcess.isAlive()) {
                currentProcess.destroy();
            }
        } finally {
            currentProcess = null;
        }
    }

    private static java.util.Optional<String> windowsVoiceName(String selectedVoice) {
        if (VOZ_WINDOWS_FEMENINA.equals(selectedVoice)) {
            return vozWindowsFemeninaPreferida().flatMap(TextToSpeechService::windowsVoiceName);
        }
        if (selectedVoice == null || !selectedVoice.startsWith(WINDOWS_PREFIX)) {
            return java.util.Optional.empty();
        }
        String name = selectedVoice.substring(WINDOWS_PREFIX.length()).trim();
        return name.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(name);
    }

    private static boolean isWindowsVoice(String selectedVoice) {
        return VOZ_WINDOWS_FEMENINA.equals(selectedVoice)
            || (selectedVoice != null && selectedVoice.startsWith(WINDOWS_PREFIX));
    }

    private static java.util.Optional<String> vozWindowsFemeninaPreferida() {
        return windowsVoices().stream()
            .filter(voice -> voice.culture().equalsIgnoreCase("es-ES"))
            .filter(WindowsVoice::female)
            .sorted(Comparator.comparingInt(TextToSpeechService::naturalVoiceRank)
                .thenComparing(WindowsVoice::name))
            .map(voice -> WINDOWS_PREFIX + voice.name())
            .findFirst();
    }

    private static int naturalVoiceRank(WindowsVoice voice) {
        String name = voice.name().toLowerCase(Locale.ROOT);
        if (name.contains("natural") || name.contains("online") || name.contains("neural")) return 0;
        if (name.contains("helena") || name.contains("elvira")) return 1;
        return 2;
    }

    private static List<WindowsVoice> windowsVoices() {
        long now = System.currentTimeMillis();
        if (cachedWindowsVoices != null && now - cachedWindowsVoicesAt < VOICE_LIST_CACHE_TTL.toMillis()) {
            return cachedWindowsVoices;
        }

        List<WindowsVoice> voices = loadWindowsVoices();
        cachedWindowsVoices = voices;
        cachedWindowsVoicesAt = now;
        return voices;
    }

    private static List<WindowsVoice> loadWindowsVoices() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            return List.of();
        }

        ProcessBuilder pb = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$speaker.GetInstalledVoices() | Where-Object { $_.Enabled } | ForEach-Object { "
                + "$_.VoiceInfo.Name + \"`t\" + $_.VoiceInfo.Culture.Name + \"`t\" + $_.VoiceInfo.Gender "
                + "}"
        );
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return List.of();
            }
            if (process.exitValue() != 0) return List.of();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<WindowsVoice> voices = new ArrayList<>();
            for (String line : output.split("\\R")) {
                String[] parts = line.split("\\t", -1);
                if (parts.length < 3 || parts[0].isBlank()) continue;
                voices.add(new WindowsVoice(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim().equalsIgnoreCase("Female")
                ));
            }
            voices.sort(Comparator
                .comparing((WindowsVoice voice) -> !voice.culture().equalsIgnoreCase("es-ES"))
                .thenComparing(voice -> !voice.female())
                .thenComparingInt(TextToSpeechService::naturalVoiceRank)
                .thenComparing(WindowsVoice::name));
            return List.copyOf(voices);
        } catch (IOException e) {
            System.err.println("No se pudieron leer las voces de Windows: " + e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private static String normalizarTexto(String text) {
        return text
            .replaceAll("(?s)```.*?```", " bloque de código ")
            .replaceAll("[#*_`>\\[\\](){}|]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private record VoiceFiles(Path model, Path config) {}
    private record WindowsVoice(String name, String culture, boolean female) {}
}
