package org.gipsybuho.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedValueFormatterTest {

    @Test
    void normalizaPreciosConSeparadoresEspanolesYEuro() {
        assertEquals("1234.56",
            TypedValueFormatter.normalizeForStorage("PRECIO", "1.234,56 €"));
        assertEquals("12.50",
            TypedValueFormatter.normalizeForStorage("PRECIO", "12,5"));
    }

    @Test
    void normalizaNumerosSinCerosFinales() {
        assertEquals("1234.5",
            TypedValueFormatter.normalizeForStorage("NUMERICO", "1.234,50"));
    }

    @Test
    void normalizaFechasAFormatoIso() {
        assertEquals("2026-06-11",
            TypedValueFormatter.normalizeForStorage("FECHA", "11/06/2026"));
    }

    @Test
    void conservaValorOriginalSiNoPuedeConvertir() {
        assertEquals("pendiente",
            TypedValueFormatter.normalizeForStorage("PRECIO", "pendiente"));
        assertTrue(TypedValueFormatter.parseDate("ayer").isEmpty());
    }

    @Test
    void normalizacionEstrictaFallaConValorNoConvertible() {
        assertTrue(TypedValueFormatter.tryNormalizeForStorage("PRECIO", "pendiente").isEmpty());
        assertTrue(TypedValueFormatter.tryNormalizeForStorage("FECHA", "ayer").isEmpty());
    }
}
