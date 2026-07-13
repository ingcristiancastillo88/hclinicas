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
import com.itextpdf.layout.properties.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Genera el PDF completo de consulta clínica.
 * Encabezado, colores, QR y pie de página tomados del PdfServiceImpl original.
 * Contenido clínico expandido con todas las secciones del formato oficial.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfConsultaServiceImpl {

    private final ConsultaRepository consultaRepo;
    private final QrService          qrService;

    // ── Colores institucionales (igual que PdfServiceImpl) ────────────────────
    private static final DeviceRgb COLOR_PRIMARIO    = new DeviceRgb(220, 190, 210);
    private static final DeviceRgb COLOR_SECUNDARIO  = new DeviceRgb(214, 58, 134);
    private static final DeviceRgb COLOR_FONDO       = new DeviceRgb(252, 247, 250);
    private static final DeviceRgb COLOR_TEXTO_CLARO = new DeviceRgb(90, 90, 90);
    private static final DeviceRgb COLOR_BORDE       = new DeviceRgb(220, 190, 210);

    // Colores adicionales para secciones clínicas
    private static final DeviceRgb VERDE_CLR  = new DeviceRgb(230, 244, 240);
    private static final DeviceRgb VERDE      = new DeviceRgb(0, 143, 104);
    private static final DeviceRgb LILA_CLR   = new DeviceRgb(250, 245, 255);
    private static final DeviceRgb LILA_BRD   = new DeviceRgb(216, 180, 254);
    private static final DeviceRgb AMBER_CLR  = new DeviceRgb(255, 251, 235);
    private static final DeviceRgb AMBER      = new DeviceRgb(180, 83, 9);
    private static final DeviceRgb ROSA_CLR   = new DeviceRgb(253, 242, 248);
    private static final DeviceRgb ROSA_BRD   = new DeviceRgb(248, 187, 208);

    @Value("${app.clinica.nombre:Consultorio Gineco-Obstétrico}")
    private String nombreClinica;

    @Value("${app.clinica.especialista:Dra. Alexandra León}")
    private String nombreEspecialista;

    @Value("${app.clinica.especialidad:Médico Especialista en Ginecología y Obstetricia}")
    private String especialidad;

    @Value("${app.clinica.direccion:Hospital San Juan - Riobamba, Ecuador}")
    private String direccionClinica;

    @Value("${app.clinica.telefono:096 044 0040 - 099 146 3226}")
    private String telefonoClinica;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── API pública ───────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long consultaId) {
        Consulta c = consultaRepo.findByIdConArchivos(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdfDoc  = new PdfDocument(new PdfWriter(baos));
            Document    doc     = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(36, 50, 36, 50);

            PdfFont regular = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            // QR de trazabilidad
            String codigoQr = UUID.randomUUID().toString().toUpperCase().substring(0, 16);
            String urlQr    = qrService.construirUrlVerificacion(codigoQr);
            byte[] imagenQr = qrService.generarQr(urlQr, 150, 150);

            // ── ENCABEZADO institucional (igual que PdfServiceImpl) ───────────
            agregarCabecera(doc, bold, regular, imagenQr, codigoQr, "CONSULTA MÉDICA");

            // ── 1. Datos del Paciente ─────────────────────────────────────────
            var pac = c.getHistoriaClinica().getPaciente();
            agregarSeccion(doc, bold, "DATOS DEL PACIENTE");
            Table tabPac = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();
            agregarFila(tabPac, "Nombre completo:", pac.getNombreCompleto(), bold, regular);
            agregarFila(tabPac, "Cédula de identidad:", nvlStr(pac.getCedula()), bold, regular);
            agregarFila(tabPac, "Fecha de nacimiento:",
                    pac.getFechaNacimiento() != null
                            ? pac.getFechaNacimiento().format(FORMATO_FECHA) : "—",
                    bold, regular);
            agregarFila(tabPac, "Historia Clínica N°:",
                    "HC-" + c.getHistoriaClinica().getId(), bold, regular);
            doc.add(tabPac);
            espacio(doc);

            // ── 2. Parámetros de la visita ────────────────────────────────────
            agregarSeccion(doc, bold, "PARÁMETROS DE LA VISITA");
            Table tabVisita = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .useAllAvailableWidth();
            agregarFila(tabVisita, "Fecha de Consulta:",
                    c.getFechaConsulta().format(FORMATO_FECHA), bold, regular);
            agregarFila(tabVisita, "Tipo de Consulta:",
                    formatTipo(getField(c, "getTipoConsulta")), bold, regular);
            Boolean emb = getBoolean(c, "getEstaEmbarazada");
            String embStr = emb == null ? "No especificado"
                    : Boolean.TRUE.equals(emb) ? "SÍ — Módulo Prenatal Activo"
                      : "NO — Módulo Ginecológico";
            agregarFila(tabVisita, "¿Embarazada?:", embStr, bold, regular);
            doc.add(tabVisita);
            espacio(doc);

            // ── 3. Motivo y Nota de Evolución ─────────────────────────────────
            agregarSeccion(doc, bold, "ANAMNESIS Y EVOLUCIÓN");
            doc.add(new Paragraph()
                    .add(new Text("Motivo de consulta:  ").setFont(bold).setFontSize(9)
                            .setFontColor(COLOR_TEXTO_CLARO))
                    .add(new Text(nvlStr(c.getMotivoConsulta())).setFont(regular).setFontSize(9)));
            String evolucion = getField(c, "getEnfermedadActual");
            if (noBlank(evolucion)) {
                doc.add(new Paragraph()
                        .add(new Text("Nota de Evolución / Enfermedad Actual:  ")
                                .setFont(bold).setFontSize(9).setFontColor(COLOR_TEXTO_CLARO))
                        .add(new Text(evolucion).setFont(regular).setFontSize(9))
                        .setMarginTop(4));
            }
            String reporte = getField(c, "getReporteExamenesPrevios");
            if (noBlank(reporte)) {
                doc.add(new Paragraph()
                        .add(new Text("Reporte / Exámenes Traídos:  ")
                                .setFont(bold).setFontSize(9).setFontColor(VERDE))
                        .add(new Text(reporte).setFont(regular).setFontSize(9))
                        .setBackgroundColor(VERDE_CLR)
                        .setBorderLeft(new SolidBorder(VERDE, 3))
                        .setPadding(6).setMarginTop(6));
            }
            espacio(doc);

            // ── 4. Signos Vitales ─────────────────────────────────────────────
            if (tieneSignosVitales(c)) {
                agregarSeccion(doc, bold, "SIGNOS VITALES Y ANTROPOMETRÍA");
                Table tabSV = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                        .useAllAvailableWidth().setBackgroundColor(COLOR_FONDO);
                if (c.getPeso() != null)
                    agregarFila(tabSV, "Peso:", c.getPeso() + " kg", bold, regular);
                if (c.getTalla() != null)
                    agregarFila(tabSV, "Talla:", c.getTalla() + " cm", bold, regular);
                if (c.getPeso() != null && c.getTalla() != null) {
                    double imc = c.getPeso() / Math.pow(c.getTalla() / 100.0, 2);
                    agregarFila(tabSV, "IMC (Auto):",
                            String.format("%.2f (%s)", imc, clasificarImc(imc)), bold, regular);
                }
                if (c.getPresionArterial() != null)
                    agregarFila(tabSV, "Tensión Arterial:", c.getPresionArterial(), bold, regular);
                if (c.getFrecuenciaCardiaca() != null)
                    agregarFila(tabSV, "Frec. Cardíaca:", c.getFrecuenciaCardiaca() + " lpm", bold, regular);
                if (c.getTemperatura() != null)
                    agregarFila(tabSV, "Temperatura:", c.getTemperatura() + " °C", bold, regular);
                if (c.getSaturacionOxigeno() != null)
                    agregarFila(tabSV, "Saturación O₂:", c.getSaturacionOxigeno() + "%", bold, regular);
                if (c.getSemanasGestacion() != null)
                    agregarFila(tabSV, "Semanas Gestación:", c.getSemanasGestacion() + " sem", bold, regular);
                // Campos texto nuevos
                String fr = getField(c, "getFrecuenciaRespiratoriaTexto");
                if (noBlank(fr)) agregarFila(tabSV, "Frec. Respiratoria:", fr, bold, regular);
                doc.add(tabSV);
                espacio(doc);
            }

            // ── 5. Módulo Materno-Fetal (si embarazada) ───────────────────────
            if (Boolean.TRUE.equals(emb)) {
                String altUt  = getField(c, "getAlturaUterina");
                String fcf    = getField(c, "getFcFetal");
                String pres   = getField(c, "getPresentacionFetal");
                String mov    = getField(c, "getMovimientosFetales");
                String epf    = getField(c, "getPesoFetalEstimado");
                String score  = getField(c, "getScoreMama");
                if (noBlank(altUt) || noBlank(fcf) || noBlank(pres)) {
                    agregarSeccion(doc, bold, "MÓDULO MATERNO-FETAL");
                    Table tabMat = new Table(UnitValue.createPercentArray(new float[]{1,1,1}))
                            .useAllAvailableWidth().setBackgroundColor(LILA_CLR)
                            .setBorder(new SolidBorder(LILA_BRD, 1));
                    if (noBlank(altUt)) agregarFila(tabMat, "Altura Uterina:", altUt, bold, regular);
                    if (noBlank(fcf))   agregarFila(tabMat, "FC Fetal (FCF):", fcf, bold, regular);
                    if (noBlank(pres))  agregarFila(tabMat, "Presentación/Posición:", pres, bold, regular);
                    if (noBlank(mov))   agregarFila(tabMat, "Movimientos Fetales:", mov, bold, regular);
                    if (noBlank(epf))   agregarFila(tabMat, "Peso Fetal (EPF):", epf, bold, regular);
                    doc.add(tabMat);
                    if (noBlank(score)) {
                        doc.add(new Paragraph("⚠ SCORE MAMÁ: " + score)
                                .setFont(bold).setFontSize(8.5f).setFontColor(AMBER)
                                .setBackgroundColor(AMBER_CLR)
                                .setBorderLeft(new SolidBorder(AMBER, 3))
                                .setPadding(6).setMarginTop(4));
                    }
                    espacio(doc);
                }
            }

            // ── 6. Módulo Ginecológico (si no embarazada) ─────────────────────
            if (Boolean.FALSE.equals(emb)) {
                String vulva  = getField(c, "getInspeccionVulva");
                String espec  = getField(c, "getEspeculoscopia");
                String tacto  = getField(c, "getTactoVaginal");
                String mamas  = getField(c, "getExamenMamas");
                if (noBlank(vulva) || noBlank(espec) || noBlank(tacto) || noBlank(mamas)) {
                    agregarSeccion(doc, bold, "EXPLORACIÓN GINECOLÓGICA");
                    Table tabGin = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                            .useAllAvailableWidth().setBackgroundColor(ROSA_CLR)
                            .setBorder(new SolidBorder(ROSA_BRD, 1));
                    if (noBlank(vulva))  agregarFila(tabGin, "Inspección Externa / Vulva:", vulva, bold, regular);
                    if (noBlank(espec))  agregarFila(tabGin, "Especuloscopia (Cérvix/Vagina):", espec, bold, regular);
                    if (noBlank(tacto))  agregarFila(tabGin, "Tacto Bimanual (Útero/Anexos):", tacto, bold, regular);
                    if (noBlank(mamas))  agregarFila(tabGin, "Examen de Mamas Bilateral:", mamas, bold, regular);
                    doc.add(tabGin);
                    espacio(doc);
                }
            }

            // ── 7. Examen Físico por Sistemas ─────────────────────────────────
            if (noBlank(c.getExamenFisico()) || noBlank(getField(c, "getExamenCabeza"))) {
                agregarSeccion(doc, bold, "EXAMEN FÍSICO POR SISTEMAS");
                Table tabEF = new Table(UnitValue.createPercentArray(new float[]{1.2f, 3.8f}))
                        .useAllAvailableWidth();
                if (noBlank(c.getExamenFisico()))
                    agregarFila(tabEF, "1. General:", c.getExamenFisico(), bold, regular);
                agregarFieldSistema(tabEF, "2. Cabeza y Cuello:", c, "getExamenCabeza", bold, regular);
                agregarFieldSistema(tabEF, "3. Tórax / Cardiopulm.:", c, "getExamenTorax", bold, regular);
                agregarFieldSistema(tabEF, "4. Abdomen:", c, "getExamenAbdomen", bold, regular);
                agregarFieldSistema(tabEF, "5. Región Genital:", c, "getExamenGenital", bold, regular);
                agregarFieldSistema(tabEF, "6. Extremidades:", c, "getExamenExtremidades", bold, regular);
                doc.add(tabEF);
                espacio(doc);
            }

            // ── 8. Diagnósticos CIE-10 ────────────────────────────────────────
            agregarSeccion(doc, bold, "DIAGNÓSTICO");
            doc.add(new Paragraph()
                    .add(new Text("Diagnóstico principal:  ").setFont(bold).setFontSize(10))
                    .add(new Text(nvlStr(c.getDiagnosticoPrincipal())).setFont(regular).setFontSize(10)));
            if (noBlank(c.getDiagnosticoSecundario())) {
                doc.add(new Paragraph()
                        .add(new Text("Diagnóstico secundario:  ").setFont(bold).setFontSize(10))
                        .add(new Text(c.getDiagnosticoSecundario()).setFont(regular).setFontSize(10)));
            }
            if (noBlank(c.getCodigoCie10())) {
                doc.add(new Paragraph()
                        .add(new Text("Código CIE-10 Principal:  ")
                                .setFont(bold).setFontSize(10).setFontColor(COLOR_SECUNDARIO))
                        .add(new Text(c.getCodigoCie10()).setFont(bold).setFontSize(10)));
            }
            if (noBlank(c.getCodigosCie10SecundariosJson())) {
                doc.add(new Paragraph()
                        .add(new Text("Códigos CIE-10 Secundarios:  ").setFont(bold).setFontSize(10))
                        .add(new Text(c.getCodigosCie10SecundariosJson())
                                .setFont(regular).setFontSize(10)));
            }
            espacio(doc);

            // ── 9. Tratamiento ────────────────────────────────────────────────
            if (noBlank(c.getTratamiento()) || noBlank(c.getMedicacion())) {
                agregarSeccion(doc, bold, "TRATAMIENTO Y MEDICACIÓN");
                if (noBlank(c.getTratamiento()))
                    doc.add(new Paragraph()
                            .add(new Text("Tratamiento:  ").setFont(bold).setFontSize(10))
                            .add(new Text(c.getTratamiento()).setFont(regular).setFontSize(10)));
                if (noBlank(c.getMedicacion()))
                    doc.add(new Paragraph()
                            .add(new Text("Medicación:  ").setFont(bold).setFontSize(10))
                            .add(new Text(c.getMedicacion()).setFont(regular).setFontSize(10)));
                if (noBlank(c.getIndicaciones()))
                    doc.add(new Paragraph()
                            .add(new Text("Indicaciones:  ").setFont(bold).setFontSize(10))
                            .add(new Text(c.getIndicaciones()).setFont(regular).setFontSize(10)));
                if (c.getProximaCita() != null)
                    doc.add(new Paragraph()
                            .add(new Text("Próxima cita:  ")
                                    .setFont(bold).setFontSize(10).setFontColor(COLOR_SECUNDARIO))
                            .add(new Text(c.getProximaCita().format(FORMATO_FECHA))
                                    .setFont(regular).setFontSize(10)));
                espacio(doc);
            }

            // ── 10. Observaciones ─────────────────────────────────────────────
            if (noBlank(c.getObservaciones())) {
                agregarSeccion(doc, bold, "OBSERVACIONES");
                doc.add(parrafoContenido(c.getObservaciones(), regular));
            }

            // ── Firma y pie de página (igual que PdfServiceImpl) ──────────────
            agregarFirma(doc, bold, regular);
            agregarPiePagina(doc, regular, codigoQr);

            doc.close();
            log.info("PDF consulta {} generado OK, QR: {}", consultaId, codigoQr);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF consulta {}: {}", consultaId, e.getMessage(), e);
            throw new ReglaNegocioException("Error al generar el PDF: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS — encabezado/pie iguales a PdfServiceImpl
    // ═══════════════════════════════════════════════════════════════════════

    private void agregarCabecera(Document doc, PdfFont bold, PdfFont regular,
                                 byte[] imagenQr, String codigoQr, String titulo)
            throws Exception {

        Image logo = new Image(ImageDataFactory.create(
                getClass().getResourceAsStream("/imagenes/logo-dra.png").readAllBytes()));
        logo.setWidth(90);

        Table cabecera = new Table(UnitValue.createPercentArray(new float[]{1.2f, 4f, 1.2f}))
                .useAllAvailableWidth()
                .setBackgroundColor(COLOR_PRIMARIO)
                .setBorder(Border.NO_BORDER);

        Cell celdaLogo = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        celdaLogo.add(logo);
        cabecera.addCell(celdaLogo);

        Cell celdaClinica = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(12);
        celdaClinica.add(new Paragraph(nombreClinica)
                .setFont(bold).setFontSize(11).setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(nombreEspecialista)
                .setFont(bold).setFontSize(9).setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(especialidad)
                .setFont(regular).setFontSize(8).setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(direccionClinica)
                .setFont(regular).setFontSize(7).setFontColor(ColorConstants.WHITE));
        cabecera.addCell(celdaClinica);

        Cell celdaQr = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER).setPadding(8);
        Image qrImg = new Image(ImageDataFactory.create(imagenQr))
                .setWidth(70).setHeight(70)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        celdaQr.add(qrImg);
        celdaQr.add(new Paragraph("Verificar autenticidad")
                .setFont(regular).setFontSize(6).setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        cabecera.addCell(celdaQr);

        // Barra de título
        Table titTab = new Table(1).useAllAvailableWidth();
        titTab.addCell(new Cell()
                .add(new Paragraph(titulo).setFont(bold).setFontSize(18)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(COLOR_PRIMARIO).setBorder(Border.NO_BORDER));

        doc.add(titTab);
        doc.add(cabecera);
        doc.add(new Paragraph("\n").setFontSize(6));
        doc.add(new LineSeparator(new SolidLine(1.5f)));
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void agregarSeccion(Document doc, PdfFont bold, String titulo) {
        doc.add(new Paragraph(titulo)
                .setFont(bold).setFontSize(10)
                .setFontColor(COLOR_PRIMARIO)
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorderBottom(new SolidBorder(COLOR_SECUNDARIO, 1f))
                .setPaddingBottom(4).setMarginTop(12));
    }

    private void agregarFila(Table t, String label, String valor,
                             PdfFont bold, PdfFont regular) {
        t.addCell(new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(9)
                        .setFontColor(COLOR_TEXTO_CLARO))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f)).setPadding(5));
        t.addCell(new Cell()
                .add(new Paragraph(valor != null ? valor : "—")
                        .setFont(regular).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f)).setPadding(5));
    }

    /** Agrega fila de examen físico solo si el campo existe y no está vacío */
    private void agregarFieldSistema(Table t, String label, Consulta c,
                                     String methodName, PdfFont bold, PdfFont regular) {
        String val = getField(c, methodName);
        if (noBlank(val)) agregarFila(t, label, val, bold, regular);
    }

    private Paragraph parrafoContenido(String texto, PdfFont font) {
        return new Paragraph(texto).setFont(font).setFontSize(9)
                .setBackgroundColor(COLOR_FONDO).setPadding(8).setMarginBottom(4);
    }

    private void agregarFirma(Document doc, PdfFont bold, PdfFont regular) {
        doc.add(new Paragraph("\n\n").setFontSize(8));
        Table tabFirma = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();
        Cell cFirma = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER).setPadding(10);
        cFirma.add(new Paragraph("_________________________________")
                .setFont(regular).setFontSize(9).setMarginBottom(4));
        cFirma.add(new Paragraph(nombreEspecialista).setFont(bold).setFontSize(9));
        cFirma.add(new Paragraph(especialidad)
                .setFont(regular).setFontSize(8).setFontColor(COLOR_TEXTO_CLARO));
        tabFirma.addCell(cFirma);
        tabFirma.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(tabFirma);
    }

    private void agregarPiePagina(Document doc, PdfFont regular, String codigoQr) {
        doc.add(new LineSeparator(new SolidLine(0.5f)));
        doc.add(new Paragraph(
                "Documento generado por el sistema HClínicas  ·  "
                        + "Código de verificación: " + codigoQr + "  ·  "
                        + "Fecha: " + LocalDate.now().format(FORMATO_FECHA) + "  ·  "
                        + nombreEspecialista)
                .setFont(regular).setFontSize(7).setFontColor(COLOR_TEXTO_CLARO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
    }

    private void espacio(Document doc) {
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS DE DATOS
    // ═══════════════════════════════════════════════════════════════════════

    private boolean noBlank(String s) { return s != null && !s.isBlank(); }
    private String nvlStr(String s)   { return s != null ? s : "—"; }

    private boolean tieneSignosVitales(Consulta c) {
        return c.getPeso() != null || c.getTalla() != null
                || c.getPresionArterial() != null || c.getFrecuenciaCardiaca() != null
                || c.getTemperatura() != null || c.getSaturacionOxigeno() != null
                || c.getSemanasGestacion() != null;
    }

    private String clasificarImc(double v) {
        if (v < 18.5) return "Bajo Peso";
        if (v < 25)   return "Normopeso";
        if (v < 30)   return "Sobrepeso";
        if (v < 35)   return "Obesidad I";
        return "Obesidad II+";
    }

    private String formatTipo(String tipo) {
        if (tipo == null) return "—";
        return switch (tipo) {
            case "PRENATAL"       -> "Control Prenatal";
            case "GINECO_GENERAL" -> "Ginecología General / Control";
            case "PROCEDIMIENTO"  -> "Procedimiento Ginecológico";
            case "RESULTADOS"     -> "Lectura de Resultados";
            case "PRIMERA_VEZ"   -> "Primera Vez";
            default -> tipo;
        };
    }

    /** Reflexión segura — retorna null si el método no existe en la entidad */
    private String getField(Object obj, String method) {
        try {
            Object r = obj.getClass().getMethod(method).invoke(obj);
            return r != null ? r.toString() : null;
        } catch (Exception e) { return null; }
    }

    private Boolean getBoolean(Object obj, String method) {
        try {
            Object r = obj.getClass().getMethod(method).invoke(obj);
            return r != null ? (Boolean) r : null;
        } catch (Exception e) { return null; }
    }
}