package ec.salud.citas.hclinicas.controller;


import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.entity.Rol;
import ec.salud.citas.hclinicas.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de roles (HU-003).
 * GET /api/roles → listar todos los roles disponibles para formularios.
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolRepository rolRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<Rol>>> listar() {
        return ResponseEntity.ok(
                ApiResponse.ok("Roles obtenidos exitosamente", rolRepository.findAll())
        );
    }
}
