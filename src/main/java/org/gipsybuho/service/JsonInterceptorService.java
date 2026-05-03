package org.gipsybuho.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gipsybuho.model.AccionERP;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonInterceptorService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ```json ... ``` (el modelo etiqueta el bloque)
    private static final Pattern BLOQUE_JSON    = Pattern.compile(
        "```json\\s*\\r?\\n([\\s\\S]*?)\\r?\\n?```", Pattern.DOTALL);

    // ``` ... ``` sin etiqueta de lenguaje, pero el contenido empieza con {
    private static final Pattern BLOQUE_GENERICO = Pattern.compile(
        "```\\s*\\r?\\n(\\s*\\{[\\s\\S]*?)\\r?\\n?```", Pattern.DOTALL);

    // JSON crudo al final del texto (sin bloque de código)
    private static final Pattern BLOQUE_RAW     = Pattern.compile(
        "(\\{[\\s\\S]*\"action\"[\\s\\S]*\\}\\s*)$", Pattern.DOTALL);

    public record Resultado(String textoLimpio, Optional<AccionERP> accion) {
        public boolean tieneAccion() { return accion.isPresent(); }
    }

    public static Resultado interceptar(String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            return new Resultado(respuesta, Optional.empty());
        }

        // Estrategia 1: ```json ... ``` (más común, mayor prioridad)
        Resultado r1 = buscarEnBloque(respuesta, BLOQUE_JSON);
        if (r1.tieneAccion()) return r1;

        // Estrategia 2: ``` ... ``` sin etiqueta, contenido empieza con {
        Resultado r2 = buscarEnBloque(respuesta, BLOQUE_GENERICO);
        if (r2.tieneAccion()) return r2;

        // Estrategia 3: JSON crudo al final del texto (sin bloques de código)
        Matcher rm = BLOQUE_RAW.matcher(respuesta.trim());
        if (rm.find()) {
            Optional<AccionERP> accion = parsear(rm.group(1).trim());
            if (accion.isPresent()) {
                return new Resultado(respuesta.substring(0, rm.start()).trim(), accion);
            }
        }

        return new Resultado(respuesta, Optional.empty());
    }

    private static Resultado buscarEnBloque(String respuesta, Pattern patron) {
        Matcher m = patron.matcher(respuesta);
        if (m.find()) {
            String jsonStr = m.group(1).trim();
            Optional<AccionERP> accion = parsear(jsonStr);
            if (accion.isPresent()) {
                return new Resultado(respuesta.substring(0, m.start()).trim(), accion);
            }
        }
        return new Resultado(respuesta, Optional.empty());
    }

    private static Optional<AccionERP> parsear(String json) {
        // Intento 1: parseo directo
        try {
            AccionERP accion = MAPPER.readValue(json, AccionERP.class);
            if (accion.action != null && !accion.action.isBlank()) return Optional.of(accion);
        } catch (Exception ignored) {}

        // Intento 2: limpiar JSON antes de reintentar
        try {
            String limpio = limpiarJson(json);
            AccionERP accion = MAPPER.readValue(limpio, AccionERP.class);
            if (accion.action != null && !accion.action.isBlank()) return Optional.of(accion);
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    /**
     * Intenta reparar JSON malformado que el modelo pudo haber generado:
     * elimina comas finales antes de } o ], recorta texto sobrante y
     * fuerza que empiece y termine con llaves balanceadas.
     */
    private static String limpiarJson(String json) {
        // Eliminar comas finales antes de } o ] (error frecuente en LLMs)
        String s = json.replaceAll(",\\s*([}\\]])", "$1");
        // Eliminar comentarios de estilo // ... hasta fin de línea
        s = s.replaceAll("//[^\\n]*", "");
        // Eliminar caracteres de control excepto saltos de línea
        s = s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        // Truncar al último } balanceado si hay texto sobrante al final
        int depth = 0, lastClose = -1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') { depth--; if (depth == 0) { lastClose = i; break; } }
        }
        if (lastClose > 0 && lastClose < s.length() - 1) s = s.substring(0, lastClose + 1);
        return s.trim();
    }
}
