package org.gipsybuho.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentImportSpecTest {

    @Test
    void documentSpecsAcceptBareClientNifHeader() {
        assertEquals("cliente_nif", Pedido.IMPORT_SPEC.matcher().sugerirCampo("nif"));
        assertEquals("cliente_nif", Presupuesto.IMPORT_SPEC.matcher().sugerirCampo("nif"));
        assertEquals("cliente_nif", Factura.IMPORT_SPEC.matcher().sugerirCampo("nif"));
        assertEquals("cliente_nif", Albaran.IMPORT_SPEC.matcher().sugerirCampo("nif"));
    }

    @Test
    void albaranSpecAcceptsBareNumeroHeader() {
        assertEquals("numero", Albaran.IMPORT_SPEC.matcher().sugerirCampo("numero"));
    }
}
