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

public class OllamaService {

    // Recuperamos el SYSTEM_PROMPT original que no estaba en AppConstants
    private static final String SYSTEM_PROMPT = """
        Eres Mulberry Assistant, el asistente IA especializado del ERP Gráficas Mulberry.
        Tu objetivo es ayudar al usuario con consultas sobre el sistema, datos y procesos.
        Eres educado, profesional y eficiente.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String modeloActual;
    private boolean routingAutomatico = true;
    private volatile String contextoERP = null;

    private final List<String[]> historial = new ArrayList<>();
    private static final int MAX_HISTORIAL = 10;

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        // Usamos el modelo por defecto de las constantes
        this.modeloActual = "phi3:mini";
    }

    public void chatStreaming(String prompt, Consumer<String> onChunk, Runnable onComplete, Consumer<String> onError, Consumer<String> onModelo) {
        Thread.ofVirtual().start(() -> {
            try {
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("model", modeloActual);
                rootNode.put("prompt", construirPromptConContexto(prompt));
                rootNode.put("stream", true);

                // Usamos las URLs de AppConstants
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AppConstants.OLLAMA_API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(rootNode.toString()))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    StringBuilder respuestaCompleta = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            JsonNode node = mapper.readTree(line);
                            if (node.has("response")) {
                                String chunk = node.get("response").asText();
                                respuestaCompleta.append(chunk);
                                onChunk.accept(chunk);
                            }
                            if (node.has("model")) {
                                onModelo.accept(node.get("model").asText());
                            }
                        }
                    }
                    guardarEnHistorial(prompt, respuestaCompleta.toString());
                    onComplete.run();
                } else {
                    Platform.runLater(() -> onError.accept("Error Ollama: " + response.statusCode()));
                }
            } catch (InterruptedException e) {
                // CORRECCIÓN SONARQUBE
                Thread.currentThread().interrupt();
                Platform.runLater(() -> onError.accept("Hilo interrumpido"));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    private String construirPromptConContexto(String prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("System: ").append(SYSTEM_PROMPT).append("\n");
        if (contextoERP != null && !contextoERP.isEmpty()) {
            sb.append("Contexto ERP: ").append(contextoERP).append("\n");
        }
        synchronized (historial) {
            for (String[] par : historial) {
                sb.append("Usuario: ").append(par[0]).append("\n");
                sb.append("Asistente: ").append(par[1]).append("\n");
            }
        }
        sb.append("Usuario: ").append(prompt).append("\nAsistente: ");
        return sb.toString();
    }

    private void guardarEnHistorial(String user, String assistant) {
        synchronized (historial) {
            if (historial.size() >= MAX_HISTORIAL) historial.remove(0);
            historial.add(new String[]{user, assistant});
        }
    }

    public boolean verificarConexion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.OLLAMA_BASE_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getModelosDisponibles() {
        List<String> modelos = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.OLLAMA_BASE_URL + "/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if (root.has("models")) {
                    root.get("models").forEach(m -> modelos.add(m.get("name").asText()));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Error silencioso para no bloquear la UI
        }
        return modelos;
    }

    public void setModeloActual(String modelo) { this.modeloActual = modelo; this.routingAutomatico = false; }
    public void habilitarRouting()  { this.routingAutomatico = true; }
    public boolean isRoutingAutomatico() { return routingAutomatico; }
    public String getModeloActual() { return modeloActual; }
    public void setContextoERP(String contexto)  { this.contextoERP = contexto; }
    public void clearContextoERP()               { this.contextoERP = null; }
    public void limpiarHistorial()               { synchronized (historial) { historial.clear(); } }
}