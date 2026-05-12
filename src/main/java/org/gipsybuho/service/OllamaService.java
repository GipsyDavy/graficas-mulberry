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
 * Versión de COMPATIBILIDAD TOTAL con ModelosGestionDialog.java
 */
public class OllamaService {

    /**
     * Clase interna ModelInfo.
     * IMPORTANTE: Tu diálogo usa 'modelo.nombre' y 'modelo.tamano()'.
     * Para que funcione tanto como variable (.nombre) como método (.tamano()),
     * definimos esta clase estándar.
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

    private static final String SYSTEM_PROMPT = """
        Eres Mulberry Assistant, el asistente IA especializado del ERP Gráficas Mulberry.
        Tu objetivo es ayudar al usuario con consultas sobre el sistema, datos y procesos.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String modeloActual;
    private volatile String contextoERP = null;
    private final List<String[]> historial = new ArrayList<>();

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.modeloActual = AppConstants.OLLAMA_MODEL;
    }

    /**
     * MÉTODO CLAVE: getModelosConDetalles
     * Coincide exactamente con la llamada en tu ModelosGestionDialog.
     */
    public List<ModelInfo> getModelosConDetalles() {
        List<ModelInfo> lista = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConstants.OLLAMA_BASE_URL + "/api/tags"))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if (root.has("models")) {
                    root.get("models").forEach(m -> {
                        String name = m.get("name").asText();
                        long sizeBytes = m.has("size") ? m.get("size").asLong() : 0;
                        String sizeStr = String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
                        lista.add(new ModelInfo(name, sizeStr));
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error listando modelos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * MÉTODO CLAVE: eliminarModelo
     * Coincide con la llamada de confirmación de borrado en tu Dialog.
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
     * MÉTODO CLAVE: pullModeloStreaming
     * Gestiona la descarga y envía el array [progreso, completado, total]
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
            } catch (Exception e) { Platform.runLater(() -> onError.accept(e.getMessage())); }
        });
    }

    /**
     * Envío de chat para IAView
     */
    public void enviarConsulta(String prompt, Consumer<String> onResponse, Consumer<String> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                ObjectNode payload = mapper.createObjectNode();
                payload.put("model", modeloActual);
                payload.put("stream", true);

                StringBuilder fullPrompt = new StringBuilder(SYSTEM_PROMPT);
                if (contextoERP != null) fullPrompt.append("\nContexto: ").append(contextoERP);
                fullPrompt.append("\nUsuario: ").append(prompt);
                payload.put("prompt", fullPrompt.toString());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AppConstants.OLLAMA_API_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        JsonNode node = mapper.readTree(line);
                        if (node.has("response")) {
                            String fragment = node.get("response").asText();
                            Platform.runLater(() -> onResponse.accept(fragment));
                        }
                    }
                }
            } catch (Exception e) { Platform.runLater(() -> onError.accept(e.getMessage())); }
        });
    }

    // Getters y Setters necesarios
    public void setModeloActual(String m) { this.modeloActual = m; }
    public String getModeloActual() { return modeloActual; }
    public void setContextoERP(String c) { this.contextoERP = c; }
    public void limpiarHistorial() { historial.clear(); }
}