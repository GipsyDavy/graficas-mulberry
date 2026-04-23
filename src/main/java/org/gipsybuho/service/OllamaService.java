package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
            return root.get("message").get("content").asText();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public void setModeloActual(String modelo) { this.modeloActual = modelo; }
    public String getModeloActual() { return modeloActual; }
}
