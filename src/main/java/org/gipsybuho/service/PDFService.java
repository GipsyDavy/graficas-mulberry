package org.gipsybuho.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.gipsybuho.db.DatabaseManager;
import org.gipsybuho.model.*;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PDFService {

    private static final Color COLOR_MULBERRY = new Color(107, 45, 94);
    private static final Color COLOR_GRIS_CLARO = new Color(245, 245, 245);
    private static final Color COLOR_GRIS_BORDE = new Color(200, 200, 200);

    private Font fontTitulo, fontSubtitulo, fontNormal, fontNegrita, fontPequeno;

    private void initFonts() throws Exception {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        fontTitulo    = new Font(bfBold, 20, Font.BOLD, COLOR_MULBERRY);
        fontSubtitulo = new Font(bfBold, 12, Font.BOLD, COLOR_MULBERRY);
        fontNormal    = new Font(bf, 9);
        fontNegrita   = new Font(bfBold, 9, Font.BOLD);
        fontPequeno   = new Font(bf, 8, Font.NORMAL, Color.GRAY);
    }

    private static Path getDocumentosPath() {
        return FileSystemView.getFileSystemView().getDefaultDirectory().toPath();
    }

    private static String safeFilename(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").replaceAll("\\.{2,}", "_");
    }

    private boolean esLineaMaterial(String tecnica) {
        return "📦 Material".equals(tecnica);
    }

    private boolean esLineaMaterialAlbaran(LineaAlbaran linea) {
        return linea != null && nvl(linea.getDescripcion()).startsWith("[MATERIAL] ");
    }

    private String descripcionAlbaranVisible(LineaAlbaran linea) {
        String descripcion = nvl(linea.getDescripcion());
        return descripcion.startsWith("[MATERIAL] ") ? descripcion.substring("[MATERIAL] ".length()) : descripcion;
    }

    private String unidadAlbaranVisible(LineaAlbaran linea) {
        String unidad = nvl(linea.getUnidad());
        return "__material__".equals(unidad) ? "ud" : unidad;
    }

    public Path generarPresupuesto(Presupuesto p, Cliente c) throws Exception {
        return generarPresupuesto(p, c, false);
    }

    public Path generarPresupuesto(Presupuesto p, Cliente c, boolean incluirMateriales) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Presupuestos")
            .resolve(safeFilename(p.getNumero()) + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, writer, "PRESUPUESTO");
        addDatosDocumento(doc, p.getNumero(), p.getFecha(), p.getFechaValidez(), p.getEstado());
        addDatosCliente(doc, c);

        doc.add(Chunk.NEWLINE);
        addTablaLineas(doc,
            p.getLineas().stream()
                .filter(l -> incluirMateriales || !esLineaMaterial(l.getTecnica()))
                .map(l -> new String[]{
                    l.getDescripcion(), l.getTecnica(), String.valueOf(l.getCantidad()),
                    String.format("%.2f €", l.getPrecioUnit()),
                    l.getDescuento() > 0 ? String.format("%.0f%%", l.getDescuento()) : "-",
                    String.format("%.2f €", l.getTotal())
                }).toArray(String[][]::new)
        );

        addTotales(doc, p.getBaseImponible(), p.getIvaPorcentaje(), p.getIvaImporte(), p.getTotal());

        if (p.getNotas() != null && !p.getNotas().isBlank()) {
            doc.add(Chunk.NEWLINE);
            addSeccion(doc, "Notas", p.getNotas());
        }
        if (p.getCondiciones() != null && !p.getCondiciones().isBlank()) {
            addSeccion(doc, "Condiciones", p.getCondiciones());
        }

        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    public Path generarFactura(Factura f, Cliente c) throws Exception {
        return generarFactura(f, c, false);
    }

    public Path generarFactura(Factura f, Cliente c, boolean incluirMateriales) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Facturas")
            .resolve(safeFilename(f.getNumero()) + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, writer, "FACTURA");
        addDatosDocumento(doc, f.getNumero(), f.getFecha(), f.getFechaVencimiento(), f.getEstado());
        addDatosCliente(doc, c);

        doc.add(Chunk.NEWLINE);
        addTablaLineas(doc,
            f.getLineas().stream()
                .filter(l -> incluirMateriales || !esLineaMaterial(l.getTecnica()))
                .map(l -> new String[]{
                l.getDescripcion(), l.getTecnica(), String.valueOf(l.getCantidad()),
                String.format("%.2f €", l.getPrecioUnit()),
                l.getDescuento() > 0 ? String.format("%.0f%%", l.getDescuento()) : "-",
                String.format("%.2f €", l.getTotal())
            }).toArray(String[][]::new)
        );

        addTotales(doc, f.getBaseImponible(), f.getIvaPorcentaje(), f.getIvaImporte(), f.getTotal());

        doc.add(Chunk.NEWLINE);
        addSeccion(doc, "Forma de pago", f.getFormaPago() != null ? f.getFormaPago() : "Transferencia bancaria");
        if (f.getNotas() != null && !f.getNotas().isBlank()) addSeccion(doc, "Notas", f.getNotas());

        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    public Path generarAlbaran(org.gipsybuho.model.Albaran a, Cliente c) throws Exception {
        return generarAlbaran(a, c, false);
    }

    public Path generarAlbaran(org.gipsybuho.model.Albaran a, Cliente c, boolean incluirMateriales) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Albaranes")
            .resolve(safeFilename(a.getNumero()) + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, null, "ALBARÁN DE ENTREGA");

        // Datos del albarán
        PdfPTable tDatos = new PdfPTable(4);
        tDatos.setWidthPercentage(100);
        tDatos.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
        addCeldaInfo(tDatos, "Número:", a.getNumero());
        addCeldaInfo(tDatos, "Fecha entrega:", nvl(a.getFecha()));
        if (a.getFacturaNumero() != null && !a.getFacturaNumero().isBlank()) {
            addCeldaInfo(tDatos, "Factura ref.:", a.getFacturaNumero());
        } else {
            addCeldaInfo(tDatos, "", "");
        }
        if (a.getPedidoNumero() != null && !a.getPedidoNumero().isBlank()) {
            addCeldaInfo(tDatos, "Pedido ref.:", a.getPedidoNumero());
        } else {
            addCeldaInfo(tDatos, "", "");
        }
        doc.add(tDatos);

        addDatosCliente(doc, c);
        doc.add(Chunk.NEWLINE);

        // Tabla de líneas sin precios
        PdfPTable tLineas = new PdfPTable(3);
        tLineas.setWidthPercentage(100);
        tLineas.setWidths(new float[]{5f, 1.2f, 1.2f});

        String[] cabeceras = {"Descripción", "Cantidad", "Unidad"};
        for (String cab : cabeceras) {
            PdfPCell cell = new PdfPCell(new Phrase(cab, new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(COLOR_MULBERRY);
            cell.setPadding(5);
            cell.setBorderColor(COLOR_MULBERRY);
            tLineas.addCell(cell);
        }

        boolean par = false;
        List<LineaAlbaran> lineasVisibles = a.getLineas().stream()
            .filter(l -> incluirMateriales || !esLineaMaterialAlbaran(l))
            .toList();
        for (var linea : lineasVisibles) {
            Color bg = par ? COLOR_GRIS_CLARO : Color.WHITE;
            PdfPCell cDesc = new PdfPCell(new Phrase(descripcionAlbaranVisible(linea), fontNormal));
            cDesc.setBackgroundColor(bg); cDesc.setBorderColor(COLOR_GRIS_BORDE); cDesc.setPadding(4);
            PdfPCell cCant = new PdfPCell(new Phrase(String.valueOf(linea.getCantidad()), fontNormal));
            cCant.setBackgroundColor(bg); cCant.setBorderColor(COLOR_GRIS_BORDE); cCant.setPadding(4);
            cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell cUnid = new PdfPCell(new Phrase(unidadAlbaranVisible(linea), fontNormal));
            cUnid.setBackgroundColor(bg); cUnid.setBorderColor(COLOR_GRIS_BORDE); cUnid.setPadding(4);
            cUnid.setHorizontalAlignment(Element.ALIGN_CENTER);
            tLineas.addCell(cDesc);
            tLineas.addCell(cCant);
            tLineas.addCell(cUnid);
            par = !par;
        }
        doc.add(tLineas);

        if (a.getObservaciones() != null && !a.getObservaciones().isBlank()) {
            doc.add(Chunk.NEWLINE);
            addSeccion(doc, "Observaciones", a.getObservaciones());
        }

        // Bloque de firma
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
        PdfPTable tFirma = new PdfPTable(2);
        tFirma.setWidthPercentage(100);
        tFirma.setWidths(new float[]{1, 1});

        PdfPCell cFirmaEmpresa = new PdfPCell();
        cFirmaEmpresa.setBorder(Rectangle.NO_BORDER);
        cFirmaEmpresa.addElement(new Phrase("Entregado por:", fontNegrita));
        cFirmaEmpresa.addElement(new Phrase("\n\n\n", fontNormal));
        cFirmaEmpresa.addElement(new Phrase(DatabaseManager.getConfig("empresa_nombre"), fontNormal));
        tFirma.addCell(cFirmaEmpresa);

        PdfPCell cFirmaCliente = new PdfPCell();
        cFirmaCliente.setBorder(Rectangle.NO_BORDER);
        cFirmaCliente.addElement(new Phrase("Recibido conforme:", fontNegrita));
        cFirmaCliente.addElement(new Phrase("\n\n\n", fontNormal));
        cFirmaCliente.addElement(new Phrase("Firma y fecha: ___________________________", fontNormal));
        tFirma.addCell(cFirmaCliente);

        doc.add(tFirma);

        addPiePagina(doc, null);
        doc.close();
        return path;
    }

    public Path generarNomina(Nomina n, Empleado e) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Nominas")
            .resolve(safeFilename(n.getAnio() + "-" + String.format("%02d", n.getMes()) + "_" + e.getNombreCompleto()) + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, writer, "RECIBO DE NÓMINA");

        // Período
        doc.add(Chunk.NEWLINE);
        PdfPTable tPeriodo = new PdfPTable(2);
        tPeriodo.setWidthPercentage(100);
        addCeldaTabla(tPeriodo, "Período: " + n.getPeriodo(), fontNegrita, Color.WHITE, 1);
        addCeldaTabla(tPeriodo, "Empleado: " + e.getNombreCompleto(), fontNegrita, Color.WHITE, 1);
        addCeldaTabla(tPeriodo, "NIF: " + nvl(e.getNif()), fontNormal, Color.WHITE, 1);
        addCeldaTabla(tPeriodo, "Categoría: " + nvl(e.getCategoria()), fontNormal, Color.WHITE, 1);
        doc.add(tPeriodo);
        doc.add(Chunk.NEWLINE);

        // Percepciones
        PdfPTable tNomina = new PdfPTable(2);
        tNomina.setWidthPercentage(100);
        tNomina.setWidths(new float[]{3, 1.5f});

        addFilaSubtitulo(tNomina, "PERCEPCIONES", 2);
        addFilaNomina(tNomina, "Salario base", n.getSalarioBase());
        if (n.getComplementos() > 0) addFilaNomina(tNomina, "Complementos", n.getComplementos());
        if (n.getHorasExtraNormales() > 0) addFilaNomina(tNomina,
            "Horas extra normales (" + n.getHorasExtraNormales() + "h)",
            n.getHorasExtraNormales() * n.getPrecioHoraExtra());
        if (n.getHorasExtraFestivas() > 0) addFilaNomina(tNomina,
            "Horas extra festivas (" + n.getHorasExtraFestivas() + "h)",
            n.getHorasExtraFestivas() * n.getPrecioHoraFestiva());
        if (n.getPercepcionesNoSalariales() > 0)
            addFilaNomina(tNomina, "Percepciones no salariales", n.getPercepcionesNoSalariales());
        addFilaTotal(tNomina, "TOTAL DEVENGADO", n.getTotalBruto());

        addFilaSubtitulo(tNomina, "DEDUCCIONES", 2);
        addFilaNomina(tNomina, String.format("S.S. trabajador (%.2f%%)", NominaService.SS_TRABAJADOR_TOTAL), n.getSsTrabajador());
        addFilaNomina(tNomina, String.format("IRPF (%.2f%%)", n.getIrpfPorcentaje()), n.getIrpfImporte());
        addFilaTotal(tNomina, "TOTAL DEDUCCIONES", n.getTotalDeducciones());

        doc.add(tNomina);
        doc.add(Chunk.NEWLINE);

        // Líquido
        PdfPTable tLiquido = new PdfPTable(2);
        tLiquido.setWidthPercentage(100);
        tLiquido.setWidths(new float[]{3, 1.5f});
        PdfPCell cLabel = new PdfPCell(new Phrase("LÍQUIDO A PERCIBIR", new Font(fontNegrita.getBaseFont(), 12, Font.BOLD, Color.WHITE)));
        cLabel.setBackgroundColor(COLOR_MULBERRY);
        cLabel.setPadding(8);
        tLiquido.addCell(cLabel);
        PdfPCell cVal = new PdfPCell(new Phrase(String.format("%.2f €", n.getNeto()), new Font(fontNegrita.getBaseFont(), 12, Font.BOLD, Color.WHITE)));
        cVal.setBackgroundColor(COLOR_MULBERRY);
        cVal.setPadding(8);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tLiquido.addCell(cVal);
        doc.add(tLiquido);

        doc.add(Chunk.NEWLINE);
        addSeccion(doc, "IBAN", nvl(e.getIban()));

        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    private void addCabecera(Document doc, PdfWriter writer, String tipoDocumento) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1, 2});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        try (InputStream is = getClass().getResourceAsStream("/org/gipsybuho/img/logo.jpg")) {
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(120, 80);
                logoCell.addElement(logo);
            }
        } catch (Exception ignored) {}
        header.addCell(logoCell);

        // Datos empresa
        PdfPCell empresaCell = new PdfPCell();
        empresaCell.setBorder(Rectangle.NO_BORDER);
        empresaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        empresaCell.addElement(new Phrase("GRÁFICAS MULBERRY", fontTitulo));
        empresaCell.addElement(new Phrase(DatabaseManager.getConfig("empresa_direccion"), fontNormal));
        empresaCell.addElement(new Phrase(DatabaseManager.getConfig("empresa_cp") + " " + DatabaseManager.getConfig("empresa_ciudad"), fontNormal));
        empresaCell.addElement(new Phrase("Tel: " + DatabaseManager.getConfig("empresa_telefono"), fontNormal));
        empresaCell.addElement(new Phrase(DatabaseManager.getConfig("empresa_email"), fontNormal));
        empresaCell.addElement(new Phrase("NIF: " + DatabaseManager.getConfig("empresa_nif"), fontNormal));
        header.addCell(empresaCell);

        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // Línea decorativa
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        PdfPCell lineaCell = new PdfPCell(new Phrase(tipoDocumento, new Font(fontNegrita.getBaseFont(), 14, Font.BOLD, Color.WHITE)));
        lineaCell.setBackgroundColor(COLOR_MULBERRY);
        lineaCell.setPadding(6);
        lineaCell.setBorder(Rectangle.NO_BORDER);
        linea.addCell(lineaCell);
        doc.add(linea);
        doc.add(Chunk.NEWLINE);
    }

    private void addDatosDocumento(Document doc, String numero, String fecha, String fechaExtra, String estado) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addCeldaInfo(t, "Número:", numero);
        addCeldaInfo(t, "Fecha:", nvl(fecha));
        addCeldaInfo(t, "Validez/Vencimiento:", nvl(fechaExtra));
        addCeldaInfo(t, "Estado:", nvl(estado).toUpperCase());
        doc.add(t);
    }

    private void addDatosCliente(Document doc, Cliente c) throws Exception {
        doc.add(Chunk.NEWLINE);
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(50);
        PdfPCell titulo = new PdfPCell(new Phrase("CLIENTE", fontSubtitulo));
        titulo.setBackgroundColor(COLOR_GRIS_CLARO);
        titulo.setBorderColor(COLOR_GRIS_BORDE);
        titulo.setPadding(4);
        t.addCell(titulo);
        PdfPCell datos = new PdfPCell();
        datos.setBorderColor(COLOR_GRIS_BORDE);
        datos.setPadding(6);
        datos.addElement(new Phrase(nvl(c.getNombreCompleto()), fontNegrita));
        if (c.getNif() != null) datos.addElement(new Phrase("NIF/CIF: " + c.getNif(), fontNormal));
        if (c.getDireccion() != null) datos.addElement(new Phrase(c.getDireccion(), fontNormal));
        if (c.getCiudad() != null) datos.addElement(new Phrase(nvl(c.getCp()) + " " + c.getCiudad(), fontNormal));
        if (c.getTelefono() != null) datos.addElement(new Phrase("Tel: " + c.getTelefono(), fontNormal));
        if (c.getEmail() != null) datos.addElement(new Phrase(c.getEmail(), fontNormal));
        t.addCell(datos);
        doc.add(t);
    }

    private void addTablaLineas(Document doc, String[][] filas) throws Exception {
        PdfPTable t = new PdfPTable(6);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{4, 1.5f, 0.8f, 1.2f, 0.8f, 1.2f});

        String[] cabeceras = {"Descripción", "Técnica", "Cant.", "Precio ud.", "Dto.", "Total"};
        for (String cab : cabeceras) {
            PdfPCell c = new PdfPCell(new Phrase(cab, new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(COLOR_MULBERRY);
            c.setPadding(5);
            c.setBorderColor(COLOR_MULBERRY);
            t.addCell(c);
        }

        boolean par = false;
        for (String[] fila : filas) {
            Color bg = par ? COLOR_GRIS_CLARO : Color.WHITE;
            for (int i = 0; i < fila.length; i++) {
                PdfPCell c = new PdfPCell(new Phrase(nvl(fila[i]), fontNormal));
                c.setBackgroundColor(bg);
                c.setBorderColor(COLOR_GRIS_BORDE);
                c.setPadding(4);
                if (i >= 2) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(c);
            }
            par = !par;
        }
        doc.add(t);
    }

    private void addTotales(Document doc, double base, double ivaPct, double ivaImp, double total) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(40);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setWidths(new float[]{2, 1.5f});

        addFilaTotales(t, "Base imponible:", String.format("%.2f €", base), false);
        addFilaTotales(t, String.format("IVA (%.0f%%):", ivaPct), String.format("%.2f €", ivaImp), false);
        addFilaTotales(t, "TOTAL:", String.format("%.2f €", total), true);
        doc.add(t);
    }

    private void addSeccion(Document doc, String titulo, String contenido) throws Exception {
        doc.add(new Phrase(titulo + ": ", fontNegrita));
        doc.add(new Phrase(contenido + "\n", fontNormal));
    }

    private void addPiePagina(Document doc, PdfWriter writer) throws Exception {
        doc.add(Chunk.NEWLINE);
        PdfPTable pie = new PdfPTable(1);
        pie.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(
            "Gráficas Mulberry · " + DatabaseManager.getConfig("empresa_web") +
            " · " + DatabaseManager.getConfig("empresa_email") +
            " · Tel. " + DatabaseManager.getConfig("empresa_telefono"),
            fontPequeno));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(COLOR_MULBERRY);
        c.setPaddingTop(4);
        pie.addCell(c);
        doc.add(pie);
    }

    private void addCeldaTabla(PdfPTable t, String texto, Font font, Color bg, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setColspan(colspan);
        c.setBackgroundColor(bg);
        c.setPadding(4);
        t.addCell(c);
    }

    private void addCeldaInfo(PdfPTable t, String label, String valor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, fontNegrita));
        cLabel.setBorderColor(COLOR_GRIS_BORDE);
        cLabel.setPadding(3);
        t.addCell(cLabel);
        PdfPCell cVal = new PdfPCell(new Phrase(valor, fontNormal));
        cVal.setBorderColor(COLOR_GRIS_BORDE);
        cVal.setPadding(3);
        t.addCell(cVal);
    }

    private void addFilaNomina(PdfPTable t, String concepto, double importe) {
        PdfPCell c1 = new PdfPCell(new Phrase(concepto, fontNormal));
        c1.setPadding(3); c1.setBorderColor(COLOR_GRIS_BORDE);
        PdfPCell c2 = new PdfPCell(new Phrase(String.format("%.2f €", importe), fontNormal));
        c2.setPadding(3); c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setBorderColor(COLOR_GRIS_BORDE);
        t.addCell(c1); t.addCell(c2);
    }

    private void addFilaTotal(PdfPTable t, String concepto, double importe) {
        PdfPCell c1 = new PdfPCell(new Phrase(concepto, fontNegrita));
        c1.setPadding(4); c1.setBackgroundColor(COLOR_GRIS_CLARO); c1.setBorderColor(COLOR_GRIS_BORDE);
        PdfPCell c2 = new PdfPCell(new Phrase(String.format("%.2f €", importe), fontNegrita));
        c2.setPadding(4); c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setBackgroundColor(COLOR_GRIS_CLARO); c2.setBorderColor(COLOR_GRIS_BORDE);
        t.addCell(c1); t.addCell(c2);
    }

    private void addFilaSubtitulo(PdfPTable t, String texto, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(COLOR_MULBERRY);
        c.setPadding(4);
        c.setColspan(colspan);
        t.addCell(c);
    }

    private void addFilaTotales(PdfPTable t, String label, String valor, boolean destacado) {
        Font f = destacado ? new Font(fontNegrita.getBaseFont(), 10, Font.BOLD, Color.WHITE) : fontNormal;
        Color bg = destacado ? COLOR_MULBERRY : Color.WHITE;
        PdfPCell c1 = new PdfPCell(new Phrase(label, f));
        c1.setBackgroundColor(bg); c1.setPadding(4); c1.setBorderColor(COLOR_GRIS_BORDE);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, f));
        c2.setBackgroundColor(bg); c2.setPadding(4); c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setBorderColor(COLOR_GRIS_BORDE);
        t.addCell(c1); t.addCell(c2);
    }

    // ── Estadísticas ──────────────────────────────────────────────────────────

    public Path generarEstadisticas(int anio) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Estadisticas")
            .resolve(safeFilename("Estadisticas_" + anio) + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "INFORME DE ESTADÍSTICAS " + anio);

        double totIng    = EstadisticasService.totalIngresosAnio(anio);
        double totGMat   = EstadisticasService.totalGastosMaterialAnio(anio);
        double totNom    = EstadisticasService.totalNominasAnio(anio);
        double beneficio = totIng - totGMat - totNom;

        addTituloSeccion(doc, "FINANCIERO");
        addTablaKpisFinanciero(doc, anio, totIng, totGMat, totNom, beneficio);
        doc.add(Chunk.NEWLINE);
        addTablaMensual(doc, anio,
            EstadisticasService.ingresosPorMes(anio),
            EstadisticasService.gastosMaterialPorMes(anio),
            EstadisticasService.gastosNominasPorMes(anio));
        doc.add(Chunk.NEWLINE);
        addTablaEstados(doc,
            EstadisticasService.facturasPorEstado(),
            EstadisticasService.presupuestosPorEstado());

        doc.newPage();
        addTituloSeccion(doc, "MATERIALES");
        addTablaSimpleEst(doc, "Top 10 materiales más consumidos",
            EstadisticasService.materialesMasUsados(10), "Material", "Uds. consumidas");
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Materiales por categoría",
            EstadisticasService.materialesPorCategoria(), "Categoría", "Artículos");
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Stock actual (top 12)",
            EstadisticasService.stockMateriales(12), "Material", "Stock");

        doc.newPage();
        addTituloSeccion(doc, "CLIENTES");
        addTablaKpisClientes(doc, anio,
            EstadisticasService.totalClientesAcumulado(),
            EstadisticasService.totalClientesAnio(anio));
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Top 8 clientes por facturación",
            EstadisticasService.topClientesPorFacturacion(8), "Cliente", "Facturación €");
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Clientes por tipo",
            EstadisticasService.clientesPorTipo(), "Tipo", "Cantidad");

        doc.newPage();
        addTituloSeccion(doc, "PRODUCCIÓN");
        addTablaSimpleEst(doc, "Ingresos por técnica",
            EstadisticasService.tecnicasPorIngresos(), "Técnica", "Ingresos €");
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Volumen por técnica (líneas)",
            EstadisticasService.tecnicasPorVolumen(), "Técnica", "Líneas");
        doc.add(Chunk.NEWLINE);
        addTablaSimpleEst(doc, "Pedidos por estado",
            EstadisticasService.pedidosPorEstado(), "Estado", "Pedidos");

        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    private void addTituloSeccion(Document doc, String texto) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(texto,
            new Font(fontNegrita.getBaseFont(), 13, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(COLOR_MULBERRY);
        c.setPadding(7);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        doc.add(t);
        doc.add(Chunk.NEWLINE);
    }

    private void addTablaKpisFinanciero(Document doc, int anio,
            double ingresos, double gastosMat, double gastosNom, double beneficio) throws Exception {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);

        for (String h : new String[]{"Ingresos " + anio, "Gastos materiales", "Gastos nóminas", "Beneficio estimado"}) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(COLOR_MULBERRY);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
        Color cVerde   = new Color(39, 174, 96);
        Color cRojo    = new Color(231, 76, 60);
        Color cNaranja = new Color(243, 156, 18);
        double[] vals  = {ingresos, gastosMat, gastosNom, beneficio};
        Color[]  cols  = {cVerde, cRojo, cNaranja, beneficio >= 0 ? cVerde : cRojo};
        for (int i = 0; i < vals.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(String.format("%.2f €", vals[i]),
                new Font(fontNegrita.getBaseFont(), 13, Font.BOLD, cols[i])));
            c.setPadding(10);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(COLOR_GRIS_BORDE);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void addTablaMensual(Document doc, int anio,
            Map<String, Double> ingresos, Map<String, Double> gastosMat,
            Map<String, Double> gastosNom) throws Exception {
        doc.add(new Phrase("Ingresos vs Gastos por mes (" + anio + ")\n", fontSubtitulo));
        doc.add(Chunk.NEWLINE);
        PdfPTable t = new PdfPTable(5);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.2f, 2f, 2f, 2f, 2f});
        for (String h : new String[]{"Mes", "Ingresos €", "Gastos mat. €", "Nóminas €", "Balance €"}) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(COLOR_MULBERRY);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
        boolean par = false;
        for (String mes : ingresos.keySet()) {
            double ing = ingresos.getOrDefault(mes, 0.0);
            double gm  = gastosMat.getOrDefault(mes, 0.0);
            double gn  = gastosNom.getOrDefault(mes, 0.0);
            double bal = ing - gm - gn;
            Color bg   = par ? COLOR_GRIS_CLARO : Color.WHITE;
            Color cBal = bal >= 0 ? new Color(39, 174, 96) : new Color(231, 76, 60);
            addCeldaEst(t, mes, bg, fontNormal, Element.ALIGN_LEFT);
            addCeldaEst(t, String.format("%.2f", ing), bg, fontNormal, Element.ALIGN_RIGHT);
            addCeldaEst(t, String.format("%.2f", gm),  bg, fontNormal, Element.ALIGN_RIGHT);
            addCeldaEst(t, String.format("%.2f", gn),  bg, fontNormal, Element.ALIGN_RIGHT);
            addCeldaEst(t, String.format("%.2f", bal), bg,
                new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, cBal), Element.ALIGN_RIGHT);
            par = !par;
        }
        doc.add(t);
    }

    private void addTablaEstados(Document doc,
            Map<String, Double> estFacturas, Map<String, Double> estPresup) throws Exception {
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        PdfPCell cF = new PdfPCell();
        cF.setBorder(Rectangle.NO_BORDER);
        cF.setPadding(4);
        cF.addElement(subtablaEstados("Estado de facturas", estFacturas));
        outer.addCell(cF);
        PdfPCell cP = new PdfPCell();
        cP.setBorder(Rectangle.NO_BORDER);
        cP.setPadding(4);
        cP.addElement(subtablaEstados("Estado de presupuestos", estPresup));
        outer.addCell(cP);
        doc.add(outer);
    }

    private PdfPTable subtablaEstados(String titulo, Map<String, Double> datos) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        PdfPCell hdr = new PdfPCell(new Phrase(titulo,
            new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
        hdr.setBackgroundColor(new Color(130, 80, 115));
        hdr.setColspan(2);
        hdr.setPadding(5);
        t.addCell(hdr);
        boolean par = false;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            Color bg = par ? COLOR_GRIS_CLARO : Color.WHITE;
            PdfPCell c1 = new PdfPCell(new Phrase(e.getKey(), fontNormal));
            c1.setBackgroundColor(bg); c1.setPadding(3); c1.setBorderColor(COLOR_GRIS_BORDE);
            PdfPCell c2 = new PdfPCell(new Phrase(String.format("%.0f", e.getValue()), fontNormal));
            c2.setBackgroundColor(bg); c2.setPadding(3);
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setBorderColor(COLOR_GRIS_BORDE);
            t.addCell(c1); t.addCell(c2);
            par = !par;
        }
        return t;
    }

    private void addTablaSimpleEst(Document doc, String titulo,
            Map<String, Double> datos, String col1, String col2) throws Exception {
        doc.add(new Phrase(titulo + "\n", fontSubtitulo));
        doc.add(Chunk.NEWLINE);
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{3.5f, 1.5f});
        for (String h : new String[]{col1, col2}) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(COLOR_MULBERRY);
            c.setPadding(5);
            t.addCell(c);
        }
        boolean par = false;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            Color bg = par ? COLOR_GRIS_CLARO : Color.WHITE;
            PdfPCell c1 = new PdfPCell(new Phrase(e.getKey(), fontNormal));
            c1.setBackgroundColor(bg); c1.setPadding(4); c1.setBorderColor(COLOR_GRIS_BORDE);
            PdfPCell c2 = new PdfPCell(new Phrase(fmtEstNum(e.getValue()), fontNormal));
            c2.setBackgroundColor(bg); c2.setPadding(4);
            c2.setHorizontalAlignment(Element.ALIGN_RIGHT); c2.setBorderColor(COLOR_GRIS_BORDE);
            t.addCell(c1); t.addCell(c2);
            par = !par;
        }
        doc.add(t);
    }

    private void addTablaKpisClientes(Document doc, int anio, int total, int nuevos) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(60);
        for (String h : new String[]{"Total clientes acumulado", "Nuevos en " + anio}) {
            PdfPCell c = new PdfPCell(new Phrase(h,
                new Font(fontNegrita.getBaseFont(), 9, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(COLOR_MULBERRY);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
        for (int v : new int[]{total, nuevos}) {
            PdfPCell c = new PdfPCell(new Phrase(String.valueOf(v),
                new Font(fontNegrita.getBaseFont(), 16, Font.BOLD, COLOR_MULBERRY)));
            c.setPadding(10);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setBorderColor(COLOR_GRIS_BORDE);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void addCeldaEst(PdfPTable t, String texto, Color bg, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setBorderColor(COLOR_GRIS_BORDE);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private String fmtEstNum(double v) {
        return (v == Math.floor(v) && !Double.isInfinite(v))
            ? String.format("%.0f", v)
            : String.format("%.2f", v);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── Pedido de trabajo ──────────────────────────────────────────────────────

    public Path generarPedido(Pedido p, Cliente c) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Pedidos")
            .resolve(safeFilename(p.getNumero()) + ".pdf");
        path.getParent().toFile().mkdirs();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "PEDIDO DE TRABAJO");

        PdfPTable tDatos = new PdfPTable(4);
        tDatos.setWidthPercentage(100);
        tDatos.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
        addCeldaInfo(tDatos, "Número:", nvl(p.getNumero()));
        addCeldaInfo(tDatos, "Estado:", nvl(p.getEstadoDisplay()));
        addCeldaInfo(tDatos, "Fecha entrada:", p.getFecha() != null ? p.getFecha().toString() : "");
        addCeldaInfo(tDatos, "Entrega prevista:", p.getFechaEntregaPrevista() != null ? p.getFechaEntregaPrevista().toString() : "");
        doc.add(tDatos);
        addDatosCliente(doc, c);
        doc.add(Chunk.NEWLINE);

        if (p.getDescripcion() != null && !p.getDescripcion().isBlank())
            addSeccion(doc, "Descripción del trabajo", p.getDescripcion());

        doc.add(Chunk.NEWLINE);
        PdfPTable tImportes = new PdfPTable(2);
        tImportes.setWidthPercentage(40);
        tImportes.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tImportes.setWidths(new float[]{2f, 1.5f});
        addFilaTotales(tImportes, "Importe total:", String.format("%.2f €", p.getImporteTotal()), false);
        addFilaTotales(tImportes, "Pagado:", String.format("%.2f €", p.getImportePagado()), false);
        addFilaTotales(tImportes, "Pendiente:", String.format("%.2f €", p.getImportePendiente()), true);
        doc.add(tImportes);

        if (p.getNotas() != null && !p.getNotas().isBlank()) {
            doc.add(Chunk.NEWLINE);
            addSeccion(doc, "Notas", p.getNotas());
        }
        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    // ── Ficha de Cliente ──────────────────────────────────────────────────────

    public Path generarFichaCliente(Cliente c) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Clientes")
            .resolve(safeFilename(nvl(c.getNombreCompleto())) + ".pdf");
        path.getParent().toFile().mkdirs();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "FICHA DE CLIENTE");

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.5f, 3f});
        addCeldaInfo(t, "Nombre:", nvl(c.getNombre()));
        addCeldaInfo(t, "Apellidos:", nvl(c.getApellidos()));
        addCeldaInfo(t, "Tipo:", nvl(c.getTipo()));
        addCeldaInfo(t, "NIF/CIF:", nvl(c.getNif()));
        addCeldaInfo(t, "Teléfono:", nvl(c.getTelefono()));
        addCeldaInfo(t, "Email:", nvl(c.getEmail()));
        addCeldaInfo(t, "Dirección:", nvl(c.getDireccion()));
        addCeldaInfo(t, "C.P. / Ciudad:", nvl(c.getCp()) + " " + nvl(c.getCiudad()));
        doc.add(t);
        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    // ── Ficha de Tarifa ───────────────────────────────────────────────────────

    public Path generarFichaTarifa(Tarifa tarifa) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Tarifas")
            .resolve(safeFilename(nvl(tarifa.getTecnica()) + "_" + nvl(tarifa.getNombre())) + ".pdf");
        path.getParent().toFile().mkdirs();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "FICHA DE TARIFA");

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 3f});
        addCeldaInfo(tabla, "Técnica:", nvl(tarifa.getTecnica()));
        addCeldaInfo(tabla, "Nombre:", nvl(tarifa.getNombre()));
        addCeldaInfo(tabla, "Descripción:", nvl(tarifa.getDescripcion()));
        addCeldaInfo(tabla, "Precio por ud.:", String.format("%.2f €", tarifa.getPrecioUnit()));
        addCeldaInfo(tabla, "Setup (€):", String.format("%.2f €", tarifa.getPrecioSetup()));
        addCeldaInfo(tabla, "Mínimo uds.:", String.valueOf(tarifa.getMinimoUnidades()));
        addCeldaInfo(tabla, "Activa:", tarifa.isActiva() ? "Sí" : "No");
        doc.add(tabla);
        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    // ── Ficha de Material ─────────────────────────────────────────────────────

    public Path generarFichaMaterial(Material m) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Materiales")
            .resolve(safeFilename(nvl(m.getNombre())) + ".pdf");
        path.getParent().toFile().mkdirs();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "FICHA DE MATERIAL");

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 3f});
        addCeldaInfo(tabla, "Nombre:", nvl(m.getNombre()));
        addCeldaInfo(tabla, "Referencia:", nvl(m.getReferencia()));
        addCeldaInfo(tabla, "Categoría:", nvl(m.getCategoria()));
        addCeldaInfo(tabla, "Proveedor:", nvl(m.getProveedor()));
        addCeldaInfo(tabla, "Unidad:", nvl(m.getUnidad()));
        addCeldaInfo(tabla, "Precio/ud.:", String.format("%.2f €", m.getPrecioUnidad()));
        addCeldaInfo(tabla, "Stock actual:", String.format("%.2f %s", m.getStockActual(), nvl(m.getUnidad())));
        addCeldaInfo(tabla, "Stock mínimo:", String.format("%.2f %s", m.getStockMinimo(), nvl(m.getUnidad())));
        addCeldaInfo(tabla, "Alerta:", m.isBajoStock() ? "BAJO MÍNIMO" : "OK");
        doc.add(tabla);
        addPiePagina(doc, writer);
        doc.close();
        return path;
    }

    // ── Ficha de Empleado ─────────────────────────────────────────────────────

    public Path generarFichaEmpleado(Empleado e) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Empleados")
            .resolve(safeFilename(e.getNombreCompleto()) + ".pdf");
        path.getParent().toFile().mkdirs();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();
        addCabecera(doc, writer, "FICHA DE EMPLEADO");

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.5f, 3f});
        addCeldaInfo(tabla, "Nombre:", nvl(e.getNombre()));
        addCeldaInfo(tabla, "Apellidos:", nvl(e.getApellidos()));
        addCeldaInfo(tabla, "NIF:", nvl(e.getNif()));
        addCeldaInfo(tabla, "Categoría:", nvl(e.getCategoria()));
        addCeldaInfo(tabla, "Fecha alta:", nvl(e.getFechaAlta()));
        addCeldaInfo(tabla, "Estado:", e.isActivo() ? "ACTIVO"
            : "BAJA" + (e.getFechaBaja() != null ? " (" + e.getFechaBaja() + ")" : ""));
        addCeldaInfo(tabla, "Salario base:", String.format("%.2f €", e.getSalarioBase()));
        addCeldaInfo(tabla, "IRPF:", String.format("%.1f%%", e.getIrpf()));
        addCeldaInfo(tabla, "IBAN:", nvl(e.getIban()));
        doc.add(tabla);
        addPiePagina(doc, writer);
        doc.close();
        return path;
    }
}
