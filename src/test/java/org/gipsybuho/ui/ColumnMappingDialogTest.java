package org.gipsybuho.ui;

import org.gipsybuho.model.Presupuesto;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnMappingDialogTest {

    @Test
    void parentChildSpecsExposeLineFieldsForManualMapping() {
        Map<String, String> labels = ColumnMappingDialog.fieldLabelsForMapping(Presupuesto.IMPORT_SPEC);
        Set<String> required = ColumnMappingDialog.requiredFieldsForMapping(Presupuesto.IMPORT_SPEC);

        assertEquals("Número", labels.get("numero"));
        assertEquals("Línea: Descripción", labels.get("descripcion"));
        assertEquals("Línea: Cantidad", labels.get("cantidad"));
        assertEquals("Línea: Precio unitario", labels.get("precio_unit"));

        assertTrue(required.contains("numero"));
        assertTrue(required.contains("descripcion"));
        assertTrue(required.contains("cantidad"));
        assertTrue(required.contains("precio_unit"));
    }
}
