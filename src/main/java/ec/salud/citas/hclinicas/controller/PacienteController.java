package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.ActualizarPacienteRequest;
import ec.salud.citas.hclinicas.dto.CrearPacienteRequest;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.dto.PacienteResumenResponse;
import ec.salud.citas.hclinicas.dto.PacienteResponse;
import ec.salud.citas.hclinicas.service.PacienteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de gestión de pacientes (HU-007, HU-008, HU-009 / CU-003).
 *
 * GET    /api/pacientes                    → listado paginado con búsqueda
 * GET    /api/pacientes/{id}               → detalle completo por ID
 * GET    /api/pacientes/cedula/{cedula}    → búsqueda directa por cédula
 * POST   /api/pacientes                    → crear paciente
 * PUT    /api/pacientes/{id}               → actualizar paciente
 * PATCH  /api/pacientes/{id}/desactivar   → desactivar (eliminación lógica)
 * PATCH  /api/pacientes/{id}/activar      → activar paciente
 */
@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    // ── GET /pacientes ────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PageResponse<PacienteResumenResponse>>> listar(
            @RequestParam(defaultValue = "")     String busqueda,
            @RequestParam(defaultValue = "0")    int pagina,
            @RequestParam(defaultValue = "10")   int tamano,
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Pacientes obtenidos exitosamente",
                pacienteService.listar(busqueda, pagina, tamano, soloActivos)
        ));
    }

    // ── GET /pacientes/{id} ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PacienteResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Paciente obtenido exitosamente",
                pacienteService.obtenerPorId(id)
        ));
    }

    // ── GET /pacientes/cedula/{cedula} ────────────────────────────────────────
    @GetMapping("/cedula/{cedula}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PacienteResponse>> obtenerPorCedula(
            @PathVariable String cedula) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Paciente encontrado",
                pacienteService.obtenerPorCedula(cedula)
        ));
    }

    // ── POST /pacientes ───────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PacienteResponse>> crear(
            @Valid @RequestBody CrearPacienteRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Paciente creado exitosamente",
                pacienteService.crear(request, getIp(httpRequest))
        ));
    }

    // ── PUT /pacientes/{id} ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PacienteResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPacienteRequest request,
            HttpServletRequest httpRequest) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Paciente actualizado exitosamente",
                pacienteService.actualizar(id, request, getIp(httpRequest))
        ));
    }

    // ── PATCH /pacientes/{id}/desactivar ──────────────────────────────────────
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Long id, HttpServletRequest httpRequest) {

        pacienteService.desactivar(id, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Paciente desactivado exitosamente"));
    }

    // ── PATCH /pacientes/{id}/activar ─────────────────────────────────────────
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> activar(
            @PathVariable Long id, HttpServletRequest httpRequest) {

        pacienteService.activar(id, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Paciente activado exitosamente"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip;
    }
}