package org.gipsybuho.service.importer;

/** Clasificación de errores por fila durante la importación. */
public enum ErrorTipo {
    NULL_OBLIGATORIO,
    TIPO_INVALIDO,
    DUPLICADO,
    FK_INVALIDA,
    EXCEDE_LONGITUD,
    OTRO
}
