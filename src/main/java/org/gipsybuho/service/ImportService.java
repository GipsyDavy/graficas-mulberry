package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.gipsybuho.dao.*;
import org.gipsybuho.model.*;

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

    private static final String OLLAMA_URL = "http://localhost:11434";
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
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

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

        char sep = detectSeparator(content);
        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0) return new ImportResult(headers, rows, "CSV");

        headers.addAll(parseCsvRow(lines[0], sep));

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            List<String> vals = parseCsvRow(line, sep);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++)
                row.put(headers.get(j), j < vals.size() ? vals.get(j) : "");
            rows.add(row);
        }
        return new ImportResult(headers, rows, "CSV");
    }

    private char detectSeparator(String content) {
        String first = content.split("\\r?\\n")[0];
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
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        String ext = file.getName().toLowerCase().replaceAll(".*\\.", "").toUpperCase();

        try (Workbook wb = WorkbookFactory.create(file, null, true)) {
            Sheet sheet = wb.getSheetAt(0);
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            int firstRow = sheet.getFirstRowNum();
            int headerRowIdx = detectHeaderRow(sheet, firstRow);
            Row hRow = sheet.getRow(headerRowIdx);
            if (hRow == null) return new ImportResult(headers, rows, ext);

            // Loop explícito: incluye celdas ausentes como "" para conservar alineación
            int lastCol = hRow.getLastCellNum();
            List<String> allHeaders = new ArrayList<>();
            for (int ci = 0; ci < lastCol; ci++) {
                Cell c = hRow.getCell(ci, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                allHeaders.add(c != null ? cellStr(c, ev) : "");
            }

            // Tomar solo la primera tabla: si hay ≥3 cols a cada lado de un bloque
            // de columnas vacías, quedarse con las columnas hasta ese separador
            int blockEnd = firstTableBlockEnd(allHeaders);
            headers.addAll(allHeaders.subList(0, blockEnd));
            // Nombrar columnas sin cabecera para evitar claves "" duplicadas en el map
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).isBlank()) headers.set(i, "Columna_" + (i + 1));
            }

            // Índices de columna de origen (alineados con la hoja real)
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> map = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell != null ? cellStr(cell, ev) : "";
                    map.put(headers.get(c), val);
                    if (!val.isBlank()) hasData = true;
                }
                if (hasData) rows.add(map);
            }
        }
        return new ImportResult(headers, rows, ext);
    }

    /**
     * Devuelve el índice de fin (exclusivo) de la primera tabla en la fila de cabecera.
     * Si detecta un separador válido (columnas vacías con ≥3 cols no-vacías a cada lado),
     * corta ahí. Si no, devuelve el tamaño total (sin corte).
     */
    private int firstTableBlockEnd(List<String> headers) {
        int n = headers.size();
        for (int i = 0; i < n; i++) {
            if (!headers.get(i).isBlank()) continue;
            // Contar cols no-vacías a la izquierda
            int leftNonBlank = 0;
            for (int j = 0; j < i; j++) {
                if (!headers.get(j).isBlank()) leftNonBlank++;
            }
            if (leftNonBlank < 3) continue;
            // Encontrar fin del bloque de cols vacías
            int gapEnd = i;
            while (gapEnd < n && headers.get(gapEnd).isBlank()) gapEnd++;
            // Contar cols no-vacías a la derecha
            int rightNonBlank = 0;
            for (int j = gapEnd; j < n; j++) {
                if (!headers.get(j).isBlank()) rightNonBlank++;
            }
            if (rightNonBlank >= 3) return i; // separador válido encontrado
        }
        return n; // sin separador: usar todas las columnas
    }

    /** Elige la fila con más celdas STRING no-vacías entre las primeras 5. */
    private int detectHeaderRow(Sheet sheet, int firstRow) {
        int bestRow = firstRow;
        int bestScore = 0;
        int limit = Math.min(firstRow + 5, sheet.getLastRowNum() + 1);
        for (int r = firstRow; r < limit; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int score = 0;
            for (Cell c : row) {
                if (c.getCellType() == CellType.STRING && !c.getStringCellValue().isBlank())
                    score++;
            }
            if (score > bestScore) {
                bestScore = score;
                bestRow = r;
            }
        }
        return bestRow;
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
            fallbackMapping(mapping, tipo.campos);
        }
        return mapping;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : text;
    }

    private void fallbackMapping(Map<String, String> mapping, List<String> campos) {
        for (String col : new ArrayList<>(mapping.keySet())) {
            if (mapping.get(col) != null) continue;
            String colN = normalize(col);
            for (String campo : campos) {
                if (normalize(campo).equals(colN) || colN.contains(normalize(campo))) {
                    mapping.put(col, campo);
                    break;
                }
            }
        }
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
                            double d = Double.parseDouble(val.replace(",", "."));
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
                        boolean valid = false;
                        for (String fmt : List.of("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d/M/yyyy")) {
                            try {
                                java.time.LocalDate.parse(val,
                                    java.time.format.DateTimeFormatter.ofPattern(fmt));
                                valid = true; break;
                            } catch (Exception ignored) {}
                        }
                        if (!valid) issues.add(new ValidationIssue(i, fileCol, "Formato de fecha inválido",
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
        try { return Double.parseDouble(s.replace(",",".").replaceAll("[^0-9.\\-]","")); }
        catch (Exception e) { return 0; }
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9\\-]","")); }
        catch (Exception e) { return 0; }
    }
}
