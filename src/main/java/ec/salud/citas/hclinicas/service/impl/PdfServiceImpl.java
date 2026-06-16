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
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.HistoriaClinica;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.HistoriaClinicaRepository;
import ec.salud.citas.hclinicas.service.PdfService;
import ec.salud.citas.hclinicas.service.QrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementación del servicio de generación de PDF con iText7.
 * HU-022 — Documento de consulta + QR para verificación de autenticidad.
 * CU-004 requisito: "El código QR debe generarse con el nombre de la
 * especialista, así como debe contener fecha y hora de la generación."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final ConsultaRepository       consultaRepo;
    private final HistoriaClinicaRepository historiaRepo;
    private final QrService                qrService;

    // Colores institucionales del sistema HClínicas
    private static final DeviceRgb COLOR_PRIMARIO    = new DeviceRgb(220, 190, 210);
    private static final DeviceRgb COLOR_SECUNDARIO  = new DeviceRgb(214, 58, 134);
    private static final DeviceRgb COLOR_FONDO       = new DeviceRgb(252, 247, 250);
    private static final DeviceRgb COLOR_TEXTO_CLARO = new DeviceRgb(90, 90, 90);
    private static final DeviceRgb COLOR_BORDE       = new DeviceRgb(220, 190, 210);

    @Value("${app.clinica.nombre:Consultorio Gineco-Obstétrico}")
    private String nombreClinica;

    @Value("${app.clinica.especialista:Dra. Alexandra León}")
    private String nombreEspecialista;

    @Value("${app.clinica.especialidad:Médico Especialista en Ginecología y Obstetricia}")
    private String especialidad;

    @Value("${app.clinica.direccion:Hospital San Juan - Riobamba, Ecuador}")
    private String direccionClinica;

    @Value("${app.clinica.telefono:}")
    private String telefonoClinica;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── PDF de Consulta ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdfConsulta(Long consultaId) {
        Consulta consulta = consultaRepo.findByIdConArchivos(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer   = new PdfWriter(baos);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(36, 50, 36, 50);

            PdfFont fontRegular = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            // Generar código único para QR
            String codigoQr  = UUID.randomUUID().toString().toUpperCase().substring(0, 16);
            String urlQr     = qrService.construirUrlVerificacion(codigoQr);
            byte[] imagenQr  = qrService.generarQr(urlQr, 150, 150);
            // ── Cabecera ──────────────────────────────────────────────────────
            agregarCabecera(document, fontBold, fontRegular,
                    imagenQr, codigoQr, "CONSULTA MÉDICA");

            // ── Datos del paciente ────────────────────────────────────────────
            agregarSeccion(document, fontBold, "DATOS DEL PACIENTE");

            Table tabPaciente = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            var pac = consulta.getHistoriaClinica().getPaciente();
            agregarFilaTabla(tabPaciente, "Nombre completo:",
                    pac.getNombreCompleto(), fontBold, fontRegular);
            agregarFilaTabla(tabPaciente, "Cédula de identidad:",
                    pac.getCedula(), fontBold, fontRegular);
            agregarFilaTabla(tabPaciente, "Fecha de nacimiento:",
                    pac.getFechaNacimiento() != null
                            ? pac.getFechaNacimiento().format(FORMATO_FECHA) : "—",
                    fontBold, fontRegular);
            agregarFilaTabla(tabPaciente, "Teléfono / Celular:",
                    pac.getCelular() != null ? pac.getCelular() : "—",
                    fontBold, fontRegular);

            document.add(tabPaciente);
            document.add(new Paragraph("\n").setFontSize(4));

            // ── Datos de la consulta ──────────────────────────────────────────
            agregarSeccion(document, fontBold, "DATOS DE LA CONSULTA");

            Table tabConsulta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            agregarFilaTabla(tabConsulta, "Fecha de consulta:",
                    consulta.getFechaConsulta().format(FORMATO_FECHA),
                    fontBold, fontRegular);
            agregarFilaTabla(tabConsulta, "Motivo de consulta:",
                    consulta.getMotivoConsulta(), fontBold, fontRegular);
            document.add(tabConsulta);
            document.add(new Paragraph("\n").setFontSize(4));

            // ── Signos vitales ────────────────────────────────────────────────
            if (tieneSignosVitales(consulta)) {
                agregarSeccion(document, fontBold, "SIGNOS VITALES");
                Table tabSignos = new Table(
                        UnitValue.createPercentArray(new float[]{1, 1, 1}))
                        .useAllAvailableWidth();

                if (consulta.getPeso() != null)
                    agregarFilaTabla(tabSignos, "Peso:", consulta.getPeso() + " kg",
                            fontBold, fontRegular);
                if (consulta.getTalla() != null)
                    agregarFilaTabla(tabSignos, "Talla:", consulta.getTalla() + " cm",
                            fontBold, fontRegular);
                if (consulta.getPeso() != null && consulta.getTalla() != null) {
                    double imc = calcularImc(consulta.getPeso(), consulta.getTalla());
                    agregarFilaTabla(tabSignos, "IMC:",
                            String.format("%.2f", imc), fontBold, fontRegular);
                }
                if (consulta.getPresionArterial() != null)
                    agregarFilaTabla(tabSignos, "Presión arterial:",
                            consulta.getPresionArterial(), fontBold, fontRegular);
                if (consulta.getFrecuenciaCardiaca() != null)
                    agregarFilaTabla(tabSignos, "Frecuencia cardíaca:",
                            consulta.getFrecuenciaCardiaca() + " lpm",
                            fontBold, fontRegular);
                if (consulta.getTemperatura() != null)
                    agregarFilaTabla(tabSignos, "Temperatura:",
                            consulta.getTemperatura() + " °C", fontBold, fontRegular);
                if (consulta.getSaturacionOxigeno() != null)
                    agregarFilaTabla(tabSignos, "Saturación O₂:",
                            consulta.getSaturacionOxigeno() + "%", fontBold, fontRegular);
                if (consulta.getSemanasGestacion() != null)
                    agregarFilaTabla(tabSignos, "Semanas gestación:",
                            consulta.getSemanasGestacion() + " semanas",
                            fontBold, fontRegular);

                document.add(tabSignos);
                document.add(new Paragraph("\n").setFontSize(4));
            }

            // ── Examen físico ─────────────────────────────────────────────────
            if (consulta.getExamenFisico() != null) {
                agregarSeccion(document, fontBold, "EXAMEN FÍSICO");
                document.add(parrafoContenido(consulta.getExamenFisico(), fontRegular));
            }

            // ── Diagnóstico ───────────────────────────────────────────────────
            agregarSeccion(document, fontBold, "DIAGNÓSTICO");

            document.add(new Paragraph()
                    .add(new Text("Diagnóstico principal:  ").setFont(fontBold).setFontSize(10))
                    .add(new Text(consulta.getDiagnosticoPrincipal()).setFont(fontRegular).setFontSize(10)));

            if (consulta.getDiagnosticoSecundario() != null) {
                document.add(new Paragraph()
                        .add(new Text("Diagnóstico secundario:  ").setFont(fontBold).setFontSize(10))
                        .add(new Text(consulta.getDiagnosticoSecundario()).setFont(fontRegular).setFontSize(10)));
            }
            if (consulta.getCodigoCie10() != null) {
                document.add(new Paragraph()
                        .add(new Text("Código CIE-10:  ").setFont(fontBold).setFontSize(10))
                        .add(new Text(consulta.getCodigoCie10()).setFont(fontRegular).setFontSize(10)));
            }
            document.add(new Paragraph("\n").setFontSize(4));

            // ── Tratamiento ───────────────────────────────────────────────────
            if (consulta.getTratamiento() != null || consulta.getMedicacion() != null) {
                agregarSeccion(document, fontBold, "TRATAMIENTO Y MEDICACIÓN");

                if (consulta.getTratamiento() != null) {
                    document.add(new Paragraph()
                            .add(new Text("Tratamiento:  ").setFont(fontBold).setFontSize(10))
                            .add(new Text(consulta.getTratamiento()).setFont(fontRegular).setFontSize(10)));
                }
                if (consulta.getMedicacion() != null) {
                    document.add(new Paragraph()
                            .add(new Text("Medicación:  ").setFont(fontBold).setFontSize(10))
                            .add(new Text(consulta.getMedicacion()).setFont(fontRegular).setFontSize(10)));
                }
                if (consulta.getIndicaciones() != null) {
                    document.add(new Paragraph()
                            .add(new Text("Indicaciones:  ").setFont(fontBold).setFontSize(10))
                            .add(new Text(consulta.getIndicaciones()).setFont(fontRegular).setFontSize(10)));
                }
                if (consulta.getProximaCita() != null) {
                    document.add(new Paragraph()
                            .add(new Text("Próxima cita:  ").setFont(fontBold).setFontSize(10)
                                    .setFontColor(COLOR_SECUNDARIO))
                            .add(new Text(consulta.getProximaCita().format(FORMATO_FECHA))
                                    .setFont(fontRegular).setFontSize(10)));
                }
                document.add(new Paragraph("\n").setFontSize(4));
            }

            if (consulta.getObservaciones() != null) {
                agregarSeccion(document, fontBold, "OBSERVACIONES");
                document.add(parrafoContenido(consulta.getObservaciones(), fontRegular));
            }

            // ── Firma ─────────────────────────────────────────────────────────
            agregarFirma(document, fontBold, fontRegular);

            // ── Pie de página con QR info ─────────────────────────────────────
            agregarPiePagina(document, fontRegular, codigoQr);

            document.close();
            log.info("PDF de consulta {} generado, código QR: {}", consultaId, codigoQr);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando PDF consulta {}: {}", consultaId, e.getMessage());
            throw new ReglaNegocioException("Error al generar el PDF: " + e.getMessage());
        }
    }

    // ── PDF de Historia Clínica ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdfHistoria(Long historiaId) {
        HistoriaClinica historia = historiaRepo.findById(historiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada: " + historiaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer   = new PdfWriter(baos);
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(36, 50, 36, 50);

            PdfFont fontRegular = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
            PdfFont fontBold    = PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);

            String codigoQr = UUID.randomUUID().toString().toUpperCase().substring(0, 16);
            String urlQr    = qrService.construirUrlVerificacion(codigoQr);
            byte[] imagenQr = qrService.generarQr(urlQr, 150, 150);

            agregarCabecera(document, fontBold, fontRegular,
                    imagenQr, codigoQr, "RESUMEN DE HISTORIA CLÍNICA");

            // Datos del paciente
            agregarSeccion(document, fontBold, "DATOS DEL PACIENTE");
            Table tabPac = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            var pac = historia.getPaciente();
            agregarFilaTabla(tabPac, "Nombre:", pac.getNombreCompleto(), fontBold, fontRegular);
            agregarFilaTabla(tabPac, "Cédula:", pac.getCedula(), fontBold, fontRegular);
            agregarFilaTabla(tabPac, "Historia N°:",
                    pac.getHistoriaNumero() != null ? pac.getHistoriaNumero() : "—",
                    fontBold, fontRegular);
            agregarFilaTabla(tabPac, "Fecha nacimiento:",
                    pac.getFechaNacimiento() != null
                            ? pac.getFechaNacimiento().format(FORMATO_FECHA) : "—",
                    fontBold, fontRegular);
            document.add(tabPac);
            document.add(new Paragraph("\n").setFontSize(4));

            // Antecedentes gineco-obstétricos
            agregarSeccion(document, fontBold, "ANTECEDENTES GINECO-OBSTÉTRICOS");

            // Fórmula obstétrica en cuadro destacado
            if (historia.getGestas() != null || historia.getPartos() != null) {
                Paragraph formula = new Paragraph()
                        .add(new Text("Fórmula Obstétrica:  ").setFont(fontBold).setFontSize(11))
                        .add(new Text(
                                "G" + nvl(historia.getGestas()) +
                                        " P" + nvl(historia.getPartos()) +
                                        " C" + nvl(historia.getCesareas()) +
                                        " A" + nvl(historia.getAbortos()) +
                                        " HV" + nvl(historia.getHijosVivos()))
                                .setFont(fontBold).setFontSize(13)
                                .setFontColor(COLOR_PRIMARIO))
                        .setBackgroundColor(COLOR_FONDO)
                        .setPadding(8)
                        .setBorderLeft(new SolidBorder(COLOR_SECUNDARIO, 4));
                document.add(formula);
                document.add(new Paragraph("\n").setFontSize(4));
            }

            Table tabAnt = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            if (historia.getMenarquia() != null)
                agregarFilaTabla(tabAnt, "Menarquia:", historia.getMenarquia(), fontBold, fontRegular);
            if (historia.getCicloMenstrual() != null)
                agregarFilaTabla(tabAnt, "Ciclo menstrual:", historia.getCicloMenstrual(), fontBold, fontRegular);
            if (historia.getFechaUltimaMenstruacion() != null)
                agregarFilaTabla(tabAnt, "Fecha última menstruación:",
                        historia.getFechaUltimaMenstruacion(), fontBold, fontRegular);
            if (historia.getMetodoAnticonceptivo() != null)
                agregarFilaTabla(tabAnt, "Método anticonceptivo:",
                        historia.getMetodoAnticonceptivo(), fontBold, fontRegular);
            if (historia.getUltimoPapanicolau() != null)
                agregarFilaTabla(tabAnt, "Último Papanicolau:",
                        historia.getUltimoPapanicolau(), fontBold, fontRegular);
            if (historia.getUltimaMamografia() != null)
                agregarFilaTabla(tabAnt, "Última Mamografía:",
                        historia.getUltimaMamografia(), fontBold, fontRegular);

            document.add(tabAnt);

            if (historia.getObservacionesGenerales() != null) {
                document.add(new Paragraph("\n").setFontSize(4));
                agregarSeccion(document, fontBold, "OBSERVACIONES GENERALES");
                document.add(parrafoContenido(historia.getObservacionesGenerales(), fontRegular));
            }

            // Resumen de consultas
            var consultas = historia.getConsultas().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActiva()))
                    .sorted((a, b) -> b.getFechaConsulta().compareTo(a.getFechaConsulta()))
                    .toList();

            if (!consultas.isEmpty()) {
                document.add(new Paragraph("\n").setFontSize(4));
                agregarSeccion(document, fontBold,
                        "HISTORIAL DE CONSULTAS (" + consultas.size() + ")");

                Table tabConsultas = new Table(
                        UnitValue.createPercentArray(new float[]{0.8f, 2f, 2.5f}))
                        .useAllAvailableWidth()
                        .setBackgroundColor(COLOR_FONDO);

                // Encabezado de la tabla
                tabConsultas.addHeaderCell(celdaHeader("Fecha", fontBold));
                tabConsultas.addHeaderCell(celdaHeader("Motivo", fontBold));
                tabConsultas.addHeaderCell(celdaHeader("Diagnóstico Principal", fontBold));

                for (Consulta c : consultas) {
                    tabConsultas.addCell(celdaDato(
                            c.getFechaConsulta().format(FORMATO_FECHA), fontRegular));
                    tabConsultas.addCell(celdaDato(
                            abreviar(c.getMotivoConsulta(), 50), fontRegular));
                    tabConsultas.addCell(celdaDato(
                            abreviar(c.getDiagnosticoPrincipal(), 70), fontRegular));
                }
                document.add(tabConsultas);
            }

            agregarFirma(document, fontBold, fontRegular);
            agregarPiePagina(document, fontRegular, codigoQr);

            document.close();
            log.info("PDF historia {} generado, código QR: {}", historiaId, codigoQr);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando PDF historia {}: {}", historiaId, e.getMessage());
            throw new ReglaNegocioException("Error al generar el PDF: " + e.getMessage());
        }
    }

    // ── Helpers de construcción del PDF ───────────────────────────────────────

    private void agregarCabecera(Document doc, PdfFont fontBold, PdfFont fontRegular,
                                 byte[] imagenQr, String codigoQr, String titulo)
            throws IOException {

        Image logo = new Image(
                ImageDataFactory.create(
                        getClass()
                                .getResourceAsStream("/imagenes/logo-dra.png")
                                .readAllBytes()
                )
        );

        logo.setWidth(90);

        // Tabla cabecera: Datos clínica | Título | QR
        Table cabecera = new Table(
                UnitValue.createPercentArray(
                        new float[]{1.2f, 4f, 1.2f}
                ))
                .useAllAvailableWidth()
                .setBackgroundColor(COLOR_PRIMARIO)
                .setBorder(Border.NO_BORDER);

        Cell celdaLogo = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        celdaLogo.add(logo);

        cabecera.addCell(celdaLogo);

        // Columna izquierda: datos del consultorio
        Cell celdaClinica = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(12);
        celdaClinica.add(new Paragraph(nombreClinica)
                .setFont(fontBold).setFontSize(11).setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(nombreEspecialista)
                .setFont(fontBold).setFontSize(9)
                .setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(especialidad)
                .setFont(fontRegular).setFontSize(8).setFontColor(ColorConstants.WHITE));
        celdaClinica.add(new Paragraph(direccionClinica)
                .setFont(fontRegular).setFontSize(7).setFontColor(ColorConstants.WHITE));
        cabecera.addCell(celdaClinica);

        // Columna central: título del documento
//        Cell celdaTitulo = new Cell().setBorder(Border.NO_BORDER)
//                .setVerticalAlignment(VerticalAlignment.MIDDLE)
//                .setTextAlignment(TextAlignment.CENTER)
//                .setPadding(12);
//        celdaTitulo.add(new Paragraph(titulo)
//                .setFont(fontBold).setFontSize(14).setFontColor(ColorConstants.WHITE)
//                .setTextAlignment(TextAlignment.CENTER));
//        celdaTitulo.add(new Paragraph(
//                "Emitido: " + java.time.LocalDateTime.now().format(FORMATO_FECHA_HORA))
//                .setFont(fontRegular).setFontSize(7)
//                .setFontColor(ColorConstants.LIGHT_GRAY)
//                .setTextAlignment(TextAlignment.CENTER));
//        cabecera.addCell(celdaTitulo);

        // Columna derecha: imagen QR
        Cell celdaQr = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setPadding(8);
        Image qrImg = new Image(ImageDataFactory.create(imagenQr))
                .setWidth(70).setHeight(70)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        celdaQr.add(qrImg);
        celdaQr.add(new Paragraph("Verificar autenticidad")
                .setFont(fontRegular).setFontSize(6)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        cabecera.addCell(celdaQr);

        Table tituloTabla = new Table(1)
                .useAllAvailableWidth();

        tituloTabla.addCell(
                new Cell()
                        .add(
                                new Paragraph(titulo)
                                        .setFont(fontBold)
                                        .setFontSize(18)
                                        .setTextAlignment(TextAlignment.CENTER)
                        )
                        .setBackgroundColor(COLOR_PRIMARIO)
                        .setBorder(Border.NO_BORDER)
        );

        doc.add(tituloTabla);

        doc.add(cabecera);
        doc.add(new Paragraph("\n").setFontSize(6));

        // Línea separadora teal
        LineSeparator linea = new LineSeparator(
                new SolidLine(1.5f));
        doc.add(linea);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    private void agregarSeccion(
            Document doc,
            PdfFont fontBold,
            String titulo) {

        Paragraph sec = new Paragraph(titulo)
                .setFont(fontBold)
                .setFontSize(10)
                .setFontColor(COLOR_PRIMARIO)
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorderBottom(
                        new SolidBorder(
                                COLOR_SECUNDARIO,
                                1f))
                .setPaddingBottom(4)
                .setMarginTop(12);

        doc.add(sec);
    }

    private void agregarFilaTabla(Table table, String etiqueta, String valor,
                                  PdfFont fontBold, PdfFont fontRegular) {
        Cell cEtiqueta = new Cell()
                .add(new Paragraph(etiqueta).setFont(fontBold).setFontSize(9)
                        .setFontColor(COLOR_TEXTO_CLARO))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f))
                .setPadding(5);
        Cell cValor = new Cell()
                .add(new Paragraph(valor != null ? valor : "—")
                        .setFont(fontRegular).setFontSize(9))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f))
                .setPadding(5);
        table.addCell(cEtiqueta);
        table.addCell(cValor);
    }

    private Paragraph parrafoContenido(String texto, PdfFont font) {
        return new Paragraph(texto)
                .setFont(font)
                .setFontSize(9)
                .setBackgroundColor(COLOR_FONDO)
                .setPadding(8)
                .setMarginBottom(4);
    }

    private Cell celdaHeader(String texto, PdfFont fontBold) {
        return new Cell()
                .add(new Paragraph(texto).setFont(fontBold).setFontSize(8)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
    }

    private Cell celdaDato(String texto, PdfFont font) {
        return new Cell()
                .add(new Paragraph(texto != null ? texto : "—")
                        .setFont(font).setFontSize(8))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.3f))
                .setPadding(4);
    }

    private void agregarFirma(Document doc, PdfFont fontBold, PdfFont fontRegular) {
        doc.add(new Paragraph("\n\n").setFontSize(8));

        Table tabFirma = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        Cell celdaFirma = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER).setPadding(10);
        celdaFirma.add(new Paragraph("_________________________________")
                .setFont(fontRegular).setFontSize(9).setMarginBottom(4));
        celdaFirma.add(new Paragraph(nombreEspecialista)
                .setFont(fontBold).setFontSize(9));
        celdaFirma.add(new Paragraph(especialidad)
                .setFont(fontRegular).setFontSize(8).setFontColor(COLOR_TEXTO_CLARO));
        tabFirma.addCell(celdaFirma);
        tabFirma.addCell(new Cell().setBorder(Border.NO_BORDER));

        doc.add(tabFirma);
    }

    private void agregarPiePagina(Document doc, PdfFont fontRegular, String codigoQr) {
        doc.add(new LineSeparator(new SolidLine(0.5f)));
        doc.add(new Paragraph(
                "Documento generado por el sistema HClínicas  ·  "
                        + "Código de verificación: " + codigoQr + "  ·  "
                        + "Fecha: " + LocalDate.now().format(FORMATO_FECHA) + "  ·  "
                        + nombreEspecialista)
                .setFont(fontRegular).setFontSize(7)
                .setFontColor(COLOR_TEXTO_CLARO)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4));
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private boolean tieneSignosVitales(Consulta c) {
        return c.getPeso() != null || c.getTalla() != null
                || c.getPresionArterial() != null
                || c.getFrecuenciaCardiaca() != null
                || c.getTemperatura() != null
                || c.getSaturacionOxigeno() != null
                || c.getSemanasGestacion() != null;
    }

    private double calcularImc(double peso, double talla) {
        double m = talla / 100.0;
        return Math.round((peso / (m * m)) * 100.0) / 100.0;
    }

    private String nvl(Integer valor) {
        return valor != null ? valor.toString() : "0";
    }

    private String abreviar(String texto, int max) {
        if (texto == null) return "—";
        return texto.length() > max ? texto.substring(0, max) + "..." : texto;
    }
}