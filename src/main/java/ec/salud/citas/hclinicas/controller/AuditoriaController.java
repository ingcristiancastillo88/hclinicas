package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.PageResponse;
import ec.salud.citas.hclinicas.entity.Auditoria;
import ec.salud.citas.hclinicas.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de Auditoría (HU-006 / Sprint 2).
 * Solo lectura — ningún endpoint permite modificar registros de auditoría.
 *
 * GET /api/auditoria                          → listar con paginación
 * GET /api/auditoria/usuario/{correo}         → filtrar por usuario
 * GET /api/auditoria/modulo/{modulo}          → filtrar por módulo
 */
@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    // ── GET /auditoria ────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PageResponse<Auditoria>>> listar(
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        PageRequest pageRequest = PageRequest.of(
                pagina, tamano,
                Sort.by("fechaAccion").descending()
        );

        Page<Auditoria> page = auditoriaRepository.findAll(pageRequest);

        return ResponseEntity.ok(
                ApiResponse.ok("Registros de auditoría obtenidos",
                        PageResponse.of(page))
        );
    }

    // ── GET /auditoria/usuario/{correo} ───────────────────────────────────────
    @GetMapping("/usuario/{correo}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PageResponse<Auditoria>>> porUsuario(
            @PathVariable String correo,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        PageRequest pageRequest = PageRequest.of(pagina, tamano);

        Page<Auditoria> page = auditoriaRepository
                .findByUsuarioCorreoOrderByFechaAccionDesc(correo, pageRequest);

        return ResponseEntity.ok(
                ApiResponse.ok("Auditoría del usuario obtenida",
                        PageResponse.of(page))
        );
    }

    // ── GET /auditoria/modulo/{modulo} ────────────────────────────────────────
    @GetMapping("/modulo/{modulo}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PageResponse<Auditoria>>> porModulo(
            @PathVariable String modulo,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        PageRequest pageRequest = PageRequest.of(pagina, tamano);

        Page<Auditoria> page = auditoriaRepository
                .findByModuloOrderByFechaAccionDesc(modulo, pageRequest);

        return ResponseEntity.ok(
                ApiResponse.ok("Auditoría del módulo obtenida",
                        PageResponse.of(page))
        );
    }
}
