package org.gipsybuho.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    @Test
    void neutralizaFormulaConSigno() {
        assertEquals("'=cmd|'/c calc'!A1", ExportService.csvEscapar("=cmd|'/c calc'!A1"));
    }

    @Test
    void neutralizaFormulaConMas() {
        assertEquals("'+1+1", ExportService.csvEscapar("+1+1"));
    }

    @Test
    void neutralizaFormulaConMenos() {
        assertEquals("'-1+1", ExportService.csvEscapar("-1+1"));
    }

    @Test
    void neutralizaFormulaConArroba() {
        assertEquals("'@SUM(A1)", ExportService.csvEscapar("@SUM(A1)"));
    }

    @Test
    void textoNormalNoSeModifica() {
        assertEquals("Cliente Normal SL", ExportService.csvEscapar("Cliente Normal SL"));
    }

    @Test
    void escapaComillasYPuntoYComaTrasNeutralizar() {
        assertEquals("\"'=a;b\"\"c\"", ExportService.csvEscapar("=a;b\"c"));
    }

    @Test
    void cadenaVaciaNoFalla() {
        assertEquals("", ExportService.csvEscapar(""));
    }
}
