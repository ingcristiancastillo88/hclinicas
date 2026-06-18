package ec.salud.citas.hclinicas.service.impl;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import ec.salud.citas.hclinicas.dto.request.MedicamentoRequest;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.service.QrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Servicio que genera el PDF de la receta médica.
 * El diseño replica el formato físico del consultorio de la Dra. Alexandra León:
 * - Cabecera con logo, nombre y especialidad
 * - Sección Rp: con lista de medicamentos
 * - Sección de indicaciones generales
 * - Firma del médico + código QR de verificación
 * - Pie de página con datos de contacto del consultorio
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaPdfServiceImpl {

    private final ConsultaRepository consultaRepo;
    private final QrService          qrService;

    // Colores institucionales (rosa del consultorio)
    private static final DeviceRgb ROSA_PRIMARIO   = new DeviceRgb(233, 30, 140);  // #e91e8c
    private static final DeviceRgb ROSA_CLARO      = new DeviceRgb(252, 228, 236); // #fce4ec
    private static final DeviceRgb AZUL_OSCURO     = new DeviceRgb(10,  35,  66);  // #0a2342
    private static final DeviceRgb GRIS_TEXTO      = new DeviceRgb(100, 116, 139); // #64748b
    private static final DeviceRgb GRIS_BORDE      = new DeviceRgb(226, 232, 240); // #e2e8f0
    private static final DeviceRgb FONDO_CLARO     = new DeviceRgb(253, 242, 248); // #fdf2f8

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATETIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${app.clinica.especialista:Dra. Alexandra Leon}")
    private String nombreEspecialista;

    @Value("${app.clinica.especialidad:Medico Especialista en Ginecologia y Obstetricia}")
    private String especialidad;

    @Value("${app.clinica.direccion:Hospital San Juan - Riobamba, Ecuador}")
    private String direccion;

    @Value("${app.clinica.telefono:}")
    private String telefono;

    @Value("${app.qr.url-base:http://localhost:4200/verificar}")
    private String urlQrBase;

    // Correo del consultorio (del recetario físico)
    private static final String CORREO    = "draleon_alexandra@hotmail.com";
    private static final String HOSPITAL  = "HOSPITAL SAN JUAN";
    private static final String DIRECCION_HOSP = "(Av. Jose Veloz y Sauces)";

    // ──────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] generarRecetaPdf(Long consultaId, RecetaRequest receta) {

        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer   = new PdfWriter(baos);
            PdfDocument pdfDoc   = new PdfDocument(writer);

            // Tamaño media carta (mitad A4 — igual que el recetario físico)
            PageSize mediaCartaH = new PageSize(595, 420);
            Document  document   = new Document(pdfDoc, mediaCartaH);
            document.setMargins(28, 36, 24, 36);

            PdfFont fontR = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont fontB = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            // QR de verificación
            String codigoQr = "REC-" + UUID.randomUUID().toString()
                    .toUpperCase().replace("-","").substring(0, 12);
            String urlQr    = qrService.construirUrlVerificacion(codigoQr);
            byte[] imgQr    = qrService.generarQr(urlQr, 100, 100);

            // ── CABECERA ────────────────────────────────────────────────────
            agregarCabecera(document, fontR, fontB, imgQr, consulta);

            // ── DATOS PACIENTE ──────────────────────────────────────────────
            agregarDatosPaciente(document, fontR, fontB, consulta);

            // ── Rp: MEDICAMENTOS ────────────────────────────────────────────
            agregarMedicamentos(document, fontR, fontB, receta.getMedicamentos());

            // ── PRESCRIPCIÓN GENERAL ────────────────────────────────────────
            if (receta.getPrescripcion() != null
                    && !receta.getPrescripcion().isBlank()) {
                agregarIndicaciones(document, fontR, fontB, receta.getPrescripcion());
            }

            // ── FIRMA + PRÓXIMA CITA ────────────────────────────────────────
            agregarFirmaYProximaCita(document, fontR, fontB,
                    receta.getProximaCita());

            // ── PIE DE PÁGINA ───────────────────────────────────────────────
            agregarPiePagina(document, fontR, codigoQr);

            document.close();
            log.info("Receta generada para consulta {} — código QR: {}",
                    consultaId, codigoQr);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando PDF de receta: {}", e.getMessage());
            throw new ReglaNegocioException(
                    "No se pudo generar la receta: " + e.getMessage());
        }
    }

    // ── Secciones del PDF ─────────────────────────────────────────────────

    private void agregarCabecera(Document doc, PdfFont fontR, PdfFont fontB,
                                 byte[] imgQr, Consulta consulta)
            throws IOException {

        // Tabla cabecera: [Logo/Nombre | Datos consulta | QR]
        Table cab = new Table(UnitValue.createPercentArray(new float[]{3.5f, 3f, 1.5f}))
                .useAllAvailableWidth()
                .setBackgroundColor(ROSA_PRIMARIO);

        // Columna izquierda — nombre y especialidad
        Cell cIzq = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(10);
        cIzq.add(new Paragraph(nombreEspecialista)
                .setFont(fontB).setFontSize(12).setFontColor(ColorConstants.WHITE)
                .setMarginBottom(2));
        cIzq.add(new Paragraph("ESPECIALISTA EN GINECOLOGIA Y OBSTETRICIA")
                .setFont(fontR).setFontSize(7)
                .setFontColor(new DeviceRgb(255, 200, 230)));
        cab.addCell(cIzq);

        // Columna central — fecha y paciente
        Cell cCen = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(10);
        cCen.add(new Paragraph("RECETA MEDICA")
                .setFont(fontB).setFontSize(11)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        cCen.add(new Paragraph(LocalDate.now().format(FMT_FECHA))
                .setFont(fontR).setFontSize(8)
                .setFontColor(new DeviceRgb(255, 200, 230))
                .setTextAlignment(TextAlignment.CENTER));
        cab.addCell(cCen);

        // Columna derecha — QR
        Cell cDer = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setPadding(8);
        Image qrImg = new Image(ImageDataFactory.create(imgQr))
                .setWidth(55).setHeight(55)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        cDer.add(qrImg);
        cab.addCell(cDer);

        doc.add(cab);
    }

    private void agregarDatosPaciente(Document doc, PdfFont fontR, PdfFont fontB,
                                      Consulta consulta) {
        doc.add(new Paragraph("\n").setFontSize(3));

        var pac = consulta.getHistoriaClinica().getPaciente();

        // Línea: ciudad, fecha, edad
        String ciudad = "Riobamba";
        String edad   = pac.getFechaNacimiento() != null
                ? calcularEdad(pac.getFechaNacimiento()) + " años" : "—";

        Table tabDatos = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        tabDatos.addCell(campoLinea(fontR, fontB,
                "Paciente: ", pac.getNombreCompleto()));
        tabDatos.addCell(campoLinea(fontR, fontB,
                "Edad: ", edad));
        tabDatos.addCell(campoLinea(fontR, fontB,
                "C.I.: ", pac.getCedula()));
        tabDatos.addCell(campoLinea(fontR, fontB,
                ciudad + ", " + LocalDate.now().format(FMT_FECHA), ""));

        doc.add(tabDatos);
        doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(4).setMarginBottom(4));
    }

    private void agregarMedicamentos(Document doc, PdfFont fontR, PdfFont fontB,
                                     List<MedicamentoRequest> medicamentos) {
        if (medicamentos == null || medicamentos.isEmpty()) return;

        // Encabezado Rp:
        doc.add(new Paragraph()
                .add(new Text("Rp:").setFont(fontB).setFontSize(14)
                        .setFontColor(ROSA_PRIMARIO))
                .setMarginTop(4).setMarginBottom(4));

        for (int i = 0; i < medicamentos.size(); i++) {
            MedicamentoRequest med = medicamentos.get(i);

            // Fondo alternado
            DeviceRgb fondo = (i % 2 == 0) ? FONDO_CLARO
                    : new DeviceRgb(255, 255, 255);

            Table tabMed = new Table(UnitValue.createPercentArray(
                    new float[]{3f, 1.5f, 1.5f}))
                    .useAllAvailableWidth()
                    .setBackgroundColor(fondo)
                    .setMarginBottom(3);

            // Medicamento nombre
            tabMed.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingLeft(8).setPaddingTop(4).setPaddingBottom(4)
                    .add(new Paragraph()
                            .add(new Text(med.getNombre()).setFont(fontB)
                                    .setFontSize(9).setFontColor(AZUL_OSCURO))));

            // Dosis
            tabMed.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingTop(4).setPaddingBottom(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph(med.getDosis() != null ? med.getDosis() : "—")
                            .setFont(fontR).setFontSize(9)
                            .setFontColor(GRIS_TEXTO)));

            // Cantidad
            tabMed.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPaddingTop(4).setPaddingBottom(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph(
                            "Cant: " + (med.getCantidad() != null ? med.getCantidad() : "—"))
                            .setFont(fontR).setFontSize(9)
                            .setFontColor(ROSA_PRIMARIO)));

            doc.add(tabMed);

            // Indicaciones de administración
            if (med.getIndicaciones() != null && !med.getIndicaciones().isBlank()) {
                doc.add(new Paragraph()
                        .add(new Text("    " + med.getIndicaciones())
                                .setFont(fontR).setFontSize(8)
                                .setFontColor(GRIS_TEXTO))
                        .setMarginBottom(2).setMarginLeft(8));
            }
        }
    }

    private void agregarIndicaciones(Document doc, PdfFont fontR, PdfFont fontB,
                                     String prescripcion) {
        doc.add(new LineSeparator(new SolidLine(0.3f)).setMarginTop(4).setMarginBottom(4));
        doc.add(new Paragraph()
                .add(new Text("Indicaciones: ").setFont(fontB).setFontSize(9)
                        .setFontColor(AZUL_OSCURO))
                .add(new Text(prescripcion).setFont(fontR).setFontSize(9)
                        .setFontColor(GRIS_TEXTO))
                .setMarginBottom(3));
    }

    private void agregarFirmaYProximaCita(Document doc, PdfFont fontR,
                                          PdfFont fontB, String proximaCita) {
        doc.add(new Paragraph("\n").setFontSize(4));

        Table tabFirma = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        // Firma médico
        Cell cFirma = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT).setPadding(4);
        cFirma.add(new Paragraph("_________________________________")
                .setFont(fontR).setFontSize(8).setMarginBottom(2));
        cFirma.add(new Paragraph(nombreEspecialista)
                .setFont(fontB).setFontSize(8));
        cFirma.add(new Paragraph("Firma / Medico")
                .setFont(fontR).setFontSize(7).setFontColor(GRIS_TEXTO));
        tabFirma.addCell(cFirma);

        // Próxima cita
        Cell cProx = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                .setTextAlignment(TextAlignment.RIGHT).setPadding(4);
        if (proximaCita != null && !proximaCita.isBlank()) {
            cProx.add(new Paragraph()
                    .add(new Text("PROXIMA CITA: ").setFont(fontB).setFontSize(8)
                            .setFontColor(ROSA_PRIMARIO))
                    .add(new Text(proximaCita).setFont(fontR).setFontSize(8)
                            .setFontColor(AZUL_OSCURO))
                    .setTextAlignment(TextAlignment.RIGHT));
        }
        tabFirma.addCell(cProx);

        doc.add(tabFirma);
    }

    private void agregarPiePagina(Document doc, PdfFont fontR, String codigoQr) {
        doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(4));
        doc.add(new Paragraph()
                .add(new Text("☏ ").setFontSize(8))
                .add(new Text(telefono.isBlank() ? "096 044 0040" : telefono)
                        .setFont(fontR).setFontSize(7).setFontColor(GRIS_TEXTO))
                .add(new Text("   ✉ ").setFontSize(8))
                .add(new Text(CORREO).setFont(fontR).setFontSize(7)
                        .setFontColor(GRIS_TEXTO))
                .add(new Text("   ♦ ").setFontSize(8))
                .add(new Text(HOSPITAL + " " + DIRECCION_HOSP)
                        .setFont(fontR).setFontSize(7).setFontColor(GRIS_TEXTO))
                .add(new Text("   Cód: " + codigoQr)
                        .setFont(fontR).setFontSize(6)
                        .setFontColor(new DeviceRgb(180, 180, 180)))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(3));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Cell campoLinea(PdfFont fontR, PdfFont fontB,
                            String etiqueta, String valor) {
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS_BORDE, 0.5f))
                .setPaddingTop(3).setPaddingBottom(3);
        c.add(new Paragraph()
                .add(new Text(etiqueta).setFont(fontB).setFontSize(8)
                        .setFontColor(GRIS_TEXTO))
                .add(new Text(valor != null ? valor : "")
                        .setFont(fontR).setFontSize(9)
                        .setFontColor(AZUL_OSCURO)));
        return c;
    }

    private int calcularEdad(java.time.LocalDate nacimiento) {
        return LocalDate.now().getYear() - nacimiento.getYear();
    }
}