package org.gipsybuho.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.gipsybuho.db.DatabaseManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportService {

    // Orden de exportación: primero tablas independientes, luego las que tienen FK
    private static final String[] TABLAS = {
        "config", "clientes", "empleados", "tarifas", "materiales",
        "consumo_material_tecnica", "movimientos_material", "pagos_material",
        "presupuestos", "lineas_presupuesto", "facturas", "lineas_factura",
        "pedidos", "pagos_pedido", "nominas", "notas_calendario"
    };

    private static final DateTimeFormatter FMT_ARCHIVO =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FMT_DISPLAY =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static String timestamp() {
        return LocalDateTime.now().format(FMT_ARCHIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. COPIA DE SEGURIDAD SQLITE (.db)
    //    Usa VACUUM INTO para una copia atómica y limpia sin WAL
    // ─────────────────────────────────────────────────────────────────────────

    public static Path backupSQLite(Path destino) throws Exception {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "VACUUM INTO ?")) {
            ps.setString(1, destino.toAbsolutePath().toString());
            ps.executeUpdate();
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. EXPORTACIÓN CSV COMPRIMIDA (.zip)
    //    Una hoja por tabla, UTF-8 con BOM para compatibilidad con Excel
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarZipCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            for (String tabla : TABLAS) {
                if (!tablaExiste(conn, tabla)) continue;
                zos.putNextEntry(new ZipEntry(tabla + ".csv"));
                // BOM para que Excel detecte UTF-8 automáticamente
                zos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                zos.write(generarCSV(conn, tabla));
                zos.closeEntry();
            }
        }
        return destino;
    }

    private static byte[] generarCSV(Connection conn, String tabla) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) sb.append(';'); // Separador ; para que Excel español lo detecte
                sb.append(csvEscapar(meta.getColumnName(i)));
            }
            sb.append("\r\n");
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) sb.append(';');
                    String val = rs.getString(i);
                    sb.append(val == null ? "" : csvEscapar(val));
                }
                sb.append("\r\n");
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEscapar(String s) {
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. VOLCADO SQL (.sql)
    //    Script completo con DROP + CREATE + INSERT, restaurable en cualquier SQLite
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {

            pw.println("-- Gráficas Mulberry — Volcado SQL completo");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println("-- Compatible con SQLite 3.x");
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();

            for (String tabla : TABLAS) {
                if (!tablaExiste(conn, tabla)) continue;

                // Esquema de la tabla
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("-- ── " + tabla.toUpperCase() + " ──");
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }

                // Datos
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    // Construir lista de columnas
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");

                    int count = 0;
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO ")
                            .append(tabla).append(" ").append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            if (val == null) {
                                sb.append("NULL");
                            } else {
                                sb.append("'").append(val.replace("'", "''")).append("'");
                            }
                        }
                        sb.append(");");
                        pw.println(sb);
                        count++;
                    }
                    if (count > 0) pw.println();
                }
            }

            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. EXPORTACIÓN JSON (.json)
    //    Usa Jackson para serialización con tipos correctos
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("version", "1.0");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));

        ObjectNode tables = mapper.createObjectNode();
        Connection conn = DatabaseManager.getConnection();

        for (String tabla : TABLAS) {
            if (!tablaExiste(conn, tabla)) continue;
            ArrayNode filas = mapper.createArrayNode();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    ObjectNode fila = mapper.createObjectNode();
                    for (int i = 1; i <= cols; i++) {
                        String col = meta.getColumnName(i);
                        if (rs.getObject(i) == null) {
                            fila.putNull(col);
                        } else {
                            int type = meta.getColumnType(i);
                            switch (type) {
                                case Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT ->
                                    fila.put(col, rs.getLong(i));
                                case Types.REAL, Types.FLOAT, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL ->
                                    fila.put(col, rs.getDouble(i));
                                default ->
                                    fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            tables.set(tabla, filas);
        }

        root.set("tables", tables);
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean tablaExiste(Connection conn, String tabla) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            ps.setString(1, tabla);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public static File getDbFile() {
        String appData = System.getenv("LOCALAPPDATA");
        File dir = new File(
            appData != null && !appData.isEmpty() ? appData : System.getProperty("user.home"),
            "GraficasMulberry");
        return new File(dir, "graficas_mulberry.db");
    }

    public static long getDbSizeBytes() {
        File f = getDbFile();
        return f.exists() ? f.length() : 0;
    }

    public static int contarRegistros() {
        int total = 0;
        try {
            Connection conn = DatabaseManager.getConnection();
            String[] tablasDatos = {"clientes", "pedidos", "facturas", "presupuestos", "materiales", "empleados"};
            for (String t : tablasDatos) {
                if (!tablaExiste(conn, t)) continue;
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    if (rs.next()) total += rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}
        return total;
    }
}
