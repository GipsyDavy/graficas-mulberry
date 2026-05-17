package org.gipsybuho.service.importer;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Sugiere el campo destino de la app para un header de columna del archivo fuente.
 * Cada entidad tiene su implementación con su propio diccionario de sinónimos.
 */
@FunctionalInterface
public interface ColumnMatcher {
    /**
     * @param headerArchivo header de columna del archivo importado
     * @return clave del campo destino (p.ej. "nombre") o null si no reconocido
     */
    String sugerirCampo(String headerArchivo);

    /** Minúsculas, sin acentos, caracteres no alfanuméricos → espacio, espacios simples. */
    static String normalize(String s) {
        String d = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return d.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Selecciona el campo cuyo sinónimo coincidente sea más largo.
     * Un sinónimo más largo es más específico: "precio ud" (9) gana sobre "ud" (2),
     * eliminando los falsos positivos del primer-match con contains().
     * En caso de empate exacto de longitud, el resultado sería no determinista —
     * por eso los sinónimos de igual longitud no pueden aparecer en dos campos distintos.
     */
    static String matchLongest(String norm, Map<String, List<String>> synonyms) {
        String bestField = null;
        int bestLen = -1;
        for (var entry : synonyms.entrySet()) {
            for (String syn : entry.getValue()) {
                if (norm.contains(syn) && syn.length() > bestLen) {
                    bestLen = syn.length();
                    bestField = entry.getKey();
                }
            }
        }
        return bestField;
    }
}
