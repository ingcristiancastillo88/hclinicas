package ec.salud.citas.hclinicas.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
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
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest.MedicamentoRequest;
import ec.salud.citas.hclinicas.entity.Consulta;
import ec.salud.citas.hclinicas.entity.Receta;
import ec.salud.citas.hclinicas.exception.RecursoNoEncontradoException;
import ec.salud.citas.hclinicas.exception.ReglaNegocioException;
import ec.salud.citas.hclinicas.repository.ConsultaRepository;
import ec.salud.citas.hclinicas.repository.RecetaRepository;
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

/**
 * Genera la receta médica en formato PDF idéntico al recetario físico:
 * - Hoja A5 horizontal (landscape)
 * - DOS columnas simétricas separadas por línea central
 * - Cada columna: cabecera con logo + nombre + especialidad + servicios
 * - Columna izquierda: ciudad/fecha/edad, Paciente, Rp: (medicamentos)
 * - Columna derecha: ciudad/fecha, INDICACIONES
 * - Pie: teléfono · correo · Hospital San Juan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecetaServiceImpl {

    private final ConsultaRepository consultaRepo;
    private final RecetaRepository recetaRepo;
    private final ObjectMapper mapper;

    // ── Colores institucionales ───────────────────────────────────────────
    private static final DeviceRgb ROSA = new DeviceRgb(233, 30, 140);
    private static final DeviceRgb ROSA_CLARO = new DeviceRgb(252, 228, 236);
    private static final DeviceRgb AZUL = new DeviceRgb(10, 35, 66);
    private static final DeviceRgb GRIS = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb MORADO = new DeviceRgb(100, 20, 120);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.clinica.nombre:Consultorio Gineco-Obstetrico}")
    private String clinicaNombre;

    @Value("${app.clinica.especialista:Dra. Alexandra Leon}")
    private String especialista;

    @Value("${app.clinica.telefono:096 044 0040 - 099 146 3226}")
    private String telefono;

    private static final String CORREO = "draleon_alexandra@hotmail.com";
    private static final String HOSPITAL = "HOSPITAL SAN JUAN";
    private static final String DIR_HOSP = "(Av. Jose Veloz y Sauces)";

    private static final String SERVICIOS =
            "Atención de Embarazo normal y de Alto Riesgo Obstétrico - Ecografía Obstétrica y\n" +
                    "Ginecológica - Partos y Cesáreas - Planificación Familiar - Cirugía Ginecológica - Colposcopia\n" +
                    "- Climaterio y Menopausia - Infertilidad - Tamizaje del Cáncer - Cervicouterino y de mama.";

    // ── Guardar en BD ─────────────────────────────────────────────────────

    @Transactional
    public Receta guardarReceta(Long consultaId, RecetaRequest req) {
        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        String medJson;
        try {
            medJson = mapper.writeValueAsString(req.getMedicamentos());
        } catch (Exception e) {
            medJson = "[]";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null ? auth.getName() : "sistema";

        Receta receta = Receta.builder()
                .consulta(consulta)
                .prescripcion(req.getPrescripcion())
                .proximaCita(req.getProximaCita())
                .medicamentosJson(medJson)
                .creadoPor(usuario)
                .build();

        return recetaRepo.save(receta);
    }

    // ── Obtener receta guardada ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public RecetaRequest obtenerUltimaReceta(Long consultaId) {
        return recetaRepo.findTopByConsultaIdOrderByFechaCreacionDesc(consultaId)
                .map(r -> {
                    RecetaRequest req = new RecetaRequest();
                    req.setPrescripcion(r.getPrescripcion());
                    req.setProximaCita(r.getProximaCita());
                    try {
                        List<MedicamentoRequest> meds = mapper.readValue(
                                r.getMedicamentosJson(),
                                mapper.getTypeFactory().constructCollectionType(
                                        List.class, MedicamentoRequest.class));
                        req.setMedicamentos(meds);
                    } catch (Exception e) {
                        req.setMedicamentos(List.of());
                    }
                    return req;
                })
                .orElse(null);
    }

    // ── Generar PDF ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generarPdf(Long consultaId, RecetaRequest req) {
        Consulta consulta = consultaRepo.findById(consultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta no encontrada: " + consultaId));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));

            // A5 landscape = 595 x 420 pt
            PageSize a5h = new PageSize(595, 420);
            Document doc = new Document(pdf, a5h);
            doc.setMargins(0, 0, 0, 0);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            var pac = consulta.getHistoriaClinica().getPaciente();
            String nombrePac = pac.getNombreCompleto();
            String edadPac = pac.getFechaNacimiento() != null
                    ? (LocalDate.now().getYear() - pac.getFechaNacimiento().getYear()) + " años"
                    : "";
            String fechaHoy = LocalDate.now().format(FMT);

            // ── Tabla principal: 2 columnas iguales ──────────────────────
            Table tabla = new Table(UnitValue.createPercentArray(new float[]{1f, 1f}))
                    .useAllAvailableWidth()
                    .setHeight(420);

            // ── COLUMNA IZQUIERDA ────────────────────────────────────────
            Cell colIzq = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBorderRight(new SolidBorder(ROSA_CLARO, 1.5f))
                    .setPadding(0);

            colIzq.add(cabecera(bold, regular, italic));
            colIzq.add(lineaSeparadora());
            colIzq.add(filaCiudadFechaEdad(regular, bold, "Riobamba", fechaHoy, edadPac));
            colIzq.add(filaPaciente(regular, bold, nombrePac));
            colIzq.add(seccionRp(regular, bold, italic, req.getMedicamentos()));
            colIzq.add(pieColumna(regular));

            // ── COLUMNA DERECHA ──────────────────────────────────────────
            Cell colDer = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0);

            colDer.add(cabecera(bold, regular, italic));
            colDer.add(lineaSeparadora());
            colDer.add(filaCiudadFechaCorta(regular, bold, "Riobamba", fechaHoy));
            colDer.add(seccionIndicaciones(regular, bold, req.getPrescripcion(), req.getMedicamentos()));
            colDer.add(firmaYProximaCita(regular, bold, req.getProximaCita()));
            colDer.add(pieColumna(regular));

            tabla.addCell(colIzq);
            tabla.addCell(colDer);
            doc.add(tabla);
            doc.close();

            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando receta PDF: {}", e.getMessage());
            throw new ReglaNegocioException("No se pudo generar la receta: " + e.getMessage());
        }
    }

    // ── Secciones del PDF ─────────────────────────────────────────────────

    private Table cabecera(PdfFont bold, PdfFont regular, PdfFont italic) throws IOException {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1.2f, 3f}))
                .useAllAvailableWidth()
                .setBackgroundColor(ROSA_CLARO);

        // Logo PNG del consultorio
        Cell iconoCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(8).setPaddingTop(6).setPaddingBottom(6)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            byte[] logoBytes = getClass()
                    .getResourceAsStream("/imagenes/logo-dra.png").readAllBytes();
            Image logo = new Image(ImageDataFactory.create(logoBytes))
                    .setWidth(64).setHeight(64)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            iconoCell.add(logo);
        } catch (Exception e) {
            iconoCell.add(new Paragraph("♀")
                    .setFont(bold).setFontSize(28)
                    .setFontColor(ROSA)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMargin(0));
        }
        t.addCell(iconoCell);

        // Nombre y especialidad
        Cell nombreCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(8).setPaddingBottom(4).setPaddingRight(8)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        nombreCell.add(new Paragraph(especialista)
                .setFont(bold).setFontSize(13)
                .setFontColor(MORADO)
                .setItalic()
                .setMarginBottom(1));
        nombreCell.add(new Paragraph("ESPECIALISTA EN GINECOLOGÍA Y OBSTETRICIA")
                .setFont(bold).setFontSize(6.5f)
                .setFontColor(MORADO)
                .setMarginBottom(3));
        nombreCell.add(new Paragraph(SERVICIOS)
                .setFont(regular).setFontSize(5.5f)
                .setFontColor(GRIS)
                .setMargin(0));
        t.addCell(nombreCell);

        return t;
    }

    private Table lineaSeparadora() {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ROSA, 1.5f))
                .setHeight(4);
        t.addCell(c);
        return t;
    }

    private Table filaCiudadFechaEdad(PdfFont regular, PdfFont bold,
                                      String ciudad, String fecha, String edad) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setPaddingBottom(2);

        // Línea punteada: ciudad/fecha/edad
        Paragraph p = new Paragraph()
                .setFont(regular).setFontSize(7.5f).setFontColor(GRIS)
                .add(new Text("Riobamba").setFont(bold).setFontColor(AZUL))
                .add(new Text("  ,a  ").setFontColor(GRIS))
                .add(new Text(ciudad).setFont(bold).setFontColor(AZUL))
                .add(new Text("  de  ").setFontColor(GRIS))
                .add(new Text(fecha).setFont(bold).setFontColor(AZUL))
                .add(new Text("  Edad: ").setFontColor(GRIS))
                .add(new Text(edad).setFont(bold).setFontColor(AZUL));
        c.add(p);
        t.addCell(c);
        return t;
    }

    private Table filaCiudadFechaCorta(PdfFont regular, PdfFont bold,
                                       String ciudad, String fecha) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setPaddingBottom(2);
        Paragraph p = new Paragraph()
                .setFont(regular).setFontSize(7.5f)
                .add(new Text("Riobamba").setFont(bold).setFontColor(AZUL))
                .add(new Text("  ,a  ").setFontColor(GRIS))
                .add(new Text(ciudad).setFont(bold).setFontColor(AZUL))
                .add(new Text("  de  ").setFontColor(GRIS))
                .add(new Text(fecha).setFont(bold).setFontColor(AZUL));
        c.add(p);
        t.addCell(c);
        return t;
    }

    private Table filaPaciente(PdfFont regular, PdfFont bold, String nombrePac) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(8).setPaddingRight(8).setPaddingBottom(4)
                .setBorderBottom(new SolidBorder(ROSA_CLARO, 0.5f));
        c.add(new Paragraph()
                .add(new Text("Paciente:  ").setFont(bold).setFontSize(8).setFontColor(AZUL))
                .add(new Text(nombrePac).setFont(regular).setFontSize(9).setFontColor(AZUL))
                .setMargin(0));
        t.addCell(c);
        return t;
    }

    private Table seccionRp(PdfFont regular, PdfFont bold, PdfFont italic,
                            List<MedicamentoRequest> meds) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(10).setPaddingRight(8).setPaddingTop(6);

        // "Rp:" en cursiva grande
        c.add(new Paragraph("Rp:")
                .setFont(italic).setFontSize(16).setFontColor(ROSA)
                .setMarginBottom(4));

        if (meds != null) {
            for (MedicamentoRequest med : meds) {

                // ── Nombre genérico en negrita ─────────────────────────────
                // Ej: "Ibuprofeno 400 mg"
                String generico = med.getNombreGenerico();
                if (generico != null && !generico.isBlank()) {
                    c.add(new Paragraph("- " + generico)
                            .setFont(bold).setFontSize(9).setFontColor(AZUL)
                            .setMarginBottom(1).setMarginLeft(2));
                }

                // ── Nombre comercial subrayado (si existe) ─────────────────
                // Ej: "BUPREX FLASH"
                String comercial = med.getNombreComercial();
                if (comercial != null && !comercial.isBlank()) {
                    c.add(new Paragraph(comercial.toUpperCase())
                            .setFont(bold).setFontSize(8.5f).setFontColor(AZUL)
                            .setUnderline(0.8f, -1.5f)
                            .setMarginBottom(1).setMarginLeft(8));
                }

                // ── Presentación y cantidad ────────────────────────────────
                // Ej: "Tabletas #10 (diez)"
                String presentacion = med.getPresentacion();
                if (presentacion != null && !presentacion.isBlank()) {
                    c.add(new Paragraph(presentacion)
                            .setFont(regular).setFontSize(8).setFontColor(GRIS)
                            .setMarginBottom(4).setMarginLeft(8));
                }

                // Línea separadora sutil entre medicamentos
                if (meds.indexOf(med) < meds.size() - 1) {
                    c.add(new Paragraph(" ").setMarginBottom(2));
                }
            }
        }

        t.addCell(c);
        return t;
    }

    private Table seccionIndicaciones(PdfFont regular, PdfFont bold,
                                      String prescripcion,
                                      List<MedicamentoRequest> meds) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(10).setPaddingRight(8).setPaddingTop(6);

        c.add(new Paragraph("INDICACIONES:")
                .setFont(bold).setFontSize(12).setFontColor(AZUL)
                .setMarginBottom(6));

        // ── Indicaciones por medicamento ──────────────────────────────────
        // Ej: "- Ibuprofeno 400 mg (BUPREX FLASH): 1 tab c/8h por 3 días"
        if (meds != null) {
            for (MedicamentoRequest med : meds) {
                if (med.getIndicaciones() == null || med.getIndicaciones().isBlank()) continue;

                String generico = med.getNombreGenerico() != null ? med.getNombreGenerico() : "";
                String comercial = med.getNombreComercial() != null
                        ? " (" + med.getNombreComercial().toUpperCase() + ")" : "";

                // Línea de cabecera del medicamento
                c.add(new Paragraph("- " + generico + comercial + ":")
                        .setFont(bold).setFontSize(8f).setFontColor(AZUL)
                        .setMarginBottom(1).setMarginLeft(2));

                // Indicación propiamente
                c.add(new Paragraph(med.getIndicaciones())
                        .setFont(regular).setFontSize(8f).setFontColor(AZUL)
                        .setMarginBottom(5).setMarginLeft(8));
            }
        }

        // ── Prescripción / Recomendaciones generales ──────────────────────
        if (prescripcion != null && !prescripcion.isBlank()) {
            if (meds != null && !meds.isEmpty()) {
                c.add(new Paragraph("Recomendaciones:")
                        .setFont(bold).setFontSize(8f).setFontColor(ROSA)
                        .setMarginTop(4).setMarginBottom(2));
            }
            for (String linea : prescripcion.split("\n")) {
                c.add(new Paragraph(linea)
                        .setFont(regular).setFontSize(8f).setFontColor(AZUL)
                        .setMarginBottom(3).setMarginLeft(4));
            }
        }

        t.addCell(c);
        return t;
    }

    private Table firmaYProximaCita(PdfFont regular, PdfFont bold, String proximaCita) {
        Table t = new Table(1).useAllAvailableWidth();
        Cell c = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingLeft(10).setPaddingRight(10).setPaddingBottom(4);

        // Próxima cita
        if (proximaCita != null && !proximaCita.isBlank()) {
            c.add(new Paragraph()
                    .add(new Text("PRÓXIMA CITA: ").setFont(bold).setFontSize(7)
                            .setFontColor(ROSA))
                    .add(new Text(proximaCita).setFont(regular).setFontSize(7)
                            .setFontColor(AZUL))
                    .setMarginBottom(10));
        }

        // Línea de firma
        c.add(new Paragraph("____________________________")
                .setFont(regular).setFontSize(8).setFontColor(GRIS)
                .setMarginBottom(2));
        c.add(new Paragraph("Firma / Médico")
                .setFont(regular).setFontSize(7).setFontColor(GRIS)
                .setMarginBottom(0));

        t.addCell(c);
        return t;
    }

    private Table pieColumna(PdfFont regular) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1f, 1f, 1f}))
                .useAllAvailableWidth()
                .setBackgroundColor(ROSA_CLARO);

        String[] items = {
                "☏ " + telefono,
                "✉ " + CORREO,
                "✦ " + HOSPITAL + " " + DIR_HOSP
        };

        for (String item : items) {
            t.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPadding(4)
                    .add(new Paragraph(item)
                            .setFont(regular).setFontSize(5.5f)
                            .setFontColor(MORADO)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMargin(0)));
        }
        return t;
    }
}