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
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generar PDFs de confirmación de inscripción
 * a cursos en línea usando iText 7.
 */
@Slf4j
@Service
public class PdfService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarPdf(Curso curso) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        // ── Encabezado ──────────────────────────────────────────
        document.add(new Paragraph("CONFIRMACIÓN DE INSCRIPCIÓN")
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(10));

        document.add(new Paragraph("Plataforma de Gestión de Cursos en Línea")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(20));

        // ── Datos del curso ──────────────────────────────────────
        document.add(new Paragraph("DATOS DEL CURSO")
                .setFontSize(12)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setMarginBottom(5));

        Table tablaCurso = new Table(
                UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        agregarFila(tablaCurso, "Código de Curso:",
                curso.getCodigoCurso(), true);
        agregarFila(tablaCurso, "Temática:",
                curso.getTematica(), false);
        agregarFila(tablaCurso, "Descripción:",
                curso.getDescripcion(), true);
        agregarFila(tablaCurso, "Duración (horas):",
                curso.getDuracionHoras() != null
                        ? curso.getDuracionHoras() + " horas"
                        : "No especificada", false);
        agregarFila(tablaCurso, "Fecha de Inicio:",
                curso.getFechaInicio().format(FORMATTER), true);
        agregarFila(tablaCurso, "Estado:",
                curso.getEstado().name(), false);

        document.add(tablaCurso);

        // ── Datos del instructor ─────────────────────────────────
        document.add(new Paragraph("DATOS DEL INSTRUCTOR")
                .setFontSize(12)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setMarginBottom(5));

        Table tablaInstructor = new Table(
                UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        agregarFila(tablaInstructor, "Instructor:",
                curso.getInstructor(), true);

        document.add(tablaInstructor);

        // ── Datos del estudiante ─────────────────────────────────
        document.add(new Paragraph("DATOS DEL ESTUDIANTE")
                .setFontSize(12)
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5)
                .setMarginBottom(5));

        Table tablaEstudiante = new Table(
                UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(30);

        agregarFila(tablaEstudiante, "Estudiante inscrito:",
                curso.getEstudiante(), true);

        document.add(tablaEstudiante);

        // ── Firmas ───────────────────────────────────────────────
        Table tablaFirmas = new Table(
                UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(40);

        tablaFirmas.addCell(new Cell()
                .add(new Paragraph("\n\n___________________________")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Firma Instructor")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10))
                .add(new Paragraph(curso.getInstructor())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER));

        tablaFirmas.addCell(new Cell()
                .add(new Paragraph("\n\n___________________________")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Firma Estudiante")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10))
                .add(new Paragraph(curso.getEstudiante())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(9)
                        .setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER));

        document.add(tablaFirmas);

        // ── Pie de página ────────────────────────────────────────
        document.add(new Paragraph("Documento generado el: " +
                java.time.LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30));

        document.close();
        log.info("PDF de confirmación generado para curso: {}",
                curso.getCodigoCurso());
        return baos.toByteArray();
    }

    private void agregarFila(Table tabla, String etiqueta,
                              String valor, boolean sombreado) {
        tabla.addCell(new Cell()
                .add(new Paragraph(etiqueta).setBold().setFontSize(10))
                .setBackgroundColor(sombreado
                        ? ColorConstants.LIGHT_GRAY
                        : ColorConstants.WHITE)
                .setPadding(6));
        tabla.addCell(new Cell()
                .add(new Paragraph(valor).setFontSize(10))
                .setBackgroundColor(sombreado
                        ? ColorConstants.LIGHT_GRAY
                        : ColorConstants.WHITE)
                .setPadding(6));
    }
}
