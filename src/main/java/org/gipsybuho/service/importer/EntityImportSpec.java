package org.gipsybuho.service.importer;

import java.util.List;

/**
 * Descriptor inmutable de la configuración de importación de una entidad.
 * Se define como constante public static final IMPORT_SPEC en cada clase de modelo.
 *
 * Para entidades planas: los 3 últimos componentes ({@code claveAgrupacion},
 * {@code campoLineas}, {@code specLinea}) son {@code null}.
 *
 * Para entidades parent-child: los 3 últimos componentes son no-null y describen
 * cómo agrupar filas del CSV ancho y cómo mapear las columnas de línea. El
 * {@code politicaDefecto} de {@code specLinea} se ignora.
 */
public record EntityImportSpec(
    String nombre,
    List<FieldSpec> campos,
    ColumnMatcher matcher,
    DuplicatePolicy politicaDefecto,
    String claveAgrupacion,
    String campoLineas,
    EntityImportSpec specLinea
) {
    /**
     * Constructor canónico con validación de coherencia parent-child.
     * Los 3 campos parent-child deben estar todos a null (spec plano) o todos no-null (spec parent-child).
     */
    public EntityImportSpec {
        int nulos = (claveAgrupacion == null ? 1 : 0)
                  + (campoLineas == null ? 1 : 0)
                  + (specLinea == null ? 1 : 0);
        if (nulos != 0 && nulos != 3) {
            throw new IllegalArgumentException(
                "EntityImportSpec parent-child incoherente: claveAgrupacion, campoLineas y specLinea deben estar los 3 a null o los 3 no-null. Spec: " + nombre);
        }
    }

    /**
     * Constructor de compatibilidad para entidades planas.
     * Delega al canónico con los 3 campos parent-child a null.
     */
    public EntityImportSpec(String nombre, List<FieldSpec> campos, ColumnMatcher matcher, DuplicatePolicy politicaDefecto) {
        this(nombre, campos, matcher, politicaDefecto, null, null, null);
    }

    /** True si este spec describe una entidad parent-child (con líneas). */
    public boolean esParentChild() {
        return specLinea != null;
    }

    /** Devuelve solo los campos marcados como obligatorio (NOT NULL en el schema SQLite). */
    public List<FieldSpec> camposObligatorios() {
        return campos.stream().filter(FieldSpec::obligatorio).toList();
    }

    /** Devuelve solo los campos no obligatorios. */
    public List<FieldSpec> camposOpcionales() {
        return campos.stream().filter(f -> !f.obligatorio()).toList();
    }
}
