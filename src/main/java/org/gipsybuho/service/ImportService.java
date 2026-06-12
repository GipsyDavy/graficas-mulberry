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

        public ImportResult(List<String> headers, List<Map<String, String>> rows, String formato) {
            this.headers = headers;
            this.rows = rows;
            this.formato = formato;
        }
    }

    // ── Import config ─────────────────────────────────────────────────────────

    public static class ImportConfig {
        public final Map<String, String> mapping;
        public final List<String> pivotColumns;
        public final String pivotLabelField;
        public final String pivotValueField;

        public ImportConfig(Map<String, String> mapping) {
            this(mapping, List.of(), null, null);
        }

        public ImportConfig(Map<String, String> mapping, List<String> pivotColumns,
                            String pivotLabelField, String pivotValueField) {
            this.mapping = Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
            this.pivotColumns = List.copyOf(pivotColumns);
            this.pivotLabelField = pivotLabelField;
            this.pivotValueField = pivotValueField;
        }

        public boolean hasPivot() {
            return !pivotColumns.isEmpty() && pivotLabelField != null && pivotValueField != null;
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
        String name = file.getName().toLowerCase();
        if (name.endsWith(".json"))
            return parseJSON(file);
        if (name.endsWith(".csv") || name.endsWith(".txt"))
            return parseCSV(file);
        // Todos los formatos Excel: xlsx, xls, xlsb, xlsm, xltx, xltm…
        if (name.endsWith(".xlsx") || name.endsWith(".xls")
                || name.endsWith(".xlsb") || name.endsWith(".xlsm")
                || name.endsWith(".xltx") || name.endsWith(".xltm"))
            return parseExcel(file);
        return parseCSV(file);  // desconocido → intentar CSV
    }

    private ImportResult parseCSV(File file) throws Exception {
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
        return buildImportResultFromGrid(grid, "CSV");
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

    private ImportResult parseExcel(File file) throws Exception {
        String ext = file.getName().toLowerCase().replaceAll(".*\\.", "").toUpperCase();

        if ("XLSB".equals(ext)) {
            return parseXlsb(file);
        }

        try (Workbook wb = WorkbookFactory.create(file, null, true)) {
            Sheet sheet = wb.getSheetAt(0);
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();
            return buildImportResultFromGrid(sheetToGrid(sheet, ev), ext);
        }
    }

    private ImportResult parseXlsb(File file) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFBEventBasedExcelExtractor extractor = new XSSFBEventBasedExcelExtractor(pkg);
            String text = extractor.getText();
            List<List<String>> grid = new ArrayList<>();
            for (String line : text.split("\\R", -1)) {
                if (line.isBlank()) continue;
                grid.add(Arrays.asList(line.split("\\t", -1)));
            }
            return buildImportResultFromGrid(grid, "XLSB");
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

    private ImportResult buildImportResultFromGrid(List<List<String>> grid, String formato) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        if (grid == null || grid.isEmpty() || grid.stream().allMatch(this::isBlankRow)) {
            return new ImportResult(headers, rows, formato);
        }

        int headerRowIdx = detectHeaderRow(grid);
        int maxCol = maxColumns(grid);
        List<Integer> includedColumns = includedColumns(grid, headerRowIdx, maxCol);
        if (includedColumns.isEmpty()) {
            return new ImportResult(headers, rows, formato);
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
        return new ImportResult(headers, rows, formato);
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
        int nonBlank = 0;
        int keywords = 0;
        int numericLike = 0;
        for (String value : row) {
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

    private boolean isHeaderKeyword(String normalized) {
        return normalized.contains("unidad")
            || normalized.contains("descripcion")
            || normalized.contains("concepto")
            || normalized.contains("precio")
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
        Set<String> normalizedHeaders = headers.stream()
            .map(this::normalize)
            .collect(java.util.stream.Collectors.toSet());

        for (int i = 0; i < columns.size(); i++) {
            String value = getCell(row, columns.get(i));
            if (value.isBlank()) continue;
            nonBlank++;
            String n = normalize(value);
            if (i < headers.size() && n.equals(normalize(headers.get(i)))) samePositionHeaders++;
            if (normalizedHeaders.contains(n) || isHeaderKeyword(n)) headerLikeValues++;
        }

        if (nonBlank == 0) return true;
        if (columns.size() > 1 && nonBlank == 1) {
            for (int i = 0; i < columns.size(); i++) {
                String value = getCell(row, columns.get(i));
                if (!value.isBlank()) return i < headers.size() && isNumericTableHeader(headers.get(i));
            }
        }
        return samePositionHeaders >= 2 || (headerLikeValues >= 2 && headerLikeValues >= nonBlank - 1);
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
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String col : columnas) mapping.put(col, null);

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

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }

    private void fallbackMapping(Map<String, String> mapping, TipoEntidad tipo) {
        Set<String> usedDestinations = new HashSet<>();
        mapping.values().stream()
            .filter(Objects::nonNull)
            .forEach(usedDestinations::add);

        for (String col : new ArrayList<>(mapping.keySet())) {
            if (mapping.get(col) != null) continue;
            String matched = specFor(tipo).matcher().sugerirCampo(col);
            if (matched != null && tipo.campos.contains(matched) && usedDestinations.add(matched)) {
                mapping.put(col, matched);
                continue;
            }

            String colN = normalize(col);
            for (String campo : tipo.campos) {
                if (normalize(campo).equals(colN) || colN.contains(normalize(campo))) {
                    if (usedDestinations.add(campo)) mapping.put(col, campo);
                    break;
                }
            }
        }
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
                        ImportConfig config) throws Exception {
        if (config.hasPivot()) {
            List<Map<String, String>> expanded = expandPivot(rows, config);
            Map<String, String> expandedMapping = new LinkedHashMap<>(config.mapping);
            expandedMapping.put("__pivot_label__", config.pivotLabelField);
            expandedMapping.put("__pivot_value__", config.pivotValueField);
            return importar(tipo, expanded, expandedMapping);
        }
        return importar(tipo, rows, config.mapping);
    }

    private List<Map<String, String>> expandPivot(List<Map<String, String>> rows, ImportConfig config) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            for (String pivotCol : config.pivotColumns) {
                String value = row.getOrDefault(pivotCol, "");
                if (value.isBlank()) continue;
                Map<String, String> newRow = new LinkedHashMap<>(row);
                newRow.put("__pivot_label__", pivotCol);
                newRow.put("__pivot_value__", value);
                result.add(newRow);
            }
        }
        return result;
    }

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
