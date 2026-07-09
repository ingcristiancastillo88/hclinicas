package ec.salud.citas.hclinicas.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import ec.salud.citas.hclinicas.dto.request.PedidoLaboratorioRequest;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.PedidoLaboratorio;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.PedidoLaboratorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoLaboratorioServiceImpl {

    private final ConsultaRepository          consultaRepo;
    private final PedidoLaboratorioRepository pedidoRepo;
    private final ObjectMapper mapper;

    private static final DeviceRgb ROSA        = new DeviceRgb(233, 30, 140);
    private static final DeviceRgb ROSA_CLARO  = new DeviceRgb(252, 228, 236);
    private static final DeviceRgb MORADO      = new DeviceRgb(100, 20, 120);
    private static final DeviceRgb MORADO_OSC  = new DeviceRgb(70, 10, 90);
    private static final DeviceRgb AZUL        = new DeviceRgb(10, 35, 66);
    private static final DeviceRgb GRIS        = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb BLANCO      = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.clinica.especialista:Dra. Alexandra Leon}")
    private String especialista;

    @Value("${app.clinica.telefono:096 044 0040 - 099 146 3226}")
    private String telefono;

    private static final String CORREO   = "draleon_alexandra@hotmail.com";
    private static final String HOSPITAL = "HOSPITAL SAN JUAN";
    private static final String DIR_HOSP = "(Av. Jose Veloz y Sauces)";

    // ── Guardar en BD ─────────────────────────────────────────────────────

    @Transactional
    public PedidoLaboratorio guardarPedido(Long consultaId, PedidoLaboratorioRequest req) {
        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        String exJson;
        try {
            exJson = mapper.writeValueAsString(req.getExamenesSeleccionados());
        } catch (Exception e) {
            exJson = "{}";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null ? auth.getName() : "sistema";

        PedidoLaboratorio pedido = PedidoLaboratorio.builder()
                .consulta(consulta)
                .tipo(req.getTipo())
                .examenesJson(exJson)
                .resumenClinico(req.getResumenClinico())
                .diagnostico(req.getDiagnostico())
                .codigoCie10(req.getCodigoCie10())
                .observaciones(req.getObservaciones())
                .creadoPor(usuario)
                .build();

        return pedidoRepo.save(pedido);
    }

    // ── Obtener pedido existente ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PedidoLaboratorioRequest obtenerUltimoPedido(Long consultaId, String tipo) {
        return pedidoRepo.findByConsultaIdAndTipoOrderByFechaCreacionDesc(consultaId, tipo)
                .stream().findFirst()
                .map(p -> {
                    PedidoLaboratorioRequest req = new PedidoLaboratorioRequest();
                    req.setTipo(p.getTipo());
                    req.setResumenClinico(p.getResumenClinico());
                    req.setDiagnostico(p.getDiagnostico());
                    req.setCodigoCie10(p.getCodigoCie10());
                    req.setObservaciones(p.getObservaciones());
                    try {
                        Map<String, List<String>> examenes = mapper.readValue(
                                p.getExamenesJson(),
                                mapper.getTypeFactory().constructMapType(
                                        Map.class, String.class, List.class));
                        req.setExamenesSeleccionados(examenes);
                    } catch (Exception e) {
                        req.setExamenesSeleccionados(Map.of());
                    }
                    return req;
                }).orElse(null);
    }

    // ── PDF Imagenología ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generarPdfImagenologia(Long consultaId, PedidoLaboratorioRequest req) {
        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(20, 30, 20, 30);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            var pac = consulta.getHistoriaClinica().getPaciente();

            // ── Cabecera ─────────────────────────────────────────────────
            agregarCabeceraComun(doc, bold, regular, italic);

            // ── Título ───────────────────────────────────────────────────
            doc.add(new Paragraph("SOLICITUD DE EXÁMENES DE IMAGENOLOGÍA")
                    .setFont(bold).setFontSize(13)
                    .setFontColor(MORADO_OSC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8).setMarginBottom(10));

            // ── Datos del paciente ────────────────────────────────────────
            Table datPac = new Table(UnitValue.createPercentArray(new float[]{3f, 1f}))
                    .useAllAvailableWidth().setMarginBottom(4);
            datPac.addCell(campoLinea(bold, regular, "Paciente: ",
                    pac.getNombreCompleto(), true));
            datPac.addCell(campoLinea(bold, regular, "Fecha: ",
                    LocalDate.now().format(FMT), false));

            Table datPac2 = new Table(UnitValue.createPercentArray(new float[]{2f, 1.5f}))
                    .useAllAvailableWidth().setMarginBottom(4);
            datPac2.addCell(campoLinea(bold, regular, "C.I.: ",
                    pac.getCedula() != null ? pac.getCedula() : "", true));
            datPac2.addCell(campoLinea(bold, regular, "Edad: ",
                    pac.getFechaNacimiento() != null
                            ? (LocalDate.now().getYear() - pac.getFechaNacimiento().getYear()) + " años"
                            : "", false));

            doc.add(datPac);
            doc.add(datPac2);

            // Campo estudio solicitado
            doc.add(new Table(new float[]{1f}).useAllAvailableWidth()
                    .setMarginBottom(8)
                    .addCell(campoLinea(bold, regular, "ESTUDIO SOLICITADO: ",
                            req.getDescripcion() != null ? req.getDescripcion() : "", true)));

            // ── Checkboxes tipo estudio ───────────────────────────────────
            String[] tiposEstudio = {"RX CONVENCIONAL", "ECOGRAFÍA", "TOMOGRAFÍA", "RMN", "OTROS"};
            Table tabTipos = new Table(UnitValue.createPercentArray(
                    new float[]{1f, 1f, 1f, 1f, 1f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(8);

            Map<String, List<String>> exs = req.getExamenesSeleccionados();
            String tipoSel = exs != null && exs.containsKey("tipoEstudio")
                    ? exs.get("tipoEstudio").stream().findFirst().orElse("") : "";

            DeviceRgb VERDE = new DeviceRgb(22, 163, 74);
            for (String tipo : tiposEstudio) {
                boolean sel = tipo.equals(tipoSel) || tipo.equals(req.getTipoEstudio());
                Cell cell = new Cell()
                        .setBackgroundColor(sel ? VERDE : MORADO)
                        .setBorder(new SolidBorder(BLANCO, 2))
                        .setPadding(4)
                        .setTextAlignment(TextAlignment.CENTER);
                // Prefijo visual: "[X]" cuando seleccionado, "[ ]" cuando no
                String prefijo = sel ? "[X] " : "[ ] ";
                cell.add(new Paragraph(prefijo + tipo)
                        .setFont(bold).setFontSize(7)
                        .setFontColor(BLANCO).setMargin(0));
                tabTipos.addCell(cell);
            }
            doc.add(tabTipos);

            // ── Descripción y motivo ──────────────────────────────────────
            doc.add(campoTextoLibre(regular, "Descripción: ",
                    req.getDescripcion()));
            doc.add(campoTextoLibre(regular, "Motivo de Solicitud: ",
                    req.getMotivoSolicitud()));

            // ── Tabla Resumen Clínico / Diagnóstico / CIE-10 ─────────────
            doc.add(tablaResumenDiagnostico(bold, regular,
                    req.getResumenClinico(), req.getDiagnostico(), req.getCodigoCie10()));

            // ── Monitoreo Fetal ───────────────────────────────────────────
            Table monFetal = new Table(UnitValue.createPercentArray(new float[]{3f, 1f, 1f}))
                    .useAllAvailableWidth().setMarginTop(6).setMarginBottom(6);
            monFetal.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph()
                            .add(new Text("MONITOREO FETAL ELECTRÓNICO  ").setFont(bold).setFontSize(8))
                            .add(new Text(req.getMonitoreoFetal() != null && req.getMonitoreoFetal()
                                    ? "[X] " : "[ ]").setFont(bold).setFontSize(9)
                                    .setFontColor(req.getMonitoreoFetal() != null && req.getMonitoreoFetal()
                                            ? new DeviceRgb(22,163,74) : GRIS))));
            monFetal.addCell(campoLinea(bold, regular, "FUM: ", req.getFum(), false));
            monFetal.addCell(campoLinea(bold, regular, "EG: ", req.getEg(), false));
            doc.add(monFetal);

            // ── Segunda tabla Resumen ─────────────────────────────────────
            doc.add(tablaResumenDiagnostico(bold, regular, "", "", ""));

            // ── Firma ─────────────────────────────────────────────────────
            agregarFirmaYPie(doc, regular, bold);

            doc.close();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando PDF imagenologia: {}", e.getMessage());
            throw new ReglaNegocioException("No se pudo generar el pedido: " + e.getMessage());
        }
    }

    // ── PDF Laboratorio ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generarPdfLaboratorio(Long consultaId, PedidoLaboratorioRequest req) {
        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(20, 25, 20, 25);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            var pac = consulta.getHistoriaClinica().getPaciente();
            Map<String, List<String>> exs = req.getExamenesSeleccionados() != null
                    ? req.getExamenesSeleccionados() : Map.of();

            // ── Cabecera ─────────────────────────────────────────────────
            agregarCabeceraComun(doc, bold, regular, italic);

            // ── Título ───────────────────────────────────────────────────
            doc.add(new Paragraph("SOLICITUD DE EXÁMENES DE LABORATORIO")
                    .setFont(bold).setFontSize(13)
                    .setFontColor(MORADO_OSC)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8).setMarginBottom(8));

            // ── Datos paciente ────────────────────────────────────────────
            Table row1 = new Table(UnitValue.createPercentArray(new float[]{3f, 1f, 1.5f}))
                    .useAllAvailableWidth().setMarginBottom(4);
            row1.addCell(campoLinea(bold, regular, "Paciente: ",
                    pac.getNombreCompleto(), true));
            row1.addCell(campoLinea(bold, regular, "Edad: ",
                    pac.getFechaNacimiento() != null
                            ? (LocalDate.now().getYear() - pac.getFechaNacimiento().getYear()) + ""
                            : "", false));
            row1.addCell(campoLinea(bold, regular, "Fecha: ",
                    LocalDate.now().format(FMT), false));
            doc.add(row1);

            Table row2 = new Table(UnitValue.createPercentArray(new float[]{2.5f, 0.6f, 0.6f, 1.3f}))
                    .useAllAvailableWidth().setMarginBottom(8);
            row2.addCell(campoLinea(bold, regular, "C.I.: ", pac.getCedula() != null ? pac.getCedula() : "", true));
            boolean embarazoSi = req.getEmbarazo() != null && req.getEmbarazo();
            boolean embarazoNo = req.getEmbarazo() != null && !req.getEmbarazo();
            row2.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph()
                            .add(new Text("Embarazo: Si ").setFont(regular).setFontSize(8))
                            .add(new Text(embarazoSi ? "[X] " : "[ ]")
                                    .setFont(bold).setFontSize(8)
                                    .setFontColor(embarazoSi ? new DeviceRgb(22,163,74) : GRIS))));
            row2.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph()
                            .add(new Text("No ").setFont(regular).setFontSize(8))
                            .add(new Text(embarazoNo ? "[X] " : "[ ]")
                                    .setFont(bold).setFontSize(8)
                                    .setFontColor(embarazoNo ? new DeviceRgb(22,163,74) : GRIS))));
            row2.addCell(campoLinea(bold, regular, "Sem/Gestación: ",
                    req.getSemGestacion() != null ? req.getSemGestacion() : "", false));
            doc.add(row2);

            // ── Tres columnas de exámenes ─────────────────────────────────
            Table tabExamenes = new Table(UnitValue.createPercentArray(new float[]{1f, 1f, 1f}))
                    .useAllAvailableWidth();

            // COLUMNA 1
            Cell col1 = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(6);
            agregarGrupoExamen(col1, bold, regular, "HEMATOLOGÍA",
                    List.of("Biometría Hemática", "Hematocrito", "Hemoglobina",
                            "Plaquetas", "Reticulocitos", "VSG."),
                    exs.getOrDefault("hematologia", List.of()));
            agregarGrupoExamen(col1, bold, regular, "INMUNOHEMATOLOGÍA",
                    List.of("Grupo Sanguíneo", "Coombs Directo", "Coombs Indirecto"),
                    exs.getOrDefault("inmunohematologia", List.of()));
            agregarGrupoExamen(col1, bold, regular, "COAGULACIÓN / FIBRINÓLISIS",
                    List.of("TP", "TTP", "INR", "Tiempo de Coagulación",
                            "Tiempo de Sangría", "Anticoagulante Lúpico",
                            "Dímero D.", "Proteína CyS", "Antitrombina III",
                            "Factor de Von-Willebrand"),
                    exs.getOrDefault("coagulacion", List.of()));
            agregarGrupoExamen(col1, bold, regular, "QUÍMICA SANGUÍNEA",
                    List.of("Glucosa Basal", "Glucosa 2H Post Prandial", "Glucosa / Creatinina",
                            "Ácido Úrico", "Colesterol Total", "Triglicéridos",
                            "Bilirrubina Total  Directa  Indirecta",
                            "Proteínas Totales", "Albúmina  Globulina",
                            "Test de Sullivan",
                            "Curva de tolerancia de la glucosa____Hrs.",
                            "Hemoglobina Glicosilada",
                            "Ferritina  Transferrina", "Hierro Sérico"),
                    exs.getOrDefault("quimicaSanguinea", List.of()));
            agregarGrupoExamen(col1, bold, regular, "SEROLOGÍA",
                    List.of("VIH", "VDRL", "FTA-ABS", "Hepatitis B (HBsAg)",
                            "Hepatitis C (Anti HVC)"),
                    exs.getOrDefault("serologia", List.of()));
            tabExamenes.addCell(col1);

            // COLUMNA 2
            Cell col2 = new Cell().setBorder(Border.NO_BORDER)
                    .setBorderLeft(new SolidBorder(ROSA_CLARO, 1f))
                    .setBorderRight(new SolidBorder(ROSA_CLARO, 1f))
                    .setPaddingLeft(6).setPaddingRight(6);
            agregarGrupoExamen(col2, bold, regular, "ELECTROLITOS",
                    List.of("Sodio - Potasio - Cloro", "Calcio Total", "Magnesio"),
                    exs.getOrDefault("electrolitos", List.of()));
            agregarGrupoExamen(col2, bold, regular, "ENZIMAS",
                    List.of("TGO / AST", "TGP / ALT", "Lactato Deshidrogenasa LDH",
                            "Fosfatasa Alcalina", "Fosfatasa Ácida Total",
                            "Amilasa", "Gamma GT"),
                    exs.getOrDefault("enzimas", List.of()));
            agregarGrupoExamen(col2, bold, regular, "REACTANTES DE FASE AGUDA",
                    List.of("PCR Cuantitativo", "Procalcitonina PCT",
                            "Interleucina 6 (IL-6)"),
                    exs.getOrDefault("reactantes", List.of()));
            agregarGrupoExamen(col2, bold, regular, "INMUNOLOGÍA",
                    List.of("Anti Nucleares", "Anti DNA",
                            "Anti Fosfolípidos IgG IgM IgA",
                            "Anti Cardiolipinas IgG IgM IgA",
                            "B2 Glicoproteína IgG IgM IgA"),
                    exs.getOrDefault("inmunologia", List.of()));
            agregarGrupoExamen(col2, bold, regular, "ORINA",
                    List.of("EMO", "Cultivo / Antibiograma", "Gram / Gota fresca",
                            "Microalbumina", "Índice Proteinuria / Creatinuria"),
                    exs.getOrDefault("orina", List.of()));
            agregarGrupoExamen(col2, bold, regular, "HECES",
                    List.of("Coproparasitario", "PMN", "Sangre Oculta", "Rotavirus"),
                    exs.getOrDefault("heces", List.of()));
            agregarGrupoExamen(col2, bold, regular, "MARCADORES TUMORALES",
                    List.of("CA 125", "HE-4", "Alfafetoproteína",
                            "Índice ROMA", "CA 15-3", "CEA", "CA 19-9"),
                    exs.getOrDefault("marcadoresTumorales", List.of()));
            tabExamenes.addCell(col2);

            // COLUMNA 3
            Cell col3 = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(6);
            agregarGrupoExamen(col3, bold, regular, "MICROBIOLOGÍA",
                    List.of("Muestra de: ____", "Gram", "Fresco", "KOH", "Cultivo"),
                    exs.getOrDefault("microbiologia", List.of()));
            agregarGrupoExamen(col3, bold, regular, "HORMONAS",
                    List.of("Luteinizante LH", "FSH", "Prolactina", "Estradiol (E2)",
                            "Progesterona (P4)", "Testosterona Total",
                            "Testosterona Libre", "DHEAS", "Cortisol",
                            "Insulina", "Índice Homa", "Vitamina D",
                            "BHCG Cualitativa", "BHCG Cuantitativa",
                            "TSH", "T3", "FT4",
                            "17 OH-Progesterona", "H. Antimulleriana"),
                    exs.getOrDefault("hormonas", List.of()));
            agregarGrupoExamen(col3, bold, regular, "ESTUDIOS ESPECIALES",
                    List.of("TORCH IgG", "TORCH IgM",
                            "Toxoplasma - Test de avidez",
                            "Clamydia Trachomatis IgG-IgM",
                            "Proteinuria en 24 horas",
                            "Paptest / Citología",
                            "Genotipificación de HPV",
                            "Capacitación Espermática",
                            "Cristalografía", "Screening Prenatal"),
                    exs.getOrDefault("estudiosEspeciales", List.of()));
            // OTROS — líneas en blanco
            col3.add(new Paragraph("OTROS")
                    .setFont(bold).setFontSize(7).setFontColor(BLANCO)
                    .setBackgroundColor(MORADO)
                    .setPadding(3).setMarginBottom(3).setMarginTop(4));
            for (int i = 0; i < 4; i++) {
                col3.add(new Paragraph("☐ _______________________")
                        .setFont(regular).setFontSize(7).setFontColor(GRIS)
                        .setMarginBottom(3));
            }
            tabExamenes.addCell(col3);

            doc.add(tabExamenes);

            // ── Firma y pie ───────────────────────────────────────────────
            agregarFirmaYPie(doc, regular, bold);

            doc.close();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando PDF laboratorio: {}", e.getMessage());
            throw new ReglaNegocioException("No se pudo generar el pedido: " + e.getMessage());
        }
    }

    // ── Helpers PDF ───────────────────────────────────────────────────────

    private void agregarCabeceraComun(Document doc, PdfFont bold,
                                      PdfFont regular, PdfFont italic) {
        Table cab = new Table(UnitValue.createPercentArray(new float[]{1.5f, 4f}))
                .useAllAvailableWidth()
                .setBackgroundColor(ROSA_CLARO)
                .setMarginBottom(0);

        Cell iconoCell = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(8).setVerticalAlignment(VerticalAlignment.MIDDLE);
        iconoCell.add(new Paragraph("♀")
                .setFont(bold).setFontSize(36)
                .setFontColor(ROSA).setTextAlignment(TextAlignment.CENTER).setMargin(0));
        cab.addCell(iconoCell);

        Cell nombreCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(10).setPaddingBottom(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        nombreCell.add(new Paragraph(especialista)
                .setFont(bold).setFontSize(18)
                .setFontColor(MORADO).setItalic().setMarginBottom(2));
        nombreCell.add(new Paragraph("ESPECIALISTA EN GINECOLOGÍA Y OBSTETRICIA")
                .setFont(bold).setFontSize(8).setFontColor(MORADO).setMarginBottom(0));
        cab.addCell(nombreCell);

        doc.add(cab);

        // Línea morada bajo cabecera
        doc.add(new Table(1).useAllAvailableWidth()
                .addCell(new Cell().setBorder(Border.NO_BORDER)
                        .setBackgroundColor(MORADO).setHeight(3)));
    }

    private void agregarGrupoExamen(Cell container, PdfFont bold, PdfFont regular,
                                    String titulo, List<String> todos, List<String> seleccionados) {
        container.add(new Paragraph(titulo)
                .setFont(bold).setFontSize(7).setFontColor(BLANCO)
                .setBackgroundColor(MORADO)
                .setPadding(3).setMarginBottom(2).setMarginTop(4));

        DeviceRgb VERDE = new DeviceRgb(22, 163, 74);

        for (String examen : todos) {
            boolean sel = seleccionados.contains(examen);

            // Usamos una mini-tabla de 2 columnas: [checkbox | texto]
            // Esto evita el problema de caracteres Unicode que Helvetica no renderiza
            Table fila = new Table(UnitValue.createPercentArray(new float[]{0.5f, 9.5f}))
                    .useAllAvailableWidth()
                    .setMarginBottom(2).setMarginLeft(1);

            if (sel) {
                // Checkbox marcado: celda verde rellena con "v" en blanco
                Cell chk = new Cell()
                        .setBorder(new SolidBorder(VERDE, 0.8f))
                        .setBackgroundColor(VERDE)
                        .setPadding(0).setPaddingLeft(1)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                chk.add(new Paragraph("v")
                        .setFont(bold).setFontSize(6)
                        .setFontColor(BLANCO)
                        .setMargin(0).setTextAlignment(TextAlignment.CENTER));
                fila.addCell(chk);

                Cell txt = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(0)
                        .setPaddingLeft(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                txt.add(new Paragraph(examen)
                        .setFont(bold).setFontSize(7.5f)
                        .setFontColor(MORADO).setMargin(0));
                fila.addCell(txt);
            } else {
                // Checkbox vacío: solo borde, sin relleno
                Cell chk = new Cell()
                        .setBorder(new SolidBorder(GRIS, 0.8f))
                        .setPadding(0)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                chk.add(new Paragraph(" ")
                        .setFont(regular).setFontSize(6).setMargin(0));
                fila.addCell(chk);

                Cell txt = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(0)
                        .setPaddingLeft(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
                txt.add(new Paragraph(examen)
                        .setFont(regular).setFontSize(7)
                        .setFontColor(AZUL).setMargin(0));
                fila.addCell(txt);
            }

            container.add(fila);
        }
    }

    private Cell campoLinea(PdfFont bold, PdfFont regular,
                            String etiqueta, String valor, boolean fullWidth) {
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS, 0.5f))
                .setPaddingBottom(2).setPaddingTop(4);
        c.add(new Paragraph()
                .add(new Text(etiqueta).setFont(bold).setFontSize(8).setFontColor(GRIS))
                .add(new Text(valor).setFont(regular).setFontSize(9).setFontColor(AZUL))
                .setMargin(0));
        return c;
    }

    private Paragraph campoTextoLibre(PdfFont regular, String etiqueta, String valor) {
        return new Paragraph()
                .add(new Text(etiqueta).setFont(regular).setFontSize(8).setFontColor(GRIS))
                .add(new Text(valor != null ? valor : "").setFont(regular).setFontSize(8)
                        .setFontColor(AZUL))
                .setMarginBottom(4);
    }

    private Table tablaResumenDiagnostico(PdfFont bold, PdfFont regular,
                                          String resumen, String diagnostico, String cie10) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{3f, 2f, 1f}))
                .useAllAvailableWidth().setMarginBottom(4);

        // Encabezados
        t.addHeaderCell(headerCell(bold, "RESUMEN CLÍNICO"));
        t.addHeaderCell(headerCell(bold, "DIAGNÓSTICO"));
        t.addHeaderCell(headerCell(bold, "CIE-10"));

        // Filas de contenido (5 filas)
        String[] resLines = splitLines(resumen, 5);
        String[] diagLines = splitLines(diagnostico, 5);
        String[] cieLines = splitLines(cie10, 5);

        for (int i = 0; i < 5; i++) {
            t.addCell(lineaTabla(regular, resLines[i]));
            t.addCell(lineaTabla(regular, diagLines[i]));
            t.addCell(lineaTabla(regular, cieLines[i]));
        }
        return t;
    }

    private Cell headerCell(PdfFont bold, String texto) {
        return new Cell()
                .setBackgroundColor(ROSA)
                .setBorder(new SolidBorder(BLANCO, 1))
                .setPadding(4)
                .add(new Paragraph(texto)
                        .setFont(bold).setFontSize(7.5f)
                        .setFontColor(BLANCO)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0));
    }

    private Cell lineaTabla(PdfFont regular, String texto) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ROSA_CLARO, 0.5f))
                .setPadding(3)
                .add(new Paragraph(texto != null ? texto : "")
                        .setFont(regular).setFontSize(8).setFontColor(AZUL).setMargin(0));
    }

    private void agregarFirmaYPie(Document doc, PdfFont regular, PdfFont bold) {
        doc.add(new Paragraph("\n").setFontSize(6));
        doc.add(new Paragraph("____________________________")
                .setFont(regular).setFontSize(9).setFontColor(GRIS)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Firma / Médico")
                .setFont(regular).setFontSize(8).setFontColor(GRIS)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));

        // Pie de página
        Table pie = new Table(UnitValue.createPercentArray(new float[]{1f, 1f, 1f}))
                .useAllAvailableWidth()
                .setBackgroundColor(ROSA_CLARO);

        pie.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(5)
                .add(new Paragraph("☏ " + telefono)
                        .setFont(regular).setFontSize(7).setFontColor(MORADO)
                        .setTextAlignment(TextAlignment.CENTER).setMargin(0)));
        pie.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(5)
                .add(new Paragraph("✉ " + CORREO)
                        .setFont(regular).setFontSize(7).setFontColor(MORADO)
                        .setTextAlignment(TextAlignment.CENTER).setMargin(0)));
        pie.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(5)
                .add(new Paragraph("✦ " + HOSPITAL + " " + DIR_HOSP)
                        .setFont(regular).setFontSize(7).setFontColor(MORADO)
                        .setTextAlignment(TextAlignment.CENTER).setMargin(0)));
        doc.add(pie);
    }

    private String[] splitLines(String texto, int count) {
        String[] result = new String[count];
        if (texto == null || texto.isBlank()) {
            for (int i = 0; i < count; i++) result[i] = "";
            return result;
        }
        String[] parts = texto.split("\n");
        for (int i = 0; i < count; i++) {
            result[i] = i < parts.length ? parts[i] : "";
        }
        return result;
    }
}