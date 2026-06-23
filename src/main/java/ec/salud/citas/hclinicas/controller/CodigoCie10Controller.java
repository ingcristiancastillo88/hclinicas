package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.repository.CodigoCie10Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Autocompletado de códigos CIE-10.
 * GET /api/cie10/buscar?q=O80 → lista de sugerencias { codigo, descripcion }
 */
@RestController
@RequestMapping("/cie10")
@RequiredArgsConstructor
public class CodigoCie10Controller {

    private final CodigoCie10Repository repo;

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> buscar(
            @RequestParam(name = "q", defaultValue = "") String texto) {

        if (texto.trim().length() < 2) {
            return ResponseEntity.ok(
                    ApiResponse.ok("Escriba al menos 2 caracteres", List.of()));
        }

        List<Map<String, String>> resultados = repo.buscarSugerencias(texto.trim())
                .stream()
                .limit(10)
                .map(c -> Map.of(
                        "codigo",      c.getCodigo(),
                        "descripcion", c.getDescripcion(),
                        "label",       c.getCodigo() + " — " + c.getDescripcion()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Sugerencias CIE-10", resultados));
    }
}