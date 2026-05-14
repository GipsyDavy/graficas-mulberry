package org.gipsybuho.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.service.OllamaManager;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaInstallerDialog extends Stage {

    private static final String INSTALLER_URL    = "https://ollama.com/download/OllamaSetup.exe";
    private static final String DEFAULT_MODEL    = "llama3.2";

    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label       lblProgreso = new Label(" ");
    private final TextArea    logArea     = new TextArea();
    private final Button      btnInstalar = new Button("⬇   Instalar Ollama");
    private final Button      btnCerrar   = new Button("Cerrar");
    private final Label[]     stepIcons   = new Label[4];
    private final VisualAssistantView installerAssistant = new VisualAssistantView(true);

    private volatile boolean instalacionCompleta = false;
    private String ultimoMensajeAsistente = "";
    private long ultimoMensajeAsistenteMs = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public OllamaInstallerDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Instalación de Ollama — Gráficas Mulberry");
        setResizable(false);

        VBox root = buildUI();
        Scene scene = new Scene(root, 640, 620);
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        setScene(scene);
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private VBox buildUI() {
        // Cabecera
        Label titulo = new Label("🤖  Configuración de Ollama");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2D1A28;");

        Label desc = new Label(
            "Ollama permite ejecutar modelos de inteligencia artificial de forma completamente\n" +
            "local y privada. Ningún dato sale de tu equipo.\n\n" +
            "El asistente descargará Ollama y el modelo de lenguaje " + DEFAULT_MODEL + " (~2 GB).");
        desc.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        desc.setWrapText(true);

        boolean animacionesActivas = isInstallerAssistantEnabled();
        installerAssistant.setVisible(animacionesActivas);
        installerAssistant.setManaged(animacionesActivas);

        VBox headerText = new VBox(6, titulo, desc);
        HBox header = new HBox(14, headerText, installerAssistant);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerText, Priority.ALWAYS);

        CheckBox chkAnimaciones = new CheckBox("Mostrar asistente visual durante la instalación");
        chkAnimaciones.setSelected(animacionesActivas);
        chkAnimaciones.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5A72;");
        chkAnimaciones.selectedProperty().addListener((obs, old, selected) -> {
            DatabaseManager.setConfig(VisualAssistantView.KEY_INSTALLER_ANIMATIONS, selected ? "1" : "0");
            installerAssistant.setVisible(selected);
            installerAssistant.setManaged(selected);
            if (selected) {
                installerAssistant.decir("Vuelvo a acompañar la instalación.");
            }
        });

        // Panel de pasos
        VBox stepsPanel = buildStepsPanel();

        // Barra de progreso
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(18);
        progressBar.setStyle("-fx-accent: #6B2D5E;");

        lblProgreso.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5A72;");

        // Log
        Label lblLog = new Label("Registro:");
        lblLog.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5A72; -fx-font-weight: bold;");
        logArea.setEditable(false);
        logArea.setPrefRowCount(7);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px; -fx-control-inner-background: #1E1E1E; -fx-text-fill: #D4D4D4;");
        logArea.setWrapText(true);

        // Botones
        btnInstalar.setStyle(
            "-fx-background-color: #6B2D5E; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 10 28; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 13px;");
        btnCerrar.setStyle("-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");

        btnInstalar.setOnAction(e -> iniciarInstalacion());
        btnCerrar.setOnAction(e -> close());

        HBox botones = new HBox(10, btnInstalar, btnCerrar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(14,
            header, chkAnimaciones,
            new Separator(),
            stepsPanel,
            new Separator(),
            progressBar, lblProgreso,
            lblLog, logArea,
            botones
        );
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FAFAFA;");
        return root;
    }

    private VBox buildStepsPanel() {
        String[] nombres = {
            "Descargar instalador de Ollama",
            "Instalar Ollama en el sistema",
            "Descargar modelo IA  (" + DEFAULT_MODEL + "  ≈ 2 GB)",
            "Verificar funcionamiento"
        };

        VBox box = new VBox(10);
        box.setPadding(new Insets(4, 0, 4, 0));
        for (int i = 0; i < nombres.length; i++) {
            Label ico = new Label("⬜");
            ico.setStyle("-fx-font-size: 16px; -fx-min-width: 24;");
            stepIcons[i] = ico;

            Label num = new Label((i + 1) + ".");
            num.setStyle("-fx-font-size: 13px; -fx-text-fill: #9B59B6; -fx-font-weight: bold; -fx-min-width: 20;");

            Label txt = new Label(nombres[i]);
            txt.setStyle("-fx-text-fill: #444; -fx-font-size: 13px;");

            HBox fila = new HBox(8, ico, num, txt);
            fila.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(fila);
        }
        return box;
    }

    // ── Lógica de instalación ─────────────────────────────────────────────────

    private void iniciarInstalacion() {
        btnInstalar.setDisable(true);
        log("▶ Iniciando proceso de instalación de Ollama...");
        asistente("Preparando instalación de Ollama y sus módulos de IA.");

        Thread.ofVirtual().start(() -> {
            try {
                // PASO 1: Descargar instalador
                setStepPending(0);
                asistente("Descargando el instalador oficial de Ollama.");
                Path installerPath = descargarInstalador();
                setStepOk(0);
                asistente("Instalador descargado. Ahora toca instalar Ollama.");

                // PASO 2: Instalar
                setStepPending(1);
                log("▶ Ejecutando instalador. Sigue las instrucciones en pantalla...");
                Platform.runLater(() -> {
                    progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    lblProgreso.setText("Esperando que el instalador finalice…");
                });
                ejecutarInstalador(installerPath);
                setStepOk(1);
                asistente("Ollama ya está instalado. Voy a iniciar el servicio.");

                // PASO 3: Iniciar servicio
                log("▶ Iniciando servicio Ollama...");
                OllamaManager.startIfNeeded();
                esperarServicio(15);

                // PASO 4: Descargar modelo
                setStepPending(2);
                asistente("Descargando el modelo " + DEFAULT_MODEL + ". Este paso puede tardar bastante.");
                descargarModelo();
                setStepOk(2);
                asistente("Modelo instalado. Estoy verificando que el asistente IA responde.");

                // PASO 5: Verificar
                setStepPending(3);
                log("▶ Verificando conexión con Ollama...");
                boolean ok = esperarServicio(20);
                if (ok) setStepOk(3); else setStepError(3);

                instalacionCompleta = ok;
                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    lblProgreso.setText(ok
                        ? "✅ Instalación completada correctamente"
                        : "⚠  Instalación finalizada — reinicia la app si hay problemas");
                    btnCerrar.setText("Finalizar");
                    btnCerrar.setStyle("-fx-background-color: #6B2D5E; -fx-text-fill: white; " +
                        "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
                    log(ok
                        ? "✅ ¡Ollama listo! Cierra esta ventana y usa el Asistente IA."
                        : "⚠  Si el asistente no responde, reinicia Gráficas Mulberry.");
                    asistente(ok
                        ? "Instalación completada. Ollama y el modelo están listos."
                        : "Instalación finalizada con aviso. Reinicia la aplicación si el asistente IA no responde.");
                });

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    log("❌ Error: " + ex.getMessage());
                    progressBar.setProgress(0);
                    lblProgreso.setText("Error durante la instalación — revisa el registro");
                    btnInstalar.setDisable(false);
                    btnInstalar.setText("🔄  Reintentar");
                    asistente("Error durante la instalación. Revisa el registro y puedes reintentar.");
                });
            }
        });
    }

    // ── Paso 1: Descarga del instalador ──────────────────────────────────────

    private Path descargarInstalador() throws Exception {
        log("  Conectando con ollama.com...");

        // NORMAL: sigue redirecciones pero nunca degrada de HTTPS a HTTP
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        // Obtener tamaño total (HEAD)
        long total = -1;
        try {
            HttpRequest head = HttpRequest.newBuilder()
                .uri(URI.create(INSTALLER_URL))
                .timeout(Duration.ofSeconds(10))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> hr = client.send(head, HttpResponse.BodyHandlers.discarding());
            total = hr.headers().firstValueAsLong("content-length").orElse(-1);
        } catch (Exception ignored) {}

        if (total > 0) {
            log("  Tamaño del instalador: " + (total / 1_048_576) + " MB");
        }

        log("  Descargando OllamaSetup.exe...");
        // Guardar en directorio temporal del sistema; después verificamos que el path no fue manipulado
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        Path dest = Files.createTempFile(tmpDir, "OllamaSetup", ".exe");

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(INSTALLER_URL))
            .timeout(Duration.ofMinutes(10))
            .GET().build();

        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new Exception("El servidor devolvió HTTP " + resp.statusCode() + " al descargar el instalador");
        }

        long[] descargado = {0};
        final long totalFinal = total;
        try (InputStream in = resp.body();
             OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                descargado[0] += n;
                out.write(buf, 0, n);
                final long d = descargado[0];
                Platform.runLater(() -> {
                    if (totalFinal > 0) {
                        double pct = (double) d / totalFinal;
                        progressBar.setProgress(pct);
                        lblProgreso.setText(String.format("Descargando instalador: %.1f / %.1f MB  (%.0f%%)",
                            d / 1_048_576.0, totalFinal / 1_048_576.0, pct * 100));
                        if (pct >= 0.5 && pct < 0.52) {
                            asistente("La descarga del instalador va por la mitad.");
                        }
                    } else {
                        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        lblProgreso.setText(String.format("Descargando instalador: %.1f MB…", d / 1_048_576.0));
                    }
                });
            }
        }

        // Verificar que el archivo resultante tiene un tamaño mínimo razonable (>1 MB)
        long fileSize = Files.size(dest);
        if (fileSize < 1_048_576) {
            Files.deleteIfExists(dest);
            throw new Exception("El archivo descargado es demasiado pequeño (" + fileSize + " bytes) — posible descarga corrupta");
        }

        log("  Descarga completada (" + (fileSize / 1_048_576) + " MB) → " + dest.getFileName());
        return dest;
    }

    // ── Paso 2: Ejecutar instalador ───────────────────────────────────────────

    private void ejecutarInstalador(Path installer) throws Exception {
        Path tmpDir  = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        Path realExe = installer.toRealPath();
        if (!realExe.startsWith(tmpDir)) {
            throw new SecurityException("Ruta del instalador fuera del directorio temporal — ejecución cancelada");
        }
        if (!Files.isRegularFile(realExe)) {
            throw new SecurityException("El instalador no es un archivo regular — ejecución cancelada");
        }

        log("  Se abrirá el instalador de Ollama en una ventana separada.");
        log("  Sigue las instrucciones en pantalla y espera a que finalice.");
        asistente("Sigue el instalador de Ollama en la ventana que se abre.");

        Process proc = new ProcessBuilder(realExe.toString())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();

        long startMs = System.currentTimeMillis();
        boolean ollamaDetectado = false;

        // Monitoriza cada 3 s mientras el instalador sigue en marcha
        while (!proc.waitFor(3, TimeUnit.SECONDS)) {
            long secs = (System.currentTimeMillis() - startMs) / 1000;
            Path ollamaExe = findOllamaExe();
            if (ollamaExe != null && !ollamaDetectado) {
                ollamaDetectado = true;
                log("  ✔ ollama.exe detectado — copiando archivos...");
            }
            final String estado = ollamaDetectado
                ? "Instalando Ollama — copiando archivos... (" + secs + " s)"
                : "Esperando que el instalador de Ollama finalice... (" + secs + " s)";
            final boolean detectadoFinal = ollamaDetectado;
            Platform.runLater(() -> {
                progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                lblProgreso.setText(estado);
                if (detectadoFinal) {
                    asistente("Ollama detectado. Esperando a que termine la copia de archivos.");
                }
            });
        }

        int code = proc.exitValue();
        log("  Instalador finalizado (código de salida: " + code + ")");
    }

    // ── Paso 3: Descargar modelo ──────────────────────────────────────────────

    private static final Pattern PAT_PCT  = Pattern.compile("(\\d+)%");
    private static final Pattern PAT_SIZE = Pattern.compile("([\\d.]+)\\s*(B|KB|MB|GB)/([\\d.]+)\\s*(B|KB|MB|GB)");

    private void descargarModelo() throws Exception {
        log("▶ Descargando modelo " + DEFAULT_MODEL + " (~2 GB). Puede tardar varios minutos...");

        Platform.runLater(() -> {
            progressBar.setProgress(0);
            lblProgreso.setText("Preparando descarga del modelo IA " + DEFAULT_MODEL + "…");
        });

        Path ollamaExe = findOllamaExe();
        if (ollamaExe == null) {
            log("  ⚠ No se encontró ollama.exe. Descarga el modelo manualmente:");
            log("      ollama pull " + DEFAULT_MODEL);
            return;
        }

        Process proc = new ProcessBuilder(ollamaExe.toString(), "pull", DEFAULT_MODEL)
            .redirectErrorStream(true)
            .start();

        // 'ollama pull' usa \r para actualizar la misma línea de progreso.
        // BufferedReader.readLine() trata \r como separador, así que cada
        // actualización llega como una línea independiente.
        // Solo logueamos hitos del 10 % y actualizamos la barra en cada línea.
        String[] lastMilestone = {""};
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                Matcher mPct = PAT_PCT.matcher(trimmed);
                if (mPct.find()) {
                    int pct = Integer.parseInt(mPct.group(1));
                    Matcher mSz = PAT_SIZE.matcher(trimmed);
                    String sizeInfo = mSz.find()
                        ? mSz.group(1) + " " + mSz.group(2) + " / " + mSz.group(3) + " " + mSz.group(4)
                        : "";
                    final int p = pct;
                    final String si = sizeInfo;
                    Platform.runLater(() -> {
                        progressBar.setProgress(p / 100.0);
                        lblProgreso.setText("Descargando modelo: " + p + "%" +
                            (si.isEmpty() ? "" : "  (" + si + ")"));
                        if (p == 25 || p == 50 || p == 75) {
                            asistente("Descarga del modelo al " + p + "%. Continúo esperando.");
                        }
                    });
                    // Log solo cada 10 %
                    String hito = (pct / 10) * 10 + "%";
                    if (!hito.equals(lastMilestone[0])) {
                        lastMilestone[0] = hito;
                        log("  Progreso modelo: " + pct + "%" + (sizeInfo.isEmpty() ? "" : "  " + sizeInfo));
                    }
                } else {
                    // Líneas de estado: "pulling manifest", "verifying sha256", "success", etc.
                    log("  " + trimmed);
                    final String t = trimmed;
                    Platform.runLater(() -> lblProgreso.setText("Modelo " + DEFAULT_MODEL + ": " + t));
                }
            }
        }

        int code = proc.waitFor();
        log("  Descarga del modelo finalizada (código: " + code + ")");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean esperarServicio(int intentos) throws InterruptedException {
        for (int i = 0; i < intentos; i++) {
            if (OllamaManager.isRunning()) return true;
            Thread.sleep(1000);
        }
        return OllamaManager.isRunning();
    }

    private Path findOllamaExe() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null) {
            Path p = Path.of(local, "Programs", "Ollama", "ollama.exe");
            if (Files.exists(p)) return p;
        }
        String pf = System.getenv("ProgramFiles");
        if (pf != null) {
            Path p = Path.of(pf, "Ollama", "ollama.exe");
            if (Files.exists(p)) return p;
        }
        return null;
    }

    private boolean isInstallerAssistantEnabled() {
        return !"0".equals(DatabaseManager.getConfig(VisualAssistantView.KEY_INSTALLER_ANIMATIONS));
    }

    private void setStepPending(int i) {
        Platform.runLater(() -> stepIcons[i].setText("⏳"));
    }

    private void setStepOk(int i) {
        Platform.runLater(() -> stepIcons[i].setText("✅"));
    }

    private void setStepError(int i) {
        Platform.runLater(() -> stepIcons[i].setText("⚠️"));
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void asistente(String mensaje) {
        Platform.runLater(() -> {
            if (isInstallerAssistantEnabled()) {
                long ahora = System.currentTimeMillis();
                if (mensaje.equals(ultimoMensajeAsistente) && ahora - ultimoMensajeAsistenteMs < 10_000) {
                    return;
                }
                ultimoMensajeAsistente = mensaje;
                ultimoMensajeAsistenteMs = ahora;
                installerAssistant.decir(mensaje);
            }
        });
    }

    public boolean isInstalacionCompleta() { return instalacionCompleta; }
}
