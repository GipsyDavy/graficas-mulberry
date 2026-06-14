package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import org.gipsybuho.util.AppConstants;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * OllamaService: Motor de IA de Gráficas Mulberry.
 * Versión Final: 100% Desacoplada, compatible con ModelosGestionDialog e IAView.
 */
public class OllamaService {

    /**
     * Estructura de datos para modelos instalados.
     * Diseñada para que coincida con el acceso de campo (.nombre) y método (.tamano()) de la UI.
     */
    public static class ModelInfo {
        public final String nombre;
        private final String tamano;

        public ModelInfo(String nombre, String tamano) {
            this.nombre = nombre;
            this.tamano = tamano;
        }
        public String tamano() { return tamano; }
    }

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String modeloActual;
    private volatile String contextoERP = null;
    private final List<String[]> historial = new ArrayList<>();
    private static final int MAX_HISTORIAL = 10;
    private static final int MAX_USER_PROMPT_CHARS = 8_000;
    private static final int MAX_CONTEXT_CHARS = 20_000;
    private static final int MAX_HISTORY_USER_CHARS = 2_000;
    private static final int MAX_RESPONSE_HISTORY_CHARS = 2_000;
    private static final int MAX_TOTAL_PROMPT_CHARS = 60_000;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.modeloActual = AppConstants.OLLAMA_MODEL;
    }

    // =========================================================================
    // GESTIÓN DE MODELOS (API /tags, /pull, /delete)
    // =========================================================================

    /**
     * Obtiene los modelos instalados. Coincide con la llamada en ModelosGestionDialog.
     */
    public List<ModelInfo> getModelosConDetalles() {
        List<ModelInfo> lista = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.OLLAMA_BASE_URL + "/api/tags"))
                    .timeout(Duration.ofSeconds(15))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if (root.has("models")) {
                    root.get("models").forEach(m -> {
                        String name = m.path("name").asText("");
                        long sizeBytes = m.has("size") ? m.get("size").asLong() : 0;
                        String sizeStr = String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
                        lista.add(new ModelInfo(name, sizeStr));
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error en comunicación con Ollama: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Elimina un modelo del servidor local.
     */
    public boolean eliminarModelo(String nombre) {
        try {
            ObjectNode payload = mapper.createObjectNode();
            payload.put("name", nombre);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.OLLAMA_BASE_URL + "/api/delete"))
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    /**
     * Descarga modelos con streaming de progreso.
     */
    public void pullModeloStreaming(String nombre, Consumer<String> onEstado, Consumer<double[]> onProgreso, Runnable onExito, Consumer<String> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                ObjectNode payload = mapper.createObjectNode();
                payload.put("name", nombre);
                payload.put("stream", true);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AppConstants.OLLAMA_BASE_URL + "/api/pull"))
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        JsonNode node = mapper.readTree(line);
                        if (node.has("status")) {
                            String s = node.get("status").asText();
                            Platform.runLater(() -> onEstado.accept(s));
                        }
                        if (node.has("completed") && node.has("total")) {
                            double c = node.get("completed").asDouble();
                            double t = node.get("total").asDouble();
                            Platform.runLater(() -> onProgreso.accept(new double[]{t > 0 ? c/t : 0, c, t}));
                        }
                    }
                    Platform.runLater(onExito);
                }
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    // =========================================================================
    // LÓGICA DE CHAT (Streaming y Contexto)
    // =========================================================================

    public void enviarConsulta(String prompt, Consumer<String> onResponse, Consumer<String> onError) {
        enviarConsulta(prompt, onResponse, onError, null);
    }

    public void enviarConsulta(String prompt, Consumer<String> onResponse, Consumer<String> onError, Runnable onComplete) {
        Thread.ofVirtual().start(() -> {
            try {
                if (prompt == null || prompt.isBlank()) {
                    Platform.runLater(() -> onError.accept("La consulta está vacía."));
                    return;
                }
                if (prompt.length() > MAX_USER_PROMPT_CHARS) {
                    Platform.runLater(() -> onError.accept(
                        "La consulta supera el límite de " + MAX_USER_PROMPT_CHARS +
                        " caracteres. Reduce el texto y vuelve a intentarlo."));
                    return;
                }

                ObjectNode payload = mapper.createObjectNode();
                payload.put("model", modeloActual);
                payload.put("stream", true);

                // Centralizamos el prompt de sistema en AppConstants como sugeriste
                StringBuilder fullPrompt = new StringBuilder(AppConstants.SYSTEM_PROMPT_IA);

                String ctx = contextoERP;
                if (ctx != null) {
                    fullPrompt.append("\n[CONTEXTO ERP]\n").append(limitarTexto(ctx, MAX_CONTEXT_CHARS));
                }

                synchronized (historial) {
                    for (String[] msg : historial) {
                        fullPrompt.append("\nUsuario: ").append(limitarTexto(msg[0], MAX_HISTORY_USER_CHARS))
                            .append("\nAsistente: ").append(limitarTexto(msg[1], MAX_RESPONSE_HISTORY_CHARS));
                    }
                }
                fullPrompt.append("\nUsuario: ").append(prompt);
                payload.put("prompt", limitarTexto(fullPrompt.toString(), MAX_TOTAL_PROMPT_CHARS));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AppConstants.OLLAMA_API_URL))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    String msg = switch (response.statusCode()) {
                        case 404 -> "Modelo '" + modeloActual + "' no instalado. Instálalo desde Gestión de modelos.";
                        case 500 -> "Ollama encontró un error interno. Reinicia Ollama e inténtalo de nuevo.";
                        default  -> "Error de comunicación con Ollama (código " + response.statusCode() + ").";
                    };
                    Platform.runLater(() -> onError.accept(msg));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    StringBuilder fullAiResponse = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        JsonNode node = mapper.readTree(line);
                        if (node.has("response")) {
                            String fragment = node.get("response").asText();
                            fullAiResponse.append(fragment);
                            Platform.runLater(() -> onResponse.accept(fragment));
                        }
                    }
                    synchronized (historial) {
                        if (historial.size() >= MAX_HISTORIAL) historial.remove(0);
                        String respTruncada = limitarTexto(fullAiResponse.toString(), MAX_RESPONSE_HISTORY_CHARS);
                        historial.add(new String[]{prompt, respTruncada});
                    }
                    if (onComplete != null) {
                        Platform.runLater(onComplete);
                    }
                }
            } catch (Exception e) {
                String msg = e.getMessage();
                String friendly = (msg != null && (msg.contains("Connection refused") || msg.contains("ConnectException")))
                    ? "Ollama no está en ejecución. Ábrelo o instálalo con el botón 'Instalar Ollama'."
                    : (msg != null && msg.contains("timed out"))
                        ? "Tiempo de espera agotado. Ollama tardó demasiado en responder."
                        : "Error al conectar con Ollama: " + msg;
                Platform.runLater(() -> onError.accept(friendly));
            }
        });
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    public void setModeloActual(String m) { this.modeloActual = m; }
    public String getModeloActual() { return modeloActual; }
    public void setContextoERP(String c) { this.contextoERP = c; }
    public void limpiarHistorial() { synchronized(historial) { historial.clear(); } }

    private static String limitarTexto(String texto, int maxChars) {
        if (texto == null || texto.length() <= maxChars) return texto;
        String marca = "\n[Contenido recortado por límite de seguridad local]";
        int limite = Math.max(0, maxChars - marca.length());
        return texto.substring(0, limite) + marca;
    }
}
