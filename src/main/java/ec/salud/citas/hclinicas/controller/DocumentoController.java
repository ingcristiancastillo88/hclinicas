package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.service.PdfService;
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
 * Controlador de generación de documentos PDF — Sprint 6.
 *
 * GET /api/documentos/consulta/{consultaId}     → PDF de la consulta
 * GET /api/documentos/historia/{historiaId}     → PDF de resumen de historia
 */
@Slf4j
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final PdfService pdfService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Genera y descarga el PDF de una consulta médica específica.
     * El PDF incluye todos los datos clínicos y el código QR de verificación.
     */
    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarConsulta(@PathVariable Long consultaId) {

        log.info("Generando PDF consulta ID: {}", consultaId);
        byte[] pdf = pdfService.generarPdfConsulta(consultaId);

        String nombreArchivo = "consulta_" + consultaId + "_"
                + LocalDate.now().format(FMT) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }

    /**
     * Genera y descarga el PDF de resumen de historia clínica.
     * Incluye antecedentes gineco-obstétricos e historial de consultas.
     */
    @GetMapping("/historia/{historiaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarHistoria(@PathVariable Long historiaId) {

        log.info("Generando PDF historia clínica ID: {}", historiaId);
        byte[] pdf = pdfService.generarPdfHistoria(historiaId);

        String nombreArchivo = "historia_clinica_" + historiaId + "_"
                + LocalDate.now().format(FMT) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombreArchivo + "\"")
                .header(HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }
}