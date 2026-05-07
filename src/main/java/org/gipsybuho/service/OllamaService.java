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
    private static final String DEFAULT_MODEL = "phi3:mini"; // Modelo ligero y eficiente para 16GB RAM

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String modeloActual;
    private boolean routingAutomatico = true;
    private volatile String contextoERP = null;

    // Historial de conversación (pares user/assistant de los últimos 5 intercambios)
    private final List<String[]> historial = new ArrayList<>();
    private static final int MAX_HISTORIAL = 10; // 5 intercambios = 10 entradas

    private static final String SYSTEM_PROMPT = """
        Eres Mulberry Assistant, el asistente IA senior especializado del ERP Gráficas Mulberry.\s
        Trabajas directamente dentro de la vista "Asistente IA Local".

        MISIÓN PRINCIPAL:
        Eres la consola operativa completa del ERP. Puedes CREAR, EDITAR, ELIMINAR, LISTAR y\s
        CONSULTAR cualquier registro en todos los módulos, y también ABRIR o CERRAR módulos\s
        del ERP en ventanas emergentes, todo mediante lenguaje natural + JSON estructurado.

        MÓDULOS DEL ERP:
        clientes · presupuestos · facturas · albaranes · pedidos · tarifas · materiales ·\s
        empleados · nominas · estadisticas · calendario · configuración · importar · exportar

        REGLAS DE ORO:
        - Nunca ejecutes ninguna acción sin la confirmación explícita del usuario.
        - Si faltan datos necesarios (sobre todo el 'id' para editar/borrar), pregúntalos antes de generar el JSON.
        - Para editar o borrar SIEMPRE necesitas el 'id' numérico del registro.
        - Para listar, puedes filtrar por 'estado', 'tecnica' o 'categoria' en el campo correspondiente.
        - Sé profesional y orientado a resultados del sector de artes gráficas (Almería, España).

        ─────────────────────────────────────────────────────────────────────────────
        FORMATO DE RESPUESTA: texto natural + bloque JSON AL FINAL del mensaje.

        {
          "action": "<acción>",
          "data": { <campos según la acción> },
          "preview_summary": "Resumen legible para el panel de confirmación"
        }

        ── ACCIONES DISPONIBLES ─────────────────────────────────────────────────────

        CLIENTES:
          crear_cliente      → cliente_nombre*, apellidos, nif, email, telefono, ciudad, tipo, observaciones
          editar_cliente     → id* (o cliente_nombre para buscarlo), + campos a cambiar
          borrar_cliente     → id*
          listar_clientes    → (opcional) cliente_nombre para filtrar
          consultar_cliente  → cliente_nombre*

        PRESUPUESTOS:
          crear_presupuesto  → cliente_nombre, items[], observaciones, fecha
          editar_presupuesto → id*, estado, cliente_nombre, observaciones, iva_porcentaje, fecha
          borrar_presupuesto → id*
          listar_presupuestos→ (opcional) estado para filtrar (borrador|aceptado|rechazado|facturado)

        FACTURAS:
          generar_factura    → cliente_nombre, items[], observaciones, fecha
          editar_factura     → id*, estado, forma_pago, fecha_vencimiento, cliente_nombre, observaciones
          borrar_factura     → id*
          listar_facturas    → (opcional) estado (pendiente|pagada|cancelada)

        ALBARANES:
          crear_albaran      → cliente_nombre, items[], observaciones, fecha
          editar_albaran     → id*, estado, cliente_nombre, observaciones
          borrar_albaran     → id*
          listar_albaranes   → (opcional) estado (pendiente|enviado|entregado)

        PEDIDOS:
          crear_pedido       → cliente_nombre, items[], total_estimado, observaciones, fecha (entrega prevista)
          editar_pedido      → id*, estado, cliente_nombre, observaciones, total_estimado, fecha
          borrar_pedido      → id*
          listar_pedidos     → (opcional) estado (pendiente|en_produccion|listo|entregado|cancelado)

        TARIFAS:
          crear_tarifa       → nombre*, tecnica, descripcion, precio_unitario, precio_setup, minimo_unidades, activa
          editar_tarifa      → id*, nombre, tecnica, descripcion, precio_unitario, precio_setup, minimo_unidades, activa
          borrar_tarifa      → id*
          listar_tarifas     → (opcional) tecnica para filtrar

        MATERIALES:
          crear_material     → nombre*, referencia, categoria, unidad, stock_actual, stock_minimo, precio_unidad, proveedor
          editar_material    → id*, nombre, referencia, categoria, unidad, stock_actual, stock_minimo, precio_unidad
          borrar_material    → id*
          listar_materiales  → (opcional) categoria para filtrar
          actualizar_stock   → items[] con descripcion (nombre material), cantidad, notas ("entrada" o "salida")
          consultar_materiales → (sin data extra — devuelve bajo stock)

        EMPLEADOS:
          crear_empleado     → nombre*, apellidos, nif, email, telefono, categoria, salario_base, irpf, iban, fecha_alta
          editar_empleado    → id*, nombre, apellidos, nif, email, categoria, salario_base, irpf, iban, activa
          borrar_empleado    → id*
          listar_empleados   → (sin filtros)

        NÓMINAS:
          calcular_nomina    → cliente_nombre* (nombre del empleado), fecha (mes/año), total_estimado (complementos)
          editar_nomina      → id*, total_estimado (nuevos complementos para recalcular)
          borrar_nomina      → id*
          listar_nominas     → (sin filtros)

        OTROS:
          agendar_evento     → fecha*, observaciones, items[]
          exportar_backup    → (sin data)
          importar_datos_excel → tipo_importacion, proveedor, hojas_a_importar, actualizar_registros_existentes, crear_nuevos_registros, dry_run
          generar_estadistica → (informativo)

        VENTANAS / MÓDULOS:
          abrir_modulo  → modulo*: "clientes|presupuestos|facturas|albaranes|pedidos|tarifas|materiales|empleados|nominas|estadisticas|calendario"
          cerrar_modulo → modulo*: (mismo nombre)

        ─────────────────────────────────────────────────────────────────────────────
        ESTRUCTURA DEL CAMPO "data" (usa solo los campos relevantes a la acción):

        {
          "id": 0,
          "cliente_id": "string", "cliente_nombre": "string",
          "nombre": "string", "apellidos": "string", "nif": "string",
          "email": "string", "telefono": "string", "ciudad": "string",
          "tipo": "empresa|particular", "estado": "string", "modulo": "string",
          "tecnica": "Serigrafía|DTF|DTG|Sublimación|Vinilo|Impresión Digital",
          "descripcion": "string", "precio_unitario": 0.0, "precio_setup": 0.0,
          "minimo_unidades": 0, "activa": true,
          "salario_base": 0.0, "irpf": 0.0, "categoria": "string", "fecha_alta": "YYYY-MM-DD", "iban": "string",
          "unidad": "string", "stock_actual": 0.0, "stock_minimo": 0.0, "referencia": "string", "precio_unidad": 0.0,
          "forma_pago": "string", "fecha_vencimiento": "YYYY-MM-DD", "iva_porcentaje": 0.0, "periodo": "string",
          "total_estimado": 0.0, "observaciones": "string", "fecha": "YYYY-MM-DD",
          "tipo_importacion": "materiales|clientes|empleados|presupuestos|pedidos|albaranes",
          "proveedor": "string", "hojas_a_importar": ["Hoja1"],
          "actualizar_registros_existentes": true, "crear_nuevos_registros": true, "dry_run": false,
          "items": [
            {
              "descripcion": "string", "cantidad": 0, "tecnica": "string",
              "colores": 0, "pantones": ["string"], "soporte": "string",
              "gramaje": 0, "precio_unitario": 0.0, "merma_porcentaje": 0.0, "notas": "string"
            }
          ]
        }

        SUGERENCIAS DE SEGUIMIENTO:
        Al final de respuestas informativas (sin acción), cierra con una pregunta breve:\s
        "¿Quieres que lo cree ahora?", "¿Genero la factura?", "¿Abro el módulo de clientes?". Una sola frase.
        """;

    // ── Routing inteligente ───────────────────────────────────────────────────

    public enum TipoTarea { COMPLEJA, RAPIDA }

    private static final List<String> PALABRAS_CLAVE_COMPLEJA = List.of(
        "presupuesto", "factura", "albar", "pedido", "nomina", "nómina",
        "estadistica", "estadística", "calcul", "precio", "coste", "rentabilidad",
        "margen", "beneficio", "impuesto", "iva", "descuento", "total", "importe",
        "produccion", "producción", "planif", "merma", "gramaje", "pantone",
        "stock", "inventario", "empleado", "salario", "sueldo", "cantidad", "unidades",
        "serigraf", "sublimacion", "sublimación", "bordado", "vinilo", "dtf", "dtg"
    );

    // Substrings en orden de prioridad para tareas complejas (mayor potencia primero)
    // Modelos actualizados y optimizados para 16GB de RAM (versiones cuantificadas)
    private static final List<String> PRIORIDAD_POTENTE = List.of(
        "qwen:3.5-9b", // Qwen 3.5 9B - Excelente rendimiento-tamaño, operativo en 16GB
        "phi4:reasoning-vision-15b", // Phi-4-reasoning-vision-15B - Multimodal, operativo en 16GB
        "llama4:scout", // Llama 4 Scout - MoE, eficiente con cuantizaciones, operativo en 16GB
        "gemma4:e4b", // Gemma 4 E4B - Diseñado para laptops, operativo en 16GB
        "llama3", // Llama3 (versiones 8B cuantificadas), muy operativo y actualizado
        "mistral" // Mistral (versiones 7B cuantificadas), muy operativo y actualizado
    );

    // Substrings en orden de prioridad para tareas rápidas (menor peso primero)
    // Modelos ligeros y rápidos, los más actualizados y operativos para 16GB de RAM
    private static final List<String> PRIORIDAD_LIGERO = List.of(
        "phi3:mini", // Phi-3 Mini - Muy ligero y eficiente, actualizado
        "gemma2:2b", // Gemma 2B - Muy ligero, diseñado para dispositivos, actualizado
        "tinyllama", // TinyLlama - Extremadamente ligero, operativo
        "qwen2:1.5b" // Qwen 2 1.5B - Versión pequeña y rápida, actualizado
    );

    public TipoTarea clasificarTarea(String mensaje) {
        String lower = mensaje.toLowerCase();
        boolean esCompleja = PALABRAS_CLAVE_COMPLEJA.stream().anyMatch(lower::contains);
        return esCompleja ? TipoTarea.COMPLEJA : TipoTarea.RAPIDA;
    }

    private String seleccionarModeloOptimo(String mensaje) {
        List<String> modelos = getModelosDisponibles();
        if (modelos.isEmpty()) return modeloActual;

        TipoTarea tipo = clasificarTarea(mensaje);
        List<String> prioridad = tipo == TipoTarea.COMPLEJA ? PRIORIDAD_POTENTE : PRIORIDAD_LIGERO;

        for (String clave : prioridad) {
            for (String modelo : modelos) {
                if (modelo.toLowerCase().contains(clave.toLowerCase())) return modelo;
            }
        }
        // Fallback: si no se encuentra ninguno de los prioritarios, se usa el modelo actual o el primero/último disponible
        // Se mantiene la lógica original de fallback, pero con modelos más actuales en las listas
        return tipo == TipoTarea.COMPLEJA ? modelos.get(0) : modelos.get(modelos.size() - 1);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

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

    public void chatStreaming(String userMessage, Consumer<String> onChunk, Runnable onComplete,
                             Consumer<String> onError) {
        chatStreaming(userMessage, onChunk, onComplete, onError, null);
    }

    public void chatStreaming(String userMessage, Consumer<String> onChunk, Runnable onComplete,
                             Consumer<String> onError, Consumer<String> onModeloUsado) {
        Thread.ofVirtual().start(() -> {
            try {
                String modeloElegido = routingAutomatico
                    ? seleccionarModeloOptimo(userMessage)
                    : modeloActual;
                if (onModeloUsado != null)
                    javafx.application.Platform.runLater(() -> onModeloUsado.accept(modeloElegido));

                ObjectNode body = mapper.createObjectNode();
                body.put("model", modeloElegido);
                body.put("stream", true);

                var messages = mapper.createArrayNode();

                // System prompt principal
                var sysMsg = mapper.createObjectNode();
                sysMsg.put("role", "system");
                sysMsg.put("content", SYSTEM_PROMPT);
                messages.add(sysMsg);

                // Contexto ERP en tiempo real
                String ctxSnapshot = contextoERP;
                if (ctxSnapshot != null && !ctxSnapshot.isBlank()) {
                    var ctxMsg = mapper.createObjectNode();
                    ctxMsg.put("role", "system");
                    ctxMsg.put("content", ctxSnapshot);
                    messages.add(ctxMsg);
                }

                // Historial de los últimos intercambios (memoria conversacional)
                synchronized (historial) {
                    for (String[] entrada : historial) {
                        var hMsg = mapper.createObjectNode();
                        hMsg.put("role", entrada[0]);
                        hMsg.put("content", entrada[1]);
                        messages.add(hMsg);
                    }
                }

                // Mensaje actual del usuario
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

                StringBuilder respuestaCompleta = new StringBuilder();
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
                                respuestaCompleta.append(content);
                                javafx.application.Platform.runLater(() -> onChunk.accept(content));
                            }
                        }
                        if (node.has("done") && node.get("done").asBoolean()) break;
                    }
                }

                // Guardar en historial sin JSON (para no contaminar el contexto conversacional)
                String textoAsistente = JsonInterceptorService
                    .interceptar(respuestaCompleta.toString()).textoLimpio();
                if (textoAsistente.isBlank()) textoAsistente = respuestaCompleta.toString();
                synchronized (historial) {
                    historial.add(new String[]{"user", userMessage});
                    historial.add(new String[]{"assistant", textoAsistente});
                    while (historial.size() > MAX_HISTORIAL) historial.remove(0);
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
            String modeloElegido = routingAutomatico
                ? seleccionarModeloOptimo(userMessage)
                : modeloActual;

            ObjectNode body = mapper.createObjectNode();
            body.put("model", modeloElegido);
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

    public void setModeloActual(String modelo) {
        this.modeloActual = modelo;
        this.routingAutomatico = false;
    }

    public void habilitarRouting()  { this.routingAutomatico = true; }
    public boolean isRoutingAutomatico() { return routingAutomatico; }
    public String getModeloActual() { return modeloActual; }

    public void setContextoERP(String contexto)  { this.contextoERP = contexto; }
    public void clearContextoERP()               { this.contextoERP = null; }
    public void limpiarHistorial()               { synchronized (historial) { historial.clear(); } }

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
