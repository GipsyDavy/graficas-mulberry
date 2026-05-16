package org.gipsybuho.service.importer;

/** Describe un error producido al procesar una fila del archivo importado. */
public record RowError(
    int numeroFila,
    String campo,
    String valorOriginal,
    ErrorTipo tipo,
    String mensaje
) {}
