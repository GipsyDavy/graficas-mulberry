package org.gipsybuho.service.importer;

import java.util.List;

/**
 * Descriptor inmutable de la configuración de importación de una entidad.
 * Se define como constante public static final IMPORT_SPEC en cada clase de modelo.
 */
public record EntityImportSpec(
    String nombre,
    List<FieldSpec> campos,
    ColumnMatcher matcher,
    DuplicatePolicy politicaDefecto
) {
    /** Devuelve solo los campos marcados como obligatorio (NOT NULL en el schema SQLite). */
    public List<FieldSpec> camposObligatorios() {
        return campos.stream().filter(FieldSpec::obligatorio).toList();
    }

    /** Devuelve solo los campos no obligatorios. */
    public List<FieldSpec> camposOpcionales() {
        return campos.stream().filter(f -> !f.obligatorio()).toList();
    }
}
