package org.gipsybuho.service.importer;

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
}
