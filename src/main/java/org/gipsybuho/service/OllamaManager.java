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

    /**
     * Ruta fija de almacenamiento de modelos, fuera del perfil de usuario.
     * Antes se derivaba de %LOCALAPPDATA%, pero en perfiles de Windows con
     * tilde o espacio (ej. "C:\Users\Gipsy Dávy") esa ruta sigue siendo
     * no-ASCII y no evita el bug de llama-server (ver {@link #configurarRutaModelos}).
     * C:\ProgramData es independiente del usuario y solo contiene ASCII.
     */
    private static final Path MODELS_DIR = Path.of("C:", "ProgramData", "GraficasMulberry", "ollama-models");

    private static volatile Process ollamaProcess;
    private static volatile boolean started = false;

    public static void startAsync() {
        Thread.ofVirtual().start(OllamaManager::startIfNeeded);
    }

    /** Ruta de modelos usada por esta app — debe usarse también al hacer {@code ollama pull}. */
    public static String modelsDir() {
        return MODELS_DIR.toString();
    }

    public static void startIfNeeded() {
        if (isRunning()) {
            // Ya hay un servidor Ollama activo (autoarranque de Windows o manual) que no
            // controlamos: puede estar usando la ruta de modelos rota por defecto.
            // Se reinicia bajo nuestro control para garantizar la ruta correcta.
            reiniciarConRutaCorrecta();
            return;
        }
        lanzar();
    }

    private static void reiniciarConRutaCorrecta() {
        detenerProcesosExternos();
        for (int i = 0; i < 20; i++) {
            if (!isRunning()) { lanzar(); return; }
            try { Thread.sleep(250); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.err.println("OllamaManager: no se pudo detener la instancia previa de Ollama, se mantiene como está");
    }

    /** Termina cualquier proceso ollama.exe en ejecución, incluso si no lo iniciamos nosotros. */
    private static void detenerProcesosExternos() {
        ProcessHandle.allProcesses()
            .filter(p -> p.info().command()
                .map(c -> c.toLowerCase().endsWith("ollama.exe"))
                .orElse(false))
            .forEach(ProcessHandle::destroyForcibly);
    }

    private static void lanzar() {
        Path exe = findOllamaExe();
        if (exe == null) return;

        try {
            ProcessBuilder pb = new ProcessBuilder(exe.toString(), "serve");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            configurarRutaModelos(pb);
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

    /**
     * Fija OLLAMA_MODELS a {@link #MODELS_DIR}, sin tildes ni espacios.
     * Evita el bug de llama-server en Windows que falla al leer modelos cuando
     * la ruta contiene caracteres no ASCII (ej. "C:\Users\Gipsy Dávy"),
     * devolviendo HTTP 500 "llama-server process has terminated" en cada consulta.
     * También persiste la variable a nivel de usuario (setx) para que un
     * futuro autoarranque de Ollama por Windows (fuera del control de esta
     * app) herede la ruta correcta tras el próximo inicio de sesión.
     */
    private static void configurarRutaModelos(ProcessBuilder pb) {
        try {
            Files.createDirectories(MODELS_DIR);
            pb.environment().put("OLLAMA_MODELS", MODELS_DIR.toString());
            persistirVariablePermanente();
        } catch (Exception e) {
            System.err.println("OllamaManager: no se pudo preparar la ruta de modelos — " + e.getMessage());
        }
    }

    private static void persistirVariablePermanente() {
        if (MODELS_DIR.toString().equals(System.getenv("OLLAMA_MODELS"))) return;
        try {
            new ProcessBuilder("setx", "OLLAMA_MODELS", MODELS_DIR.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        } catch (Exception ignored) {}
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
