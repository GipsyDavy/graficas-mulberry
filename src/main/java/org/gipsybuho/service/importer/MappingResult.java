package org.gipsybuho.service.importer;

import java.util.Map;

/** Resultado devuelto por ColumnMappingDialog: mapeo de columnas del archivo a campos del spec y política de duplicados. */
public record MappingResult(Map<String, String> mapping, DuplicatePolicy policy) {}
