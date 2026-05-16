package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.LineaAlbaran;
import org.gipsybuho.model.LineaFactura;
import org.gipsybuho.model.LineaPresupuesto;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.Presupuesto;
import org.gipsybuho.model.Tarifa;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.awt.Color;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE CLIENTES
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarClientesCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // BOM para compatibilidad con Excel
            w.write(new String(generarCSV(conn, "clientes"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarClientesSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tabla clientes");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='clientes'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    pw.println("DROP TABLE IF EXISTS clientes;");
                    pw.println(rs.getString(1) + ";");
                    pw.println();
                }
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM clientes")) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                StringBuilder colNames = new StringBuilder("(");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) colNames.append(", ");
                    colNames.append(meta.getColumnName(i));
                }
                colNames.append(")");
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder("INSERT INTO clientes ")
                        .append(colNames).append(" VALUES (");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) sb.append(", ");
                        String val = rs.getString(i);
                        sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                    }
                    pw.println(sb.append(");"));
                }
            }

            pw.println();
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarClientesJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        ArrayNode filas = mapper.createArrayNode();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM clientes")) {
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
                            default -> fila.put(col, rs.getString(i));
                        }
                    }
                }
                filas.add(fila);
            }
        }
        root.set("clientes", filas);
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarClientesPDF(Path destino, List<Cliente> clientes) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Clientes — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + clientes.size() + " cliente(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.2f, 2.2f, 1.2f, 1.8f, 1.8f, 3f, 1.8f});

        for (String h : new String[]{"Nombre", "Apellidos", "Tipo", "NIF/CIF", "Teléfono", "Email", "Ciudad"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < clientes.size(); i++) {
            Cliente c = clientes.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(c.getNombre()), s(c.getApellidos()), s(c.getTipo()),
                s(c.getNif()), s(c.getTelefono()), s(c.getEmail()), s(c.getCiudad())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarClientesWord(Path destino, List<Cliente> clientes) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Clientes — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + clientes.size() + " cliente(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Nombre", "Apellidos", "Tipo", "NIF/CIF", "Teléfono", "Email", "Ciudad"};
            XWPFTable tabla = word.createTable(clientes.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 20, 20, 8, 12, 12, 20, 8);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(10);
            }

            for (int i = 0; i < clientes.size(); i++) {
                Cliente c = clientes.get(i);
                String[] vals = {
                    s(c.getNombre()), s(c.getApellidos()), s(c.getTipo()),
                    s(c.getNif()), s(c.getTelefono()), s(c.getEmail()), s(c.getCiudad())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE FACTURAS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarFacturasCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(new String(generarCSV(conn, "facturas"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarFacturasSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas facturas");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"facturas", "lineas_factura"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarFacturasJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"facturas", "lineas_factura"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarFacturasPDF(Path destino, List<Factura> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Facturas — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " factura(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.6f, 2.5f, 1.1f, 1.1f, 1.8f, 1.2f, 1.4f, 0.8f, 1.4f});

        for (String h : new String[]{"Número", "Cliente", "Fecha", "Vencimiento",
                                      "Forma pago", "Estado", "Base", "IVA%", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Factura f = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(f.getNumero()), s(f.getClienteNombre()), s(f.getFecha()),
                s(f.getFechaVencimiento()), s(f.getFormaPago()), s(f.getEstado()),
                String.format("%.2f €", f.getBaseImponible()),
                String.format("%.0f%%", f.getIvaPorcentaje()),
                String.format("%.2f €", f.getTotal())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarFacturasWord(Path destino, List<Factura> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Facturas — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " factura(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Número", "Cliente", "Fecha", "Vencimiento",
                                 "Forma pago", "Estado", "Base", "IVA%", "Total"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 10, 22, 9, 9, 12, 8, 10, 6, 10);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Factura f = lista.get(i);
                String[] vals = {
                    s(f.getNumero()), s(f.getClienteNombre()), s(f.getFecha()),
                    s(f.getFechaVencimiento()), s(f.getFormaPago()), s(f.getEstado()),
                    String.format("%.2f €", f.getBaseImponible()),
                    String.format("%.0f%%", f.getIvaPorcentaje()),
                    String.format("%.2f €", f.getTotal())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE ALBARANES
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarAlbaranesCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(new String(generarCSV(conn, "albaranes"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarAlbaranesSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas albaranes");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"albaranes", "lineas_albaran"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarAlbaranesJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"albaranes", "lineas_albaran"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarAlbaranesPDF(Path destino, List<Albaran> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Albaranes — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " albarán(es)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.6f, 2.5f, 1.2f, 1.8f, 1.8f, 1.3f, 3f});

        for (String h : new String[]{"Número", "Cliente", "Fecha", "Factura", "Pedido", "Estado", "Observaciones"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Albaran a = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(a.getNumero()), s(a.getClienteNombre()), s(a.getFecha()),
                s(a.getFacturaNumero()), s(a.getPedidoNumero()),
                s(a.getEstado()), s(a.getObservaciones())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarAlbaranesWord(Path destino, List<Albaran> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Albaranes — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " albarán(es)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Número", "Cliente", "Fecha", "Factura", "Pedido", "Estado", "Observaciones"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 10, 22, 10, 10, 10, 8, 30);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Albaran a = lista.get(i);
                String[] vals = {
                    s(a.getNumero()), s(a.getClienteNombre()), s(a.getFecha()),
                    s(a.getFacturaNumero()), s(a.getPedidoNumero()),
                    s(a.getEstado()), s(a.getObservaciones())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE PEDIDOS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarPedidosCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(new String(generarCSV(conn, "pedidos"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarPedidosSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas pedidos");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"pedidos", "pagos_pedido"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarPedidosJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"pedidos", "pagos_pedido"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarPedidosPDF(Path destino, List<Pedido> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Pedidos — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " pedido(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 2.4f, 1.2f, 1.2f, 1.4f, 2.5f, 1.4f, 1.4f});

        for (String h : new String[]{"Número", "Cliente", "Fecha", "Entrega prev.",
                                      "Estado", "Descripción", "Total", "Pendiente"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Pedido p = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(p.getNumero()),
                s(p.getClienteNombre()),
                p.getFecha() != null ? p.getFecha().format(fmtFecha) : "",
                p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().format(fmtFecha) : "",
                s(p.getEstadoDisplay()),
                s(p.getDescripcion()),
                String.format("%.2f €", p.getImporteTotal()),
                String.format("%.2f €", p.getImportePendiente())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarPedidosWord(Path destino, List<Pedido> lista) throws Exception {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Pedidos — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " pedido(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Número", "Cliente", "Fecha", "Entrega prev.",
                                 "Estado", "Descripción", "Total", "Pendiente"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 9, 20, 9, 9, 10, 24, 10, 9);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Pedido p = lista.get(i);
                String[] vals = {
                    s(p.getNumero()),
                    s(p.getClienteNombre()),
                    p.getFecha() != null ? p.getFecha().format(fmtFecha) : "",
                    p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().format(fmtFecha) : "",
                    s(p.getEstadoDisplay()),
                    s(p.getDescripcion()),
                    String.format("%.2f €", p.getImporteTotal()),
                    String.format("%.2f €", p.getImportePendiente())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE MATERIALES
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarMaterialesCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write((char) 0xFEFF);
            w.write(new String(generarCSV(conn, "materiales"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarMaterialesSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas materiales");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"materiales", "consumo_material_tecnica",
                                              "movimientos_material", "pagos_material"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarMaterialesJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"materiales", "consumo_material_tecnica",
                                          "movimientos_material", "pagos_material"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarMaterialesPDF(Path destino, List<Material> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Materiales — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " material(es)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.8f, 1.4f, 1.4f, 1.3f, 1.3f, 0.8f, 1.2f, 2f, 0.8f});

        for (String h : new String[]{"Nombre", "Referencia", "Categoría",
                                      "Stock actual", "Stock mín.", "Unidad",
                                      "Precio/ud.", "Proveedor", "Alerta"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg   = new Color(245, 238, 244);
        Color alertBg = new Color(255, 235, 235);
        for (int i = 0; i < lista.size(); i++) {
            Material m = lista.get(i);
            Color bg = m.isBajoStock() ? alertBg : (i % 2 == 0 ? Color.WHITE : altBg);
            for (String val : new String[]{
                s(m.getNombre()),
                s(m.getReferencia()),
                s(m.getCategoria()),
                String.valueOf(m.getStockActual()),
                String.valueOf(m.getStockMinimo()),
                s(m.getUnidad()),
                String.format("%.2f €", m.getPrecioUnidad()),
                s(m.getProveedor()),
                m.isBajoStock() ? "⚠ Bajo" : "OK"
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarMaterialesWord(Path destino, List<Material> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Materiales — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " material(es)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Nombre", "Referencia", "Categoría",
                                 "Stock actual", "Stock mín.", "Unidad",
                                 "Precio/ud.", "Proveedor", "Alerta"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 18, 12, 12, 9, 9, 7, 10, 16, 7);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Material m = lista.get(i);
                String[] vals = {
                    s(m.getNombre()),
                    s(m.getReferencia()),
                    s(m.getCategoria()),
                    String.valueOf(m.getStockActual()),
                    String.valueOf(m.getStockMinimo()),
                    s(m.getUnidad()),
                    String.format("%.2f €", m.getPrecioUnidad()),
                    s(m.getProveedor()),
                    m.isBajoStock() ? "¡Bajo!" : "OK"
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = m.isBajoStock() ? "FFEBEB" : (i % 2 == 0 ? "FFFFFF" : "F5EEF4");
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE TARIFAS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarTarifasCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write((char) 0xFEFF);
            w.write(new String(generarCSV(conn, "tarifas"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarTarifasSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tabla tarifas");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            String tabla = "tarifas";
            if (tablaExiste(conn, tabla)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarTarifasJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        String tabla = "tarifas";
        if (tablaExiste(conn, tabla)) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarTarifasPDF(Path destino, List<Tarifa> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Tarifas — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " tarifa(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 2.5f, 3f, 1.2f, 1.2f, 0.9f, 0.8f});

        for (String h : new String[]{"Técnica", "Nombre", "Descripción",
                                      "Precio/ud.", "Setup (€)", "Mín. uds.", "Activa"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Tarifa t = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(t.getTecnica()),
                s(t.getNombre()),
                s(t.getDescripcion()),
                String.format("%.2f €", t.getPrecioUnit()),
                String.format("%.2f €", t.getPrecioSetup()),
                String.valueOf(t.getMinimoUnidades()),
                t.isActiva() ? "Sí" : "No"
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarTarifasWord(Path destino, List<Tarifa> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Tarifas — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " tarifa(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Técnica", "Nombre", "Descripción",
                                 "Precio/ud.", "Setup (€)", "Mín. uds.", "Activa"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 12, 18, 30, 12, 12, 10, 6);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Tarifa t = lista.get(i);
                String[] vals = {
                    s(t.getTecnica()),
                    s(t.getNombre()),
                    s(t.getDescripcion()),
                    String.format("%.2f €", t.getPrecioUnit()),
                    String.format("%.2f €", t.getPrecioSetup()),
                    String.valueOf(t.getMinimoUnidades()),
                    t.isActiva() ? "Sí" : "No"
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE NÓMINAS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarNominasCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write((char) 0xFEFF);
            w.write(new String(generarCSV(conn, "nominas"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarNominasSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tabla nominas");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            String tabla = "nominas";
            if (tablaExiste(conn, tabla)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarNominasJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        String tabla = "nominas";
        if (tablaExiste(conn, tabla)) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarNominasPDF(Path destino, List<Nomina> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Nóminas — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " nómina(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.5f, 1.5f, 1.4f, 1.4f, 1.3f, 0.9f, 1.3f, 1.4f, 1.6f});

        for (String h : new String[]{"Empleado", "Período", "Salario base",
                                      "Bruto", "SS Trab.", "IRPF%", "IRPF €", "Neto", "Coste empresa"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Nomina n = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(n.getEmpleadoNombre()),
                s(n.getPeriodo()),
                String.format("%.2f €", n.getSalarioBase()),
                String.format("%.2f €", n.getTotalBruto()),
                String.format("%.2f €", n.getSsTrabajador()),
                String.format("%.1f%%", n.getIrpfPorcentaje()),
                String.format("%.2f €", n.getIrpfImporte()),
                String.format("%.2f €", n.getNeto()),
                String.format("%.2f €", n.getCosteTotalEmpresa())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarNominasWord(Path destino, List<Nomina> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Nóminas — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " nómina(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Empleado", "Período", "Salario base",
                                 "Bruto", "SS Trab.", "IRPF%", "IRPF €", "Neto", "Coste empresa"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 18, 10, 11, 11, 10, 7, 10, 12, 11);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Nomina n = lista.get(i);
                String[] vals = {
                    s(n.getEmpleadoNombre()),
                    s(n.getPeriodo()),
                    String.format("%.2f €", n.getSalarioBase()),
                    String.format("%.2f €", n.getTotalBruto()),
                    String.format("%.2f €", n.getSsTrabajador()),
                    String.format("%.1f%%", n.getIrpfPorcentaje()),
                    String.format("%.2f €", n.getIrpfImporte()),
                    String.format("%.2f €", n.getNeto()),
                    String.format("%.2f €", n.getCosteTotalEmpresa())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE EMPLEADOS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarEmpleadosCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write((char) 0xFEFF);
            w.write(new String(generarCSV(conn, "empleados"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarEmpleadosSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas empleados");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"empleados", "nominas"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarEmpleadosJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"empleados", "nominas"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarEmpleadosPDF(Path destino, List<Empleado> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Empleados — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " empleado(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.8f, 1.8f, 1.2f, 1.5f, 1.3f, 0.8f, 1.2f, 1.3f});

        for (String h : new String[]{"Nombre", "Apellidos", "NIF", "Categoría",
                                      "Salario base", "IRPF%", "Fecha alta", "Estado"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg  = new Color(245, 238, 244);
        Color bajaBg = new Color(255, 235, 235);
        for (int i = 0; i < lista.size(); i++) {
            Empleado e = lista.get(i);
            Color bg = !e.isActivo() ? bajaBg : (i % 2 == 0 ? Color.WHITE : altBg);
            String estado = e.isActivo() ? "ACTIVO"
                : "BAJA" + (e.getFechaBaja() != null ? " " + e.getFechaBaja() : "");
            for (String val : new String[]{
                s(e.getNombre()),
                s(e.getApellidos()),
                s(e.getNif()),
                s(e.getCategoria()),
                String.format("%.2f €", e.getSalarioBase()),
                String.format("%.1f%%", e.getIrpf()),
                s(e.getFechaAlta()),
                estado
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarEmpleadosWord(Path destino, List<Empleado> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Empleados — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " empleado(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Nombre", "Apellidos", "NIF", "Categoría",
                                 "Salario base", "IRPF%", "Fecha alta", "Estado"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 16, 18, 11, 14, 13, 9, 11, 8);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(9);
            }

            for (int i = 0; i < lista.size(); i++) {
                Empleado e = lista.get(i);
                String estado = e.isActivo() ? "ACTIVO"
                    : "BAJA" + (e.getFechaBaja() != null ? " " + e.getFechaBaja() : "");
                String[] vals = {
                    s(e.getNombre()),
                    s(e.getApellidos()),
                    s(e.getNif()),
                    s(e.getCategoria()),
                    String.format("%.2f €", e.getSalarioBase()),
                    String.format("%.1f%%", e.getIrpf()),
                    s(e.getFechaAlta()),
                    estado
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = !e.isActivo() ? "FFEBEB" : (i % 2 == 0 ? "FFFFFF" : "F5EEF4");
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    private static String s(String v) { return v != null ? v : ""; }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES ESPECÍFICAS DE PRESUPUESTOS
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarPresupuestosCSV(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(new String(generarCSV(conn, "presupuestos"), StandardCharsets.UTF_8));
        }
        return destino;
    }

    public static Path exportarPresupuestosSQL(Path destino) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(destino.toFile()), StandardCharsets.UTF_8))) {
            pw.println("-- Gráficas Mulberry — Exportación tablas presupuestos");
            pw.println("-- Generado: " + LocalDateTime.now().format(FMT_DISPLAY));
            pw.println();
            pw.println("PRAGMA foreign_keys = OFF;");
            pw.println("BEGIN TRANSACTION;");
            pw.println();
            for (String tabla : new String[]{"presupuestos", "lineas_presupuesto"}) {
                if (!tablaExiste(conn, tabla)) continue;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
                    ps.setString(1, tabla);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        pw.println("DROP TABLE IF EXISTS " + tabla + ";");
                        pw.println(rs.getString(1) + ";");
                        pw.println();
                    }
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM " + tabla)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    StringBuilder colNames = new StringBuilder("(");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) colNames.append(", ");
                        colNames.append(meta.getColumnName(i));
                    }
                    colNames.append(")");
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder("INSERT INTO " + tabla + " ")
                            .append(colNames).append(" VALUES (");
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) sb.append(", ");
                            String val = rs.getString(i);
                            sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        }
                        pw.println(sb.append(");"));
                    }
                    pw.println();
                }
            }
            pw.println("COMMIT;");
            pw.println("PRAGMA foreign_keys = ON;");
        }
        return destino;
    }

    public static Path exportarPresupuestosJSON(Path destino) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        root.put("app", "Graficas Mulberry");
        root.put("exportDate", LocalDateTime.now().format(FMT_DISPLAY));
        Connection conn = DatabaseManager.getConnection();
        for (String tabla : new String[]{"presupuestos", "lineas_presupuesto"}) {
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
                                default -> fila.put(col, rs.getString(i));
                            }
                        }
                    }
                    filas.add(fila);
                }
            }
            root.set(tabla, filas);
        }
        mapper.writeValue(destino.toFile(), root);
        return destino;
    }

    public static Path exportarPresupuestosPDF(Path destino, List<Presupuesto> lista) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, new FileOutputStream(destino.toFile()));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        Color mulberry  = new Color(107, 45, 94);

        Font fTitulo = new Font(bfBold, 16, Font.BOLD,   mulberry);
        Font fSubtit = new Font(bf,     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold,  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf,       9, Font.NORMAL);

        Paragraph titulo = new Paragraph("Listado de Presupuestos — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph fecha = new Paragraph(
            "Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
            "  ·  Total: " + lista.size() + " presupuesto(s)", fSubtit);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(14);
        doc.add(fecha);

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.8f, 2.8f, 1.2f, 1.2f, 1.3f, 1.5f, 0.9f, 1.5f});

        for (String h : new String[]{"Número", "Cliente", "Fecha", "Validez", "Estado", "Base", "IVA%", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
            cell.setBackgroundColor(mulberry);
            cell.setPadding(6);
            cell.setBorderColor(mulberry);
            tabla.addCell(cell);
        }

        Color altBg = new Color(245, 238, 244);
        for (int i = 0; i < lista.size(); i++) {
            Presupuesto p = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : altBg;
            for (String val : new String[]{
                s(p.getNumero()), s(p.getClienteNombre()), s(p.getFecha()), s(p.getFechaValidez()),
                s(p.getEstado()),
                String.format("%.2f €", p.getBaseImponible()),
                String.format("%.0f%%", p.getIvaPorcentaje()),
                String.format("%.2f €", p.getTotal())
            }) {
                PdfPCell cell = new PdfPCell(new Phrase(val, fNormal));
                cell.setBackgroundColor(bg);
                cell.setPadding(5);
                cell.setBorderColor(new Color(220, 220, 220));
                tabla.addCell(cell);
            }
        }

        doc.add(tabla);
        doc.close();
        return destino;
    }

    public static Path exportarPresupuestosWord(Path destino, List<Presupuesto> lista) throws Exception {
        try (XWPFDocument word = new XWPFDocument()) {
            XWPFParagraph parTitulo = word.createParagraph();
            parTitulo.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rTitulo = parTitulo.createRun();
            rTitulo.setText("Listado de Presupuestos — Gráficas Mulberry");
            rTitulo.setBold(true);
            rTitulo.setFontSize(16);
            rTitulo.setColor("6B2D5E");
            rTitulo.addBreak();

            XWPFParagraph parFecha = word.createParagraph();
            parFecha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rFecha = parFecha.createRun();
            rFecha.setText("Generado: " + LocalDateTime.now().format(FMT_DISPLAY) +
                "  ·  Total: " + lista.size() + " presupuesto(s)");
            rFecha.setFontSize(10);
            rFecha.setColor("888888");
            rFecha.addBreak();

            String[] headers = {"Número", "Cliente", "Fecha", "Validez", "Estado", "Base", "IVA%", "Total"};
            XWPFTable tabla = word.createTable(lista.size() + 1, headers.length);
            setTableWidth(tabla, "100%");
            setCeldasAncho(tabla, 9000, 10, 22, 9, 9, 10, 12, 7, 12);

            XWPFTableRow headerRow = tabla.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setText(headers[i]);
                cell.setColor("6B2D5E");
                XWPFRun r = cell.getParagraphs().get(0).getRuns().get(0);
                r.setBold(true);
                r.setColor("FFFFFF");
                r.setFontSize(10);
            }

            for (int i = 0; i < lista.size(); i++) {
                Presupuesto p = lista.get(i);
                String[] vals = {
                    s(p.getNumero()), s(p.getClienteNombre()), s(p.getFecha()), s(p.getFechaValidez()),
                    s(p.getEstado()),
                    String.format("%.2f €", p.getBaseImponible()),
                    String.format("%.0f%%", p.getIvaPorcentaje()),
                    String.format("%.2f €", p.getTotal())
                };
                XWPFTableRow row = tabla.getRow(i + 1);
                String bgColor = i % 2 == 0 ? "FFFFFF" : "F5EEF4";
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    cell.setText(vals[j]);
                    cell.setColor(bgColor);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) {
                word.write(fos);
            }
        }
        return destino;
    }

    public static Path exportarPresupuestoDetalladoWord(Path destino, Presupuesto p, Cliente c) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            // Configuración de estilos y colores
            String colorMulberry = "6B2D5E"; // Hex para Color(107, 45, 94)
            String colorGrisClaro = "F5F5F5"; // Hex para Color(245, 245, 245)
            String colorGrisBorde = "C8C8C8"; // Hex para Color(200, 200, 200)

            // 1. Cabecera de la empresa
            XWPFParagraph pHeader = document.createParagraph();
            pHeader.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun rHeader = pHeader.createRun();
            rHeader.setText("GRÁFICAS MULBERRY");
            rHeader.setBold(true);
            rHeader.setFontSize(20);
            rHeader.setColor(colorMulberry);
            rHeader.addBreak();
            rHeader.setFontSize(9);
            rHeader.setColor("000000"); // Negro para texto normal
            rHeader.setText(DatabaseManager.getConfig("empresa_direccion"));
            rHeader.addBreak();
            rHeader.setText(DatabaseManager.getConfig("empresa_cp") + " " + DatabaseManager.getConfig("empresa_ciudad"));
            rHeader.addBreak();
            rHeader.setText("Tel: " + DatabaseManager.getConfig("empresa_telefono"));
            rHeader.addBreak();
            rHeader.setText(DatabaseManager.getConfig("empresa_email"));
            rHeader.addBreak();
            rHeader.setText("NIF: " + DatabaseManager.getConfig("empresa_nif"));

            // Título del documento
            XWPFParagraph pDocTitle = document.createParagraph();
            pDocTitle.setAlignment(ParagraphAlignment.CENTER);
            pDocTitle.setSpacingAfter(200); // Espacio después del título
            XWPFRun rDocTitle = pDocTitle.createRun();
            rDocTitle.setText("PRESUPUESTO");
            rDocTitle.setBold(true);
            rDocTitle.setFontSize(14);
            rDocTitle.setColor("FFFFFF"); // Blanco
            pDocTitle.getCTP().getPPr().addNewShd().setFill(colorMulberry); // Fondo Mulberry

            // 2. Datos del documento (Número, Fecha, Validez, Estado)
            addSectionTitle(document, "DATOS DEL PRESUPUESTO");
            XWPFTable docDataTable = document.createTable(0, 4);
            setTableWidth(docDataTable, "100%");
            setCeldasAncho(docDataTable, 9000, 20, 30, 20, 30);
            addTableRow(docDataTable, new String[]{"Número:", p.getNumero(), "Estado:", s(p.getEstado()).toUpperCase()}, true, colorGrisClaro, colorGrisBorde);
            addTableRow(docDataTable, new String[]{"Fecha:", s(p.getFecha()), "Validez:", s(p.getFechaValidez())}, false, colorGrisClaro, colorGrisBorde);
            addParagraph(document, ""); // Espacio

            // 3. Datos del cliente
            addSectionTitle(document, "CLIENTE");
            XWPFTable clientDataTable = document.createTable(1, 1);
            setTableWidth(clientDataTable, "50%");
            XWPFTableRow clientRow = clientDataTable.getRow(0);
            XWPFTableCell clientCell = clientRow.getCell(0);
            clientCell.setColor(colorGrisClaro); // Fondo gris claro para la celda
            addRunToCell(clientCell, s(c.getNombreCompleto()), true, 10, "000000");
            if (c.getNif() != null) addRunToCell(clientCell, "NIF/CIF: " + c.getNif(), false, 9, "000000");
            if (c.getDireccion() != null) addRunToCell(clientCell, c.getDireccion(), false, 9, "000000");
            if (c.getCiudad() != null) addRunToCell(clientCell, s(c.getCp()) + " " + c.getCiudad(), false, 9, "000000");
            if (c.getTelefono() != null) addRunToCell(clientCell, "Tel: " + c.getTelefono(), false, 9, "000000");
            if (c.getEmail() != null) addRunToCell(clientCell, c.getEmail(), false, 9, "000000");
            addParagraph(document, ""); // Espacio

            // 4. Tabla de líneas del presupuesto
            addSectionTitle(document, "DETALLE DEL PRESUPUESTO");
            XWPFTable lineasTable = document.createTable(0, 6);
            setTableWidth(lineasTable, "100%");
            String[] headers = {"Descripción", "Técnica", "Cant.", "Precio ud.", "Dto.", "Total"};
            addTableHeader(lineasTable, headers, colorMulberry);
            setCeldasAncho(lineasTable, 9000, 35, 20, 8, 14, 8, 15);

            boolean par = false;
            for (LineaPresupuesto linea : p.getLineas()) {
                String bgColor = par ? colorGrisClaro : "FFFFFF";
                XWPFTableRow row = lineasTable.createRow();
                addCell(row, 0, s(linea.getDescripcion()), bgColor, false, ParagraphAlignment.LEFT);
                addCell(row, 1, s(linea.getTecnica()), bgColor, false, ParagraphAlignment.LEFT);
                addCell(row, 2, String.valueOf(linea.getCantidad()), bgColor, false, ParagraphAlignment.RIGHT);
                addCell(row, 3, String.format("%.2f €", linea.getPrecioUnit()), bgColor, false, ParagraphAlignment.RIGHT);
                addCell(row, 4, linea.getDescuento() > 0 ? String.format("%.0f%%", linea.getDescuento()) : "-", bgColor, false, ParagraphAlignment.RIGHT);
                addCell(row, 5, String.format("%.2f €", linea.getTotal()), bgColor, false, ParagraphAlignment.RIGHT);
                par = !par;
            }
            addParagraph(document, ""); // Espacio

            // 5. Totales
            XWPFTable totalesTable = document.createTable(0, 2);
            setTableWidth(totalesTable, "40%");
            setCeldasAncho(totalesTable, 3600, 60, 40);
            totalesTable.setTableAlignment(TableRowAlign.RIGHT); // Alinea la tabla a la derecha
            addTableRow(totalesTable, new String[]{"Base imponible:", String.format("%.2f €", p.getBaseImponible())}, false, "FFFFFF", colorGrisBorde);
            addTableRow(totalesTable, new String[]{String.format("IVA (%.0f%%):", p.getIvaPorcentaje()), String.format("%.2f €", p.getIvaImporte())}, false, "FFFFFF", colorGrisBorde);
            addTableRow(totalesTable, new String[]{"TOTAL:", String.format("%.2f €", p.getTotal())}, true, colorMulberry, colorGrisBorde);
            addParagraph(document, ""); // Espacio

            // 6. Notas y condiciones
            if (p.getNotas() != null && !p.getNotas().isBlank()) {
                addSectionTitle(document, "NOTAS");
                addParagraph(document, p.getNotas());
            }
            if (p.getCondiciones() != null && !p.getCondiciones().isBlank()) {
                addSectionTitle(document, "CONDICIONES");
                addParagraph(document, p.getCondiciones());
            }

            // 7. Pie de página (adaptado de PDFService)
            XWPFParagraph pFooter = document.createParagraph();
            pFooter.setAlignment(ParagraphAlignment.CENTER);
            pFooter.setSpacingBefore(200); // Espacio antes del pie de página
            XWPFRun rFooter = pFooter.createRun();
            rFooter.setFontSize(8);
            rFooter.setColor("888888"); // Gris
            rFooter.setText("Gráficas Mulberry · " + DatabaseManager.getConfig("empresa_web") +
                " · " + DatabaseManager.getConfig("empresa_email") +
                " · Tel. " + DatabaseManager.getConfig("empresa_telefono"));


            try (FileOutputStream out = new FileOutputStream(destino.toFile())) {
                document.write(out);
            }
        }
        return destino;
    }

    // Helper para añadir un título de sección en Word
    private static void addSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(title);
        r.setBold(true);
        r.setFontSize(12);
        r.setColor("6B2D5E"); // Color Mulberry
    }

    // Helper para añadir un párrafo de texto normal
    private static void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(9);
    }

    // Helper para añadir texto a una celda
    private static void addRunToCell(XWPFTableCell cell, String text, boolean bold, int fontSize, String colorHex) {
        XWPFParagraph p = cell.addParagraph();
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(fontSize);
        r.setColor(colorHex);
    }

    // ── Ancho de tabla en DXA. 9000 twips ≈ ancho de texto A4 con márgenes estándar.
    private static void setTableWidth(XWPFTable table, String widthPct) {
        int dxa = 9000 * Integer.parseInt(widthPct.replace("%", "")) / 100;
        CTTblWidth tblW = table.getCTTbl().getTblPr().getTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(dxa));
    }

    // ── Distribuye anchos de columna proporcionales a los pesos. Llámalo ANTES de
    //    añadir filas de datos (solo actúa sobre las filas ya presentes).
    private static void setCeldasAncho(XWPFTable table, int totalDxa, int... pesos) {
        int sumaPesos = 0;
        for (int p : pesos) sumaPesos += p;
        int[] anchos = new int[pesos.length];
        for (int i = 0; i < pesos.length; i++) anchos[i] = totalDxa * pesos[i] / sumaPesos;

        CTTblGrid grid = table.getCTTbl().addNewTblGrid();
        for (int a : anchos) grid.addNewGridCol().setW(BigInteger.valueOf(a));

        for (XWPFTableRow row : table.getRows()) {
            java.util.List<XWPFTableCell> cells = row.getTableCells();
            for (int i = 0; i < Math.min(cells.size(), anchos.length); i++) {
                var ctTc = cells.get(i).getCTTc();
                CTTcPr tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
                CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
                tcW.setType(STTblWidth.DXA);
                tcW.setW(BigInteger.valueOf(anchos[i]));
            }
        }
    }

    // ── Configura tabla de lista: ancho, columnas proporcionales y fila cabecera.
    private static void prepararCabeceraTabla(XWPFTable tabla, String[] headers, int... pesos) {
        setTableWidth(tabla, "100%");
        setCeldasAncho(tabla, 9000, pesos);
        XWPFTableRow headerRow = tabla.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = i < headerRow.getTableCells().size()
                ? headerRow.getCell(i) : headerRow.addNewTableCell();
            cell.setColor("6B2D5E");
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setSpacingAfter(0); p.setSpacingBefore(0);
            XWPFRun r = p.getRuns().isEmpty() ? p.createRun() : p.getRuns().get(0);
            r.setBold(true); r.setColor("FFFFFF"); r.setFontSize(9);
            r.setText(headers[i]);
        }
    }

    // ── Rellena una celda de lista (fondo + texto + tamaño de fuente).
    private static void setWordCelda(XWPFTableCell cell, String text, String bgColor, int fontSize) {
        cell.setColor(bgColor);
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingAfter(0); p.setSpacingBefore(0);
        XWPFRun r = p.getRuns().isEmpty() ? p.createRun() : p.getRuns().get(0);
        r.setFontSize(fontSize); r.setText(text);
    }

    // Helper para añadir una fila a una tabla de datos de documento/cliente
    private static void addTableRow(XWPFTable table, String[] data, boolean boldValue, String bgColorHex, String borderColorHex) {
        XWPFTableRow row = table.createRow();
        for (int i = 0; i < data.length; i++) {
            XWPFTableCell cell = row.getCell(i);
            if (cell == null) cell = row.addNewTableCell(); // Asegurarse de que la celda existe
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            cell.setColor(bgColorHex); // Fondo de la celda

            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setSpacingAfter(0);
            p.setSpacingBefore(0);
            XWPFRun r = p.createRun();
            r.setFontSize(9);
            r.setColor("000000"); // Texto negro

            if (i % 2 == 0) { // Es una etiqueta
                r.setBold(true);
                r.setText(data[i]);
            } else { // Es un valor
                r.setBold(boldValue);
                r.setText(data[i]);
            }
        }
    }

    // Helper para configurar una celda de líneas por índice de columna
    private static void addCell(XWPFTableRow row, int colIdx, String text, String bgColorHex, boolean bold, ParagraphAlignment align) {
        XWPFTableCell cell = colIdx < row.getTableCells().size() ? row.getCell(colIdx) : row.addNewTableCell();
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        cell.setColor(bgColorHex);

        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(bold);
        r.setFontSize(9);
        r.setColor("000000");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALBARÁN INDIVIDUAL DETALLADO (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarAlbaranDetalladoWord(Path destino, Albaran a, Cliente c) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";

            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "ALBARÁN DE ENTREGA", colorMulberry);

            addSectionTitle(document, "DATOS DEL ALBARÁN");
            XWPFTable tDatos = document.createTable(0, 4);
            setTableWidth(tDatos, "100%");
            setCeldasAncho(tDatos, 9000, 20, 30, 20, 30);
            addTableRow(tDatos, new String[]{"Número:", s(a.getNumero()), "Fecha:", s(a.getFecha())}, false, colorGrisClaro, colorGrisBorde);
            addTableRow(tDatos, new String[]{"Factura ref.:", s(a.getFacturaNumero()), "Pedido ref.:", s(a.getPedidoNumero())}, false, colorGrisClaro, colorGrisBorde);
            addParagraph(document, "");

            addSectionTitle(document, "CLIENTE");
            addWordClienteBlock(document, c, colorGrisClaro);
            addParagraph(document, "");

            if (a.getLineas() != null && !a.getLineas().isEmpty()) {
                addSectionTitle(document, "ARTÍCULOS");
                XWPFTable tLineas = document.createTable(0, 3);
                setTableWidth(tLineas, "100%");
                addTableHeader(tLineas, new String[]{"Descripción", "Cantidad", "Unidad"}, colorMulberry);
                setCeldasAncho(tLineas, 9000, 60, 20, 20);
                boolean par = false;
                for (LineaAlbaran linea : a.getLineas()) {
                    String bg = par ? colorGrisClaro : "FFFFFF";
                    XWPFTableRow row = tLineas.createRow();
                    addCell(row, 0, s(linea.getDescripcion()), bg, false, ParagraphAlignment.LEFT);
                    addCell(row, 1, String.valueOf(linea.getCantidad()), bg, false, ParagraphAlignment.CENTER);
                    addCell(row, 2, s(linea.getUnidad()), bg, false, ParagraphAlignment.CENTER);
                    par = !par;
                }
                addParagraph(document, "");
            }

            if (a.getObservaciones() != null && !a.getObservaciones().isBlank()) {
                addSectionTitle(document, "OBSERVACIONES");
                addParagraph(document, a.getObservaciones());
            }

            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NÓMINA INDIVIDUAL DETALLADA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarNominaDetalladaWord(Path destino, Nomina n, Empleado e) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";

            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "RECIBO DE NÓMINA", colorMulberry);

            addSectionTitle(document, "DATOS DEL EMPLEADO");
            XWPFTable tEmp = document.createTable(0, 4);
            setTableWidth(tEmp, "100%");
            setCeldasAncho(tEmp, 9000, 20, 30, 20, 30);
            addTableRow(tEmp, new String[]{"Empleado:", e.getNombreCompleto(), "Período:", s(n.getPeriodo())}, true, colorGrisClaro, colorGrisBorde);
            addTableRow(tEmp, new String[]{"NIF:", s(e.getNif()), "Categoría:", s(e.getCategoria())}, false, colorGrisClaro, colorGrisBorde);
            addParagraph(document, "");

            addSectionTitle(document, "PERCEPCIONES");
            XWPFTable tPerc = document.createTable(0, 2);
            setTableWidth(tPerc, "100%");
            addTableHeader(tPerc, new String[]{"Concepto", "Importe"}, colorMulberry);
            setCeldasAncho(tPerc, 9000, 70, 30);
            addWordNominaFila(tPerc, "Salario base", String.format("%.2f €", n.getSalarioBase()), "FFFFFF", colorGrisBorde);
            if (n.getComplementos() > 0)
                addWordNominaFila(tPerc, "Complementos", String.format("%.2f €", n.getComplementos()), colorGrisClaro, colorGrisBorde);
            if (n.getHorasExtraNormales() > 0)
                addWordNominaFila(tPerc, "Horas extra normales", String.format("%.2f €", n.getHorasExtraNormales() * n.getPrecioHoraExtra()), "FFFFFF", colorGrisBorde);
            if (n.getHorasExtraFestivas() > 0)
                addWordNominaFila(tPerc, "Horas extra festivas", String.format("%.2f €", n.getHorasExtraFestivas() * n.getPrecioHoraFestiva()), colorGrisClaro, colorGrisBorde);
            addWordNominaFila(tPerc, "TOTAL DEVENGADO", String.format("%.2f €", n.getTotalBruto()), colorMulberry, colorGrisBorde);
            addParagraph(document, "");

            addSectionTitle(document, "DEDUCCIONES");
            XWPFTable tDed = document.createTable(0, 2);
            setTableWidth(tDed, "100%");
            addTableHeader(tDed, new String[]{"Concepto", "Importe"}, colorMulberry);
            setCeldasAncho(tDed, 9000, 70, 30);
            addWordNominaFila(tDed, String.format("S.S. trabajador"), String.format("%.2f €", n.getSsTrabajador()), "FFFFFF", colorGrisBorde);
            addWordNominaFila(tDed, String.format("IRPF (%.1f%%)", n.getIrpfPorcentaje()), String.format("%.2f €", n.getIrpfImporte()), colorGrisClaro, colorGrisBorde);
            addWordNominaFila(tDed, "TOTAL DEDUCCIONES", String.format("%.2f €", n.getTotalDeducciones()), colorMulberry, colorGrisBorde);
            addParagraph(document, "");

            addSectionTitle(document, "LÍQUIDO A PERCIBIR");
            XWPFTable tLiq = document.createTable(0, 2);
            setTableWidth(tLiq, "60%");
            setCeldasAncho(tLiq, 5400, 70, 30);
            addWordNominaFila(tLiq, "NETO A PERCIBIR", String.format("%.2f €", n.getNeto()), colorMulberry, colorGrisBorde);
            addParagraph(document, "");

            if (e.getIban() != null && !e.getIban().isBlank()) {
                addSectionTitle(document, "FORMA DE PAGO");
                addParagraph(document, "IBAN: " + e.getIban());
            }

            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PEDIDO INDIVIDUAL DETALLADO (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarPedidoDetalladoWord(Path destino, Pedido p, Cliente c) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";

            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "PEDIDO DE TRABAJO", colorMulberry);

            addSectionTitle(document, "DATOS DEL PEDIDO");
            XWPFTable tDatos = document.createTable(0, 4);
            setTableWidth(tDatos, "100%");
            setCeldasAncho(tDatos, 9000, 20, 30, 20, 30);
            addTableRow(tDatos, new String[]{"Número:", s(p.getNumero()), "Estado:", s(p.getEstadoDisplay())}, true, colorGrisClaro, colorGrisBorde);
            addTableRow(tDatos, new String[]{"Fecha entrada:", p.getFecha() != null ? p.getFecha().toString() : "", "Entrega prevista:", p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().toString() : ""}, false, colorGrisClaro, colorGrisBorde);
            addParagraph(document, "");

            addSectionTitle(document, "CLIENTE");
            addWordClienteBlock(document, c, colorGrisClaro);
            addParagraph(document, "");

            if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) {
                addSectionTitle(document, "DESCRIPCIÓN DEL TRABAJO");
                addParagraph(document, p.getDescripcion());
            }

            addSectionTitle(document, "IMPORTES");
            XWPFTable tImp = document.createTable(0, 2);
            setTableWidth(tImp, "50%");
            setCeldasAncho(tImp, 4500, 60, 40);
            addWordNominaFila(tImp, "Importe total:", String.format("%.2f €", p.getImporteTotal()), "FFFFFF", colorGrisBorde);
            addWordNominaFila(tImp, "Pagado:", String.format("%.2f €", p.getImportePagado()), colorGrisClaro, colorGrisBorde);
            addWordNominaFila(tImp, "Pendiente:", String.format("%.2f €", p.getImportePendiente()), colorMulberry, colorGrisBorde);
            addParagraph(document, "");

            if (p.getNotas() != null && !p.getNotas().isBlank()) {
                addSectionTitle(document, "NOTAS");
                addParagraph(document, p.getNotas());
            }

            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENTE INDIVIDUAL — FICHA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarClienteDetalladoWord(Path destino, Cliente c) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";
            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "FICHA DE CLIENTE", colorMulberry);
            addSectionTitle(document, "DATOS DEL CLIENTE");
            XWPFTable t = document.createTable(0, 2);
            setTableWidth(t, "100%");
            setCeldasAncho(t, 9000, 30, 70);
            addWordFichaFila(t, "Nombre:",     s(c.getNombre()),    "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Apellidos:",  s(c.getApellidos()), colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Tipo:",       s(c.getTipo()),       "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "NIF/CIF:",    s(c.getNif()),        colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Teléfono:",   s(c.getTelefono()),   "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Email:",      s(c.getEmail()),      colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Dirección:",  s(c.getDireccion()),  "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "C.P./Ciudad:", s(c.getCp()) + " " + s(c.getCiudad()), colorGrisClaro, colorGrisBorde);
            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TARIFA INDIVIDUAL — FICHA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarTarifaDetalladaWord(Path destino, Tarifa tarifa) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";
            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "FICHA DE TARIFA", colorMulberry);
            addSectionTitle(document, "DATOS DE LA TARIFA");
            XWPFTable t = document.createTable(0, 2);
            setTableWidth(t, "100%");
            setCeldasAncho(t, 9000, 30, 70);
            addWordFichaFila(t, "Técnica:",       s(tarifa.getTecnica()),      "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Nombre:",         s(tarifa.getNombre()),       colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Descripción:",    s(tarifa.getDescripcion()),  "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Precio por ud.:", String.format("%.2f €", tarifa.getPrecioUnit()),   colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Setup (€):",      String.format("%.2f €", tarifa.getPrecioSetup()),  "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Mínimo uds.:",    String.valueOf(tarifa.getMinimoUnidades()),          colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Activa:",         tarifa.isActiva() ? "Sí" : "No",                   "FFFFFF",      colorGrisBorde);
            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MATERIAL INDIVIDUAL — FICHA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarMaterialDetalladoWord(Path destino, Material m) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";
            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "FICHA DE MATERIAL", colorMulberry);
            addSectionTitle(document, "DATOS DEL MATERIAL");
            XWPFTable t = document.createTable(0, 2);
            setTableWidth(t, "100%");
            setCeldasAncho(t, 9000, 30, 70);
            addWordFichaFila(t, "Nombre:",        s(m.getNombre()),       "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Referencia:",     s(m.getReferencia()),   colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Categoría:",      s(m.getCategoria()),    "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Proveedor:",      s(m.getProveedor()),    colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Unidad:",         s(m.getUnidad()),       "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Precio/ud.:",     String.format("%.2f €", m.getPrecioUnidad()),                             colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Stock actual:",   String.format("%.2f %s", m.getStockActual(), s(m.getUnidad())),            "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Stock mínimo:",   String.format("%.2f %s", m.getStockMinimo(), s(m.getUnidad())),            colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Alerta:",         m.isBajoStock() ? "BAJO MÍNIMO" : "OK",                                   "FFFFFF",      colorGrisBorde);
            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLEADO INDIVIDUAL — FICHA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarEmpleadoDetalladoWord(Path destino, Empleado e) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";
            addWordEmpresaHeader(document, colorMulberry);
            addWordTituloDocumento(document, "FICHA DE EMPLEADO", colorMulberry);
            addSectionTitle(document, "DATOS DEL EMPLEADO");
            XWPFTable t = document.createTable(0, 2);
            setTableWidth(t, "100%");
            setCeldasAncho(t, 9000, 30, 70);
            addWordFichaFila(t, "Nombre:",      s(e.getNombre()),       "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Apellidos:",   s(e.getApellidos()),    colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "NIF:",         s(e.getNif()),          "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Categoría:",   s(e.getCategoria()),    colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Fecha alta:",  s(e.getFechaAlta()),    "FFFFFF",      colorGrisBorde);
            addWordFichaFila(t, "Estado:",      e.isActivo() ? "ACTIVO" : "BAJA" + (e.getFechaBaja() != null ? " (" + e.getFechaBaja() + ")" : ""), colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "Salario base:", String.format("%.2f €", e.getSalarioBase()), "FFFFFF", colorGrisBorde);
            addWordFichaFila(t, "IRPF:",        String.format("%.1f%%", e.getIrpf()),          colorGrisClaro, colorGrisBorde);
            addWordFichaFila(t, "IBAN:",        s(e.getIban()),         "FFFFFF",      colorGrisBorde);
            addWordFooter(document);
            try (FileOutputStream out = new FileOutputStream(destino.toFile())) { document.write(out); }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers compartidos para documentos Word detallados
    // ─────────────────────────────────────────────────────────────────────────

    private static void addWordEmpresaHeader(XWPFDocument document, String colorMulberry) {
        XWPFParagraph p0 = document.createParagraph();
        p0.setAlignment(ParagraphAlignment.RIGHT);
        p0.setSpacingAfter(0);
        XWPFRun r0 = p0.createRun();
        r0.setText("GRÁFICAS MULBERRY"); r0.setBold(true); r0.setFontSize(18); r0.setColor(colorMulberry);

        String[] lineas = {
            DatabaseManager.getConfig("empresa_direccion"),
            DatabaseManager.getConfig("empresa_cp") + " " + DatabaseManager.getConfig("empresa_ciudad"),
            "Tel: " + DatabaseManager.getConfig("empresa_telefono"),
            DatabaseManager.getConfig("empresa_email"),
            "NIF: " + DatabaseManager.getConfig("empresa_nif")
        };
        for (String linea : lineas) {
            XWPFParagraph p = document.createParagraph();
            p.setAlignment(ParagraphAlignment.RIGHT);
            p.setSpacingAfter(0); p.setSpacingBefore(0);
            XWPFRun r = p.createRun();
            r.setText(linea != null ? linea : ""); r.setFontSize(9); r.setColor("333333");
        }
    }

    private static void addWordTituloDocumento(XWPFDocument document, String titulo, String colorMulberry) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(200);
        XWPFRun r = p.createRun();
        r.setText(titulo); r.setBold(true); r.setFontSize(14); r.setColor("FFFFFF");
        p.getCTP().getPPr().addNewShd().setFill(colorMulberry);
    }

    private static void addWordClienteBlock(XWPFDocument document, Cliente c, String colorGrisClaro) {
        XWPFTable t = document.createTable(1, 1);
        setTableWidth(t, "50%");
        XWPFTableCell cell = t.getRow(0).getCell(0);
        cell.setColor(colorGrisClaro);
        addRunToCell(cell, s(c.getNombreCompleto()), true,  10, "000000");
        if (c.getNif()       != null) addRunToCell(cell, "NIF/CIF: " + c.getNif(),   false, 9, "000000");
        if (c.getDireccion() != null) addRunToCell(cell, c.getDireccion(),             false, 9, "000000");
        if (c.getCiudad()    != null) addRunToCell(cell, s(c.getCp()) + " " + c.getCiudad(), false, 9, "000000");
        if (c.getTelefono()  != null) addRunToCell(cell, "Tel: " + c.getTelefono(),   false, 9, "000000");
        if (c.getEmail()     != null) addRunToCell(cell, c.getEmail(),                 false, 9, "000000");
    }

    private static void addWordNominaFila(XWPFTable t, String concepto, String importe, String bg, String border) {
        XWPFTableRow row = t.createRow();
        XWPFTableCell c1 = row.getCell(0);
        if (c1 == null) c1 = row.addNewTableCell();
        c1.setColor(bg);
        XWPFParagraph p1 = c1.getParagraphs().get(0); p1.setSpacingAfter(0); p1.setSpacingBefore(0);
        XWPFRun r1 = p1.createRun(); r1.setText(concepto); r1.setFontSize(9); r1.setColor("000000");
        XWPFTableCell c2 = row.getCell(1);
        if (c2 == null) c2 = row.addNewTableCell();
        c2.setColor(bg);
        XWPFParagraph p2 = c2.getParagraphs().get(0);
        p2.setAlignment(ParagraphAlignment.RIGHT); p2.setSpacingAfter(0); p2.setSpacingBefore(0);
        XWPFRun r2 = p2.createRun(); r2.setText(importe); r2.setFontSize(9); r2.setColor("000000"); r2.setBold(true);
    }

    private static void addWordFichaFila(XWPFTable t, String label, String value, String bg, String border) {
        XWPFTableRow row = t.createRow();
        XWPFTableCell c1 = row.getCell(0);
        if (c1 == null) c1 = row.addNewTableCell();
        c1.setColor(bg);
        XWPFParagraph p1 = c1.getParagraphs().get(0); p1.setSpacingAfter(0); p1.setSpacingBefore(0);
        XWPFRun r1 = p1.createRun(); r1.setText(label); r1.setBold(true); r1.setFontSize(9); r1.setColor("000000");
        XWPFTableCell c2 = row.getCell(1);
        if (c2 == null) c2 = row.addNewTableCell();
        c2.setColor(bg);
        XWPFParagraph p2 = c2.getParagraphs().get(0); p2.setSpacingAfter(0); p2.setSpacingBefore(0);
        XWPFRun r2 = p2.createRun(); r2.setText(value); r2.setFontSize(9); r2.setColor("000000");
    }

    private static void addWordFooter(XWPFDocument document) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingBefore(200);
        XWPFRun r = p.createRun();
        r.setFontSize(8); r.setColor("888888");
        r.setText("Gráficas Mulberry · " + DatabaseManager.getConfig("empresa_web") +
            " · " + DatabaseManager.getConfig("empresa_email") +
            " · Tel. " + DatabaseManager.getConfig("empresa_telefono"));
    }

    // Helper para añadir la fila de cabecera de una tabla de líneas
    private static void addTableHeader(XWPFTable table, String[] headers, String colorHex) {
        XWPFTableRow row = table.getNumberOfRows() > 0 ? table.getRow(0) : table.createRow();
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = row.getCell(i);
            if (cell == null) cell = row.addNewTableCell();
            cell.setColor(colorHex);
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setAlignment(ParagraphAlignment.CENTER);
            p.setSpacingAfter(0);
            p.setSpacingBefore(0);
            XWPFRun r = p.createRun();
            r.setText(headers[i]);
            r.setBold(true);
            r.setFontSize(9);
            r.setColor("FFFFFF");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FACTURA INDIVIDUAL DETALLADA (Word)
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarFacturaDetalladaWord(Path destino, Factura f, Cliente c) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            String colorMulberry  = "6B2D5E";
            String colorGrisClaro = "F5F5F5";
            String colorGrisBorde = "C8C8C8";

            // 1. Cabecera empresa
            XWPFParagraph pHeader = document.createParagraph();
            pHeader.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun rHeader = pHeader.createRun();
            rHeader.setText("GRÁFICAS MULBERRY");
            rHeader.setBold(true);
            rHeader.setFontSize(20);
            rHeader.setColor(colorMulberry);
            rHeader.addBreak();
            rHeader.setFontSize(9);
            rHeader.setColor("000000");
            rHeader.setText(DatabaseManager.getConfig("empresa_direccion"));
            rHeader.addBreak();
            rHeader.setText(DatabaseManager.getConfig("empresa_cp") + " " + DatabaseManager.getConfig("empresa_ciudad"));
            rHeader.addBreak();
            rHeader.setText("Tel: " + DatabaseManager.getConfig("empresa_telefono"));
            rHeader.addBreak();
            rHeader.setText(DatabaseManager.getConfig("empresa_email"));
            rHeader.addBreak();
            rHeader.setText("NIF: " + DatabaseManager.getConfig("empresa_nif"));

            // Título del documento
            XWPFParagraph pDocTitle = document.createParagraph();
            pDocTitle.setAlignment(ParagraphAlignment.CENTER);
            pDocTitle.setSpacingAfter(200);
            XWPFRun rDocTitle = pDocTitle.createRun();
            rDocTitle.setText("FACTURA");
            rDocTitle.setBold(true);
            rDocTitle.setFontSize(14);
            rDocTitle.setColor("FFFFFF");
            pDocTitle.getCTP().getPPr().addNewShd().setFill(colorMulberry);

            // 2. Datos del documento
            addSectionTitle(document, "DATOS DE LA FACTURA");
            XWPFTable docDataTable = document.createTable(0, 4);
            setTableWidth(docDataTable, "100%");
            setCeldasAncho(docDataTable, 9000, 20, 30, 20, 30);
            addTableRow(docDataTable, new String[]{"Número:", s(f.getNumero()), "Estado:", s(f.getEstado()).toUpperCase()}, true, colorGrisClaro, colorGrisBorde);
            addTableRow(docDataTable, new String[]{"Fecha:", s(f.getFecha()), "Vencimiento:", s(f.getFechaVencimiento())}, false, colorGrisClaro, colorGrisBorde);
            addTableRow(docDataTable, new String[]{"Forma de pago:", s(f.getFormaPago()), "", ""}, false, colorGrisClaro, colorGrisBorde);
            addParagraph(document, "");

            // 3. Datos del cliente
            addSectionTitle(document, "CLIENTE");
            XWPFTable clientDataTable = document.createTable(1, 1);
            setTableWidth(clientDataTable, "50%");
            XWPFTableCell clientCell = clientDataTable.getRow(0).getCell(0);
            clientCell.setColor(colorGrisClaro);
            addRunToCell(clientCell, s(c.getNombreCompleto()), true, 10, "000000");
            if (c.getNif()       != null) addRunToCell(clientCell, "NIF/CIF: " + c.getNif(),   false, 9, "000000");
            if (c.getDireccion() != null) addRunToCell(clientCell, c.getDireccion(),             false, 9, "000000");
            if (c.getCiudad()    != null) addRunToCell(clientCell, s(c.getCp()) + " " + c.getCiudad(), false, 9, "000000");
            if (c.getTelefono()  != null) addRunToCell(clientCell, "Tel: " + c.getTelefono(),   false, 9, "000000");
            if (c.getEmail()     != null) addRunToCell(clientCell, c.getEmail(),                 false, 9, "000000");
            addParagraph(document, "");

            // 4. Tabla de líneas
            addSectionTitle(document, "DETALLE DE LA FACTURA");
            XWPFTable lineasTable = document.createTable(0, 6);
            setTableWidth(lineasTable, "100%");
            addTableHeader(lineasTable, new String[]{"Descripción", "Técnica", "Cant.", "Precio ud.", "Dto.", "Total"}, colorMulberry);
            setCeldasAncho(lineasTable, 9000, 35, 20, 8, 14, 8, 15);

            boolean par = false;
            for (LineaFactura linea : f.getLineas()) {
                String bg = par ? colorGrisClaro : "FFFFFF";
                XWPFTableRow row = lineasTable.createRow();
                addCell(row, 0, s(linea.getDescripcion()),                                           bg, false, ParagraphAlignment.LEFT);
                addCell(row, 1, s(linea.getTecnica()),                                               bg, false, ParagraphAlignment.LEFT);
                addCell(row, 2, String.valueOf(linea.getCantidad()),                                 bg, false, ParagraphAlignment.RIGHT);
                addCell(row, 3, String.format("%.2f €", linea.getPrecioUnit()),                      bg, false, ParagraphAlignment.RIGHT);
                addCell(row, 4, linea.getDescuento() > 0 ? String.format("%.0f%%", linea.getDescuento()) : "-", bg, false, ParagraphAlignment.RIGHT);
                addCell(row, 5, String.format("%.2f €", linea.getTotal()),                           bg, false, ParagraphAlignment.RIGHT);
                par = !par;
            }
            addParagraph(document, "");

            // 5. Totales
            XWPFTable totalesTable = document.createTable(0, 2);
            setTableWidth(totalesTable, "40%");
            setCeldasAncho(totalesTable, 3600, 60, 40);
            totalesTable.setTableAlignment(TableRowAlign.RIGHT);
            addTableRow(totalesTable, new String[]{"Base imponible:", String.format("%.2f €", f.getBaseImponible())}, false, "FFFFFF", colorGrisBorde);
            addTableRow(totalesTable, new String[]{String.format("IVA (%.0f%%):", f.getIvaPorcentaje()), String.format("%.2f €", f.getIvaImporte())}, false, "FFFFFF", colorGrisBorde);
            addTableRow(totalesTable, new String[]{"TOTAL:", String.format("%.2f €", f.getTotal())}, true, colorMulberry, colorGrisBorde);
            addParagraph(document, "");

            // 6. Forma de pago y notas
            addSectionTitle(document, "FORMA DE PAGO");
            addParagraph(document, f.getFormaPago() != null ? f.getFormaPago() : "Transferencia bancaria");
            if (f.getNotas() != null && !f.getNotas().isBlank()) {
                addSectionTitle(document, "NOTAS");
                addParagraph(document, f.getNotas());
            }

            // 7. Pie de página
            XWPFParagraph pFooter = document.createParagraph();
            pFooter.setAlignment(ParagraphAlignment.CENTER);
            pFooter.setSpacingBefore(200);
            XWPFRun rFooter = pFooter.createRun();
            rFooter.setFontSize(8);
            rFooter.setColor("888888");
            rFooter.setText("Gráficas Mulberry · " + DatabaseManager.getConfig("empresa_web") +
                " · " + DatabaseManager.getConfig("empresa_email") +
                " · Tel. " + DatabaseManager.getConfig("empresa_telefono"));

            try (FileOutputStream out = new FileOutputStream(destino.toFile())) {
                document.write(out);
            }
        }
        return destino;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES EXCEL (.xlsx) — helpers internos
    // ─────────────────────────────────────────────────────────────────────────

    private static XSSFCellStyle excelHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)107, (byte)45, (byte)94}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static XSSFCellStyle excelRowStyle(XSSFWorkbook wb, boolean par) {
        XSSFCellStyle style = wb.createCellStyle();
        if (!par) {
            style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xF5, (byte)0xEE, (byte)0xF4}, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private static void excelFillRow(XSSFRow row, String[] vals, XSSFCellStyle style) {
        for (int j = 0; j < vals.length; j++) {
            XSSFCell c = row.createCell(j);
            c.setCellValue(vals[j] != null ? vals[j] : "");
            c.setCellStyle(style);
        }
    }

    private static XSSFSheet excelCreateSheet(XSSFWorkbook wb, String nombre, String[] headers) {
        XSSFSheet sheet = wb.createSheet(nombre);
        XSSFRow hRow = sheet.createRow(0);
        XSSFCellStyle hStyle = excelHeaderStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(hStyle);
        }
        return sheet;
    }

    private static void excelAutosize(XSSFSheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTACIONES EXCEL (.xlsx) — por entidad
    // ─────────────────────────────────────────────────────────────────────────

    public static Path exportarClientesExcel(Path destino, List<Cliente> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Nombre", "Apellidos", "Tipo", "NIF/CIF", "Teléfono", "Email", "Ciudad"};
            XSSFSheet sheet = excelCreateSheet(wb, "Clientes", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Cliente c = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(c.getNombre()), s(c.getApellidos()), s(c.getTipo()),
                                 s(c.getNif()), s(c.getTelefono()), s(c.getEmail()), s(c.getCiudad())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarFacturasExcel(Path destino, List<Factura> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Número", "Cliente", "Fecha", "Vencimiento",
                                 "Forma pago", "Estado", "Base", "IVA%", "Total"};
            XSSFSheet sheet = excelCreateSheet(wb, "Facturas", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Factura f = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(f.getNumero()), s(f.getClienteNombre()), s(f.getFecha()),
                                 s(f.getFechaVencimiento()), s(f.getFormaPago()), s(f.getEstado()),
                                 String.format("%.2f", f.getBaseImponible()),
                                 String.format("%.0f", f.getIvaPorcentaje()),
                                 String.format("%.2f", f.getTotal())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarAlbaranesExcel(Path destino, List<Albaran> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Número", "Cliente", "Fecha", "Factura", "Pedido", "Estado", "Observaciones"};
            XSSFSheet sheet = excelCreateSheet(wb, "Albaranes", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Albaran a = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(a.getNumero()), s(a.getClienteNombre()), s(a.getFecha()),
                                 s(a.getFacturaNumero()), s(a.getPedidoNumero()),
                                 s(a.getEstado()), s(a.getObservaciones())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarPedidosExcel(Path destino, List<Pedido> lista) throws Exception {
        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Número", "Cliente", "Fecha", "Entrega prev.",
                                 "Estado", "Descripción", "Total", "Pendiente"};
            XSSFSheet sheet = excelCreateSheet(wb, "Pedidos", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Pedido p = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(p.getNumero()), s(p.getClienteNombre()),
                                 p.getFecha() != null ? p.getFecha().format(fmtFecha) : "",
                                 p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().format(fmtFecha) : "",
                                 s(p.getEstadoDisplay()), s(p.getDescripcion()),
                                 String.format("%.2f", p.getImporteTotal()),
                                 String.format("%.2f", p.getImportePendiente())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarMaterialesExcel(Path destino, List<Material> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Nombre", "Referencia", "Categoría",
                                 "Stock actual", "Stock mín.", "Unidad",
                                 "Precio/ud.", "Proveedor", "Alerta"};
            XSSFSheet sheet = excelCreateSheet(wb, "Materiales", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            XSSFCellStyle alertaStyle = wb.createCellStyle();
            alertaStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFF, (byte)0xEB, (byte)0xEB}, null));
            alertaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < lista.size(); i++) {
                Material m = lista.get(i);
                XSSFCellStyle st = m.isBajoStock() ? alertaStyle : (i % 2 == 0 ? par : impar);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(m.getNombre()), s(m.getReferencia()), s(m.getCategoria()),
                                 String.valueOf(m.getStockActual()), String.valueOf(m.getStockMinimo()),
                                 s(m.getUnidad()), String.format("%.2f", m.getPrecioUnidad()),
                                 s(m.getProveedor()), m.isBajoStock() ? "¡Bajo!" : "OK"},
                    st);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarTarifasExcel(Path destino, List<Tarifa> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Técnica", "Nombre", "Descripción",
                                 "Precio/ud.", "Setup (€)", "Mín. uds.", "Activa"};
            XSSFSheet sheet = excelCreateSheet(wb, "Tarifas", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Tarifa t = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(t.getTecnica()), s(t.getNombre()), s(t.getDescripcion()),
                                 String.format("%.2f", t.getPrecioUnit()),
                                 String.format("%.2f", t.getPrecioSetup()),
                                 String.valueOf(t.getMinimoUnidades()),
                                 t.isActiva() ? "Sí" : "No"},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarNominasExcel(Path destino, List<Nomina> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Empleado", "Período", "Salario base",
                                 "Bruto", "SS Trab.", "IRPF%", "IRPF €", "Neto", "Coste empresa"};
            XSSFSheet sheet = excelCreateSheet(wb, "Nóminas", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Nomina n = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(n.getEmpleadoNombre()), s(n.getPeriodo()),
                                 String.format("%.2f", n.getSalarioBase()),
                                 String.format("%.2f", n.getTotalBruto()),
                                 String.format("%.2f", n.getSsTrabajador()),
                                 String.format("%.1f", n.getIrpfPorcentaje()),
                                 String.format("%.2f", n.getIrpfImporte()),
                                 String.format("%.2f", n.getNeto()),
                                 String.format("%.2f", n.getCosteTotalEmpresa())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarEmpleadosExcel(Path destino, List<Empleado> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Nombre", "Apellidos", "NIF", "Categoría",
                                 "Salario base", "IRPF%", "Fecha alta", "Estado"};
            XSSFSheet sheet = excelCreateSheet(wb, "Empleados", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            XSSFCellStyle bajaStyle = wb.createCellStyle();
            bajaStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)0xFF, (byte)0xEB, (byte)0xEB}, null));
            bajaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < lista.size(); i++) {
                Empleado e = lista.get(i);
                String estado = e.isActivo() ? "ACTIVO"
                    : "BAJA" + (e.getFechaBaja() != null ? " " + e.getFechaBaja() : "");
                XSSFCellStyle st = !e.isActivo() ? bajaStyle : (i % 2 == 0 ? par : impar);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(e.getNombre()), s(e.getApellidos()), s(e.getNif()),
                                 s(e.getCategoria()),
                                 String.format("%.2f", e.getSalarioBase()),
                                 String.format("%.1f", e.getIrpf()),
                                 s(e.getFechaAlta()), estado},
                    st);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

    public static Path exportarPresupuestosExcel(Path destino, List<Presupuesto> lista) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String[] headers = {"Número", "Cliente", "Fecha", "Validez", "Estado", "Base", "IVA%", "Total"};
            XSSFSheet sheet = excelCreateSheet(wb, "Presupuestos", headers);
            XSSFCellStyle par = excelRowStyle(wb, true), impar = excelRowStyle(wb, false);
            for (int i = 0; i < lista.size(); i++) {
                Presupuesto p = lista.get(i);
                excelFillRow(sheet.createRow(i + 1),
                    new String[]{s(p.getNumero()), s(p.getClienteNombre()), s(p.getFecha()),
                                 s(p.getFechaValidez()), s(p.getEstado()),
                                 String.format("%.2f", p.getBaseImponible()),
                                 String.format("%.0f", p.getIvaPorcentaje()),
                                 String.format("%.2f", p.getTotal())},
                    i % 2 == 0 ? par : impar);
            }
            excelAutosize(sheet, headers.length);
            try (FileOutputStream fos = new FileOutputStream(destino.toFile())) { wb.write(fos); }
        }
        return destino;
    }

}
