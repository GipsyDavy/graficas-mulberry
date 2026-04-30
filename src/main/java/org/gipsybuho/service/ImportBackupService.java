package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gipsybuho.db.DatabaseManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
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
            if (arrAlb    != null && arrAlb.isArray()    && arrAlb.has(0))
                count += importarTablaJSON(conn, "albaranes",     arrAlb);
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0))
                count += importarTablaJSON(conn, "lineas_albaran", arrLineas);
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
            if (arrFact   != null && arrFact.isArray()   && arrFact.has(0))
                count += importarTablaJSON(conn, "facturas",       arrFact);
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0))
                count += importarTablaJSON(conn, "lineas_factura", arrLineas);
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
            if (arrPres != null && arrPres.isArray() && arrPres.has(0))
                count += importarTablaJSON(conn, "presupuestos", arrPres);
            if (arrLineas != null && arrLineas.isArray() && arrLineas.has(0))
                count += importarTablaJSON(conn, "lineas_presupuesto", arrLineas);
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
            if (arrEmp != null && arrEmp.isArray() && arrEmp.has(0))
                count += importarTablaJSON(conn, "empleados", arrEmp);
            if (arrNom != null && arrNom.isArray() && arrNom.has(0))
                count += importarTablaJSON(conn, "nominas",   arrNom);
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
            if (arrMat     != null && arrMat.isArray()     && arrMat.has(0))
                count += importarTablaJSON(conn, "materiales",               arrMat);
            if (arrConsumo != null && arrConsumo.isArray() && arrConsumo.has(0))
                count += importarTablaJSON(conn, "consumo_material_tecnica", arrConsumo);
            if (arrMovim   != null && arrMovim.isArray()   && arrMovim.has(0))
                count += importarTablaJSON(conn, "movimientos_material",     arrMovim);
            if (arrPagos   != null && arrPagos.isArray()   && arrPagos.has(0))
                count += importarTablaJSON(conn, "pagos_material",           arrPagos);
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
            if (arrPed   != null && arrPed.isArray()   && arrPed.has(0))
                count += importarTablaJSON(conn, "pedidos",      arrPed);
            if (arrPagos != null && arrPagos.isArray() && arrPagos.has(0))
                count += importarTablaJSON(conn, "pagos_pedido", arrPagos);
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
