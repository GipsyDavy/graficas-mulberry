package org.gipsybuho.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.gipsybuho.model.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PdfPreviewService {

    private static final Color MULBERRY  = new Color(107, 45, 94);
    private static final Color ALT_BG    = new Color(245, 238, 244);
    private static final Color BORDER    = new Color(220, 220, 220);

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static String s(Object o) { return o == null ? "" : o.toString(); }

    private static String ahora() {
        return java.time.LocalDateTime.now().format(FMT);
    }

    // ─── helpers comunes ─────────────────────────────────────────────────────

    private static BaseFont bf() throws Exception {
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
    }

    private static BaseFont bfBold() throws Exception {
        return BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
    }

    private static byte[] cerrar(Document doc, ByteArrayOutputStream baos) {
        doc.close();
        return baos.toByteArray();
    }

    private static void cabecera(Document doc, Font fTitulo, Font fSubtit,
            String titulo, int total, String entidad) throws Exception {
        Paragraph p = new Paragraph(titulo, fTitulo);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        Paragraph sub = new Paragraph("Generado: " + ahora() + "  ·  Total: " + total + " " + entidad, fSubtit);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        doc.add(sub);
    }

    private static PdfPCell hdrCell(String texto, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(MULBERRY);
        c.setPadding(6);
        c.setBorderColor(MULBERRY);
        return c;
    }

    private static PdfPCell dataCell(String texto, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setBorderColor(BORDER);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENTES
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarClientes(List<Cliente> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Clientes — Gráficas Mulberry", lista.size(), "cliente(s)");

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.2f, 2.2f, 1.2f, 1.8f, 1.8f, 3f, 1.8f});
        for (String h : new String[]{"Nombre","Apellidos","Tipo","NIF/CIF","Teléfono","Email","Ciudad"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Cliente c = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{s(c.getNombre()),s(c.getApellidos()),s(c.getTipo()),
                    s(c.getNif()),s(c.getTelefono()),s(c.getEmail()),s(c.getCiudad())})
                tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRESUPUESTOS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarPresupuestos(List<Presupuesto> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Presupuestos — Gráficas Mulberry", lista.size(), "presupuesto(s)");

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.8f, 2.8f, 1.2f, 1.2f, 1.3f, 1.5f, 0.9f, 1.5f});
        for (String h : new String[]{"Número","Cliente","Fecha","Validez","Estado","Base","IVA%","Total"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Presupuesto p = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(p.getNumero()), s(p.getClienteNombre()), s(p.getFecha()), s(p.getFechaValidez()),
                s(p.getEstado()),
                String.format("%.2f €", p.getBaseImponible()),
                String.format("%.0f%%", p.getIvaPorcentaje()),
                String.format("%.2f €", p.getTotal())
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FACTURAS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarFacturas(List<Factura> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Facturas — Gráficas Mulberry", lista.size(), "factura(s)");

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.6f, 2.5f, 1.1f, 1.1f, 1.8f, 1.2f, 1.4f, 0.8f, 1.4f});
        for (String h : new String[]{"Número","Cliente","Fecha","Vencimiento","Forma pago","Estado","Base","IVA%","Total"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Factura f = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(f.getNumero()), s(f.getClienteNombre()), s(f.getFecha()),
                s(f.getFechaVencimiento()), s(f.getFormaPago()), s(f.getEstado()),
                String.format("%.2f €", f.getBaseImponible()),
                String.format("%.0f%%", f.getIvaPorcentaje()),
                String.format("%.2f €", f.getTotal())
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALBARANES
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarAlbaranes(List<Albaran> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Albaranes — Gráficas Mulberry", lista.size(), "albarán(es)");

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.6f, 2.5f, 1.2f, 1.8f, 1.8f, 1.3f, 3f});
        for (String h : new String[]{"Número","Cliente","Fecha","Factura","Pedido","Estado","Observaciones"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Albaran a = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(a.getNumero()), s(a.getClienteNombre()), s(a.getFecha()),
                s(a.getFacturaNumero()), s(a.getPedidoNumero()),
                s(a.getEstado()), s(a.getObservaciones())
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PEDIDOS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarPedidos(List<Pedido> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Pedidos — Gráficas Mulberry", lista.size(), "pedido(s)");

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 2.4f, 1.2f, 1.2f, 1.4f, 2.5f, 1.4f, 1.4f});
        for (String h : new String[]{"Número","Cliente","Fecha","Entrega prev.","Estado","Descripción","Total","Pendiente"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Pedido p = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(p.getNumero()), s(p.getClienteNombre()),
                p.getFecha() != null ? p.getFecha().format(FMT_FECHA) : "",
                p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().format(FMT_FECHA) : "",
                s(p.getEstadoDisplay()), s(p.getDescripcion()),
                String.format("%.2f €", p.getImporteTotal()),
                String.format("%.2f €", p.getImportePendiente())
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TARIFAS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarTarifas(List<Tarifa> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Tarifas — Gráficas Mulberry", lista.size(), "tarifa(s)");

        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 2.5f, 3f, 1.2f, 1.2f, 0.9f, 0.8f});
        for (String h : new String[]{"Técnica","Nombre","Descripción","Precio/ud.","Setup (€)","Mín. uds.","Activa"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Tarifa t = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(t.getTecnica()), s(t.getNombre()), s(t.getDescripcion()),
                String.format("%.2f €", t.getPrecioUnit()),
                String.format("%.2f €", t.getPrecioSetup()),
                String.valueOf(t.getMinimoUnidades()),
                t.isActiva() ? "Sí" : "No"
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MATERIALES
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarMateriales(List<Material> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Materiales — Gráficas Mulberry", lista.size(), "material(es)");

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.8f, 1.4f, 1.4f, 1.3f, 1.3f, 0.8f, 1.2f, 2f, 0.8f});
        for (String h : new String[]{"Nombre","Referencia","Categoría","Stock actual","Stock mín.","Unidad","Precio/ud.","Proveedor","Alerta"})
            tabla.addCell(hdrCell(h, fHeader));

        Color alertBg = new Color(255, 235, 235);
        for (int i = 0; i < lista.size(); i++) {
            Material m = lista.get(i);
            Color bg = m.isBajoStock() ? alertBg : (i % 2 == 0 ? Color.WHITE : ALT_BG);
            for (String v : new String[]{
                s(m.getNombre()), s(m.getReferencia()), s(m.getCategoria()),
                String.valueOf(m.getStockActual()),
                String.valueOf(m.getStockMinimo()),
                s(m.getUnidad()),
                String.format("%.2f €", m.getPrecioUnidad()),
                s(m.getProveedor()),
                m.isBajoStock() ? "⚠ Bajo" : "OK"
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLEADOS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarEmpleados(List<Empleado> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Empleados — Gráficas Mulberry", lista.size(), "empleado(s)");

        PdfPTable tabla = new PdfPTable(8);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.8f, 1.8f, 1.2f, 1.5f, 1.3f, 0.8f, 1.2f, 1.3f});
        for (String h : new String[]{"Nombre","Apellidos","NIF","Categoría","Salario base","IRPF%","Fecha alta","Estado"})
            tabla.addCell(hdrCell(h, fHeader));

        Color bajaBg = new Color(255, 235, 235);
        for (int i = 0; i < lista.size(); i++) {
            Empleado e = lista.get(i);
            Color bg = !e.isActivo() ? bajaBg : (i % 2 == 0 ? Color.WHITE : ALT_BG);
            String estado = e.isActivo() ? "ACTIVO"
                : "BAJA" + (e.getFechaBaja() != null ? " " + e.getFechaBaja() : "");
            for (String v : new String[]{
                s(e.getNombre()), s(e.getApellidos()), s(e.getNif()), s(e.getCategoria()),
                String.format("%.2f €", e.getSalarioBase()),
                String.format("%.1f%%", e.getIrpf()),
                s(e.getFechaAlta()), estado
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NÓMINAS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarNominas(List<Nomina> lista) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 50, 30);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font fTitulo = new Font(bfBold(), 16, Font.BOLD,   MULBERRY);
        Font fSubtit = new Font(bf(),     10, Font.NORMAL, Color.GRAY);
        Font fHeader = new Font(bfBold(),  9, Font.BOLD,   Color.WHITE);
        Font fNormal = new Font(bf(),       9, Font.NORMAL);

        cabecera(doc, fTitulo, fSubtit, "Listado de Nóminas — Gráficas Mulberry", lista.size(), "nómina(s)");

        PdfPTable tabla = new PdfPTable(9);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.5f, 1.5f, 1.4f, 1.4f, 1.3f, 0.9f, 1.3f, 1.4f, 1.6f});
        for (String h : new String[]{"Empleado","Período","Salario base","Bruto","SS Trab.","IRPF%","IRPF €","Neto","Coste empresa"})
            tabla.addCell(hdrCell(h, fHeader));

        for (int i = 0; i < lista.size(); i++) {
            Nomina n = lista.get(i);
            Color bg = i % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : new String[]{
                s(n.getEmpleadoNombre()), s(n.getPeriodo()),
                String.format("%.2f €", n.getSalarioBase()),
                String.format("%.2f €", n.getTotalBruto()),
                String.format("%.2f €", n.getSsTrabajador()),
                String.format("%.1f%%", n.getIrpfPorcentaje()),
                String.format("%.2f €", n.getIrpfImporte()),
                String.format("%.2f €", n.getNeto()),
                String.format("%.2f €", n.getCosteTotalEmpresa())
            }) tabla.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(tabla);
        return cerrar(doc, baos);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESTADÍSTICAS
    // ─────────────────────────────────────────────────────────────────────────

    public static byte[] previsualizarEstadisticas(int anio) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        BaseFont bfb = bfBold();
        BaseFont bfn = bf();
        Font fTitulo    = new Font(bfb, 16, Font.BOLD,   MULBERRY);
        Font fSubtit    = new Font(bfn, 10, Font.NORMAL, Color.GRAY);
        Font fSeccion   = new Font(bfb, 13, Font.BOLD,   Color.WHITE);
        Font fHdr       = new Font(bfb,  9, Font.BOLD,   Color.WHITE);
        Font fNormal    = new Font(bfn,  9, Font.NORMAL);
        Font fKpi       = new Font(bfb, 13, Font.BOLD);

        Paragraph titulo = new Paragraph("Informe de Estadísticas " + anio + " — Gráficas Mulberry", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph sub = new Paragraph("Generado: " + ahora(), fSubtit);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(16);
        doc.add(sub);

        double totIng    = EstadisticasService.totalIngresosAnio(anio);
        double totGMat   = EstadisticasService.totalGastosMaterialAnio(anio);
        double totNom    = EstadisticasService.totalNominasAnio(anio);
        double beneficio = totIng - totGMat - totNom;

        agregarSeccion(doc, "FINANCIERO", fSeccion);
        agregarKpisFinanciero(doc, anio, fHdr, fKpi, totIng, totGMat, totNom, beneficio);
        doc.add(Chunk.NEWLINE);
        agregarTablaSimple(doc, "Ingresos vs Gastos por mes (" + anio + ")",
            fHdr, fNormal, new String[]{"Mes","Ingresos €","Gastos mat. €","Nóminas €","Balance €"},
            buildMensual(anio));
        doc.add(Chunk.NEWLINE);

        doc.newPage();
        agregarSeccion(doc, "MATERIALES", fSeccion);
        agregarTablaKV(doc, "Top 10 materiales más consumidos", fHdr, fNormal,
            "Material", "Uds. consumidas", EstadisticasService.materialesMasUsados(10));
        doc.add(Chunk.NEWLINE);
        agregarTablaKV(doc, "Materiales por categoría", fHdr, fNormal,
            "Categoría", "Artículos", EstadisticasService.materialesPorCategoria());
        doc.add(Chunk.NEWLINE);
        agregarTablaKV(doc, "Stock actual (top 12)", fHdr, fNormal,
            "Material", "Stock", EstadisticasService.stockMateriales(12));

        doc.newPage();
        agregarSeccion(doc, "CLIENTES", fSeccion);
        agregarTablaKV(doc, "Top 8 clientes por facturación", fHdr, fNormal,
            "Cliente", "Facturación €", EstadisticasService.topClientesPorFacturacion(8));
        doc.add(Chunk.NEWLINE);
        agregarTablaKV(doc, "Clientes por tipo", fHdr, fNormal,
            "Tipo", "Cantidad", EstadisticasService.clientesPorTipo());

        doc.newPage();
        agregarSeccion(doc, "PRODUCCIÓN", fSeccion);
        agregarTablaKV(doc, "Ingresos por técnica", fHdr, fNormal,
            "Técnica", "Ingresos €", EstadisticasService.tecnicasPorIngresos());
        doc.add(Chunk.NEWLINE);
        agregarTablaKV(doc, "Volumen por técnica (líneas)", fHdr, fNormal,
            "Técnica", "Líneas", EstadisticasService.tecnicasPorVolumen());
        doc.add(Chunk.NEWLINE);
        agregarTablaKV(doc, "Pedidos por estado", fHdr, fNormal,
            "Estado", "Pedidos", EstadisticasService.pedidosPorEstado());

        return cerrar(doc, baos);
    }

    private static void agregarSeccion(Document doc, String texto, Font f) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(MULBERRY);
        c.setPadding(7);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
        doc.add(Chunk.NEWLINE);
    }

    private static void agregarKpisFinanciero(Document doc, int anio,
            Font fHdr, Font fKpi,
            double ingresos, double gastosMat, double gastosNom, double beneficio) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        for (String h : new String[]{"Ingresos " + anio,"Gastos materiales","Gastos nóminas","Beneficio estimado"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fHdr));
            c.setBackgroundColor(MULBERRY);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
        Color cVerde   = new Color(39, 174, 96);
        Color cRojo    = new Color(231, 76, 60);
        Color cNaranja = new Color(243, 156, 18);
        double[] vals = {ingresos, gastosMat, gastosNom, beneficio};
        Color[]  cols = {cVerde, cRojo, cNaranja, beneficio >= 0 ? cVerde : cRojo};
        for (int i = 0; i < vals.length; i++) {
            Font f = new Font(fKpi.getBaseFont(), 13, Font.BOLD, cols[i]);
            PdfPCell c = new PdfPCell(new Phrase(String.format("%.2f €", vals[i]), f));
            c.setPadding(10);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(new Color(200,200,200));
            t.addCell(c);
        }
        doc.add(t);
    }

    private static String[][] buildMensual(int anio) {
        Map<String,Double> ing  = EstadisticasService.ingresosPorMes(anio);
        Map<String,Double> gmat = EstadisticasService.gastosMaterialPorMes(anio);
        Map<String,Double> gnom = EstadisticasService.gastosNominasPorMes(anio);
        String[] meses = {"01","02","03","04","05","06","07","08","09","10","11","12"};
        String[] nombres = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        String[][] rows = new String[12][];
        for (int i = 0; i < 12; i++) {
            String k = anio + "-" + meses[i];
            double i2 = ing.getOrDefault(k,  0.0);
            double m2 = gmat.getOrDefault(k, 0.0);
            double n2 = gnom.getOrDefault(k, 0.0);
            rows[i] = new String[]{nombres[i],
                String.format("%.2f", i2), String.format("%.2f", m2),
                String.format("%.2f", n2), String.format("%.2f", i2-m2-n2)};
        }
        return rows;
    }

    private static void agregarTablaSimple(Document doc, String titulo,
            Font fHdr, Font fNormal, String[] headers, String[][] rows) throws Exception {
        doc.add(new Phrase(titulo + "\n", new Font(fHdr.getBaseFont(), 10, Font.BOLD, MULBERRY)));
        doc.add(Chunk.NEWLINE);
        PdfPTable t = new PdfPTable(headers.length);
        t.setWidthPercentage(100);
        for (String h : headers) t.addCell(hdrCell(h, fHdr));
        for (int r = 0; r < rows.length; r++) {
            Color bg = r % 2 == 0 ? Color.WHITE : ALT_BG;
            for (String v : rows[r]) t.addCell(dataCell(v, fNormal, bg));
        }
        doc.add(t);
    }

    private static void agregarTablaKV(Document doc, String titulo,
            Font fHdr, Font fNormal, String col1, String col2, Map<String,?> datos) throws Exception {
        doc.add(new Phrase(titulo + "\n", new Font(fHdr.getBaseFont(), 10, Font.BOLD, MULBERRY)));
        doc.add(Chunk.NEWLINE);
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(70);
        t.addCell(hdrCell(col1, fHdr));
        t.addCell(hdrCell(col2, fHdr));
        int r = 0;
        for (Map.Entry<String,?> e : datos.entrySet()) {
            Color bg = r % 2 == 0 ? Color.WHITE : ALT_BG;
            t.addCell(dataCell(s(e.getKey()),   fNormal, bg));
            t.addCell(dataCell(s(e.getValue()), fNormal, bg));
            r++;
        }
        doc.add(t);
    }
}
