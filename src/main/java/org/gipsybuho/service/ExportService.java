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
import org.apache.poi.xwpf.usermodel.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.Cliente;
import org.gipsybuho.model.Albaran;
import org.gipsybuho.model.Empleado;
import org.gipsybuho.model.Material;
import org.gipsybuho.model.Nomina;
import org.gipsybuho.model.Pedido;
import org.gipsybuho.model.Factura;
import org.gipsybuho.model.Presupuesto;
import org.gipsybuho.model.Tarifa;

import java.awt.Color;
import java.io.*;
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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
            tabla.setWidth("100%");

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
}
