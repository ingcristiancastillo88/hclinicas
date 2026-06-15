package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.ActualizarConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearConsultaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearHistoriaClinicaRequest;
import ec.salud.citas.hclinicas.dto.response.*;
import ec.salud.citas.hclinicas.service.HistoriaClinicaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints del Sprint 4 — Historias Clínicas y Consultas.
 *
 * Historia:
 *   POST   /api/historias                        → crear o actualizar
 *   GET    /api/historias/{id}                   → por ID
 *   GET    /api/historias/paciente/{pacienteId}  → por paciente
 *
 * Consultas:
 *   GET    /api/historias/{historiaId}/consultas → listar paginado
 *   POST   /api/historias/consultas              → crear
 *   GET    /api/historias/consultas/{id}         → detalle
 *   PUT    /api/historias/consultas/{id}         → actualizar
 *   DELETE /api/historias/consultas/{id}         → eliminar lógico
 *
 * Archivos:
 *   POST   /api/historias/consultas/{id}/archivos → subir
 *   DELETE /api/historias/archivos/{archivoId}    → eliminar
 */
@RestController
@RequestMapping("/historias")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService service;

    // ── Historia Clínica ──────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> crearOActualizar(
            @Valid @RequestBody CrearHistoriaClinicaRequest req,
            HttpServletRequest http) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Historia clínica guardada exitosamente",
                service.crearOActualizar(req, ip(http))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> obtenerPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Historia clínica obtenida", service.obtenerPorId(id)));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<HistoriaClinicaResponse>> obtenerPorPaciente(
            @PathVariable Long pacienteId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Historia clínica obtenida", service.obtenerPorPaciente(pacienteId)));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @GetMapping("/{historiaId}/consultas")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<PageResponse<ConsultaResumenResponse>>> listarConsultas(
            @PathVariable Long historiaId,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Consultas obtenidas",
                service.listarConsultas(historiaId, pagina, tamano)
        ));
    }

    @PostMapping("/consultas")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> crearConsulta(
            @Valid @RequestBody CrearConsultaRequest req,
            HttpServletRequest http) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Consulta registrada exitosamente",
                service.crearConsulta(req, ip(http))
        ));
    }

    @GetMapping("/consultas/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> obtenerConsulta(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Consulta obtenida", service.obtenerConsulta(id)));
    }

    @PutMapping("/consultas/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<ConsultaResponse>> actualizarConsulta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarConsultaRequest req,
            HttpServletRequest http) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Consulta actualizada exitosamente",
                service.actualizarConsulta(id, req, ip(http))
        ));
    }

    @DeleteMapping("/consultas/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> eliminarConsulta(
            @PathVariable Long id, HttpServletRequest http) {
        service.eliminarConsulta(id, ip(http));
        return ResponseEntity.ok(ApiResponse.ok("Consulta eliminada exitosamente"));
    }

    // ── Archivos ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/consultas/{id}/archivos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> subirArchivo(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile file,
            @RequestParam(defaultValue = "OTRO") String tipoArchivo,
            @RequestParam(required = false)      String descripcion,
            HttpServletRequest http) {

        service.subirArchivo(id, file, tipoArchivo, descripcion, ip(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Archivo subido exitosamente"));
    }

    @DeleteMapping("/archivos/{archivoId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> eliminarArchivo(
            @PathVariable Long archivoId, HttpServletRequest http) {
        service.eliminarArchivo(archivoId, ip(http));
        return ResponseEntity.ok(ApiResponse.ok("Archivo eliminado exitosamente"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String ip(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? req.getRemoteAddr() : ip;
    }
}