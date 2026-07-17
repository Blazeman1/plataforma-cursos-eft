package com.duocuc.cursos.service;

import com.duocuc.cursos.model.Curso;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar PDFs de cursos en línea usando iText 7.
 */
@Slf4j
@Service
public class PdfService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Genera el PDF de una curso en línea.
     *
     * @param guia entidad con los datos de la guía
     * @return bytes del PDF generado
     */
    public byte[] generarPdf(Curso guia) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        // ── Encabezado ──────────────────────────────────────────────────────────
        Paragraph titulo = new Paragraph("GUÍA DE DESPACHO")
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(10);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Sistema de Gestión de Pedidos y Generación de Cursos en Línea")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(20);
        document.add(subtitulo);

        // ── Datos principales ────────────────────────────────────────────────────
        Table tablaInfo = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        agregarFila(tablaInfo, "N° de Guía:", guia.getNumeroGuia(), true);
        agregarFila(tablaInfo, "Fecha de Despacho:", guia.getFechaDespacho().format(FORMATTER), false);
        agregarFila(tablaInfo, "Transportista:", guia.getTransportista(), true);
        agregarFila(tablaInfo, "Estado:", guia.getEstado().name(), false);

        document.add(tablaInfo);

        // ── Datos de destino ─────────────────────────────────────────────────────
        document.add(new Paragraph("DATOS DE DESTINO")
                .setFontSize(12)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setMarginBottom(5));

        Table tablaDestino = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        agregarFila(tablaDestino, "Destinatario:", guia.getDestinatario(), true);
        agregarFila(tablaDestino, "Dirección:", guia.getDireccionDestino(), false);

        document.add(tablaDestino);

        // ── Descripción de la carga ───────────────────────────────────────────────
        document.add(new Paragraph("DESCRIPCIÓN DE LA CARGA")
                .setFontSize(12)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setMarginBottom(5));

        Table tablaCarga = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(30);

        agregarFila(tablaCarga, "Descripción:", guia.getDescripcionCarga(), true);
        agregarFila(tablaCarga, "Peso (kg):",
                guia.getPesoKg() != null ? guia.getPesoKg().toString() : "No especificado", false);

        document.add(tablaCarga);

        // ── Firmas ───────────────────────────────────────────────────────────────
        Table tablaFirmas = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(40);

        Cell firmaTransportista = new Cell()
                .add(new Paragraph("\n\n___________________________").setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Firma Transportista").setTextAlignment(TextAlignment.CENTER).setFontSize(10))
                .add(new Paragraph(guia.getTransportista()).setTextAlignment(TextAlignment.CENTER).setFontSize(9).setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER);

        Cell firmaDestinatario = new Cell()
                .add(new Paragraph("\n\n___________________________").setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Firma Destinatario").setTextAlignment(TextAlignment.CENTER).setFontSize(10))
                .add(new Paragraph(guia.getDestinatario()).setTextAlignment(TextAlignment.CENTER).setFontSize(9).setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER);

        tablaFirmas.addCell(firmaTransportista);
        tablaFirmas.addCell(firmaDestinatario);
        document.add(tablaFirmas);

        // ── Pie de página ─────────────────────────────────────────────────────────
        document.add(new Paragraph("Generado el: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30));

        document.close();
        log.info("PDF generado para guía: {}", guia.getNumeroGuia());
        return baos.toByteArray();
    }

    private void agregarFila(Table tabla, String etiqueta, String valor, boolean sombreado) {
        tabla.addCell(new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(10))
                .setBackgroundColor(sombreado ? ColorConstants.LIGHT_GRAY : ColorConstants.WHITE)
                .setPadding(6));
        tabla.addCell(new Cell()
                .add(new Paragraph(valor).setFontSize(10))
                .setBackgroundColor(sombreado ? ColorConstants.LIGHT_GRAY : ColorConstants.WHITE)
                .setPadding(6));
    }
}
