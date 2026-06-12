package org.gipsybuho.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Tarifa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceParsingTest {

    @TempDir
    Path tempDir;

    @Test
    void parseCsvEmptyFileReturnsEmptyResult() throws Exception {
        Path file = tempDir.resolve("empty.csv");
        Files.writeString(file, "\n", StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals("CSV", result.formato);
        assertTrue(result.headers.isEmpty());
        assertTrue(result.rows.isEmpty());
    }

    @Test
    void parseCsvDetectsHeaderAfterTitleRowsAndKeepsSideBySideTables() throws Exception {
        Path file = tempDir.resolve("tarifas.csv");
        Files.writeString(file, String.join("\n",
            "TARJETAS COLOR;;;TARJETAS B/N;;",
            "TARIFA 2017;;;TARIFA 2017;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO;UNIDADES;DESCRIPCIÓN;PRECIO",
            "100;Color 300g;12;100;BN 300g;9"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(6, result.headers.size());
        assertEquals("UNIDADES", result.headers.get(0));
        assertEquals("TARJETAS B/N - UNIDADES", result.headers.get(3));
        assertEquals("Color 300g", result.rows.get(0).get("DESCRIPCIÓN"));
        assertEquals("9", result.rows.get(0).get("TARJETAS B/N - PRECIO"));
    }

    @Test
    void parseCsvSkipsRepeatedHeadersAndSectionTitlesInsideData() throws Exception {
        Path file = tempDir.resolve("tarifas_repeated.csv");
        Files.writeString(file, String.join("\n",
            "UNIDADES;DESCRIPCIÓN;PRECIO",
            "100;Tarjetas 300g;12",
            "SECCION NUEVA;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO",
            "200;Tarjetas 350g;18"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(2, result.rows.size());
        assertEquals("Tarjetas 300g", result.rows.get(0).get("DESCRIPCIÓN"));
        assertEquals("Tarjetas 350g", result.rows.get(1).get("DESCRIPCIÓN"));
    }

    @Test
    void parseCsvInfersBlankHeadersFromColumnValues() throws Exception {
        Path file = tempDir.resolve("ovalos.csv");
        Files.writeString(file, String.join("\n",
            ";;;Precio;Precio/ud",
            ";500;Ovalos 5x2,5;180;0,018",
            ";1000;Ovalos 5x2,5;240;0,016"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals("UNIDADES", result.headers.get(0));
        assertEquals("DESCRIPCIÓN", result.headers.get(1));
        assertEquals("Precio", result.headers.get(2));
    }

    @Test
    void parseXlsxSkipsTitleRowWhenNextRowLooksLikeHeader() throws Exception {
        Path file = tempDir.resolve("disenos.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("DISEÑOS");
            Row title = sheet.createRow(0);
            title.createCell(1).setCellValue("DISEÑO");
            Row header = sheet.createRow(1);
            header.createCell(1).setCellValue("CONCEPTO");
            header.createCell(2).setCellValue("PRECIO");
            Row data = sheet.createRow(2);
            data.createCell(1).setCellValue("DISEÑO TARJETAS");
            data.createCell(2).setCellValue(10);
            try (var out = Files.newOutputStream(file)) {
                wb.write(out);
            }
        }

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(2, result.headers.size());
        assertEquals("CONCEPTO", result.headers.get(0));
        assertEquals("PRECIO", result.headers.get(1));
        assertEquals("DISEÑO TARJETAS", result.rows.get(0).get("CONCEPTO"));
    }

    @Test
    void localMatchersPreferRequiredBusinessNamesForTarifasAndMateriales() {
        assertEquals("nombre", Tarifa.IMPORT_SPEC.matcher().sugerirCampo("CONCEPTO"));
        assertEquals("nombre", Tarifa.IMPORT_SPEC.matcher().sugerirCampo("DESCRIPCIÓN"));
        assertEquals("minimo_unidades", Tarifa.IMPORT_SPEC.matcher().sugerirCampo("UNIDADES"));
        assertEquals("nombre", Material.IMPORT_SPEC.matcher().sugerirCampo("tipo_papel"));
        assertEquals("precio_unidad", Material.IMPORT_SPEC.matcher().sugerirCampo("precio_pliego_o_unidad"));
    }
}
