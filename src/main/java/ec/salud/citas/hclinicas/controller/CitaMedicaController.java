package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.ActualizarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CancelarCitaRequest;
import ec.salud.citas.hclinicas.dto.request.CrearCitaRequest;
import ec.salud.citas.hclinicas.dto.response.*;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.service.CitaMedicaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador de Citas Médicas — Sprint 5.
 *
 * POST   /api/citas                              → crear cita
 * GET    /api/citas                              → listar paginado con filtros
 * GET    /api/citas/{id}                         → detalle
 * PUT    /api/citas/{id}                         → actualizar
 * PATCH  /api/citas/{id}/cancelar               → cancelar
 * PATCH  /api/citas/{id}/atendida               → marcar atendida
 * PATCH  /api/citas/{id}/no-asistio             → marcar no asistió
 *
 * GET    /api/citas/calendario?inicio=&fin=      → rango de fechas (calendario)
 * GET    /api/citas/dia?fecha=                   → citas de un día
 * GET    /api/citas/disponibilidad?fecha=&hora=&duracion= → verificar slot
 * GET    /api/citas/paciente/{id}               → historial de citas del paciente
 */
@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaMedicaController {

    private final CitaMedicaService citaService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<CitaMedicaResponse>> crear(
            @Valid @RequestBody CrearCitaRequest request,
            HttpServletRequest http) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Cita agendada exitosamente",
                citaService.crear(request, ip(http))
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PageResponse<CitaMedicaResumenResponse>>> listar(
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "")  String busqueda,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Citas obtenidas",
                citaService.listar(estado, fecha, busqueda, pagina, tamano)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<CitaMedicaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cita obtenida", citaService.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<CitaMedicaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCitaRequest request,
            HttpServletRequest http) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Cita actualizada exitosamente",
                citaService.actualizar(id, request, ip(http))
        ));
    }

    // ── Cambios de estado ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarCitaRequest request,
            HttpServletRequest http) {

        citaService.cancelar(id, request, ip(http));
        return ResponseEntity.ok(ApiResponse.ok("Cita cancelada exitosamente"));
    }

    @PatchMapping("/{id}/atendida")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> marcarAtendida(
            @PathVariable Long id, HttpServletRequest http) {

        citaService.marcarAtendida(id, ip(http));
        return ResponseEntity.ok(ApiResponse.ok("Cita marcada como atendida"));
    }

    @PatchMapping("/{id}/no-asistio")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<Void>> marcarNoAsistio(
            @PathVariable Long id, HttpServletRequest http) {

        citaService.marcarNoAsistio(id, ip(http));
        return ResponseEntity.ok(ApiResponse.ok("Cita marcada como no asistió"));
    }

    // ── Calendario (HU-019) ───────────────────────────────────────────────────

    @GetMapping("/calendario")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<CitaMedicaResumenResponse>>> calendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Citas del período obtenidas",
                citaService.obtenerPorRangoFechas(inicio, fin)
        ));
    }

    @GetMapping("/dia")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<CitaMedicaResumenResponse>>> porDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Citas del día obtenidas",
                citaService.obtenerPorFecha(fecha)
        ));
    }

    // ── Disponibilidad (HU-017) ───────────────────────────────────────────────

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<DisponibilidadResponse>> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam String hora,
            @RequestParam(defaultValue = "30") int duracion) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Disponibilidad verificada",
                citaService.verificarDisponibilidad(fecha, hora, duracion)
        ));
    }

    // ── Historial del paciente (HU-020) ───────────────────────────────────────

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA','PACIENTE')")
    public ResponseEntity<ApiResponse<PageResponse<CitaMedicaResumenResponse>>> porPaciente(
            @PathVariable Long pacienteId,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Historial de citas obtenido",
                citaService.citasPorPaciente(pacienteId, pagina, tamano)
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String ip(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? req.getRemoteAddr() : ip;
    }
}