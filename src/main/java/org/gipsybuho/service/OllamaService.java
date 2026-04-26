package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

public class OllamaService {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3.2";
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String modeloActual;

    private static final String SYSTEM_PROMPT = """
        Eres el asistente de IA de Gráficas Mulberry, una empresa de serigrafía y artes gráficas
        ubicada en Almería, España. Ayudas al equipo con:
        - Presupuestos y precios de trabajos de serigrafía, DTF, bordado, vinilo y sublimación
        - Gestión de materiales y stock
        - Análisis de costes y rentabilidad
        - Nóminas y gestión de empleados
        - Optimización de procesos productivos
        Responde siempre en español, de forma clara y profesional.
        """;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.mapper = new ObjectMapper();
        this.modeloActual = DEFAULT_MODEL;
    }

    public boolean isDisponible() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public java.util.List<String> getModelosDisponibles() {
        java.util.List<String> modelos = new java.util.ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode models = root.get("models");
            if (models != null && models.isArray()) {
                for (JsonNode m : models) {
                    modelos.add(m.get("name").asText());
                }
            }
        } catch (Exception ignored) {}
        return modelos;
    }

    public void chatStreaming(String userMessage, Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                ObjectNode body = mapper.createObjectNode();
                body.put("model", modeloActual);
                body.put("stream", true);

                var messages = mapper.createArrayNode();
                var sysMsg = mapper.createObjectNode();
                sysMsg.put("role", "system");
                sysMsg.put("content", SYSTEM_PROMPT);
                messages.add(sysMsg);
                var userMsg = mapper.createObjectNode();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                messages.add(userMsg);
                body.set("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        JsonNode node = mapper.readTree(line);
                        JsonNode messageNode = node.get("message");
                        if (messageNode != null) {
                            String content = messageNode.get("content").asText();
                            if (!content.isEmpty()) {
                                javafx.application.Platform.runLater(() -> onChunk.accept(content));
                            }
                        }
                        if (node.has("done") && node.get("done").asBoolean()) break;
                    }
                }
                javafx.application.Platform.runLater(onComplete);

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    onError.accept("Error de conexión con Ollama: " + e.getMessage()));
            }
        });
    }

    public String chat(String userMessage) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", modeloActual);
            body.put("stream", false);

            var messages = mapper.createArrayNode();
            var sysMsg = mapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", SYSTEM_PROMPT);
            messages.add(sysMsg);
            var userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            body.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode msg = root.get("message");
            if (msg == null || msg.get("content") == null) return "";
            return msg.get("content").asText();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public List<ModelInfo> getModelosConDetalles() {
        List<ModelInfo> lista = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(resp.body());
            JsonNode models = root.get("models");
            if (models != null && models.isArray()) {
                for (JsonNode m : models) {
                    String nombre = m.get("name").asText();
                    long size = m.has("size") ? m.get("size").asLong() : 0;
                    lista.add(new ModelInfo(nombre, size));
                }
            }
        } catch (Exception ignored) {}
        return lista;
    }

    public void eliminarModelo(String nombre) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", nombre);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/delete"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .method("DELETE", HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new Exception("HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    public void pullModeloStreaming(String nombre, Consumer<String> onEstado,
            Consumer<double[]> onProgreso, Runnable onComplete, Consumer<String> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                ObjectNode body = mapper.createObjectNode();
                body.put("name", nombre);
                body.put("stream", true);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/pull"))
                    .timeout(Duration.ofMinutes(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

                try (BufferedReader br = new BufferedReader(new InputStreamReader(resp.body()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.isBlank()) continue;
                        JsonNode node = mapper.readTree(line);
                        String estado = node.has("status") ? node.get("status").asText() : "";
                        long total     = node.has("total")     ? node.get("total").asLong()     : 0;
                        long completado = node.has("completed") ? node.get("completed").asLong() : 0;

                        final String est = estado;
                        final long tot = total, comp = completado;
                        javafx.application.Platform.runLater(() -> {
                            onEstado.accept(est);
                            if (tot > 0) onProgreso.accept(new double[]{(double) comp / tot, comp, tot});
                        });

                        if ("success".equals(estado)) break;
                    }
                }
                javafx.application.Platform.runLater(onComplete);
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    public void setModeloActual(String modelo) { this.modeloActual = modelo; }
    public String getModeloActual() { return modeloActual; }

    // ── ModelInfo ─────────────────────────────────────────────────────────────

    public static class ModelInfo {
        public final String nombre;
        public final long bytes;

        public ModelInfo(String nombre, long bytes) {
            this.nombre = nombre;
            this.bytes  = bytes;
        }

        public String tamano() {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024L * 1024 * 1024) return String.format("%.0f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }

        @Override public String toString() { return nombre; }
    }
}
