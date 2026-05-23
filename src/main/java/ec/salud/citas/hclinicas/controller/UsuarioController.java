package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.*;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de gestión de usuarios (HU-004, HU-005).
 * Acceso: SUPERADMINISTRADOR y ADMINISTRADOR.
 *
 * GET    /api/usuarios              → listar con búsqueda paginada
 * GET    /api/usuarios/{id}         → obtener por ID
 * POST   /api/usuarios              → crear usuario
 * PUT    /api/usuarios/{id}         → actualizar usuario
 * PATCH  /api/usuarios/{id}/desactivar → desactivar usuario
 * PATCH  /api/usuarios/{id}/activar    → activar usuario
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ── GET /usuarios ─────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> listar(
            @RequestParam(required = false, defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return ResponseEntity.ok(
                ApiResponse.ok("Usuarios obtenidos exitosamente",
                        usuarioService.listar(busqueda, pagina, tamano))
        );
    }

    // ── GET /usuarios/{id} ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario obtenido exitosamente",
                        usuarioService.obtenerPorId(id))
        );
    }

    // ── POST /usuarios ────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(
            @Valid @RequestBody CrearUsuarioRequest request,
            HttpServletRequest httpRequest) {

        UsuarioResponse response = usuarioService.crear(request, getIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente", response));
    }

    // ── PUT /usuarios/{id} ────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request,
            HttpServletRequest httpRequest) {

        UsuarioResponse response = usuarioService.actualizar(id, request, getIp(httpRequest));
        return ResponseEntity.ok(
                ApiResponse.ok("Usuario actualizado exitosamente", response));
    }

    // ── PATCH /usuarios/{id}/desactivar ───────────────────────────────────────
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> desactivar(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        usuarioService.desactivar(id, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Usuario desactivado exitosamente"));
    }

    // ── PATCH /usuarios/{id}/activar ──────────────────────────────────────────
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<Void>> activar(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        usuarioService.activar(id, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Usuario activado exitosamente"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip;
    }
}
