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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void parseCsvDetectsHeaderAfterTitleRowsAndNormalizesSideBySideTables() throws Exception {
        Path file = tempDir.resolve("tarifas.csv");
        Files.writeString(file, String.join("\n",
            "TARJETAS COLOR;;;TARJETAS B/N;;",
            "TARIFA 2017;;;TARIFA 2017;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO;UNIDADES;DESCRIPCIÓN;PRECIO",
            "100;Color 300g;12;100;BN 300g;9"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(4, result.headers.size());
        assertEquals("GRUPO", result.headers.get(0));
        assertEquals("UNIDADES", result.headers.get(1));
        assertEquals(2, result.rows.size());
        assertEquals("TARJETAS COLOR", result.rows.get(0).get("GRUPO"));
        assertEquals("Color 300g", result.rows.get(0).get("DESCRIPCIÓN"));
        assertEquals("TARJETAS B/N", result.rows.get(1).get("GRUPO"));
        assertEquals("9", result.rows.get(1).get("PRECIO"));
    }

    @Test
    void parseCsvExpandsMultiplePriceColumnsInDetectedRegions() throws Exception {
        Path file = tempDir.resolve("tarifas_pivot.csv");
        Files.writeString(file, String.join("\n",
            "TARJETAS COLOR;;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO NORMAL;PRECIO PLASTIFICADO",
            "100;Tarjetas color 300g;12;17,75",
            "200;Tarjetas color 300g;15;"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(List.of("TECNICA", "NOMBRE", "MINIMO_UNIDADES", "PRECIO_UNIT"), result.headers);
        assertEquals(3, result.rows.size());
        assertEquals("TARJETAS COLOR - PRECIO NORMAL", result.rows.get(0).get("TECNICA"));
        assertEquals("100", result.rows.get(0).get("MINIMO_UNIDADES"));
        assertEquals("12", result.rows.get(0).get("PRECIO_UNIT"));
        assertEquals("TARJETAS COLOR - PRECIO PLASTIFICADO", result.rows.get(1).get("TECNICA"));
        assertEquals("17,75", result.rows.get(1).get("PRECIO_UNIT"));
        assertEquals("200", result.rows.get(2).get("MINIMO_UNIDADES"));
        assertEquals("15", result.rows.get(2).get("PRECIO_UNIT"));
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
    void parseCsvSkipsHumanNotesAndSectionTitlesInsideTarifaTables() throws Exception {
        Path file = tempDir.resolve("tarjetas_visita.csv");
        Files.writeString(file, String.join("\n",
            "TARJETAS DE VISITA COLOR;;;;",
            "TARIFA 2017/2018;;;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO;;UNIDADES;DESCRIPCIÓN;PRECIO",
            "100;Tarjetas color 300g;12;;100;Tarjetas BN 300g;9",
            "10000;Tarjetas color 300g;170;;10000;Tarjetas BN 300g;120",
            ";;;;;;",
            ";cambios 2024;;;;;",
            ";diseño de tarjetas: entre 5 y 15 €;;;;;",
            ";;;;;;",
            "TARJETAS B/N+COLOR;;;;TARJETAS DOBLES;;;;",
            "TARIFA 2017/2018;;;;TARIFA 2017/2018;;;;",
            "UNIDADES;DESCRIPCIÓN;PRECIO;;UNIDADES;DESCRIPCIÓN;PRECIO",
            "500;Tarjetas mixtas 300g;33;;500;Tarjetas dobles 300g;65"
        ), StandardCharsets.UTF_8);

        ImportService.ImportResult result = new ImportService().parseFile(file.toFile());

        assertEquals(6, result.rows.size());
        assertEquals("Tarjetas color 300g", result.rows.get(0).get("DESCRIPCIÓN"));
        assertEquals("Tarjetas color 300g", result.rows.get(1).get("DESCRIPCIÓN"));
        assertEquals("Tarjetas BN 300g", result.rows.get(2).get("DESCRIPCIÓN"));
        assertEquals("Tarjetas mixtas 300g", result.rows.get(4).get("DESCRIPCIÓN"));
    }

    @Test
    void analyzeGridStructureDetectsSideBySideTablesAndHumanNotes() {
        List<List<String>> grid = List.of(
            List.of("TARJETAS COLOR", "", "", "", "TARJETAS B/N", "", ""),
            List.of("TARIFA 2017", "", "", "", "TARIFA 2017", "", ""),
            List.of("UNIDADES", "DESCRIPCIÓN", "PRECIO", "", "UNIDADES", "DESCRIPCIÓN", "PRECIO"),
            List.of("100", "Color 300g", "12", "", "100", "BN 300g", "9"),
            List.of("", "cambios 2024", "", "", "", "", ""),
            List.of("UNIDADES", "DESCRIPCIÓN", "PRECIO", "", "UNIDADES", "DESCRIPCIÓN", "PRECIO"),
            List.of("200", "Mixta 300g", "18", "", "200", "Doble 300g", "30")
        );

        ImportService.FileStructureAnalysis analysis = new ImportService().analyzeGridStructure(grid);

        assertTrue(analysis.complex());
        assertEquals(4, analysis.tableRegions().size());
        assertEquals(1, analysis.regions().stream()
            .filter(r -> r.type() == ImportService.RegionType.NOTES)
            .count());
    }

    @Test
    void analyzeGridStructureKeepsSimpleTableAsSimple() {
        List<List<String>> grid = List.of(
            List.of("nombre", "nif", "email"),
            List.of("Cliente A", "12345678Z", "a@example.com"),
            List.of("Cliente B", "87654321X", "b@example.com")
        );

        ImportService.FileStructureAnalysis analysis = new ImportService().analyzeGridStructure(grid);

        assertFalse(analysis.complex());
        assertEquals(1, analysis.tableRegions().size());
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
        assertEquals("tecnica", Tarifa.IMPORT_SPEC.matcher().sugerirCampo("GRUPO"));
        assertEquals("minimo_unidades", Tarifa.IMPORT_SPEC.matcher().sugerirCampo("UNIDADES"));
        assertEquals("nombre", Material.IMPORT_SPEC.matcher().sugerirCampo("tipo_papel"));
        assertEquals("precio_unidad", Material.IMPORT_SPEC.matcher().sugerirCampo("precio_pliego_o_unidad"));
    }
}
