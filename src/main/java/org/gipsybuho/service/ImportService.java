package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.extractor.XSSFBEventBasedExcelExtractor;
import org.gipsybuho.dao.*;
import org.gipsybuho.model.*;
import org.gipsybuho.util.AppConstants;
import org.gipsybuho.util.TypedValueFormatter;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ImportService {

    private static final String OLLAMA_URL = AppConstants.OLLAMA_BASE_URL;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private final ObjectMapper mapper = new ObjectMapper();

    // ── Entity catalog ────────────────────────────────────────────────────────

    public enum TipoEntidad {
        CLIENTES("Clientes", List.of(
            "nombre", "apellido", "tipo", "nif", "direccion", "ciudad", "cp", "telefono", "email", "notas"
        )),
        MATERIALES("Materiales", List.of(
            "nombre", "referencia", "categoria", "stock_actual", "stock_minimo", "unidad", "precio_unidad", "proveedor"
        )),
        EMPLEADOS("Empleados", List.of(
            "nombre", "apellido", "nif", "categoria", "salario_base", "fecha_alta", "iban", "irpf", "telefono", "email", "direccion"
        )),
        TARIFAS("Tarifas de servicio", List.of(
            "tecnica", "nombre", "descripcion", "precio_unit", "precio_setup", "minimo_unidades"
        ));

        public final String label;
        public final List<String> campos;

        TipoEntidad(String label, List<String> campos) {
            this.label = label;
            this.campos = campos;
        }

        @Override public String toString() { return label; }
    }

    // ── Parse result ──────────────────────────────────────────────────────────

    public static class ImportResult {
        public final List<String> headers;
        public final List<Map<String, String>> rows;
        public final String formato;
        public final FileStructureAnalysis structureAnalysis;

        public ImportResult(List<String> headers, List<Map<String, String>> rows, String formato) {
            this(headers, rows, formato, null);
        }

        public ImportResult(List<String> headers, List<Map<String, String>> rows, String formato,
                            FileStructureAnalysis structureAnalysis) {
            this.headers = headers;
            this.rows = rows;
            this.formato = formato;
            this.structureAnalysis = structureAnalysis != null
                ? structureAnalysis
                : new FileStructureAnalysis(List.of(), false, "Sin análisis estructural.");
        }
    }

    public enum RegionType { TABLE, NOTES, TITLE, EMPTY, UNKNOWN }

    public record DetectedRegion(
        int id,
        RegionType type,
        int startRow,
        int endRow,
        int startCol,
        int endCol,
        double confidence
    ) {}

    public record FileStructureAnalysis(
        List<DetectedRegion> regions,
        boolean complex,
        String summary
    ) {
        public List<DetectedRegion> tableRegions() {
            return regions.stream().filter(r -> r.type() == RegionType.TABLE).toList();
        }
    }

    public record DynamicFieldSuggestion(String sourceColumn, String label, String dataType) {}
    public record RowValueFix(int rowIndex, String columnName, String value) {}
    public record ImportRepairPlan(
        String summary,
        Map<String, String> mapping,
        List<DynamicFieldSuggestion> dynamicFields,
        Map<String, String> fixedValues,
        List<RowValueFix> rowFixes
    ) {
        public static ImportRepairPlan empty() {
            return new ImportRepairPlan("", Map.of(), List.of(), Map.of(), List.of());
        }

        public boolean hasChanges() {
            return !mapping.isEmpty() || !dynamicFields.isEmpty()
                || !fixedValues.isEmpty() || !rowFixes.isEmpty();
        }
    }

    // ── File parsing ──────────────────────────────────────────────────────────

    public ImportResult parseFile(File file) throws Exception {
        return parseFile(file, false);
    }

    /**
     * Parsea el archivo. Con {@code expandPriceMatrix} activo, las matrices de precios
     * detectadas se expanden a filas normalizadas TECNICA/NOMBRE/MINIMO_UNIDADES/PRECIO_UNIT.
     * Solo tiene sentido al importar Tarifas; para el resto de módulos debe ir desactivado
     * para no destruir la estructura original del archivo.
     */
    public ImportResult parseFile(File file, boolean expandPriceMatrix) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".json"))
            return parseJSON(file);
        if (name.endsWith(".csv") || name.endsWith(".txt"))
            return parseCSV(file, expandPriceMatrix);
        // Todos los formatos Excel: xlsx, xls, xlsb, xlsm, xltx, xltm…
        if (name.endsWith(".xlsx") || name.endsWith(".xls")
                || name.endsWith(".xlsb") || name.endsWith(".xlsm")
                || name.endsWith(".xltx") || name.endsWith(".xltm"))
            return parseExcel(file, expandPriceMatrix);
        return parseCSV(file, expandPriceMatrix);  // desconocido → intentar CSV
    }

    public FileStructureAnalysis analyzeFileStructure(File file) throws Exception {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return new FileStructureAnalysis(List.of(), false, "JSON estructurado: no requiere análisis tabular.");
        }
        List<List<String>> grid = readGridForAnalysis(file);
        return analyzeGridStructure(grid);
    }

    public FileStructureAnalysis analyzeGridStructure(List<List<String>> grid) {
        List<DetectedRegion> regions = detectFileRegions(grid);
        long tables = regions.stream().filter(r -> r.type() == RegionType.TABLE).count();
        long notes = regions.stream().filter(r -> r.type() == RegionType.NOTES).count();
        boolean complex = tables > 1 || notes > 0;
        String summary = complex
            ? "Archivo complejo: " + tables + " tabla(s) y " + notes + " bloque(s) de notas detectados."
            : "Archivo simple: una tabla principal detectada.";
        return new FileStructureAnalysis(Collections.unmodifiableList(regions), complex, summary);
    }

    private List<DetectedRegion> detectFileRegions(List<List<String>> grid) {
        if (grid == null || grid.isEmpty()) return List.of();
        List<DetectedRegion> regions = new ArrayList<>();
        int id = 1;
        for (int r = 0; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            if (isBlankRow(row)) continue;

            List<int[]> tableSegments = headerSegments(row);
            if (!tableSegments.isEmpty()) {
                int endRow = findTableEndRow(grid, r + 1);
                for (int[] segment : tableSegments) {
                    regions.add(new DetectedRegion(id++, RegionType.TABLE, r, endRow,
                        segment[0], segment[1], tableConfidence(row, segment)));
                }
                r = Math.max(r, endRow);
                continue;
            }

            if (isNoteOnlyRow(row)) {
                int endRow = r;
                while (endRow + 1 < grid.size() && isNoteOnlyRow(grid.get(endRow + 1))) endRow++;
                regions.add(new DetectedRegion(id++, RegionType.NOTES, r, endRow,
                    firstNonBlankCol(row), lastNonBlankCol(row), 0.80));
                r = endRow;
                continue;
            }

            if (countNonBlank(row) <= 2) {
                regions.add(new DetectedRegion(id++, RegionType.TITLE, r, r,
                    firstNonBlankCol(row), lastNonBlankCol(row), 0.65));
            }
        }
        return regions;
    }

    private List<int[]> headerSegments(List<String> row) {
        if (headerScore(row) < 12) return List.of();
        List<int[]> byBlanks = splitSegmentsByBlankColumns(row);
        if (byBlanks.size() > 1) return byBlanks;

        List<int[]> byRepeatedFirstHeader = splitSegmentsByRepeatedFirstHeader(row);
        return byRepeatedFirstHeader.size() > 1 ? byRepeatedFirstHeader : byBlanks;
    }

    private List<int[]> splitSegmentsByBlankColumns(List<String> row) {
        List<int[]> segments = new ArrayList<>();
        int start = -1;
        for (int c = 0; c < row.size(); c++) {
            if (getCell(row, c).isBlank()) {
                if (start >= 0) {
                    addHeaderSegmentIfUseful(row, segments, start, c - 1);
                    start = -1;
                }
            } else if (start < 0) {
                start = c;
            }
        }
        if (start >= 0) addHeaderSegmentIfUseful(row, segments, start, row.size() - 1);
        return segments;
    }

    private List<int[]> splitSegmentsByRepeatedFirstHeader(List<String> row) {
        int first = firstNonBlankCol(row);
        if (first < 0) return List.of();
        String firstHeader = normalize(getCell(row, first));
        if (firstHeader.isBlank() || !isHeaderKeyword(firstHeader)) return List.of();

        List<Integer> starts = new ArrayList<>();
        starts.add(first);
        for (int c = first + 1; c < row.size(); c++) {
            if (normalize(getCell(row, c)).equals(firstHeader)) starts.add(c);
        }
        if (starts.size() < 2) return List.of();

        List<int[]> segments = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) - 1 : row.size() - 1;
            while (end >= start && getCell(row, end).isBlank()) end--;
            addHeaderSegmentIfUseful(row, segments, start, end);
        }
        return segments;
    }

    private void addHeaderSegmentIfUseful(List<String> row, List<int[]> segments, int start, int end) {
        if (start < 0 || end < start) return;
        int keywords = 0;
        int nonBlank = 0;
        for (int c = start; c <= end; c++) {
            String value = getCell(row, c);
            if (value.isBlank()) continue;
            nonBlank++;
            if (isHeaderKeyword(normalize(value))) keywords++;
        }
        if (nonBlank >= 2 && keywords >= 2) segments.add(new int[]{start, end});
    }

    private int findTableEndRow(List<List<String>> grid, int startRow) {
        int end = startRow - 1;
        for (int r = startRow; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            if (isBlankRow(row) || isNoteOnlyRow(row)) break;
            if (!headerSegments(row).isEmpty()) break;
            end = r;
        }
        return Math.max(startRow - 1, end);
    }

    private double tableConfidence(List<String> row, int[] segment) {
        int nonBlank = 0;
        int keywords = 0;
        for (int c = segment[0]; c <= segment[1]; c++) {
            String value = getCell(row, c);
            if (value.isBlank()) continue;
            nonBlank++;
            if (isHeaderKeyword(normalize(value))) keywords++;
        }
        if (nonBlank == 0) return 0.0;
        return Math.min(0.95, 0.55 + (keywords / (double) nonBlank) * 0.40);
    }

    private boolean isNoteOnlyRow(List<String> row) {
        if (countNonBlank(row) != 1) return false;
        for (String value : row) {
            if (value != null && !value.isBlank()) return looksLikeNoteRow(value);
        }
        return false;
    }

    private int countNonBlank(List<String> row) {
        if (row == null) return 0;
        int count = 0;
        for (String value : row) {
            if (value != null && !value.isBlank()) count++;
        }
        return count;
    }

    private int firstNonBlankCol(List<String> row) {
        if (row == null) return -1;
        for (int c = 0; c < row.size(); c++) {
            if (!getCell(row, c).isBlank()) return c;
        }
        return -1;
    }

    private int lastNonBlankCol(List<String> row) {
        if (row == null) return -1;
        for (int c = row.size() - 1; c >= 0; c--) {
            if (!getCell(row, c).isBlank()) return c;
        }
        return -1;
    }

    private List<List<String>> readGridForAnalysis(File file) throws Exception {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv") || name.endsWith(".txt")) return readCsvGrid(file);
        if (name.endsWith(".xlsb")) return readXlsbGrid(file);
        if (name.endsWith(".xlsx") || name.endsWith(".xls")
                || name.endsWith(".xlsm") || name.endsWith(".xltx") || name.endsWith(".xltm")) {
            try (Workbook wb = WorkbookFactory.create(file, null, true)) {
                Sheet sheet = wb.getSheetAt(0);
                FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();
                return sheetToGrid(sheet, ev);
            }
        }
        return readCsvGrid(file);
    }

    private List<List<String>> readCsvGrid(File file) throws Exception {
        byte[] raw;
        try (var fis = new FileInputStream(file)) { raw = fis.readAllBytes(); }
        String content;
        if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
            content = new String(raw, 3, raw.length - 3, StandardCharsets.UTF_8);
        } else {
            try {
                content = decodeStrict(raw, StandardCharsets.UTF_8);
            } catch (CharacterCodingException e) {
                content = new String(raw, Charset.forName("windows-1252"));
            }
        }
        if (content.isBlank()) return List.of();

        char sep = detectSeparator(content);
        List<List<String>> grid = new ArrayList<>();
        for (String line : content.split("\\R", -1)) {
            if (line.isBlank()) continue;
            grid.add(parseCsvRow(line, sep));
        }
        return grid;
    }

    private List<List<String>> readXlsbGrid(File file) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFBEventBasedExcelExtractor extractor = new XSSFBEventBasedExcelExtractor(pkg);
            String text = extractor.getText();
            List<List<String>> grid = new ArrayList<>();
            for (String line : text.split("\\R", -1)) {
                if (line.isBlank()) continue;
                grid.add(Arrays.asList(line.split("\\t", -1)));
            }
            return grid;
        }
    }

    private ImportResult parseCSV(File file, boolean expandPriceMatrix) throws Exception {
        byte[] raw;
        try (var fis = new FileInputStream(file)) { raw = fis.readAllBytes(); }
        String content;
        if (raw.length >= 3 && raw[0] == (byte) 0xEF && raw[1] == (byte) 0xBB && raw[2] == (byte) 0xBF) {
            content = new String(raw, 3, raw.length - 3, StandardCharsets.UTF_8);
        } else {
            try {
                content = decodeStrict(raw, StandardCharsets.UTF_8);
            } catch (CharacterCodingException e) {
                content = new String(raw, Charset.forName("windows-1252"));
            }
        }

        if (content.isBlank()) {
            return new ImportResult(new ArrayList<>(), new ArrayList<>(), "CSV");
        }

        char sep = detectSeparator(content);
        List<List<String>> grid = new ArrayList<>();
        String[] lines = content.split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) continue;
            grid.add(parseCsvRow(line, sep));
        }
        return buildImportResultFromGrid(grid, "CSV", expandPriceMatrix);
    }

    private char detectSeparator(String content) {
        String first = Arrays.stream(content.split("\\R", -1))
            .filter(s -> !s.isBlank())
            .findFirst()
            .orElse("");
        long tabs = first.chars().filter(c -> c == '\t').count();
        long semis = first.chars().filter(c -> c == ';').count();
        long commas = first.chars().filter(c -> c == ',').count();
        if (tabs > semis && tabs > commas) return '\t';
        if (semis > commas) return ';';
        return ',';
    }

    private List<String> parseCsvRow(String line, char sep) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQ && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQ = !inQ;
                }
            } else if (c == sep && !inQ) {
                fields.add(cur.toString().trim());
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString().trim());
        return fields;
    }

    private String decodeStrict(byte[] raw, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(raw))
            .toString();
    }

    private ImportResult parseExcel(File file, boolean expandPriceMatrix) throws Exception {
        String ext = file.getName().toLowerCase().replaceAll(".*\\.", "").toUpperCase();

        if ("XLSB".equals(ext)) {
            return parseXlsb(file, expandPriceMatrix);
        }

        try (Workbook wb = WorkbookFactory.create(file, null, true)) {
            Sheet sheet = wb.getSheetAt(0);
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();
            return buildImportResultFromGrid(sheetToGrid(sheet, ev), ext, expandPriceMatrix);
        }
    }

    private ImportResult parseXlsb(File file, boolean expandPriceMatrix) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFBEventBasedExcelExtractor extractor = new XSSFBEventBasedExcelExtractor(pkg);
            String text = extractor.getText();
            List<List<String>> grid = new ArrayList<>();
            for (String line : text.split("\\R", -1)) {
                if (line.isBlank()) continue;
                grid.add(Arrays.asList(line.split("\\t", -1)));
            }
            return buildImportResultFromGrid(grid, "XLSB", expandPriceMatrix);
        }
    }

    private List<List<String>> sheetToGrid(Sheet sheet, FormulaEvaluator ev) {
        List<List<String>> grid = new ArrayList<>();
        if (sheet.getFirstRowNum() < 0 || sheet.getLastRowNum() < 0) return grid;

        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            List<String> values = new ArrayList<>();
            if (row != null && row.getLastCellNum() > 0) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.add(cell != null ? cellStr(cell, ev) : "");
                }
            }
            grid.add(values);
        }
        return grid;
    }

    private ImportResult buildImportResultFromGrid(List<List<String>> grid, String formato,
                                                   boolean expandPriceMatrix) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        FileStructureAnalysis analysis = analyzeGridStructure(grid);
        if (grid == null || grid.isEmpty() || grid.stream().allMatch(this::isBlankRow)) {
            return new ImportResult(headers, rows, formato, analysis);
        }
        if (shouldParseByRegions(grid, analysis, expandPriceMatrix)) {
            ImportResult regional = buildImportResultFromTableRegions(grid, formato, analysis, expandPriceMatrix);
            if (!regional.rows.isEmpty()) return regional;
        }

        int headerRowIdx = detectHeaderRow(grid);
        int maxCol = maxColumns(grid);
        List<Integer> includedColumns = includedColumns(grid, headerRowIdx, maxCol);
        if (includedColumns.isEmpty()) {
            return new ImportResult(headers, rows, formato, analysis);
        }

        headers.addAll(buildUniqueHeaders(grid, headerRowIdx, includedColumns));
        for (int r = headerRowIdx + 1; r < grid.size(); r++) {
            List<String> sourceRow = grid.get(r);
            if (isLikelyNonDataRow(sourceRow, includedColumns, headers)) continue;
            Map<String, String> map = new LinkedHashMap<>();
            boolean hasData = false;
            for (int i = 0; i < includedColumns.size(); i++) {
                String val = getCell(sourceRow, includedColumns.get(i));
                map.put(headers.get(i), val);
                if (!val.isBlank()) hasData = true;
            }
            if (hasData) rows.add(map);
        }
        return new ImportResult(headers, rows, formato, analysis);
    }

    private boolean shouldParseByRegions(List<List<String>> grid, FileStructureAnalysis analysis,
                                         boolean expandPriceMatrix) {
        if (analysis.tableRegions().size() > 1) return true;
        return expandPriceMatrix && analysis.tableRegions().stream()
            .anyMatch(region -> {
                int headerRow = regionHeaderRow(grid, region);
                List<Integer> priceColumns = priceColumnsInRegion(grid, region, headerRow);
                return isPriceMatrixRegion(grid, region, headerRow, priceColumns);
            });
    }

    private ImportResult buildImportResultFromTableRegions(List<List<String>> grid, String formato,
                                                           FileStructureAnalysis analysis,
                                                           boolean expandPriceMatrix) {
        List<DetectedRegion> tableRegions = analysis.tableRegions();
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        List<Map<String, String>> rows = new ArrayList<>();

        for (DetectedRegion region : tableRegions) {
            int headerRow = regionHeaderRow(grid, region);
            List<String> regionHeaders = regionHeaders(grid, region, headerRow);
            List<Integer> priceColumns = priceColumnsInRegion(grid, region, headerRow);
            boolean pivotPrices = expandPriceMatrix && isPriceMatrixRegion(grid, region, headerRow, priceColumns);
            if (pivotPrices) {
                headers.addAll(List.of("TECNICA", "NOMBRE", "MINIMO_UNIDADES", "PRECIO_UNIT"));
            } else {
                String context = regionContext(grid, region);
                if (!context.isBlank()) headers.add("GRUPO");
                headers.addAll(regionHeaders);
            }

            String context = regionContext(grid, region);
            for (int r = headerRow + 1; r <= region.endRow() && r < grid.size(); r++) {
                List<String> sourceRow = grid.get(r);
                if (isLikelyNonDataRow(sourceRow, columnsForRegion(region), regionHeaders)) continue;

                if (pivotPrices) {
                    rows.addAll(expandPriceColumns(grid, region, headerRow, sourceRow, context, priceColumns));
                    continue;
                }

                Map<String, String> row = new LinkedHashMap<>();
                if (!context.isBlank()) row.put("GRUPO", context);
                boolean hasData = false;
                for (int c = region.startCol(); c <= region.endCol(); c++) {
                    String header = getCell(grid.get(headerRow), c);
                    if (header.isBlank()) continue;
                    String value = getCell(sourceRow, c);
                    row.put(header, value);
                    if (!value.isBlank()) hasData = true;
                }
                if (hasData) rows.add(row);
            }
        }

        return new ImportResult(new ArrayList<>(headers), rows, formato, analysis);
    }

    private List<Integer> priceColumnsInRegion(List<List<String>> grid, DetectedRegion region) {
        return priceColumnsInRegion(grid, region, region.startRow());
    }

    private List<Integer> priceColumnsInRegion(List<List<String>> grid, DetectedRegion region, int headerRowIndex) {
        List<Integer> columns = new ArrayList<>();
        List<String> headerRow = grid.get(headerRowIndex);
        for (int c = region.startCol(); c <= region.endCol(); c++) {
            String header = getCell(headerRow, c);
            if (isPriceHeader(header)) columns.add(c);
        }
        return columns;
    }

    private List<Map<String, String>> expandPriceColumns(List<List<String>> grid, DetectedRegion region,
                                                         int headerRowIndex, List<String> sourceRow, String context,
                                                         List<Integer> priceColumns) {
        List<Map<String, String>> expanded = new ArrayList<>();
        String units = firstValueForHeader(grid, region, headerRowIndex, sourceRow, "unidad", "cantidad");
        String name = firstValueForHeader(grid, region, headerRowIndex, sourceRow, "descripcion", "concepto", "nombre");
        if (units.isBlank() || name.isBlank()) return expanded;

        for (int priceCol : priceColumns) {
            String price = getCell(sourceRow, priceCol);
            if (price.isBlank()) continue;
            Map<String, String> row = new LinkedHashMap<>();
            String priceHeader = getCell(grid.get(headerRowIndex), priceCol);
            row.put("TECNICA", buildTechniqueFromContext(context, priceHeader));
            row.put("NOMBRE", name);
            row.put("MINIMO_UNIDADES", units);
            row.put("PRECIO_UNIT", price);
            expanded.add(row);
        }
        return expanded;
    }

    private String firstValueForHeader(List<List<String>> grid, DetectedRegion region,
                                       int headerRowIndex, List<String> sourceRow, String... headerNeedles) {
        for (int c = region.startCol(); c <= region.endCol(); c++) {
            String header = normalize(getCell(grid.get(headerRowIndex), c));
            for (String headerNeedle : headerNeedles) {
                if (header.contains(headerNeedle)) return getCell(sourceRow, c);
            }
        }
        return "";
    }

    private String buildTechniqueFromContext(String context, String priceHeader) {
        if (context == null || context.isBlank()) return priceHeader;
        if (priceHeader == null || priceHeader.isBlank() || normalize(priceHeader).equals("precio")) return context;
        return context + " - " + priceHeader;
    }

    private List<String> regionHeaders(List<List<String>> grid, DetectedRegion region) {
        return regionHeaders(grid, region, region.startRow());
    }

    private List<String> regionHeaders(List<List<String>> grid, DetectedRegion region, int headerRowIndex) {
        List<String> headers = new ArrayList<>();
        List<String> row = grid.get(headerRowIndex);
        for (int c = region.startCol(); c <= region.endCol(); c++) {
            String header = getCell(row, c);
            if (!header.isBlank() && !headers.contains(header)) headers.add(header);
        }
        return headers;
    }

    private List<Integer> columnsForRegion(DetectedRegion region) {
        List<Integer> columns = new ArrayList<>();
        for (int c = region.startCol(); c <= region.endCol(); c++) columns.add(c);
        return columns;
    }

    private String regionContext(List<List<String>> grid, DetectedRegion region) {
        String context = contextHeader(grid, region.startRow(), region.startCol());
        return context != null ? context.trim() : "";
    }

    private int regionHeaderRow(List<List<String>> grid, DetectedRegion region) {
        int bestRow = region.startRow();
        int bestScore = Integer.MIN_VALUE;
        int limit = Math.min(region.endRow(), region.startRow() + 4);
        for (int r = region.startRow(); r <= limit && r < grid.size(); r++) {
            int score = headerScore(grid.get(r), region.startCol(), region.endCol());
            if (score > bestScore) {
                bestScore = score;
                bestRow = r;
            }
        }
        return bestRow;
    }

    /** Elige la fila que parece cabecera real, no una fila título encima de la tabla. */
    private int detectHeaderRow(List<List<String>> grid) {
        int bestRow = 0;
        int bestScore = Integer.MIN_VALUE;
        int limit = Math.min(30, grid.size());
        for (int r = 0; r < limit; r++) {
            List<String> row = grid.get(r);
            int score = headerScore(row);
            if (score > bestScore) {
                bestScore = score;
                bestRow = r;
            }
        }
        return bestRow;
    }

    private int headerScore(List<String> row) {
        return headerScore(row, 0, row.size() - 1);
    }

    private int headerScore(List<String> row, int startCol, int endCol) {
        int nonBlank = 0;
        int keywords = 0;
        int numericLike = 0;
        for (int c = startCol; c <= endCol; c++) {
            String value = getCell(row, c);
            String v = value != null ? value.trim() : "";
            if (v.isBlank()) continue;
            nonBlank++;
            String n = normalize(v);
            if (isHeaderKeyword(n)) keywords++;
            if (v.matches("[+-]?\\d+(?:[.,]\\d+)?")) numericLike++;
        }
        if (nonBlank == 0) return Integer.MIN_VALUE / 2;
        int score = nonBlank + (keywords * 8) - numericLike;
        if (nonBlank == 1) score -= 6;
        return score;
    }

    private boolean isPriceMatrixRegion(List<List<String>> grid, DetectedRegion region,
                                        int headerRowIndex, List<Integer> priceColumns) {
        if (priceColumns.size() <= 1) return false;
        boolean hasUnits = false;
        boolean hasName = false;
        for (int c = region.startCol(); c <= region.endCol(); c++) {
            String header = normalize(getCell(grid.get(headerRowIndex), c));
            hasUnits |= header.contains("unidad") || header.contains("cantidad");
            hasName |= header.contains("descripcion") || header.contains("concepto") || header.contains("nombre");
        }
        return hasUnits && hasName;
    }

    private boolean isPriceHeader(String header) {
        String n = normalize(header);
        return n.contains("precio") || n.startsWith("preci") || n.contains("importe") || n.contains("eur");
    }

    private boolean isHeaderKeyword(String normalized) {
        return normalized.contains("unidad")
            || normalized.contains("descripcion")
            || normalized.contains("concepto")
            || normalized.contains("precio")
            || normalized.startsWith("preci")
            || normalized.contains("importe")
            || normalized.contains("cantidad")
            || normalized.contains("nombre")
            || normalized.contains("referencia")
            || normalized.contains("proveedor")
            || normalized.contains("categoria")
            || normalized.contains("tecnica")
            || normalized.contains("stock")
            || normalized.contains("fecha")
            || normalized.contains("email")
            || normalized.contains("telefono")
            || normalized.contains("direccion")
            || normalized.contains("salario")
            || normalized.contains("modelo")
            || normalized.contains("producto")
            || normalized.contains("gramaje")
            || normalized.contains("tamano")
            || normalized.equals("nif")
            || normalized.equals("cp")
            || normalized.equals("iban")
            || normalized.equals("irpf");
    }

    private List<Integer> includedColumns(List<List<String>> grid, int headerRowIdx, int maxCol) {
        List<Integer> included = new ArrayList<>();
        for (int c = 0; c < maxCol; c++) {
            boolean hasData = false;
            for (int r = headerRowIdx; r < grid.size(); r++) {
                if (!getCell(grid.get(r), c).isBlank()) {
                    hasData = true;
                    break;
                }
            }
            if (hasData) included.add(c);
        }
        return included;
    }

    private List<String> buildUniqueHeaders(List<List<String>> grid, int headerRowIdx, List<Integer> columns) {
        List<String> raw = new ArrayList<>();
        Map<String, Integer> rawCounts = new HashMap<>();
        for (int col : columns) {
            String header = getCell(grid.get(headerRowIdx), col);
            raw.add(header);
            if (!header.isBlank()) rawCounts.merge(normalize(header), 1, Integer::sum);
        }

        List<String> result = new ArrayList<>();
        Map<String, Integer> usedRaw = new HashMap<>();
        Map<String, Integer> usedFinal = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            int sourceCol = columns.get(i);
            String header = raw.get(i);
            String normalized = normalize(header);
            int occurrence = normalized.isBlank() ? 1 : usedRaw.merge(normalized, 1, Integer::sum);

            String name;
            if (header.isBlank()) {
                String inferred = inferBlankHeader(grid, headerRowIdx, sourceCol);
                String context = contextHeader(grid, headerRowIdx, sourceCol);
                name = !inferred.isBlank()
                    ? inferred
                    : (!context.isBlank() ? context : "Columna_" + (sourceCol + 1));
            } else if (rawCounts.getOrDefault(normalized, 0) > 1 && occurrence > 1) {
                String context = contextHeader(grid, headerRowIdx, sourceCol);
                name = !context.isBlank() && !normalize(context).equals(normalized)
                    ? context + " - " + header
                    : header;
            } else {
                name = header;
            }

            String finalName = name.trim();
            String finalKey = normalize(finalName);
            int finalOccurrence = usedFinal.merge(finalKey, 1, Integer::sum);
            if (finalOccurrence > 1) {
                finalName = finalName + " (" + finalOccurrence + ")";
            }
            result.add(finalName);
        }
        return result;
    }

    private String inferBlankHeader(List<List<String>> grid, int headerRowIdx, int col) {
        int nonBlank = 0;
        int numeric = 0;
        int price = 0;
        int text = 0;
        int limit = Math.min(grid.size(), headerRowIdx + 31);
        for (int r = headerRowIdx + 1; r < limit; r++) {
            String value = getCell(grid.get(r), col);
            if (value.isBlank()) continue;
            nonBlank++;
            if (looksLikePrice(value)) price++;
            if (looksLikeNumber(value)) numeric++;
            if (!looksLikeNumber(value) && value.length() > 2) text++;
        }
        if (nonBlank < 2) return "";
        if (price >= Math.max(2, nonBlank / 2)) return "PRECIO";
        if (numeric >= Math.max(2, (int) Math.ceil(nonBlank * 0.7))) return "UNIDADES";
        if (text >= Math.max(2, (int) Math.ceil(nonBlank * 0.5))) return "DESCRIPCIÓN";
        return "";
    }

    private boolean looksLikePrice(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        return v.contains("€") || v.contains("eur") || v.matches(".*\\d+[,.]\\d{2}.*");
    }

    private boolean looksLikeNumber(String value) {
        String v = value.trim()
            .replace("€", "")
            .replace("EUR", "")
            .replace("eur", "")
            .replace(".", "")
            .replace(',', '.')
            .trim();
        return v.matches("[+-]?\\d+(?:\\.\\d+)?");
    }

    private boolean isLikelyNonDataRow(List<String> row, List<Integer> columns, List<String> headers) {
        int nonBlank = 0;
        int samePositionHeaders = 0;
        int headerLikeValues = 0;
        int numericHeaderNonNumericValues = 0;
        int onlyNonBlankIndex = -1;
        String onlyNonBlankValue = "";
        Set<String> normalizedHeaders = headers.stream()
            .map(this::normalize)
            .collect(java.util.stream.Collectors.toSet());

        for (int i = 0; i < columns.size(); i++) {
            String value = getCell(row, columns.get(i));
            if (value.isBlank()) continue;
            nonBlank++;
            onlyNonBlankIndex = i;
            onlyNonBlankValue = value;
            String n = normalize(value);
            if (i < headers.size() && n.equals(normalize(headers.get(i)))) samePositionHeaders++;
            if (normalizedHeaders.contains(n) || isHeaderKeyword(n)) headerLikeValues++;
            if (i < headers.size() && isNumericTableHeader(headers.get(i)) && !looksLikeNumber(value)) {
                numericHeaderNonNumericValues++;
            }
        }

        if (nonBlank == 0) return true;
        if (numericHeaderNonNumericValues > 0 && numericHeaderNonNumericValues == nonBlank) return true;
        if (columns.size() > 1 && nonBlank == 1) {
            if (onlyNonBlankIndex < headers.size() && isNumericTableHeader(headers.get(onlyNonBlankIndex))) {
                return true;
            }
            if (looksLikeNoteRow(onlyNonBlankValue)) return true;
        }
        return samePositionHeaders >= 2 || (headerLikeValues >= 2 && headerLikeValues >= nonBlank - 1);
    }

    private boolean looksLikeNoteRow(String value) {
        String n = normalize(value);
        return value.contains(":")
            || n.startsWith("cambio")
            || n.startsWith("nota")
            || n.startsWith("observacion")
            || n.startsWith("diseno")
            || n.startsWith("incluye");
    }

    private boolean isNumericTableHeader(String header) {
        String n = normalize(header);
        return n.contains("precio")
            || n.contains("importe")
            || n.contains("cantidad")
            || n.contains("unidades")
            || n.contains("stock")
            || n.contains("minimo")
            || n.contains("gramaje")
            || n.contains("eur");
    }

    private String contextHeader(List<List<String>> grid, int headerRowIdx, int col) {
        for (int r = 0; r < headerRowIdx; r++) {
            String value = filledContextValue(grid.get(r), col);
            if (isUsefulContext(value)) return value;
        }
        return "";
    }

    private String filledContextValue(List<String> row, int col) {
        String current = "";
        int limit = Math.min(col, Math.max(0, row.size() - 1));
        for (int c = 0; c <= limit; c++) {
            String value = getCell(row, c);
            if (!value.isBlank()) current = value;
        }
        return current;
    }

    private boolean isUsefulContext(String value) {
        if (value == null || value.isBlank()) return false;
        String n = normalize(value);
        return !n.startsWith("tarifa") && !isHeaderKeyword(n);
    }

    private int maxColumns(List<List<String>> grid) {
        int max = 0;
        for (List<String> row : grid) max = Math.max(max, row.size());
        return max;
    }

    private boolean isBlankRow(List<String> row) {
        return row == null || row.stream().allMatch(v -> v == null || v.isBlank());
    }

    private String getCell(List<String> row, int col) {
        if (row == null || col < 0 || col >= row.size()) return "";
        String value = row.get(col);
        return value != null ? value.trim() : "";
    }

    private String cellStr(Cell cell, FormulaEvaluator ev) {
        try {
            CellValue cv = ev.evaluate(cell);
            if (cv == null) return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "";
            return switch (cv.getCellType()) {
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        yield DateUtil.getLocalDateTime(cv.getNumberValue()).toLocalDate()
                            .format(DATE_FORMATTER);
                    }
                    double d = cv.getNumberValue();
                    yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
                }
                case STRING  -> cv.getStringValue().trim();
                case BOOLEAN -> String.valueOf(cv.getBooleanValue());
                default      -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private ImportResult parseJSON(File file) throws Exception {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        JsonNode root = mapper.readTree(file);
        JsonNode array = root.isArray() ? root : null;
        if (array == null) {
            // Try first field value
            Iterator<JsonNode> it = root.elements();
            if (it.hasNext()) {
                JsonNode first = it.next();
                if (first.isArray()) array = first;
            }
        }
        if (array == null || array.size() == 0) return new ImportResult(headers, rows, "JSON");

        array.get(0).fieldNames().forEachRemaining(headers::add);
        for (JsonNode item : array) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String h : headers) {
                JsonNode v = item.get(h);
                row.put(h, v != null && !v.isNull() ? v.asText().trim() : "");
            }
            rows.add(row);
        }
        return new ImportResult(headers, rows, "JSON");
    }

    // ── AI: detect entity type ────────────────────────────────────────────────

    public TipoEntidad sugerirTipoEntidad(List<String> headers) {
        try {
            String prompt = """
                Tienes columnas de un archivo de datos: %s

                ¿Qué tipo de datos es más probable?
                - CLIENTES: personas/empresas (nombre, NIF, email, teléfono, dirección)
                - MATERIALES: inventario/stock (nombre, referencia, stock, precio, proveedor)
                - EMPLEADOS: trabajadores (nombre, NIF, salario, IBAN, categoría laboral)
                - TARIFAS: precios de servicios (técnica, precio, setup, mínimo)

                Responde SOLO con una palabra: CLIENTES, MATERIALES, EMPLEADOS o TARIFAS
                """.formatted(headers);

            String resp = ollamaChat(prompt).trim().toUpperCase();
            for (TipoEntidad tipo : TipoEntidad.values()) {
                if (resp.contains(tipo.name())) return tipo;
            }
        } catch (Exception e) {
            System.err.println("AI entity detection error: " + e.getMessage());
        }
        return TipoEntidad.CLIENTES;
    }

    // ── AI: map columns to fields ─────────────────────────────────────────────

    public Map<String, String> mapearCampos(TipoEntidad tipo, List<String> columnas) {
        Map<String, String> mapping = emptyMapping(columnas);

        try {
            StringBuilder camposDesc = new StringBuilder();
            for (String c : tipo.campos) camposDesc.append("  - ").append(c).append("\n");

            String prompt = """
                Mapea las columnas del archivo importado a los campos de la tabla "%s".

                Columnas del archivo: %s

                Campos destino disponibles:
                %s

                Reglas:
                - Usa el mismo nombre exacto del campo destino.
                - Usa null si no hay equivalencia razonable.
                - Considera sinónimos en español e inglés: Company→nombre, Phone→telefono, VAT/CIF→nif, Price→precio_unit, Setup→precio_setup, etc.
                - No uses campos que no aparezcan en la lista de destino.

                Responde SOLO con un JSON válido sin texto extra. Formato exacto:
                {"columna1": "campo_destino", "columna2": null}
                """.formatted(tipo.label, columnas, camposDesc);

            String resp = ollamaChat(prompt).trim();
            resp = extractJson(resp);

            JsonNode json = mapper.readTree(resp);
            json.fields().forEachRemaining(e -> {
                if (mapping.containsKey(e.getKey())) {
                    String destino = e.getValue().isNull() ? null : e.getValue().asText();
                    // Validate the mapped field exists in the schema
                    if (destino != null && tipo.campos.contains(destino)) {
                        mapping.put(e.getKey(), destino);
                    } else if (destino == null) {
                        mapping.put(e.getKey(), null);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("AI mapping error: " + e.getMessage());
        }
        fallbackMapping(mapping, tipo);
        return mapping;
    }

    Map<String, String> mapearCamposLocal(TipoEntidad tipo, List<String> columnas) {
        Map<String, String> mapping = emptyMapping(columnas);
        fallbackMapping(mapping, tipo);
        return mapping;
    }

    private Map<String, String> emptyMapping(List<String> columnas) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String col : columnas) mapping.put(col, null);
        return mapping;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }

    private void fallbackMapping(Map<String, String> mapping, TipoEntidad tipo) {
        sanitizeMapping(mapping, tipo);
        applyExactDestinationMappings(mapping, tipo);

        Set<String> usedDestinations = new HashSet<>();
        mapping.values().stream()
            .filter(Objects::nonNull)
            .forEach(usedDestinations::add);

        for (String col : new ArrayList<>(mapping.keySet())) {
            if (mapping.get(col) != null) continue;
            String matched = specFor(tipo).matcher().sugerirCampo(col);
            if (matched != null && tipo.campos.contains(matched)
                    && isPlausibleMapping(tipo, col, matched)
                    && usedDestinations.add(matched)) {
                mapping.put(col, matched);
                continue;
            }

            String colN = normalize(col);
            for (String campo : tipo.campos) {
                if (normalize(campo).equals(colN) || colN.contains(normalize(campo))) {
                    if (isPlausibleMapping(tipo, col, campo) && usedDestinations.add(campo)) {
                        mapping.put(col, campo);
                    }
                    break;
                }
            }
        }
    }

    private void sanitizeMapping(Map<String, String> mapping, TipoEntidad tipo) {
        for (var entry : new ArrayList<>(mapping.entrySet())) {
            String destino = entry.getValue();
            if (destino == null) continue;
            if (!tipo.campos.contains(destino) || !isPlausibleMapping(tipo, entry.getKey(), destino)) {
                mapping.put(entry.getKey(), null);
            }
        }
    }

    private void applyExactDestinationMappings(Map<String, String> mapping, TipoEntidad tipo) {
        for (String col : new ArrayList<>(mapping.keySet())) {
            String exact = exactDestination(tipo, col);
            if (exact == null || !isPlausibleMapping(tipo, col, exact)) continue;
            for (String other : new ArrayList<>(mapping.keySet())) {
                if (!other.equals(col) && exact.equals(mapping.get(other))) {
                    mapping.put(other, null);
                }
            }
            mapping.put(col, exact);
        }
    }

    private String exactDestination(TipoEntidad tipo, String sourceColumn) {
        String source = normalize(sourceColumn);
        for (String campo : tipo.campos) {
            if (normalize(campo).equals(source)) return campo;
        }
        return null;
    }

    private boolean isPlausibleMapping(TipoEntidad tipo, String sourceColumn, String destino) {
        if (tipo != TipoEntidad.MATERIALES) return true;

        String source = normalize(sourceColumn);
        if ("unidad".equals(destino)) {
            return isUnitHeader(source);
        }
        if ("stock_actual".equals(destino) || "stock_minimo".equals(destino)) {
            return isStockHeader(source);
        }
        if ("precio_unidad".equals(destino)) {
            return isPriceHeaderForMapping(source);
        }
        return true;
    }

    private boolean isUnitHeader(String normalizedHeader) {
        return Set.of("unidad", "ud", "uds", "und", "unit", "um", "medida")
            .contains(normalizedHeader);
    }

    private boolean isStockHeader(String normalizedHeader) {
        return normalizedHeader.contains("stock")
            || normalizedHeader.contains("existencia")
            || normalizedHeader.equals("cantidad")
            || normalizedHeader.equals("quantity")
            || normalizedHeader.equals("unidades")
            || normalizedHeader.equals("uds");
    }

    private boolean isPriceHeaderForMapping(String normalizedHeader) {
        return normalizedHeader.contains("precio")
            || normalizedHeader.contains("coste")
            || normalizedHeader.contains("price")
            || normalizedHeader.contains("pvp")
            || normalizedHeader.contains("eur")
            || normalizedHeader.contains("importe");
    }

    private org.gipsybuho.service.importer.EntityImportSpec specFor(TipoEntidad tipo) {
        return switch (tipo) {
            case CLIENTES   -> Cliente.IMPORT_SPEC;
            case MATERIALES -> Material.IMPORT_SPEC;
            case EMPLEADOS  -> Empleado.IMPORT_SPEC;
            case TARIFAS    -> Tarifa.IMPORT_SPEC;
        };
    }

    private String normalize(String s) {
        return s.toLowerCase()
            .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u")
            .replace("ñ","n").replace(" ","_").replace("-","_").replaceAll("[^a-z0-9_]","");
    }

    // ── Import to database ────────────────────────────────────────────────────

    public int importar(TipoEntidad tipo, List<Map<String, String>> rows,
                        Map<String, String> mapping) throws Exception {
        return switch (tipo) {
            case CLIENTES   -> importarClientes(rows, mapping);
            case MATERIALES -> importarMateriales(rows, mapping);
            case EMPLEADOS  -> importarEmpleados(rows, mapping);
            case TARIFAS    -> importarTarifas(rows, mapping);
        };
    }

    private int importarClientes(List<Map<String, String>> rows, Map<String, String> mapping) throws Exception {
        ClienteDAO dao = new ClienteDAO();
        int n = 0;
        for (Map<String, String> row : rows) {
            Cliente c = new Cliente();
            c.setCiudad("Almería");
            c.setTipo("empresa");
            for (var e : mapping.entrySet()) {
                if (e.getValue() == null) continue;
                String v = row.getOrDefault(e.getKey(), "").trim();
                switch (e.getValue()) {
                    case "nombre"    -> c.setNombre(v);
                    case "apellido"  -> c.setApellidos(v);
                    case "tipo"      -> { if (!v.isEmpty()) c.setTipo(v); }
                    case "nif"       -> c.setNif(v);
                    case "direccion" -> c.setDireccion(v);
                    case "ciudad"    -> { if (!v.isEmpty()) c.setCiudad(v); }
                    case "cp"        -> c.setCp(v);
                    case "telefono"  -> c.setTelefono(v);
                    case "email"     -> c.setEmail(v);
                    case "notas"     -> c.setNotas(v);
                }
            }
            if (c.getNombre() == null || c.getNombre().isBlank()) continue;
            dao.save(c);
            n++;
        }
        return n;
    }

    private int importarMateriales(List<Map<String, String>> rows, Map<String, String> mapping) throws Exception {
        MaterialDAO dao = new MaterialDAO();
        int n = 0;
        for (Map<String, String> row : rows) {
            Material m = new Material();
            m.setCategoria("consumibles");
            m.setUnidad("ud");
            for (var e : mapping.entrySet()) {
                if (e.getValue() == null) continue;
                String v = row.getOrDefault(e.getKey(), "").trim();
                switch (e.getValue()) {
                    case "nombre"        -> m.setNombre(v);
                    case "referencia"    -> m.setReferencia(v);
                    case "categoria"     -> { if (!v.isEmpty()) m.setCategoria(v); }
                    case "stock_actual"  -> m.setStockActual(toDouble(v));
                    case "stock_minimo"  -> m.setStockMinimo(toDouble(v));
                    case "unidad"        -> { if (!v.isEmpty()) m.setUnidad(v); }
                    case "precio_unidad" -> m.setPrecioUnidad(toDouble(v));
                    case "proveedor"     -> m.setProveedor(v);
                }
            }
            if (m.getNombre() == null || m.getNombre().isBlank()) continue;
            dao.save(m);
            n++;
        }
        return n;
    }

    private int importarEmpleados(List<Map<String, String>> rows, Map<String, String> mapping) throws Exception {
        EmpleadoDAO dao = new EmpleadoDAO();
        int n = 0;
        for (Map<String, String> row : rows) {
            Empleado emp = new Empleado();
            emp.setActivo(true);
            emp.setSalarioBase(1200.0);
            emp.setIrpf(15.0);
            emp.setCategoria("Operario");
            for (var e : mapping.entrySet()) {
                if (e.getValue() == null) continue;
                String v = row.getOrDefault(e.getKey(), "").trim();
                switch (e.getValue()) {
                    case "nombre"       -> emp.setNombre(v);
                    case "apellido"     -> emp.setApellidos(v);
                    case "nif"          -> emp.setNif(v);
                    case "categoria"    -> { if (!v.isEmpty()) emp.setCategoria(v); }
                    case "salario_base" -> { double d = toDouble(v); if (d > 0) emp.setSalarioBase(d); }
                    case "fecha_alta"   -> emp.setFechaAlta(v);
                    case "iban"         -> emp.setIban(v);
                    case "irpf"         -> { double d = toDouble(v); if (d > 0) emp.setIrpf(d); }
                    case "telefono"     -> emp.setTelefono(v);
                    case "email"        -> emp.setEmail(v);
                    case "direccion"    -> emp.setDireccion(v);
                }
            }
            if (emp.getNombre() == null || emp.getNombre().isBlank()) continue;
            dao.save(emp);
            n++;
        }
        return n;
    }

    private int importarTarifas(List<Map<String, String>> rows, Map<String, String> mapping) throws Exception {
        TarifaDAO dao = new TarifaDAO();
        int n = 0;
        for (Map<String, String> row : rows) {
            Tarifa t = new Tarifa();
            t.setActiva(true);
            t.setMinimoUnidades(1);
            t.setTecnica("General");
            for (var e : mapping.entrySet()) {
                if (e.getValue() == null) continue;
                String v = row.getOrDefault(e.getKey(), "").trim();
                switch (e.getValue()) {
                    case "tecnica"          -> { if (!v.isEmpty()) t.setTecnica(v); }
                    case "nombre"           -> t.setNombre(v);
                    case "descripcion"      -> t.setDescripcion(v);
                    case "precio_unit"      -> t.setPrecioUnit(toDouble(v));
                    case "precio_setup"     -> t.setPrecioSetup(toDouble(v));
                    case "minimo_unidades"  -> { int i = toInt(v); if (i > 0) t.setMinimoUnidades(i); }
                }
            }
            if (t.getNombre() == null || t.getNombre().isBlank()) continue;
            dao.save(t);
            n++;
        }
        return n;
    }

    // ── Ollama ────────────────────────────────────────────────────────────────

    private String ollamaChat(String prompt) throws Exception {
        String modelo = org.gipsybuho.db.DatabaseManager.getConfig("ollama_modelo");
        if (modelo == null || modelo.isBlank()) modelo = "llama3.2";

        var body = mapper.createObjectNode();
        body.put("model", modelo);
        body.put("stream", false);
        var msgs = mapper.createArrayNode();
        var sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", "Eres un experto en migración de datos. Responde SIEMPRE en el formato exacto que se pide, sin texto adicional, sin markdown, sin explicaciones.");
        var usr = mapper.createObjectNode();
        usr.put("role", "user");
        usr.put("content", prompt);
        msgs.add(sys); msgs.add(usr);
        body.set("messages", msgs);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL + "/api/chat"))
            .timeout(Duration.ofSeconds(90))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(resp.body());
        JsonNode msg = root.get("message");
        if (msg == null || msg.get("content") == null) return "";
        return msg.get("content").asText();
    }

    public boolean isOllamaDisponible() {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest r = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL + "/api/tags"))
                .timeout(Duration.ofSeconds(3)).GET().build();
            return c.send(r, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    // ── Validation ───────────────────────────────────────────────────────────

    public List<ValidationIssue> validateImportData(
            List<Map<String, String>> rows,
            Map<String, String> mappingActual,
            TipoEntidad tipo) {
        if (isOllamaDisponible()) {
            try {
                int n = Math.min(rows.size(), 20);
                String rowsJson = mapper.writeValueAsString(rows.subList(0, n));
                String mappingJson = mapper.writeValueAsString(mappingActual);
                String prompt = buildOllamaValidationPrompt(rowsJson, mappingJson, tipo, n);
                String resp = extractJsonArray(ollamaChat(prompt).trim());
                List<ValidationIssue> issues = parseOllamaValidationResponse(resp);
                if (!issues.isEmpty()) return issues;
            } catch (Exception ex) {
                System.err.println("AI validation error: " + ex.getMessage());
            }
        }
        return performLocalValidation(rows, mappingActual, tipo);
    }

    public Optional<String> corregirValor(Map<String, String> row, String fileColumnName,
            String issueDescription, TipoEntidad tipo, Map<String, String> mappingActual) {
        try {
            String rowJson = mapper.writeValueAsString(row);
            String mappingJson = mapper.writeValueAsString(mappingActual);
            String prompt = """
                Eres un asistente de corrección de datos para la entidad "%s".
                Mapeo columnas: %s
                Problema en la columna "%s": %s
                Fila: %s
                Devuelve SOLO JSON: {"correctedValue": "valor_corregido"} o {"correctedValue": null}.
                """.formatted(tipo.label, mappingJson, fileColumnName, issueDescription, rowJson);
            String resp = extractJson(ollamaChat(prompt).trim());
            JsonNode node = mapper.readTree(resp);
            JsonNode val = node.get("correctedValue");
            if (val == null || val.isNull()) return Optional.empty();
            return Optional.of(val.asText());
        } catch (Exception ex) {
            System.err.println("AI correction error: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public ImportRepairPlan proponerReparacionImportacion(
            TipoEntidad tipo,
            List<Map<String, String>> rows,
            List<String> headers,
            Map<String, String> mappingActual,
            List<String> dynamicFields) {
        if (!isOllamaDisponible()) return ImportRepairPlan.empty();
        try {
            int n = Math.min(rows.size(), 30);
            String rowsJson = mapper.writeValueAsString(rows.subList(0, n));
            String mappingJson = mapper.writeValueAsString(mappingActual);
            String prompt = """
                Eres un asistente de migración de datos para la entidad "%s".

                Columnas del archivo: %s
                Campos base disponibles: %s
                Campos dinámicos existentes: %s
                Mapeo actual columna_archivo→campo_destino: %s
                Primeras %d filas: %s

                Objetivo:
                - Propón un mapeo mejor si hay columnas ignoradas que sí representan datos útiles.
                - Si hace falta conservar una columna que no encaja en campos base, propón crear un campo dinámico.
                - Para campos dinámicos usa dataType solo entre TEXTO, NUMERICO, PRECIO, FECHA.
                - Propón fixedValues solo para valores genéricos seguros, por ejemplo tecnica o categoria si el archivo completo pertenece a una técnica/categoría clara.
                - No inventes NIF, emails, clientes, proveedores ni relaciones con otros registros.
                - Propón rowFixes solo para normalizar formatos evidentes: fechas, precios, números o símbolos de moneda.

                Responde SOLO JSON válido, sin markdown:
                {
                  "summary": "resumen breve",
                  "mapping": {"Columna archivo": "campo_destino_o_null"},
                  "dynamicFields": [{"sourceColumn":"Columna archivo","label":"Etiqueta","dataType":"TEXTO|NUMERICO|PRECIO|FECHA"}],
                  "fixedValues": {"campo_destino":"valor"},
                  "rowFixes": [{"rowIndex":0,"columnName":"Columna archivo","value":"valor normalizado"}]
                }
                """.formatted(tipo.label, headers, tipo.campos, dynamicFields, mappingJson, n, rowsJson);
            String resp = extractJson(ollamaChat(prompt).trim());
            return parseRepairPlan(resp, headers, tipo, dynamicFields);
        } catch (Exception ex) {
            System.err.println("AI repair plan error: " + ex.getMessage());
            return ImportRepairPlan.empty();
        }
    }

    private ImportRepairPlan parseRepairPlan(
            String json,
            List<String> headers,
            TipoEntidad tipo,
            List<String> dynamicFields) {
        try {
            JsonNode root = mapper.readTree(json);
            Set<String> headerSet = new LinkedHashSet<>(headers);
            Set<String> allowedDestinations = new LinkedHashSet<>(tipo.campos);
            allowedDestinations.addAll(dynamicFields);

            Map<String, String> mapping = new LinkedHashMap<>();
            JsonNode mappingNode = root.get("mapping");
            if (mappingNode != null && mappingNode.isObject()) {
                mappingNode.fields().forEachRemaining(e -> {
                    String col = e.getKey();
                    if (!headerSet.contains(col)) return;
                    String dest = e.getValue().isNull() ? null : e.getValue().asText(null);
                    if (dest == null || allowedDestinations.contains(dest)) mapping.put(col, dest);
                });
            }

            List<DynamicFieldSuggestion> dynamic = new ArrayList<>();
            JsonNode dynamicNode = root.get("dynamicFields");
            if (dynamicNode != null && dynamicNode.isArray()) {
                for (JsonNode node : dynamicNode) {
                    String source = node.path("sourceColumn").asText(null);
                    String label = node.path("label").asText(null);
                    String type = normalizeRepairType(node.path("dataType").asText("TEXTO"));
                    if (source != null && headerSet.contains(source) && label != null && !label.isBlank()) {
                        dynamic.add(new DynamicFieldSuggestion(source, label.trim(), type));
                    }
                }
            }

            Map<String, String> fixed = new LinkedHashMap<>();
            JsonNode fixedNode = root.get("fixedValues");
            if (fixedNode != null && fixedNode.isObject()) {
                fixedNode.fields().forEachRemaining(e -> {
                    String field = e.getKey();
                    if (tipo.campos.contains(field) && !e.getValue().isNull()) {
                        fixed.put(field, e.getValue().asText(""));
                    }
                });
            }

            List<RowValueFix> fixes = new ArrayList<>();
            JsonNode fixesNode = root.get("rowFixes");
            if (fixesNode != null && fixesNode.isArray()) {
                for (JsonNode node : fixesNode) {
                    int row = node.path("rowIndex").asInt(-1);
                    String col = node.path("columnName").asText(null);
                    JsonNode value = node.get("value");
                    if (row >= 0 && col != null && headerSet.contains(col) && value != null && !value.isNull()) {
                        fixes.add(new RowValueFix(row, col, value.asText("")));
                    }
                }
            }

            return new ImportRepairPlan(
                root.path("summary").asText(""),
                Collections.unmodifiableMap(mapping),
                Collections.unmodifiableList(dynamic),
                Collections.unmodifiableMap(fixed),
                Collections.unmodifiableList(fixes)
            );
        } catch (Exception ex) {
            System.err.println("Repair JSON parse error: " + ex.getMessage());
            return ImportRepairPlan.empty();
        }
    }

    private String normalizeRepairType(String dataType) {
        return switch (dataType != null ? dataType.trim().toUpperCase(Locale.ROOT) : "TEXTO") {
            case "NUMERICO", "NUMÉRICO" -> "NUMERICO";
            case "PRECIO" -> "PRECIO";
            case "FECHA" -> "FECHA";
            default -> "TEXTO";
        };
    }

    private String buildOllamaValidationPrompt(String rowsJson, String mappingJson,
            TipoEntidad tipo, int n) {
        List<String> required = requiredFieldsFor(tipo);
        return """
            Eres un validador de datos. Analiza estos datos de importación para la entidad "%s".
            Mapeo columna_archivo→campo_entidad: %s
            Campos obligatorios: %s
            Primeras %d filas: %s

            Detecta: formato NIF inválido, email sin @, precio no numérico, fecha inválida, campo obligatorio vacío.
            Responde SOLO con array JSON (sin texto extra):
            [{"rowIndex":0,"columnName":"col","issue":"desc","suggestedFix":"sugerencia o null","severity":"ERROR o WARNING"}]
            Sin problemas: []
            """.formatted(tipo.label, mappingJson, required, n, rowsJson);
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : "[]";
    }

    private List<ValidationIssue> parseOllamaValidationResponse(String json) {
        List<ValidationIssue> issues = new ArrayList<>();
        try {
            JsonNode arr = mapper.readTree(json);
            if (!arr.isArray()) return issues;
            for (JsonNode node : arr) {
                int row = node.path("rowIndex").asInt(-1);
                String col = node.path("columnName").asText(null);
                String issue = node.path("issue").asText(null);
                if (row < 0 || col == null || issue == null) continue;
                JsonNode fixNode = node.get("suggestedFix");
                Optional<String> fix = (fixNode == null || fixNode.isNull())
                    ? Optional.empty() : Optional.of(fixNode.asText());
                String sev = node.path("severity").asText("WARNING");
                ValidationIssue.Severity severity = "ERROR".equals(sev)
                    ? ValidationIssue.Severity.ERROR : ValidationIssue.Severity.WARNING;
                issues.add(new ValidationIssue(row, col, issue, fix, severity));
            }
        } catch (Exception ex) {
            System.err.println("Validation JSON parse error: " + ex.getMessage());
        }
        return issues;
    }

    private List<ValidationIssue> performLocalValidation(List<Map<String, String>> rows,
            Map<String, String> mappingActual, TipoEntidad tipo) {
        List<ValidationIssue> issues = new ArrayList<>();
        List<String> required = requiredFieldsFor(tipo);
        java.util.regex.Pattern nif = java.util.regex.Pattern.compile("(?i)^[XYZ\\d]\\d{7}[A-Z]$");
        java.util.regex.Pattern email = java.util.regex.Pattern.compile("^\\S+@\\S+\\.\\S+$");
        Set<String> nifsSeen = new LinkedHashSet<>();
        Set<String> emailsSeen = new LinkedHashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            for (Map.Entry<String, String> e : mappingActual.entrySet()) {
                String fileCol = e.getKey();
                String entityField = e.getValue();
                if (entityField == null) continue;
                String val = row.getOrDefault(fileCol, "").trim();

                if (required.contains(entityField) && val.isEmpty()) {
                    issues.add(new ValidationIssue(i, fileCol, "Campo obligatorio vacío",
                        Optional.empty(), ValidationIssue.Severity.ERROR));
                    continue;
                }
                if (val.isEmpty()) continue;

                switch (entityField) {
                    case "nif" -> {
                        if (!nif.matcher(val).matches())
                            issues.add(new ValidationIssue(i, fileCol, "Formato NIF/CIF inválido",
                                Optional.empty(), ValidationIssue.Severity.ERROR));
                        if (!nifsSeen.add(val.toUpperCase()))
                            issues.add(new ValidationIssue(i, fileCol, "NIF duplicado en el archivo",
                                Optional.empty(), ValidationIssue.Severity.WARNING));
                    }
                    case "email" -> {
                        if (!email.matcher(val).matches())
                            issues.add(new ValidationIssue(i, fileCol, "Formato de email inválido",
                                Optional.of("nombre@dominio.com"), ValidationIssue.Severity.WARNING));
                        if (!emailsSeen.add(val.toLowerCase()))
                            issues.add(new ValidationIssue(i, fileCol, "Email duplicado en el archivo",
                                Optional.empty(), ValidationIssue.Severity.WARNING));
                    }
                    case "precio_unidad", "precio_unit", "precio_setup", "salario_base" -> {
                        try {
                            double d = TypedValueFormatter.parseDecimal(val)
                                .orElseThrow(NumberFormatException::new)
                                .doubleValue();
                            if (d < 0) issues.add(new ValidationIssue(i, fileCol, "Valor negativo",
                                Optional.empty(), ValidationIssue.Severity.WARNING));
                        } catch (NumberFormatException ex) {
                            issues.add(new ValidationIssue(i, fileCol, "Se esperaba un número decimal",
                                Optional.of("Usa dígitos con punto o coma decimal"), ValidationIssue.Severity.ERROR));
                        }
                    }
                    case "stock_actual", "stock_minimo", "minimo_unidades" -> {
                        try { Integer.parseInt(val.replaceAll("[.,].*", "")); }
                        catch (NumberFormatException ex) {
                            issues.add(new ValidationIssue(i, fileCol, "Se esperaba un número entero",
                                Optional.empty(), ValidationIssue.Severity.ERROR));
                        }
                    }
                    case "fecha_alta" -> {
                        if (TypedValueFormatter.parseDate(val).isEmpty())
                            issues.add(new ValidationIssue(i, fileCol, "Formato de fecha inválido",
                            Optional.of("DD/MM/AAAA o AAAA-MM-DD"), ValidationIssue.Severity.ERROR));
                    }
                    default -> {}
                }
            }
        }
        return issues;
    }

    private List<String> requiredFieldsFor(TipoEntidad tipo) {
        return switch (tipo) {
            case CLIENTES   -> List.of("nombre");
            case MATERIALES -> List.of("nombre");
            case EMPLEADOS  -> List.of("nombre", "nif");
            case TARIFAS    -> List.of("tecnica", "nombre");
        };
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private double toDouble(String s) {
        return TypedValueFormatter.parseDecimal(s).map(java.math.BigDecimal::doubleValue).orElse(0.0);
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9\\-]","")); }
        catch (Exception e) { return 0; }
    }
}
