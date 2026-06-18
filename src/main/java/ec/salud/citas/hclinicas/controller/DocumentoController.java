package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.service.PdfService;
import ec.salud.citas.hclinicas.service.impl.RecetaPdfServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controlador de generación de documentos PDF.
 *
 * GET  /api/documentos/consulta/{id}    → PDF de la consulta
 * GET  /api/documentos/historia/{id}    → PDF de resumen de historia
 * POST /api/documentos/receta/{id}      → PDF de receta médica
 */
@Slf4j
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final PdfService          pdfService;
    private final RecetaPdfServiceImpl recetaService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── PDF Consulta ──────────────────────────────────────────────────────

    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR'," +
            "'MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarConsulta(
            @PathVariable Long consultaId) {

        log.info("Generando PDF consulta ID: {}", consultaId);
        byte[] pdf = pdfService.generarPdfConsulta(consultaId);

        return pdfResponse(pdf,
                "consulta_" + consultaId + "_" + LocalDate.now().format(FMT) + ".pdf");
    }

    // ── PDF Historia Clínica ──────────────────────────────────────────────

    @GetMapping("/historia/{historiaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR'," +
            "'MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarHistoria(
            @PathVariable Long historiaId) {

        log.info("Generando PDF historia ID: {}", historiaId);
        byte[] pdf = pdfService.generarPdfHistoria(historiaId);

        return pdfResponse(pdf,
                "historia_clinica_" + historiaId + "_" + LocalDate.now().format(FMT) + ".pdf");
    }

    // ── PDF Receta Médica ─────────────────────────────────────────────────

    /**
     * Genera la receta médica en PDF a partir de los medicamentos
     * y prescripción enviados desde el frontend.
     *
     * @param consultaId ID de la consulta asociada a la receta.
     * @param receta     Datos de medicamentos e indicaciones.
     */
    @PostMapping("/receta/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<byte[]> generarReceta(
            @PathVariable Long consultaId,
            @RequestBody RecetaRequest receta) {

        log.info("Generando receta para consulta ID: {}", consultaId);
        byte[] pdf = recetaService.generarRecetaPdf(consultaId, receta);

        return pdfResponse(pdf,
                "receta_" + consultaId + "_" + LocalDate.now().format(FMT) + ".pdf");
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }
}