package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gipsybuho.db.DatabaseManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ImportBackupService {

    private static final String[] TABLAS_ORDEN = {
        "config", "clientes", "empleados", "tarifas", "materiales",
        "consumo_material_tecnica", "movimientos_material", "pagos_material",
        "presupuestos", "lineas_presupuesto", "facturas", "lineas_factura",
        "pedidos", "pagos_pedido", "nominas", "notas_calendario",
        "albaranes", "lineas_albaran"
    };

    // ─── 1. RESTAURAR DESDE .db ───────────────────────────────────────────────

    public static void restaurarSQLite(Path origen) throws Exception {
        File dbActual = ExportService.getDbFile();
        Path dbPath   = dbActual.toPath();
        Path tempPath = dbPath.getParent().resolve("graficas_mulberry_restore_temp.db");

        DatabaseManager.closeConnection();
        Files.copy(dbPath, tempPath, StandardCopyOption.REPLACE_EXISTING);

        try {
            Files.copy(origen, dbPath, StandardCopyOption.REPLACE_EXISTING);
            DatabaseManager.initialize();
            Files.deleteIfExists(tempPath);
        } catch (Exception e) {
            try {
                Files.copy(tempPath, dbPath, StandardCopyOption.REPLACE_EXISTING);
                DatabaseManager.initialize();
            } catch (Exception ignored) {}
            Files.deleteIfExists(tempPath);
            throw e;
        }
    }

    // ─── 2. RESTAURAR DESDE .zip (CSV) ────────────────────────────────────────

    public static void restaurarZipCSV(Path origen) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        try {
            try (ZipInputStream zis = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(origen.toFile())),
                    StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String nombre = entry.getName();
                    if (!nombre.endsWith(".csv")) { zis.closeEntry(); continue; }
                    String tabla = nombre.substring(0, nombre.length() - 4);
                    if (!tablaExiste(conn, tabla)) { zis.closeEntry(); continue; }

                    byte[] bytes = zis.readAllBytes();
                    String contenido = new String(bytes, StandardCharsets.UTF_8);
                    if (contenido.startsWith("﻿")) contenido = contenido.substring(1);
                    restaurarTablaCSV(conn, tabla, contenido);
                    zis.closeEntry();
                }
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
    }

    private static void restaurarTablaCSV(Connection conn, String tabla, String contenido) throws Exception {
        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 1) return;

        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return;

        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM " + tabla);
        }
        if (lineas.length < 2) return;

        String colNames     = String.join(", ", cols);
        String placeholders = String.join(", ", Collections.nCopies(cols.length, "?"));
        String sql = "INSERT OR REPLACE INTO " + tabla + " (" + colNames + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ─── 3. RESTAURAR DESDE .sql ──────────────────────────────────────────────

    public static void restaurarSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        try (Statement st = conn.createStatement()) {
            StringBuilder buffer    = new StringBuilder();
            boolean       inStatement = false;

            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                if (!inStatement && (trimmed.startsWith("--") || trimmed.isEmpty())) continue;

                buffer.append(linea).append("\n");
                inStatement = true;

                if (trimmed.endsWith(";")) {
                    String stmt = buffer.toString().trim();
                    if (stmt.endsWith(";")) stmt = stmt.substring(0, stmt.length() - 1).trim();
                    if (!stmt.isEmpty()) st.execute(stmt);
                    buffer.setLength(0);
                    inStatement = false;
                }
            }
        } catch (Exception e) {
            try (Statement st = conn.createStatement()) { st.execute("ROLLBACK"); }
            catch (Exception ignored) {}
            try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
            catch (Exception ignored) {}
            throw e;
        }
    }

    // ─── 4. RESTAURAR DESDE .json ─────────────────────────────────────────────

    public static void restaurarJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tables = root.get("tables");
        if (tables == null)
            throw new Exception("El archivo JSON no tiene el formato de backup esperado (falta la clave 'tables').");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        try {
            for (String tabla : TABLAS_ORDEN) {
                JsonNode arr = tables.get(tabla);
                if (arr == null || !arr.isArray() || !tablaExiste(conn, tabla)) continue;
                restaurarTablaJSON(conn, tabla, arr);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
    }

    private static void restaurarTablaJSON(Connection conn, String tabla, JsonNode arr) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM " + tabla);
        }
        if (!arr.has(0)) return;

        List<String> cols = new ArrayList<>();
        arr.get(0).fieldNames().forEachRemaining(cols::add);
        if (cols.isEmpty()) return;

        String colNames     = String.join(", ", cols);
        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT OR REPLACE INTO " + tabla + " (" + colNames + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode fila : arr) {
                for (int j = 0; j < cols.size(); j++) {
                    JsonNode val = fila.get(cols.get(j));
                    if (val == null || val.isNull()) {
                        ps.setNull(j + 1, Types.NULL);
                    } else if (val.isIntegralNumber()) {
                        ps.setLong(j + 1, val.asLong());
                    } else if (val.isFloatingPointNumber()) {
                        ps.setDouble(j + 1, val.asDouble());
                    } else {
                        ps.setString(j + 1, val.asText());
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ─── IMPORTAR PEDIDOS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ──────

    public static int importarPedidosExcel(Path origen) throws Exception {
        List<String[]> filasPed    = new ArrayList<>();
        List<String[]> filasPagos  = new ArrayList<>();
        leerLibroExcel(origen, filasPed, filasPagos);
        if (filasPed.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "pedidos",      filasPed);
            count += importarFilasEnTabla(conn, "pagos_pedido", filasPagos);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR PEDIDOS (WORD: .docx / .doc) ───────────────────────────────

    public static int importarPedidosWord(Path origen) throws Exception {
        List<String[]> filasPed   = new ArrayList<>();
        List<String[]> filasPagos = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasPed, filasPagos);
        else
            leerWordDocx(origen, filasPed, filasPagos);
        if (filasPed.size() < 2)
            throw new Exception("No se encontraron tablas con datos de pedidos en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "pedidos",      filasPed);
            count += importarFilasEnTabla(conn, "pagos_pedido", filasPagos);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR PEDIDOS (PDF) ───────────────────────────────────────────────

    public static int importarPedidosPDF(Path origen) throws Exception {
        List<String[]> filasPed = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasPed.add(vals);
            }
        }
        if (filasPed.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "pedidos", filasPed);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── Helpers: lectura de formatos externos ────────────────────────────────

    private static void leerLibroExcel(Path path, List<String[]> filasPres, List<String[]> filasLineas) throws Exception {
        String nombre = path.getFileName().toString().toLowerCase();
        try (org.apache.poi.ss.usermodel.Workbook wb =
                 org.apache.poi.ss.usermodel.WorkbookFactory.create(path.toFile(), null, true)) {
            if (wb.getNumberOfSheets() > 0) leerHojaExcel(wb.getSheetAt(0), filasPres);
            if (wb.getNumberOfSheets() > 1) leerHojaExcel(wb.getSheetAt(1), filasLineas);
        } catch (Exception e) {
            if (nombre.endsWith(".xlsb")) {
                throw new Exception("El formato .xlsb (Excel Binario) no está soportado directamente. " +
                    "Ábrelo en Excel y guárdalo como .xlsx antes de importar.", e);
            }
            throw e;
        }
    }

    private static void leerHojaExcel(org.apache.poi.ss.usermodel.Sheet hoja, List<String[]> dest) {
        org.apache.poi.ss.usermodel.DataFormatter fmt = new org.apache.poi.ss.usermodel.DataFormatter();
        for (org.apache.poi.ss.usermodel.Row fila : hoja) {
            int nc = fila.getLastCellNum();
            if (nc <= 0) continue;
            String[] vals = new String[nc];
            boolean blank = true;
            for (int c = 0; c < nc; c++) {
                org.apache.poi.ss.usermodel.Cell cell = fila.getCell(c,
                    org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                vals[c] = cell == null ? "" : fmt.formatCellValue(cell).trim();
                if (!vals[c].isBlank()) blank = false;
            }
            if (!blank) dest.add(vals);
        }
    }

    private static void leerWordDocx(Path path, List<String[]> filasPres, List<String[]> filasLineas) throws Exception {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc =
                 new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.FileInputStream(path.toFile()))) {
            List<org.apache.poi.xwpf.usermodel.XWPFTable> tablas = doc.getTables();
            if (!tablas.isEmpty()) leerTablaXWPF(tablas.get(0), filasPres);
            if (tablas.size() > 1)  leerTablaXWPF(tablas.get(1), filasLineas);
        }
    }

    private static void leerTablaXWPF(org.apache.poi.xwpf.usermodel.XWPFTable tabla, List<String[]> dest) {
        for (org.apache.poi.xwpf.usermodel.XWPFTableRow fila : tabla.getRows()) {
            List<org.apache.poi.xwpf.usermodel.XWPFTableCell> celdas = fila.getTableCells();
            String[] vals = new String[celdas.size()];
            boolean blank = true;
            for (int c = 0; c < celdas.size(); c++) {
                vals[c] = celdas.get(c).getText().trim();
                if (!vals[c].isBlank()) blank = false;
            }
            if (!blank) dest.add(vals);
        }
    }

    private static void leerWordDoc(Path path, List<String[]> filasPres, List<String[]> filasLineas) throws Exception {
        try (org.apache.poi.hwpf.HWPFDocument doc =
                 new org.apache.poi.hwpf.HWPFDocument(new java.io.FileInputStream(path.toFile()))) {
            org.apache.poi.hwpf.usermodel.TableIterator it =
                new org.apache.poi.hwpf.usermodel.TableIterator(doc.getRange());
            if (it.hasNext()) leerTablaHWPF(it.next(), filasPres);
            if (it.hasNext()) leerTablaHWPF(it.next(), filasLineas);
        }
    }

    private static void leerTablaHWPF(org.apache.poi.hwpf.usermodel.Table tabla, List<String[]> dest) {
        for (int r = 0; r < tabla.numRows(); r++) {
            org.apache.poi.hwpf.usermodel.TableRow fila = tabla.getRow(r);
            String[] vals = new String[fila.numCells()];
            boolean blank = true;
            for (int c = 0; c < fila.numCells(); c++) {
                vals[c] = fila.getCell(c).text().trim().replace("", "");
                if (!vals[c].isBlank()) blank = false;
            }
            if (!blank) dest.add(vals);
        }
    }

    private static String[] parsearLineaPDF(String linea) {
        if (linea.contains("\t"))    return linea.split("\t", -1);
        if (linea.contains(" | "))   return linea.split(" \\| ", -1);
        if (linea.contains("|"))     return linea.split("\\|", -1);
        if (linea.contains(";"))     return linea.split(";", -1);
        String[] parts = linea.trim().split("\\s{2,}");
        return parts.length > 1 ? parts : new String[]{linea.trim()};
    }

    private static int importarFilasEnTabla(Connection conn, String tabla, List<String[]> filas) throws Exception {
        if (filas.size() < 2) return 0;
        List<String> cols = Arrays.stream(filas.get(0))
            .map(h -> h.trim().replaceAll("[^\\p{L}\\p{N}_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", ""))
            .filter(h -> !h.isBlank())
            .collect(Collectors.toList());
        if (cols.isEmpty()) return 0;

        asegurarColumnas(conn, tabla, cols);

        String sql = "INSERT OR REPLACE INTO \"" + tabla + "\" (" +
            cols.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", ")) +
            ") VALUES (" + String.join(", ", Collections.nCopies(cols.size(), "?")) + ")";

        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < filas.size(); i++) {
                String[] vals = filas.get(i);
                for (int j = 0; j < cols.size(); j++) {
                    String v = j < vals.length ? vals[j].trim() : null;
                    ps.setString(j + 1, (v == null || v.isBlank()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private static void asegurarColumnas(Connection conn, String tabla, List<String> columnas) throws SQLException {
        Set<String> existentes = new HashSet<>();
        try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(\"" + tabla + "\")")) {
            while (rs.next()) existentes.add(rs.getString("name").toLowerCase());
        }
        for (String col : columnas) {
            if (col == null || col.isBlank()) continue;
            if (!existentes.contains(col.toLowerCase())) {
                conn.createStatement().execute("ALTER TABLE \"" + tabla + "\" ADD COLUMN \"" + col + "\" TEXT");
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** INSERT OR REPLACE de todos los registros de un array JSON en la tabla dada. */
    private static int importarTablaJSON(Connection conn, String tabla, JsonNode arr) throws Exception {
        List<String> cols = new ArrayList<>();
        arr.get(0).fieldNames().forEachRemaining(cols::add);
        if (cols.isEmpty()) return 0;

        String sql = "INSERT OR REPLACE INTO " + tabla + " (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.size(), "?")) + ")";

        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode fila : arr) {
                for (int j = 0; j < cols.size(); j++) {
                    JsonNode val = fila.get(cols.get(j));
                    if (val == null || val.isNull()) {
                        ps.setNull(j + 1, Types.NULL);
                    } else if (val.isIntegralNumber()) {
                        ps.setLong(j + 1, val.asLong());
                    } else if (val.isFloatingPointNumber()) {
                        ps.setDouble(j + 1, val.asDouble());
                    } else {
                        ps.setString(j + 1, val.asText());
                    }
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private static String[] parsearLineaCSV(String linea) {
        List<String> campos = new ArrayList<>();
        boolean dentroComillas = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (dentroComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    dentroComillas = !dentroComillas;
                }
            } else if (c == ';' && !dentroComillas) {
                campos.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        campos.add(sb.toString());
        return campos.toArray(new String[0]);
    }

    private static boolean tablaExiste(Connection conn, String tabla) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tabla);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ─── IMPORTAR ALBARANES (CSV / SQL / JSON) ───────────────────────────────
    // CSV exporta solo 'albaranes'. SQL y JSON exportan además 'lineas_albaran'.

    public static int importarAlbaranesCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "albaranes", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO albaranes (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarAlbaranesSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO ALBARANES") &&
                    !upper.startsWith("INSERT INTO LINEAS_ALBARAN")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarAlbaranesJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode  = root.has("tables") ? root.get("tables") : root;
        JsonNode arrAlb      = tablesNode.get("albaranes");
        JsonNode arrLineas   = tablesNode.get("lineas_albaran");

        if ((arrAlb == null || !arrAlb.isArray()) && (arrLineas == null || !arrLineas.isArray()))
            throw new Exception("El archivo JSON no contiene datos de albaranes válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrAlb != null && arrAlb.isArray() && arrAlb.has(0)) {
                List<String> colsAlb = new ArrayList<>();
                arrAlb.get(0).fieldNames().forEachRemaining(colsAlb::add);
                asegurarColumnas(conn, "albaranes", colsAlb);
                count += importarTablaJSON(conn, "albaranes", arrAlb);
            }
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0)) {
                List<String> colsLin = new ArrayList<>();
                arrLineas.get(0).fieldNames().forEachRemaining(colsLin::add);
                asegurarColumnas(conn, "lineas_albaran", colsLin);
                count += importarTablaJSON(conn, "lineas_albaran", arrLineas);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR ALBARANES (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ────

    public static int importarAlbaranesExcel(Path origen) throws Exception {
        List<String[]> filasAlb    = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        leerLibroExcel(origen, filasAlb, filasLineas);
        if (filasAlb.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "albaranes",      filasAlb);
            count += importarFilasEnTabla(conn, "lineas_albaran", filasLineas);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR ALBARANES (WORD: .docx / .doc) ─────────────────────────────

    public static int importarAlbaranesWord(Path origen) throws Exception {
        List<String[]> filasAlb    = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasAlb, filasLineas);
        else
            leerWordDocx(origen, filasAlb, filasLineas);
        if (filasAlb.size() < 2)
            throw new Exception("No se encontraron tablas con datos de albaranes en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "albaranes",      filasAlb);
            count += importarFilasEnTabla(conn, "lineas_albaran", filasLineas);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR ALBARANES (PDF) ─────────────────────────────────────────────

    public static int importarAlbaranesPDF(Path origen) throws Exception {
        List<String[]> filasAlb = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasAlb.add(vals);
            }
        }
        if (filasAlb.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF. " +
                "El documento debe tener una cabecera y filas separadas por tabuladores, «|» o dobles espacios.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "albaranes", filasAlb);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR FACTURAS (CSV / SQL / JSON) ────────────────────────────────
    // CSV exporta solo 'facturas'. SQL y JSON exportan además 'lineas_factura'.

    public static int importarFacturasCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "facturas", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO facturas (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarFacturasSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO FACTURAS") &&
                    !upper.startsWith("INSERT INTO LINEAS_FACTURA")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarFacturasJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arrFact    = tablesNode.get("facturas");
        JsonNode arrLineas  = tablesNode.get("lineas_factura");

        if ((arrFact == null || !arrFact.isArray()) && (arrLineas == null || !arrLineas.isArray()))
            throw new Exception("El archivo JSON no contiene datos de facturas válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrFact != null && arrFact.isArray() && arrFact.has(0)) {
                List<String> colsFact = new ArrayList<>();
                arrFact.get(0).fieldNames().forEachRemaining(colsFact::add);
                asegurarColumnas(conn, "facturas", colsFact);
                count += importarTablaJSON(conn, "facturas", arrFact);
            }
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0)) {
                List<String> colsLin = new ArrayList<>();
                arrLineas.get(0).fieldNames().forEachRemaining(colsLin::add);
                asegurarColumnas(conn, "lineas_factura", colsLin);
                count += importarTablaJSON(conn, "lineas_factura", arrLineas);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR FACTURAS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ────

    public static int importarFacturasExcel(Path origen) throws Exception {
        List<String[]> filasFact   = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        leerLibroExcel(origen, filasFact, filasLineas);
        if (filasFact.size() < 2) return 0;

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "facturas",       filasFact);
            count += importarFilasEnTabla(conn, "lineas_factura", filasLineas);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR FACTURAS (WORD: .docx / .doc) ──────────────────────────────

    public static int importarFacturasWord(Path origen) throws Exception {
        List<String[]> filasFact   = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        String nombre = origen.getFileName().toString().toLowerCase();
        if (nombre.endsWith(".doc")) {
            leerWordDoc(origen, filasFact, filasLineas);
        } else {
            leerWordDocx(origen, filasFact, filasLineas);
        }
        if (filasFact.size() < 2)
            throw new Exception("No se encontraron tablas con datos de facturas en el documento Word.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "facturas",       filasFact);
            count += importarFilasEnTabla(conn, "lineas_factura", filasLineas);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR FACTURAS (PDF) ─────────────────────────────────────────────

    public static int importarFacturasPDF(Path origen) throws Exception {
        List<String[]> filasFact = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasFact.add(vals);
            }
        }
        if (filasFact.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF. " +
                "El documento debe tener una cabecera y filas separadas por tabuladores, «|» o dobles espacios.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "facturas", filasFact);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR PRESUPUESTOS (CSV / SQL / JSON) ────────────────────────────
    // CSV exporta solo 'presupuestos'. SQL y JSON exportan además 'lineas_presupuesto'.

    public static int importarPresupuestosCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "presupuestos", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO presupuestos (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarPresupuestosSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO PRESUPUESTOS") &&
                    !upper.startsWith("INSERT INTO LINEAS_PRESUPUESTO")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarPresupuestosJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        // Admite exportación individual {"presupuestos":[...], "lineas_presupuesto":[...]}
        // y backup completo {"tables":{"presupuestos":[...], "lineas_presupuesto":[...]}}
        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;

        JsonNode arrPres   = tablesNode.get("presupuestos");
        JsonNode arrLineas = tablesNode.get("lineas_presupuesto");

        if ((arrPres == null || !arrPres.isArray()) && (arrLineas == null || !arrLineas.isArray()))
            throw new Exception("El archivo JSON no contiene datos de presupuestos válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrPres != null && arrPres.isArray() && arrPres.has(0)) {
                List<String> colsPres = new ArrayList<>();
                arrPres.get(0).fieldNames().forEachRemaining(colsPres::add);
                asegurarColumnas(conn, "presupuestos", colsPres);
                count += importarTablaJSON(conn, "presupuestos", arrPres);
            }
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0)) {
                List<String> colsLin = new ArrayList<>();
                arrLineas.get(0).fieldNames().forEachRemaining(colsLin::add);
                asegurarColumnas(conn, "lineas_presupuesto", colsLin);
                count += importarTablaJSON(conn, "lineas_presupuesto", arrLineas);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR PRESUPUESTOS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ─

    public static int importarPresupuestosExcel(Path origen) throws Exception {
        List<String[]> filasPres   = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        leerLibroExcel(origen, filasPres, filasLineas);
        if (filasPres.size() < 2) return 0;

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "presupuestos",       filasPres);
            count += importarFilasEnTabla(conn, "lineas_presupuesto", filasLineas);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR PRESUPUESTOS (WORD: .docx / .doc) ──────────────────────────

    public static int importarPresupuestosWord(Path origen) throws Exception {
        List<String[]> filasPres   = new ArrayList<>();
        List<String[]> filasLineas = new ArrayList<>();
        String nombre = origen.getFileName().toString().toLowerCase();
        if (nombre.endsWith(".doc")) {
            leerWordDoc(origen, filasPres, filasLineas);
        } else {
            leerWordDocx(origen, filasPres, filasLineas);
        }
        if (filasPres.size() < 2)
            throw new Exception("No se encontraron tablas con datos de presupuestos en el documento Word.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "presupuestos",       filasPres);
            count += importarFilasEnTabla(conn, "lineas_presupuesto", filasLineas);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR PRESUPUESTOS (PDF) ─────────────────────────────────────────

    public static int importarPresupuestosPDF(Path origen) throws Exception {
        List<String[]> filasPres = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasPres.add(vals);
            }
        }
        if (filasPres.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF. " +
                "El documento debe tener una cabecera y filas separadas por tabuladores, «|» o dobles espacios.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "presupuestos", filasPres);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR NÓMINAS (CSV / SQL / JSON) ─────────────────────────────────

    public static int importarNominasCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "nominas", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO nominas (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarNominasSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                if (!trimmed.toUpperCase().startsWith("INSERT INTO NOMINAS")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO nominas" +
                    stmt.substring("INSERT INTO nominas".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarNominasJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arr = tablesNode.get("nominas");
        if (arr == null || !arr.isArray())
            throw new Exception("El archivo JSON no contiene datos de nóminas válidos.");
        if (!arr.has(0)) return 0;

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            List<String> colsNom = new ArrayList<>();
            arr.get(0).fieldNames().forEachRemaining(colsNom::add);
            asegurarColumnas(conn, "nominas", colsNom);
            int count = importarTablaJSON(conn, "nominas", arr);
            conn.commit();
            return count;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─── IMPORTAR NÓMINAS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ──────

    public static int importarNominasExcel(Path origen) throws Exception {
        List<String[]> filasNom  = new ArrayList<>();
        List<String[]> filasIgn  = new ArrayList<>();
        leerLibroExcel(origen, filasNom, filasIgn);
        if (filasNom.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "nominas", filasNom);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR NÓMINAS (WORD: .docx / .doc) ───────────────────────────────

    public static int importarNominasWord(Path origen) throws Exception {
        List<String[]> filasNom = new ArrayList<>();
        List<String[]> filasIgn = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasNom, filasIgn);
        else
            leerWordDocx(origen, filasNom, filasIgn);
        if (filasNom.size() < 2)
            throw new Exception("No se encontraron tablas con datos de nóminas en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "nominas", filasNom);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR NÓMINAS (PDF) ───────────────────────────────────────────────

    public static int importarNominasPDF(Path origen) throws Exception {
        List<String[]> filasNom = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasNom.add(vals);
            }
        }
        if (filasNom.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF.");
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "nominas", filasNom);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR EMPLEADOS (CSV / SQL / JSON) ───────────────────────────────
    // CSV exporta solo 'empleados'. SQL y JSON exportan además 'nominas'.

    public static int importarEmpleadosCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "empleados", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO empleados (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarEmpleadosSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO EMPLEADOS") &&
                    !upper.startsWith("INSERT INTO NOMINAS")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarEmpleadosJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arrEmp     = tablesNode.get("empleados");
        JsonNode arrNom     = tablesNode.get("nominas");

        if ((arrEmp == null || !arrEmp.isArray()) && (arrNom == null || !arrNom.isArray()))
            throw new Exception("El archivo JSON no contiene datos de empleados válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrEmp != null && arrEmp.isArray() && arrEmp.has(0)) {
                List<String> colsEmp = new ArrayList<>();
                arrEmp.get(0).fieldNames().forEachRemaining(colsEmp::add);
                asegurarColumnas(conn, "empleados", colsEmp);
                count += importarTablaJSON(conn, "empleados", arrEmp);
            }
            if (arrNom != null && arrNom.isArray() && arrNom.has(0)) {
                List<String> colsNom = new ArrayList<>();
                arrNom.get(0).fieldNames().forEachRemaining(colsNom::add);
                asegurarColumnas(conn, "nominas", colsNom);
                count += importarTablaJSON(conn, "nominas", arrNom);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR EMPLEADOS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ────

    public static int importarEmpleadosExcel(Path origen) throws Exception {
        List<String[]> filasEmp    = new ArrayList<>();
        List<String[]> filasNom    = new ArrayList<>();
        leerLibroExcel(origen, filasEmp, filasNom);
        if (filasEmp.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "empleados", filasEmp);
            count += importarFilasEnTabla(conn, "nominas",   filasNom);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR EMPLEADOS (WORD: .docx / .doc) ─────────────────────────────

    public static int importarEmpleadosWord(Path origen) throws Exception {
        List<String[]> filasEmp = new ArrayList<>();
        List<String[]> filasNom = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasEmp, filasNom);
        else
            leerWordDocx(origen, filasEmp, filasNom);
        if (filasEmp.size() < 2)
            throw new Exception("No se encontraron tablas con datos de empleados en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "empleados", filasEmp);
            count += importarFilasEnTabla(conn, "nominas",   filasNom);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR EMPLEADOS (PDF) ─────────────────────────────────────────────

    public static int importarEmpleadosPDF(Path origen) throws Exception {
        List<String[]> filasEmp = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasEmp.add(vals);
            }
        }
        if (filasEmp.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "empleados", filasEmp);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR MATERIALES (CSV / SQL / JSON) ──────────────────────────────
    // CSV exporta solo 'materiales'. SQL y JSON exportan además
    // 'consumo_material_tecnica', 'movimientos_material' y 'pagos_material'.

    public static int importarMaterialesCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "materiales", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO materiales (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarMaterialesSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO MATERIALES") &&
                    !upper.startsWith("INSERT INTO CONSUMO_MATERIAL_TECNICA") &&
                    !upper.startsWith("INSERT INTO MOVIMIENTOS_MATERIAL") &&
                    !upper.startsWith("INSERT INTO PAGOS_MATERIAL")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarMaterialesJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arrMat     = tablesNode.get("materiales");
        JsonNode arrConsumo = tablesNode.get("consumo_material_tecnica");
        JsonNode arrMovim   = tablesNode.get("movimientos_material");
        JsonNode arrPagos   = tablesNode.get("pagos_material");

        if ((arrMat == null || !arrMat.isArray()) && (arrConsumo == null || !arrConsumo.isArray()) &&
            (arrMovim == null || !arrMovim.isArray()) && (arrPagos == null || !arrPagos.isArray()))
            throw new Exception("El archivo JSON no contiene datos de materiales válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrMat != null && arrMat.isArray() && arrMat.has(0)) {
                List<String> colsMat = new ArrayList<>();
                arrMat.get(0).fieldNames().forEachRemaining(colsMat::add);
                asegurarColumnas(conn, "materiales", colsMat);
                count += importarTablaJSON(conn, "materiales", arrMat);
            }
            if (arrConsumo != null && arrConsumo.isArray() && arrConsumo.has(0)) {
                List<String> colsCon = new ArrayList<>();
                arrConsumo.get(0).fieldNames().forEachRemaining(colsCon::add);
                asegurarColumnas(conn, "consumo_material_tecnica", colsCon);
                count += importarTablaJSON(conn, "consumo_material_tecnica", arrConsumo);
            }
            if (arrMovim != null && arrMovim.isArray() && arrMovim.has(0)) {
                List<String> colsMov = new ArrayList<>();
                arrMovim.get(0).fieldNames().forEachRemaining(colsMov::add);
                asegurarColumnas(conn, "movimientos_material", colsMov);
                count += importarTablaJSON(conn, "movimientos_material", arrMovim);
            }
            if (arrPagos != null && arrPagos.isArray() && arrPagos.has(0)) {
                List<String> colsPag = new ArrayList<>();
                arrPagos.get(0).fieldNames().forEachRemaining(colsPag::add);
                asegurarColumnas(conn, "pagos_material", colsPag);
                count += importarTablaJSON(conn, "pagos_material", arrPagos);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── IMPORTAR MATERIALES (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ───

    public static int importarMaterialesExcel(Path origen) throws Exception {
        List<String[]> filasMat = new ArrayList<>();
        List<String[]> filasIgn = new ArrayList<>();
        leerLibroExcel(origen, filasMat, filasIgn);
        if (filasMat.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "materiales", filasMat);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR MATERIALES (WORD: .docx / .doc) ────────────────────────────

    public static int importarMaterialesWord(Path origen) throws Exception {
        List<String[]> filasMat = new ArrayList<>();
        List<String[]> filasIgn = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasMat, filasIgn);
        else
            leerWordDocx(origen, filasMat, filasIgn);
        if (filasMat.size() < 2)
            throw new Exception("No se encontraron tablas con datos de materiales en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "materiales", filasMat);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR MATERIALES (PDF) ────────────────────────────────────────────

    public static int importarMaterialesPDF(Path origen) throws Exception {
        List<String[]> filasMat = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasMat.add(vals);
            }
        }
        if (filasMat.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF.");
        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "materiales", filasMat);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); activarFK(conn); }
        return count;
    }

    // ─── IMPORTAR TARIFAS (CSV / SQL / JSON) ─────────────────────────────────

    public static int importarTarifasCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "tarifas", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO tarifas (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarTarifasSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                if (!trimmed.toUpperCase().startsWith("INSERT INTO TARIFAS")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO tarifas" +
                    stmt.substring("INSERT INTO tarifas".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarTarifasJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arr = tablesNode.get("tarifas");
        if (arr == null || !arr.isArray())
            throw new Exception("El archivo JSON no contiene datos de tarifas válidos.");
        if (!arr.has(0)) return 0;

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            List<String> colsTar = new ArrayList<>();
            arr.get(0).fieldNames().forEachRemaining(colsTar::add);
            asegurarColumnas(conn, "tarifas", colsTar);
            int count = importarTablaJSON(conn, "tarifas", arr);
            conn.commit();
            return count;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─── IMPORTAR TARIFAS (EXCEL: .xlsx / .xls / .xlsb / .xlsm / .xltx) ──────

    public static int importarTarifasExcel(Path origen) throws Exception {
        List<String[]> filasTar = new ArrayList<>();
        List<String[]> filasIgn = new ArrayList<>();
        leerLibroExcel(origen, filasTar, filasIgn);
        if (filasTar.size() < 2) return 0;
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "tarifas", filasTar);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR TARIFAS (WORD: .docx / .doc) ───────────────────────────────

    public static int importarTarifasWord(Path origen) throws Exception {
        List<String[]> filasTar = new ArrayList<>();
        List<String[]> filasIgn = new ArrayList<>();
        if (origen.getFileName().toString().toLowerCase().endsWith(".doc"))
            leerWordDoc(origen, filasTar, filasIgn);
        else
            leerWordDocx(origen, filasTar, filasIgn);
        if (filasTar.size() < 2)
            throw new Exception("No se encontraron tablas con datos de tarifas en el documento Word.");
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "tarifas", filasTar);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR TARIFAS (PDF) ───────────────────────────────────────────────

    public static int importarTarifasPDF(Path origen) throws Exception {
        List<String[]> filasTar = new ArrayList<>();
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                 org.apache.pdfbox.pdmodel.PDDocument.load(origen.toFile())) {
            org.apache.pdfbox.text.PDFTextStripper ts = new org.apache.pdfbox.text.PDFTextStripper();
            ts.setSortByPosition(true);
            for (String linea : ts.getText(doc).split("\r?\n")) {
                if (linea.trim().isEmpty()) continue;
                String[] vals = parsearLineaPDF(linea);
                if (vals.length > 1) filasTar.add(vals);
            }
        }
        if (filasTar.size() < 2)
            throw new Exception("No se encontraron datos tabulares en el PDF.");
        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        int count = 0;
        try {
            count += importarFilasEnTabla(conn, "tarifas", filasTar);
            conn.commit();
        } catch (Exception e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
        return count;
    }

    // ─── IMPORTAR CLIENTES (CSV / SQL / JSON) ────────────────────────────────
    // Semántica: INSERT OR REPLACE — añade nuevos y actualiza existentes por ID.

    public static int importarClientesCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        String sql = "INSERT OR REPLACE INTO clientes (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarClientesSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                if (!trimmed.toUpperCase().startsWith("INSERT INTO CLIENTES")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO clientes" +
                    stmt.substring("INSERT INTO clientes".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarClientesJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arr = tablesNode.get("clientes");
        if (arr == null || !arr.isArray())
            throw new Exception("El archivo JSON no contiene datos de clientes válidos.");
        if (!arr.has(0)) return 0;

        Connection conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        try {
            int count = importarTablaJSON(conn, "clientes", arr);
            conn.commit();
            return count;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─── IMPORTAR PEDIDOS (CSV / SQL / JSON) ─────────────────────────────────
    // CSV exporta solo 'pedidos'. SQL y JSON exportan además 'pagos_pedido'.

    public static int importarPedidosCSV(Path origen) throws Exception {
        byte[] bytes = Files.readAllBytes(origen);
        String contenido = new String(bytes, StandardCharsets.UTF_8);
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);

        String[] lineas = contenido.split("\r?\n");
        if (lineas.length < 2) return 0;
        String[] cols = parsearLineaCSV(lineas[0]);
        if (cols.length == 0) return 0;

        Connection conn = DatabaseManager.getConnection();
        asegurarColumnas(conn, "pedidos", Arrays.asList(cols));
        String sql = "INSERT OR REPLACE INTO pedidos (" +
            String.join(", ", cols) + ") VALUES (" +
            String.join(", ", Collections.nCopies(cols.length, "?")) + ")";

        int count = 0;
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i < lineas.length; i++) {
                if (lineas[i].trim().isEmpty()) continue;
                String[] vals = parsearLineaCSV(lineas[i]);
                for (int j = 0; j < cols.length; j++) {
                    String v = (j < vals.length) ? vals[j] : null;
                    ps.setString(j + 1, (v == null || v.isEmpty()) ? null : v);
                }
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarPedidosSQL(Path origen) throws Exception {
        String contenido = Files.readString(origen, StandardCharsets.UTF_8);
        Connection conn  = DatabaseManager.getConnection();

        int count = 0;
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String linea : contenido.split("\n")) {
                String trimmed = linea.trim();
                String upper   = trimmed.toUpperCase();
                if (!upper.startsWith("INSERT INTO PEDIDOS") &&
                    !upper.startsWith("INSERT INTO PAGOS_PEDIDO")) continue;
                String stmt = trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                stmt = "INSERT OR REPLACE INTO " + stmt.substring("INSERT INTO ".length());
                st.execute(stmt);
                count++;
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return count;
    }

    public static int importarPedidosJSON(Path origen) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(origen.toFile());

        JsonNode tablesNode = root.has("tables") ? root.get("tables") : root;
        JsonNode arrPed     = tablesNode.get("pedidos");
        JsonNode arrPagos   = tablesNode.get("pagos_pedido");

        if ((arrPed == null || !arrPed.isArray()) && (arrPagos == null || !arrPagos.isArray()))
            throw new Exception("El archivo JSON no contiene datos de pedidos válidos.");

        Connection conn = DatabaseManager.getConnection();
        desactivarFK(conn);
        conn.setAutoCommit(false);
        int count = 0;
        try {
            if (arrPed != null && arrPed.isArray() && arrPed.has(0)) {
                List<String> colsPed = new ArrayList<>();
                arrPed.get(0).fieldNames().forEachRemaining(colsPed::add);
                asegurarColumnas(conn, "pedidos", colsPed);
                count += importarTablaJSON(conn, "pedidos", arrPed);
            }
            if (arrPagos != null && arrPagos.isArray() && arrPagos.has(0)) {
                List<String> colsPag = new ArrayList<>();
                arrPagos.get(0).fieldNames().forEachRemaining(colsPag::add);
                asegurarColumnas(conn, "pagos_pedido", colsPag);
                count += importarTablaJSON(conn, "pagos_pedido", arrPagos);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            activarFK(conn);
        }
        return count;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static void desactivarFK(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = OFF"); }
    }

    private static void activarFK(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys = ON"); }
    }
}
