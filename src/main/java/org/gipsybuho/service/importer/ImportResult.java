package org.gipsybuho.service.importer;

import java.time.Duration;
import java.util.List;

/** Resultado devuelto por EntityImportService al finalizar la importación de un archivo. */
public record ImportResult(
    int totalFilasProcesadas,
    int filasImportadas,
    int filasActualizadas,
    int filasDescartadas,
    List<RowError> errores,
    Duration duracion
) {}
