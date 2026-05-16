package org.gipsybuho.service.importer;

/** Descriptor de un campo importable de una entidad. */
public record FieldSpec(
    String clave,
    String etiqueta,
    boolean obligatorio
) {}
