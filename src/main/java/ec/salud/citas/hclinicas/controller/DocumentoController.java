package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.PedidoLaboratorioRequest;
import ec.salud.citas.hclinicas.dto.request.RecetaRequest;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.entity.PedidoLaboratorio;
import ec.salud.citas.hclinicas.entity.Receta;
import ec.salud.citas.hclinicas.service.PdfService;
import ec.salud.citas.hclinicas.service.impl.PedidoLaboratorioServiceImpl;
import ec.salud.citas.hclinicas.service.impl.RecetaServiceImpl;
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
 * Controller de documentos PDF.
 *
 * GET  /api/documentos/consulta/{id}             → PDF consulta
 * GET  /api/documentos/historia/{id}             → PDF historia
 *
 * POST /api/documentos/receta/{id}/guardar       → Guarda receta en BD
 * POST /api/documentos/receta/{id}/pdf           → Genera PDF receta
 * GET  /api/documentos/receta/{id}               → Obtiene última receta
 *
 * POST /api/documentos/pedido/{id}/guardar       → Guarda pedido en BD
 * POST /api/documentos/pedido/{id}/pdf           → Genera PDF pedido
 * GET  /api/documentos/pedido/{id}               → Obtiene último pedido por tipo
 */
@Slf4j
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final PdfService                  pdfService;
    private final RecetaServiceImpl           recetaService;
    private final PedidoLaboratorioServiceImpl pedidoService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── PDF Consulta ──────────────────────────────────────────────────────

    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarConsulta(@PathVariable Long consultaId) {
        log.info("PDF consulta ID: {}", consultaId);
        return pdfResponse(pdfService.generarPdfConsulta(consultaId),
                "consulta_" + consultaId + "_" + hoy() + ".pdf");
    }

    @GetMapping("/historia/{historiaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<byte[]> descargarHistoria(@PathVariable Long historiaId) {
        log.info("PDF historia ID: {}", historiaId);
        return pdfResponse(pdfService.generarPdfHistoria(historiaId),
                "historia_" + historiaId + "_" + hoy() + ".pdf");
    }

    // ── RECETA ────────────────────────────────────────────────────────────

    /**
     * Guarda la receta en base de datos.
     */
    @PostMapping("/receta/{consultaId}/guardar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Long>> guardarReceta(
            @PathVariable Long consultaId,
            @RequestBody RecetaRequest req) {
        log.info("Guardando receta consulta ID: {}", consultaId);
        Receta receta = recetaService.guardarReceta(consultaId, req);
        return ResponseEntity.ok(
                ApiResponse.ok("Receta guardada correctamente", receta.getId()));
    }

    /**
     * Genera el PDF de la receta (puede guardarse o no previamente).
     */
    @PostMapping("/receta/{consultaId}/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<byte[]> generarPdfReceta(
            @PathVariable Long consultaId,
            @RequestBody RecetaRequest req) {
        log.info("Generando PDF receta consulta ID: {}", consultaId);
        byte[] pdf = recetaService.generarPdf(consultaId, req);
        return pdfResponse(pdf, "receta_" + consultaId + "_" + hoy() + ".pdf");
    }

    /**
     * Obtiene la última receta guardada para una consulta.
     */
    @GetMapping("/receta/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<RecetaRequest>> obtenerReceta(
            @PathVariable Long consultaId) {
        RecetaRequest receta = recetaService.obtenerUltimaReceta(consultaId);
        if (receta == null) {
            return ResponseEntity.ok(
                    ApiResponse.<RecetaRequest>ok("Sin receta registrada"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Receta encontrada", receta));
    }

    // ── PEDIDO LABORATORIO / IMAGENOLOGÍA ─────────────────────────────────

    /**
     * Guarda el pedido de laboratorio o imagenología en BD.
     */
    @PostMapping("/pedido/{consultaId}/guardar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Long>> guardarPedido(
            @PathVariable Long consultaId,
            @RequestBody PedidoLaboratorioRequest req) {
        log.info("Guardando pedido {} consulta ID: {}", req.getTipo(), consultaId);
        PedidoLaboratorio pedido = pedidoService.guardarPedido(consultaId, req);
        return ResponseEntity.ok(
                ApiResponse.ok("Pedido guardado correctamente", pedido.getId()));
    }

    /**
     * Genera el PDF del pedido según el tipo (LABORATORIO o IMAGENOLOGIA).
     */
    @PostMapping("/pedido/{consultaId}/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<byte[]> generarPdfPedido(
            @PathVariable Long consultaId,
            @RequestBody PedidoLaboratorioRequest req) {
        log.info("Generando PDF pedido {} consulta ID: {}", req.getTipo(), consultaId);
        byte[] pdf;
        if ("IMAGENOLOGIA".equalsIgnoreCase(req.getTipo())) {
            pdf = pedidoService.generarPdfImagenologia(consultaId, req);
        } else {
            pdf = pedidoService.generarPdfLaboratorio(consultaId, req);
        }
        String prefix = "IMAGENOLOGIA".equalsIgnoreCase(req.getTipo()) ? "imagenologia" : "laboratorio";
        return pdfResponse(pdf, prefix + "_" + consultaId + "_" + hoy() + ".pdf");
    }

    /**
     * Obtiene el último pedido guardado para una consulta por tipo.
     */
    @GetMapping("/pedido/{consultaId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<PedidoLaboratorioRequest>> obtenerPedido(
            @PathVariable Long consultaId,
            @RequestParam(defaultValue = "LABORATORIO") String tipo) {
        PedidoLaboratorioRequest pedido = pedidoService.obtenerUltimoPedido(consultaId, tipo);
        if (pedido == null) {
            return ResponseEntity.ok(
                    ApiResponse.<PedidoLaboratorioRequest>ok("Sin pedido registrado"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Pedido encontrado", pedido));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                .body(pdf);
    }

    private String hoy() {
        return LocalDate.now().format(FMT);
    }
}