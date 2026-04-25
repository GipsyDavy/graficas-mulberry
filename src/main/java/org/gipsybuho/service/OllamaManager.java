package org.gipsybuho.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el ciclo de vida del proceso Ollama.
 * Busca el ejecutable en las rutas estándar de instalación y lo arranca
 * automáticamente si no está ya en ejecución.
 */
public class OllamaManager {

    private static volatile Process ollamaProcess;
    private static volatile boolean started = false;

    public static void startAsync() {
        Thread.ofVirtual().start(OllamaManager::startIfNeeded);
    }

    public static void startIfNeeded() {
        if (isRunning()) { started = true; return; }

        Path exe = findOllamaExe();
        if (exe == null) return;

        try {
            ProcessBuilder pb = new ProcessBuilder(exe.toString(), "serve");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            ollamaProcess = pb.start();

            // Esperar hasta 8 s a que el servidor esté listo
            for (int i = 0; i < 16; i++) {
                Thread.sleep(500);
                if (isRunning()) { started = true; return; }
            }
        } catch (Exception e) {
            System.err.println("OllamaManager: no se pudo arrancar Ollama — " + e.getMessage());
        }
    }

    public static void stop() {
        if (ollamaProcess != null && ollamaProcess.isAlive()) {
            ollamaProcess.destroyForcibly();
            ollamaProcess = null;
        }
    }

    public static boolean isRunning() {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
            c.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .timeout(Duration.ofSeconds(1)).GET().build(),
                   HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInstalled() {
        return findOllamaExe() != null;
    }

    // ── Búsqueda del ejecutable ───────────────────────────────────────────────

    private static Path findOllamaExe() {
        for (Path p : candidatePaths()) {
            if (Files.exists(p)) return p;
        }
        return null;
    }

    private static List<Path> candidatePaths() {
        List<Path> paths = new ArrayList<>();

        // 1. Ollama bundleado junto a la app-image (app/ollama/ollama.exe)
        try {
            Path installDir = Path.of(System.getProperty("java.home")).getParent();
            paths.add(installDir.resolve("app/ollama/ollama.exe"));
        } catch (Exception ignored) {}

        // 2. Instalación estándar por usuario (%LOCALAPPDATA%)
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            paths.add(Path.of(localAppData, "Programs", "Ollama", "ollama.exe"));
        }

        // 3. Instalación global (%ProgramFiles%)
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) {
            paths.add(Path.of(programFiles, "Ollama", "ollama.exe"));
        }

        // 4. Directorio actual (distribución portátil)
        paths.add(Path.of("ollama.exe"));

        return paths;
    }
}
