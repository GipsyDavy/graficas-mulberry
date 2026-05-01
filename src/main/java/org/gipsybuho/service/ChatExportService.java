package org.gipsybuho.service;

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
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatExportService {

    public record MensajeChat(String rol, String texto) {}

    private static final Color MULBERRY = new Color(107, 45,  94);
    private static final Color LILA     = new Color(240, 230, 239);
    private static final Color IA_TEXTO = new Color(26,  26,  46);
    private static final Color GRIS     = new Color(120, 120, 120);
    private static final Color VIOLETA  = new Color(93,  74,  122);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void exportarPDF(File archivo, List<MensajeChat> mensajes) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();

        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
        BaseFont bfObl  = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252, BaseFont.EMBEDDED);

        Font fTitulo  = new Font(bfBold, 17, Font.BOLD,   MULBERRY);
        Font fEmpresa = new Font(bf,     10, Font.NORMAL, MULBERRY);
        Font fFecha   = new Font(bf,      9, Font.NORMAL, GRIS);
        Font fLblUser = new Font(bfBold,  8, Font.BOLD,   LILA);
        Font fTxtUser = new Font(bf,     10, Font.NORMAL, Color.WHITE);
        Font fLblIA   = new Font(bfBold,  8, Font.BOLD,   VIOLETA);
        Font fTxtIA   = new Font(bf,     10, Font.NORMAL, IA_TEXTO);
        Font fSistema = new Font(bfObl,   9, Font.ITALIC, GRIS);

        Paragraph parTitulo = new Paragraph("Chat con Asistente IA", fTitulo);
        parTitulo.setAlignment(Element.ALIGN_CENTER);
        parTitulo.setSpacingAfter(2);
        doc.add(parTitulo);

        Paragraph parEmpresa = new Paragraph("Gráficas Mulberry", fEmpresa);
        parEmpresa.setAlignment(Element.ALIGN_CENTER);
        parEmpresa.setSpacingAfter(4);
        doc.add(parEmpresa);

        Paragraph parFecha = new Paragraph("Exportado el " + LocalDateTime.now().format(FMT), fFecha);
        parFecha.setAlignment(Element.ALIGN_CENTER);
        parFecha.setSpacingAfter(22);
        doc.add(parFecha);

        for (MensajeChat msg : mensajes) {
            switch (msg.rol()) {
                case "usuario" -> {
                    PdfPTable tabla = new PdfPTable(1);
                    tabla.setWidthPercentage(70);
                    tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    tabla.setSpacingBefore(8);
                    tabla.setSpacingAfter(4);
                    tabla.addCell(celda(new Phrase("Tú", fLblUser),   MULBERRY, 10, 7, 10, 2));
                    tabla.addCell(celda(new Phrase(msg.texto(), fTxtUser), MULBERRY, 10, 1, 10, 10));
                    doc.add(tabla);
                }
                case "ia" -> {
                    PdfPTable tabla = new PdfPTable(1);
                    tabla.setWidthPercentage(82);
                    tabla.setHorizontalAlignment(Element.ALIGN_LEFT);
                    tabla.setSpacingBefore(8);
                    tabla.setSpacingAfter(4);
                    tabla.addCell(celda(new Phrase("Asistente IA", fLblIA), LILA, 10, 7, 10, 2));
                    tabla.addCell(celda(new Phrase(msg.texto(), fTxtIA),    LILA, 10, 1, 10, 10));
                    doc.add(tabla);
                }
                case "sistema" -> {
                    Paragraph p = new Paragraph(msg.texto(), fSistema);
                    p.setAlignment(Element.ALIGN_CENTER);
                    p.setSpacingBefore(8);
                    p.setSpacingAfter(8);
                    doc.add(p);
                }
            }
        }

        doc.close();
    }

    private static PdfPCell celda(Phrase contenido, Color fondo,
                                  float pL, float pT, float pR, float pB) {
        PdfPCell c = new PdfPCell(contenido);
        c.setBorder(0);
        c.setBackgroundColor(fondo);
        c.setPaddingLeft(pL);
        c.setPaddingTop(pT);
        c.setPaddingRight(pR);
        c.setPaddingBottom(pB);
        return c;
    }

    public static void exportarWord(File archivo, List<MensajeChat> mensajes) throws Exception {
        XWPFDocument doc = new XWPFDocument();

        XWPFParagraph parTitulo = doc.createParagraph();
        parTitulo.setAlignment(ParagraphAlignment.CENTER);
        parTitulo.setSpacingAfter(80);
        XWPFRun rTitulo = parTitulo.createRun();
        rTitulo.setText("Chat con Asistente IA — Gráficas Mulberry");
        rTitulo.setBold(true);
        rTitulo.setFontSize(16);
        rTitulo.setColor("6B2D5E");

        XWPFParagraph parFecha = doc.createParagraph();
        parFecha.setAlignment(ParagraphAlignment.CENTER);
        parFecha.setSpacingAfter(280);
        XWPFRun rFecha = parFecha.createRun();
        rFecha.setText("Exportado el " + LocalDateTime.now().format(FMT));
        rFecha.setFontSize(9);
        rFecha.setColor("999999");

        for (MensajeChat msg : mensajes) {
            switch (msg.rol()) {
                case "usuario" -> {
                    XWPFParagraph par = doc.createParagraph();
                    par.setAlignment(ParagraphAlignment.RIGHT);
                    par.setSpacingBefore(120);
                    par.setSpacingAfter(60);
                    fondoParrafo(par, "6B2D5E");

                    XWPFRun rLabel = par.createRun();
                    rLabel.setText("Tú:  ");
                    rLabel.setBold(true);
                    rLabel.setFontSize(8);
                    rLabel.setColor("F0E6EF");

                    XWPFRun rMsg = par.createRun();
                    rMsg.setText(msg.texto());
                    rMsg.setFontSize(11);
                    rMsg.setColor("FFFFFF");
                }
                case "ia" -> {
                    XWPFParagraph par = doc.createParagraph();
                    par.setAlignment(ParagraphAlignment.LEFT);
                    par.setSpacingBefore(120);
                    par.setSpacingAfter(60);
                    fondoParrafo(par, "F0E6EF");

                    XWPFRun rLabel = par.createRun();
                    rLabel.setText("Asistente IA:  ");
                    rLabel.setBold(true);
                    rLabel.setFontSize(8);
                    rLabel.setColor("5D4A7A");

                    XWPFRun rMsg = par.createRun();
                    rMsg.setText(msg.texto());
                    rMsg.setFontSize(11);
                    rMsg.setColor("1A1A2E");
                }
                case "sistema" -> {
                    XWPFParagraph par = doc.createParagraph();
                    par.setAlignment(ParagraphAlignment.CENTER);
                    par.setSpacingBefore(80);
                    par.setSpacingAfter(80);
                    XWPFRun r = par.createRun();
                    r.setText(msg.texto());
                    r.setItalic(true);
                    r.setFontSize(9);
                    r.setColor("888888");
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            doc.write(fos);
        }
        doc.close();
    }

    private static void fondoParrafo(XWPFParagraph par, String colorHex) {
        CTPPr ppr = par.getCTP().isSetPPr() ? par.getCTP().getPPr() : par.getCTP().addNewPPr();
        CTShd shd = ppr.isSetShd() ? ppr.getShd() : ppr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(colorHex);
    }
}
