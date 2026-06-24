package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.gipsybuho.dao.ClienteDAO;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * Importación de clientes desde cualquier formato externo.
 * Detecta el formato por la extensión del archivo, extrae las cabeceras,
 * las compara con las columnas existentes en la tabla «clientes» y
 * añade dinámicamente las que no existan vía ALTER TABLE.
 *
 * Formatos soportados: xlsx, xlsm, xlsb, xltx, xltm, xls, csv, json, sql, docx, doc, pdf.
 */
public class ImportarClientesService {

    // ── Campos fijos de la tabla clientes ────────────────────────────────────

    private static final Set<String> CAMPOS_FIJOS = Set.of(
        "id", "nombre", "apellidos", "tipo", "nif",
        "direccion", "ciudad", "cp", "telefono", "email", "notas", "created_at"
    );

    // ── Records ───────────────────────────────────────────────────────────────

    public record ResultadoImportacion(
        int creados, int actualizados, int omitidos,
        List<String> columnasNuevas,
        List<String> errores
    ) {
        public String resumen() {
            StringBuilder sb = new StringBuilder();
            sb.append("✨ ").append(creados).append(" nuevo(s)   ·   ")
              .append("🔄 ").append(actualizados).append(" actualizado(s)   ·   ")
              .append(omitidos).append(" omitido(s)");
            if (!columnasNuevas.isEmpty())
                sb.append("\n\n📋 Columnas nuevas añadidas a la aplicación:\n")
                  .append("   ").append(String.join(", ", columnasNuevas));
            if (!errores.isEmpty()) {
                sb.append("\n\n⚠ Problemas (").append(errores.size()).append("):\n");
                errores.stream().limit(5).forEach(e -> sb.append("  • ").append(e).append("\n"));
                if (errores.size() > 5) sb.append("  … y ").append(errores.size() - 5).append(" más.");
            }
            return sb.toString().trim();
        }
    }

    private record TablaImportada(List<String> headers, List<Map<String, String>> filas) {}

    // ── Punto de entrada ─────────────────────────────────────────────────────

    public ResultadoImportacion importar(File archivo) throws Exception {
        String nombre = archivo.getName().toLowerCase();
        TablaImportada tabla;

        if (nombre.endsWith(".csv"))
            tabla = leerCSV(archivo.toPath());
        else if (nombre.endsWith(".json"))
            tabla = leerJSON(archivo.toPath());
        else if (nombre.endsWith(".sql"))
            tabla = leerSQL(archivo.toPath());
        else if (nombre.endsWith(".pdf"))
            tabla = leerPDF(archivo);
        else if (nombre.endsWith(".docx"))
            tabla = leerDocx(archivo);
        else if (nombre.endsWith(".doc"))
            tabla = leerDoc(archivo);
        else
            // Excel: xlsx, xls, xlsm, xlsb, xltx, xltm…
            tabla = leerExcel(archivo);

        if (tabla == null || tabla.headers().isEmpty())
            throw new Exception("No se encontraron datos con cabecera en el archivo.");

        return procesarTabla(tabla);
    }

    // ── Mapeo semántico de cabeceras → nombre de columna DB ──────────────────

    private String mapearColumna(String header) {
        String h = header.toLowerCase().trim();

        if (CAMPOS_FIJOS.contains(h)) return h; // ya es un nombre de columna exacto

        if (m(h, "nombre","razón social","razon social","empresa","company","nombre empresa",
                "nombre cliente","denominación","denominacion")) return "nombre";
        if (m(h, "apellidos","apellido"))                         return "apellidos";
        if (m(h, "nif","dni","cif","n.i.f","c.i.f","d.n.i","documento","tax id","vat"))
                                                                   return "nif";
        if (m(h, "email","correo","e-mail","mail","correo electrónico")) return "email";
        if (m(h, "teléfono","telefono","tel.","telf.","móvil","movil","phone","contacto",
                "teléfono contacto"))                              return "telefono";
        if (m(h, "dirección","direccion","domicilio","calle","address","dirección fiscal"))
                                                                   return "direccion";
        if (m(h, "ciudad","población","localidad","municipio","city","localidad / ciudad"))
                                                                   return "ciudad";
        if (m(h, "código postal","cod postal","c.p.","cp","postal","zip","código postal"))
                                                                   return "cp";
        if (m(h, "tipo","tipo cliente","tipo empresa","tipo persona","categoría cliente"))
                                                                   return "tipo";
        if (m(h, "notas","observaciones","comentarios","nota","obs","anotaciones"))
                                                                   return "notas";

        // Columna desconocida → prefijo ext_ para no colisionar con campos futuros
        return "ext_" + sanitizar(h);
    }

    private String sanitizar(String s) {
        return s.replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ñ","n")
                .replace("ü","u").replace("Á","a").replace("É","e")
                .replace("Í","i").replace("Ó","o").replace("Ú","u")
                .replace("Ñ","n")
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private boolean m(String h, String... keys) {
        for (String k : keys) if (h.contains(k)) return true;
        return false;
    }

    // ── Comparación y actualización del esquema ───────────────────────────────

    private List<String> obtenerColumnasDB() throws SQLException {
        List<String> cols = new ArrayList<>();
        try (Statement st = DatabaseManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(clientes)")) {
            while (rs.next()) cols.add(rs.getString("name").toLowerCase());
        }
        return cols;
    }

    private List<String> asegurarColumnas(Collection<String> dbCols) {
        List<String> nuevas = new ArrayList<>();
        try {
            List<String> existentes = obtenerColumnasDB();
            for (String col : dbCols) {
                if (CAMPOS_FIJOS.contains(col)) continue;
                if (existentes.contains(col.toLowerCase())) continue;
                try (Statement st = DatabaseManager.getConnection().createStatement()) {
                    st.execute("ALTER TABLE clientes ADD COLUMN \"" + col + "\" TEXT");
                    nuevas.add(col);
                    existentes.add(col.toLowerCase());
                } catch (SQLException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("ImportarClientesService: error asegurando columnas dinámicas — " + e.getMessage());
        }
        return nuevas;
    }

    // ── Lógica central de importación ─────────────────────────────────────────

    private ResultadoImportacion procesarTabla(TablaImportada tabla) throws Exception {
        // 1. Resolver la columna DB para cada cabecera del archivo
        Map<String, String> headerToCol = new LinkedHashMap<>();
        for (String h : tabla.headers()) {
            if (!h.isBlank()) headerToCol.put(h, mapearColumna(h));
        }

        // 2. Crear columnas nuevas en la BD si es necesario
        List<String> columnasNuevas = asegurarColumnas(headerToCol.values());

        // 3. Cargar clientes existentes para deduplicar
        ClienteDAO dao = new ClienteDAO(DatabaseManager.getConnection());
        List<Cliente> existentes;
        try { existentes = dao.findAll(); } catch (Exception e) { existentes = new ArrayList<>(); }

        // 4. Procesar filas
        int creados = 0, actualizados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        for (Map<String, String> fila : tabla.filas()) {
            try {
                Cliente nuevo = filaACliente(fila, headerToCol);
                if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) { omitidos++; continue; }

                final String nif    = nuevo.getNif();
                final String ncmpl  = nuevo.getNombreCompleto();
                Cliente encontrado  = existentes.stream().filter(e -> {
                    if (nif != null && !nif.isBlank() && e.getNif() != null)
                        return e.getNif().equalsIgnoreCase(nif);
                    return e.getNombreCompleto().equalsIgnoreCase(ncmpl);
                }).findFirst().orElse(null);

                if (encontrado != null) {
                    fusionar(encontrado, nuevo);
                    dao.save(encontrado);
                    actualizados++;
                } else {
                    dao.save(nuevo);
                    existentes.add(nuevo);
                    creados++;
                }
            } catch (Exception e) {
                errores.add(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }
        return new ResultadoImportacion(creados, actualizados, omitidos, columnasNuevas, errores);
    }

    private Cliente filaACliente(Map<String, String> fila, Map<String, String> headerToCol) {
        Cliente c = new Cliente();
        for (Map.Entry<String, String> entry : fila.entrySet()) {
            String val = entry.getValue();
            if (val == null || val.isBlank()) continue;
            String col = headerToCol.get(entry.getKey());
            if (col == null) continue;
            switch (col) {
                case "nombre"    -> c.setNombre(val.trim());
                case "apellidos" -> c.setApellidos(val.trim());
                case "tipo"      -> c.setTipo(val.trim().toLowerCase().contains("particular") ? "particular" : "empresa");
                case "nif"       -> c.setNif(val.trim());
                case "email"     -> c.setEmail(val.trim());
                case "telefono"  -> c.setTelefono(val.trim());
                case "direccion" -> c.setDireccion(val.trim());
                case "ciudad"    -> c.setCiudad(val.trim());
                case "cp"        -> c.setCp(val.trim());
                case "notas"     -> c.setNotas(val.trim());
                case "id", "created_at" -> { /* ignorar */ }
                default          -> c.setExtra(col, val.trim());
            }
        }
        if (c.getTipo() == null) c.setTipo("empresa");
        return c;
    }

    private void fusionar(Cliente dest, Cliente src) {
        if (v(src.getApellidos())) dest.setApellidos(src.getApellidos());
        if (v(src.getTipo()))      dest.setTipo(src.getTipo());
        if (v(src.getNif()))       dest.setNif(src.getNif());
        if (v(src.getEmail()))     dest.setEmail(src.getEmail());
        if (v(src.getTelefono()))  dest.setTelefono(src.getTelefono());
        if (v(src.getDireccion())) dest.setDireccion(src.getDireccion());
        if (v(src.getCiudad()))    dest.setCiudad(src.getCiudad());
        if (v(src.getCp()))        dest.setCp(src.getCp());
        if (v(src.getNotas()))     dest.setNotas(src.getNotas());
        src.getExtras().forEach((k, val) -> { if (v(val)) dest.setExtra(k, val); });
    }

    // ── Lectores por formato ──────────────────────────────────────────────────

    // --- Excel (xlsx, xls, xlsm, xlsb, xltx, xltm, …) -----------------------

    private TablaImportada leerExcel(File archivo) throws Exception {
        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook wb = WorkbookFactory.create(fis)) {

            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                int hRow = detectarCabecera(sheet);
                if (hRow < 0) continue;

                List<String> headers = new ArrayList<>();
                for (Cell cell : sheet.getRow(hRow)) {
                    String h = celdaString(cell).trim();
                    headers.add(h.isBlank() ? "col_" + cell.getColumnIndex() : h);
                }
                if (headers.isEmpty()) continue;

                List<Map<String, String>> filas = new ArrayList<>();
                for (int ri = hRow + 1; ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null || !tieneDatos(row)) continue;
                    Map<String, String> fila = new LinkedHashMap<>();
                    for (int ci = 0; ci < headers.size(); ci++)
                        fila.put(headers.get(ci), celdaString(row.getCell(ci)));
                    filas.add(fila);
                }
                if (!filas.isEmpty()) return new TablaImportada(headers, filas);
            }
        }
        throw new Exception("El archivo Excel no contiene hojas con datos y cabecera.");
    }

    // --- CSV -----------------------------------------------------------------

    private TablaImportada leerCSV(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1); // BOM

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) throw new Exception("El CSV no tiene filas de datos.");

        char sep = detectarSeparadorCSV(lineas[0]);
        String[] headers = parsearCSV(lineas[0], sep);

        List<Map<String, String>> filas = new ArrayList<>();
        for (int i = 1; i < lineas.length; i++) {
            if (lineas[i].trim().isEmpty()) continue;
            String[] vals = parsearCSV(lineas[i], sep);
            Map<String, String> fila = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++)
                fila.put(headers[j], j < vals.length ? vals[j] : "");
            filas.add(fila);
        }
        return new TablaImportada(List.of(headers), filas);
    }

    private char detectarSeparadorCSV(String linea) {
        long puntos = linea.chars().filter(c -> c == ';').count();
        long comas  = linea.chars().filter(c -> c == ',').count();
        return puntos >= comas ? ';' : ',';
    }

    // --- JSON ----------------------------------------------------------------

    private TablaImportada leerJSON(Path path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(path.toFile());

        JsonNode arr = null;
        if (root.isArray()) {
            arr = root;
        } else if (root.has("clientes") && root.get("clientes").isArray()) {
            arr = root.get("clientes");
        } else if (root.has("tables") && root.get("tables").has("clientes")) {
            arr = root.get("tables").get("clientes");
        } else {
            for (JsonNode node : root) {
                if (node.isArray() && node.size() > 0) { arr = node; break; }
            }
        }

        if (arr == null || !arr.isArray() || arr.size() == 0)
            throw new Exception("El JSON no contiene un array de clientes reconocible.");

        List<String> headers = new ArrayList<>();
        arr.get(0).fieldNames().forEachRemaining(headers::add);

        List<Map<String, String>> filas = new ArrayList<>();
        for (JsonNode node : arr) {
            Map<String, String> fila = new LinkedHashMap<>();
            for (String h : headers) {
                JsonNode val = node.get(h);
                fila.put(h, val == null || val.isNull() ? "" : val.asText());
            }
            filas.add(fila);
        }
        return new TablaImportada(headers, filas);
    }

    // --- SQL -----------------------------------------------------------------

    private TablaImportada leerSQL(Path path) throws Exception {
        String contenido = Files.readString(path, StandardCharsets.UTF_8);
        List<String> headers = null;
        List<Map<String, String>> filas = new ArrayList<>();

        String upper = contenido.toUpperCase();
        int pos = 0;
        while (true) {
            int idx = upper.indexOf("INSERT", pos);
            if (idx < 0) break;

            String slice = upper.substring(idx);
            boolean esNuestro = slice.startsWith("INSERT INTO CLIENTES") ||
                                slice.startsWith("INSERT OR REPLACE INTO CLIENTES");
            if (!esNuestro) { pos = idx + 6; continue; }

            int fin = contenido.indexOf(';', idx);
            if (fin < 0) fin = contenido.length();
            String stmt = contenido.substring(idx, fin).trim();

            // Extraer lista de columnas
            int cp1 = stmt.indexOf('(');
            int cp2 = stmt.indexOf(')', cp1 + 1);
            if (cp1 < 0 || cp2 < 0) { pos = fin + 1; continue; }
            String[] cols = Arrays.stream(stmt.substring(cp1 + 1, cp2).split(","))
                .map(s -> s.trim().replaceAll("[`\"'\\[\\]]", ""))
                .toArray(String[]::new);
            if (headers == null) headers = Arrays.asList(cols);

            // Extraer VALUES (…)
            int vi = stmt.toUpperCase().indexOf("VALUES", cp2);
            if (vi < 0) { pos = fin + 1; continue; }
            String valsPart = stmt.substring(vi + "VALUES".length()).trim();
            if (valsPart.startsWith("(")) valsPart = valsPart.substring(1);
            if (valsPart.endsWith(")"))   valsPart = valsPart.substring(0, valsPart.length() - 1);
            List<String> vals = parsearValoresSQL(valsPart);

            Map<String, String> fila = new LinkedHashMap<>();
            for (int i = 0; i < cols.length; i++)
                fila.put(cols[i], i < vals.size() ? vals.get(i) : null);
            filas.add(fila);

            pos = fin + 1;
        }

        if (headers == null || filas.isEmpty())
            throw new Exception("El SQL no contiene INSERT INTO clientes válidos.");

        return new TablaImportada(headers, filas);
    }

    // --- Word .docx ----------------------------------------------------------

    private TablaImportada leerDocx(File archivo) throws Exception {
        try (FileInputStream fis = new FileInputStream(archivo);
             XWPFDocument doc = new XWPFDocument(fis)) {
            if (!doc.getTables().isEmpty())
                return parsearTablaXWPF(doc.getTables().get(0));

            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText().trim();
                if (!t.isEmpty()) sb.append(t).append("\n");
            }
            return parsearTextoTabular(sb.toString());
        }
    }

    // --- Word .doc -----------------------------------------------------------

    private TablaImportada leerDoc(File archivo) throws Exception {
        try (FileInputStream fis = new FileInputStream(archivo);
             HWPFDocument doc = new HWPFDocument(fis)) {
            TableIterator it = new TableIterator(doc.getRange());
            if (it.hasNext()) return parsearTablaHWPF(it.next());
            return parsearTextoTabular(doc.getDocumentText());
        }
    }

    // --- PDF -----------------------------------------------------------------

    private TablaImportada leerPDF(File archivo) throws Exception {
        try (PDDocument doc = PDDocument.load(archivo)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return parsearTextoTabular(stripper.getText(doc));
        }
    }

    // ── Parsers de tabla Word ─────────────────────────────────────────────────

    private TablaImportada parsearTablaXWPF(XWPFTable table) throws Exception {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) throw new Exception("La tabla del documento Word está vacía.");

        List<String> headers = new ArrayList<>();
        for (XWPFTableCell cell : rows.get(0).getTableCells()) {
            String h = cell.getText().trim();
            headers.add(h.isBlank() ? "col_" + headers.size() : h);
        }

        List<Map<String, String>> filas = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<XWPFTableCell> cells = rows.get(i).getTableCells();
            Map<String, String> fila = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                String val = j < cells.size() ? cells.get(j).getText().trim() : "";
                fila.put(headers.get(j), val);
            }
            filas.add(fila);
        }
        return new TablaImportada(headers, filas);
    }

    private TablaImportada parsearTablaHWPF(org.apache.poi.hwpf.usermodel.Table table) throws Exception {
        if (table.numRows() == 0) throw new Exception("La tabla del documento .doc está vacía.");

        List<String> headers = new ArrayList<>();
        TableRow hr = table.getRow(0);
        for (int c = 0; c < hr.numCells(); c++) {
            String h = hr.getCell(c).text().trim().replaceAll("[\r\n]", "");
            headers.add(h.isBlank() ? "col_" + c : h);
        }

        List<Map<String, String>> filas = new ArrayList<>();
        for (int r = 1; r < table.numRows(); r++) {
            TableRow row = table.getRow(r);
            Map<String, String> fila = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String val = c < row.numCells()
                    ? row.getCell(c).text().trim().replaceAll("[\r\n]", "") : "";
                fila.put(headers.get(c), val);
            }
            filas.add(fila);
        }
        return new TablaImportada(headers, filas);
    }

    // ── Parser de texto plano (PDF / texto sin tabla) ─────────────────────────

    private TablaImportada parsearTextoTabular(String texto) throws Exception {
        String[] lineas = texto.split("\r?\n");
        List<String[]> rows = new ArrayList<>();

        for (String linea : lineas) {
            if (linea.trim().isEmpty()) continue;
            String[] partes;
            if (linea.contains("\t")) {
                partes = linea.split("\t");
            } else if (linea.contains(";")) {
                partes = parsearCSV(linea, ';');
            } else if (linea.contains(",")) {
                partes = parsearCSV(linea, ',');
            } else {
                partes = linea.trim().split("\\s{2,}");
            }
            if (partes.length > 1)
                rows.add(Arrays.stream(partes).map(String::trim).toArray(String[]::new));
        }

        if (rows.size() < 2)
            throw new Exception("No se detectó una tabla con datos en el documento.\n" +
                "Asegúrate de que el documento contiene una tabla o datos en columnas.");

        String[] headers = rows.get(0);
        List<Map<String, String>> filas = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            Map<String, String> fila = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++)
                fila.put(headers[j], j < rows.get(i).length ? rows.get(i)[j] : "");
            filas.add(fila);
        }
        return new TablaImportada(List.of(headers), filas);
    }

    // ── Parsers auxiliares ────────────────────────────────────────────────────

    private String[] parsearCSV(String linea, char sep) {
        List<String> campos = new ArrayList<>();
        boolean enComillas = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else {
                    enComillas = !enComillas;
                }
            } else if (c == sep && !enComillas) {
                campos.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }

    private List<String> parsearValoresSQL(String input) {
        List<String> vals = new ArrayList<>();
        boolean inStr = false;
        char sc = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!inStr && (c == '\'' || c == '"')) {
                inStr = true; sc = c;
            } else if (inStr && c == sc) {
                if (i + 1 < input.length() && input.charAt(i + 1) == sc) {
                    sb.append(c); i++;
                } else {
                    inStr = false;
                }
            } else if (!inStr && c == ',') {
                String t = sb.toString().trim();
                vals.add(t.equalsIgnoreCase("NULL") ? null : t);
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        String last = sb.toString().trim();
        vals.add(last.equalsIgnoreCase("NULL") ? null : last);
        return vals;
    }

    // ── Helpers Excel ─────────────────────────────────────────────────────────

    private int detectarCabecera(Sheet sheet) {
        for (Row row : sheet) { if (tieneDatos(row)) return row.getRowNum(); }
        return -1;
    }

    private boolean tieneDatos(Row row) {
        if (row == null) return false;
        for (Cell cell : row) if (!celdaString(cell).isBlank()) return true;
        return false;
    }

    private String celdaString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try { yield cell.getLocalDateTimeCellValue().toLocalDate().toString(); }
                    catch (Exception e) { yield ""; }
                }
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) && !Double.isInfinite(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { try { yield cell.getStringCellValue(); } catch (Exception e2) { yield ""; } }
            }
            default -> "";
        };
    }

    private boolean v(String s) { return s != null && !s.isBlank(); }
}
