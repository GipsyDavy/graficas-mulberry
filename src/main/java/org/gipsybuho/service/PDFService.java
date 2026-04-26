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

    public Path generarPresupuesto(Presupuesto p, Cliente c) throws Exception {
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Presupuestos")
            .resolve(p.getNumero() + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, writer, "PRESUPUESTO");
        addDatosDocumento(doc, p.getNumero(), p.getFecha(), p.getFechaValidez(), p.getEstado());
        addDatosCliente(doc, c);

        doc.add(Chunk.NEWLINE);
        addTablaLineas(doc,
            p.getLineas().stream().map(l -> new String[]{
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
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Facturas")
            .resolve(f.getNumero() + ".pdf");
        path.getParent().toFile().mkdirs();

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(path.toFile()));
        doc.open();

        addCabecera(doc, writer, "FACTURA");
        addDatosDocumento(doc, f.getNumero(), f.getFecha(), f.getFechaVencimiento(), f.getEstado());
        addDatosCliente(doc, c);

        doc.add(Chunk.NEWLINE);
        addTablaLineas(doc,
            f.getLineas().stream().map(l -> new String[]{
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
        initFonts();
        Path path = getDocumentosPath().resolve("Mulberry").resolve("Albaranes")
            .resolve(a.getNumero() + ".pdf");
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
        for (var linea : a.getLineas()) {
            Color bg = par ? COLOR_GRIS_CLARO : Color.WHITE;
            PdfPCell cDesc = new PdfPCell(new Phrase(nvl(linea.getDescripcion()), fontNormal));
            cDesc.setBackgroundColor(bg); cDesc.setBorderColor(COLOR_GRIS_BORDE); cDesc.setPadding(4);
            PdfPCell cCant = new PdfPCell(new Phrase(String.valueOf(linea.getCantidad()), fontNormal));
            cCant.setBackgroundColor(bg); cCant.setBorderColor(COLOR_GRIS_BORDE); cCant.setPadding(4);
            cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell cUnid = new PdfPCell(new Phrase(nvl(linea.getUnidad()), fontNormal));
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
            .resolve(n.getAnio() + "-" + String.format("%02d", n.getMes()) + "_" + e.getNombreCompleto().replace(" ", "_") + ".pdf");
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

    private String nvl(String s) { return s != null ? s : ""; }
}
