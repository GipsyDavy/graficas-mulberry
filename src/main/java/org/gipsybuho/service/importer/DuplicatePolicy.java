package org.gipsybuho.service.importer;

/** Política aplicada cuando la clave de negocio de una fila entrante ya existe en BD. */
public enum DuplicatePolicy {
    /** Omite la fila sin error; el registro existente se conserva intacto. Valor por defecto. */
    SKIP_IF_EXISTS,
    /** Sobreescribe el registro existente con los datos entrantes. */
    UPDATE_EXISTING,
    /** Inserta siempre sin comprobar duplicados por clave de negocio. */
    CREATE_NEW
}
