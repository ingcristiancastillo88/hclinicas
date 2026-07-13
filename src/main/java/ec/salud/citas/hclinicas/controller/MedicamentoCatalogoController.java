package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.MedicamentoCatalogoResponse;
import ec.salud.citas.hclinicas.entity.Medicamento;
import ec.salud.citas.hclinicas.repository.MedicamentoCatalogoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Catálogo de medicamentos para autocompletado.
 * Guarda UN registro por nombre genérico — sin dosis ni cantidades.
 *
 * GET  /api/medicamentos/buscar?q=ibu   → sugerencias
 * POST /api/medicamentos/registrar-uso  → guarda o actualiza contador
 */
@Slf4j
@RestController
@RequestMapping("/medicamentos")
@RequiredArgsConstructor
public class MedicamentoCatalogoController {

    private final MedicamentoCatalogoRepository repo;
    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}");

    // ── DTO de entrada ────────────────────────────────────────────────────────
    @Data
    public static class RegistrarUsoRequest {
        private String nombreGenerico;   // Ej: "Ibuprofeno"
        private String nombreComercial;  // Ej: "BUPREX FLASH"  (opcional)
    }

    // ── Buscar sugerencias ────────────────────────────────────────────────────
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<MedicamentoCatalogoResponse>>> buscar(
            @RequestParam(name = "q", defaultValue = "") String texto) {

        if (texto.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.ok("Escriba al menos 2 letras", List.of()));
        }

        String normalizado = normalizar(texto);
        List<MedicamentoCatalogoResponse> resultados = repo.buscarSugerencias(normalizado)
                .stream().limit(10)
                .map(m -> MedicamentoCatalogoResponse.builder()
                        .id(m.getId())
                        .nombreGenerico(m.getNombreGenerico())
                        .nombreComercial(m.getNombreComercial())
                        .vecesUsado(m.getVecesUsado())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Sugerencias encontradas", resultados));
    }

    // ── Registrar uso ─────────────────────────────────────────────────────────
    /**
     * Se llama al agregar un medicamento a una receta.
     * Clave de deduplicación: nombre genérico normalizado.
     * Si ya existe → solo incrementa el contador y actualiza nombre comercial.
     * Si no existe → crea el registro.
     */
    @PostMapping("/registrar-uso")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> registrarUso(
            @RequestBody RegistrarUsoRequest req) {

        if (req.getNombreGenerico() == null || req.getNombreGenerico().isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Sin nombre genérico, no se registra", null));
        }

        String normalizado = normalizar(req.getNombreGenerico());

        repo.findByNombreNormalizado(normalizado).ifPresentOrElse(existente -> {
            // Actualizar comercial solo si viene uno nuevo
            if (req.getNombreComercial() != null && !req.getNombreComercial().isBlank()) {
                existente.setNombreComercial(req.getNombreComercial().toUpperCase().trim());
            }
            existente.setVecesUsado(existente.getVecesUsado() + 1);
            existente.setUltimaVezUsado(LocalDateTime.now());
            repo.save(existente);
            log.debug("Medicamento actualizado: {}", existente.getNombreGenerico());
        }, () -> {
            Medicamento nuevo = Medicamento.builder()
                    .nombreGenerico(capitalizar(req.getNombreGenerico().trim()))
                    .nombreComercial(req.getNombreComercial() != null
                            ? req.getNombreComercial().toUpperCase().trim() : null)
                    .nombreNormalizado(normalizado)
                    .vecesUsado(1)
                    .ultimaVezUsado(LocalDateTime.now())
                    .build();
            repo.save(nuevo);
            log.info("Medicamento nuevo registrado: {}", nuevo.getNombreGenerico());
        });

        return ResponseEntity.ok(ApiResponse.ok("Registrado", null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String normalizar(String texto) {
        String s = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return DIACRITICOS.matcher(s).replaceAll("").toUpperCase();
    }

    private String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}