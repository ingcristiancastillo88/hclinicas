package ec.salud.citas.hclinicas.controller;

import ec.salud.citas.hclinicas.dto.request.RecetaRequest.MedicamentoRequest;
import ec.salud.citas.hclinicas.dto.response.ApiResponse;
import ec.salud.citas.hclinicas.dto.response.MedicamentoCatalogoResponse;
import ec.salud.citas.hclinicas.entity.Medicamento;
import ec.salud.citas.hclinicas.repository.MedicamentoCatalogoRepository;
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
 * Controller del catálogo simple de medicamentos para autocompletado.
 * NO es un inventario — solo guarda lo que el médico ha escrito antes
 * para sugerirlo en futuras recetas.
 *
 * GET  /api/medicamentos/buscar?q=para         → sugerencias de autocompletado
 * POST /api/medicamentos/registrar-uso         → guarda/actualiza tras usar un medicamento
 */
@Slf4j
@RestController
@RequestMapping("/medicamentos")
@RequiredArgsConstructor
public class MedicamentoCatalogoController {

    private final MedicamentoCatalogoRepository repo;

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}");

    // ── Buscar sugerencias ───────────────────────────────────────────────────

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<MedicamentoCatalogoResponse>>> buscar(
            @RequestParam(name = "q", defaultValue = "") String texto) {

        if (texto.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.ok("Escriba al menos 2 letras", List.of()));
        }

        String normalizado = normalizar(texto);
        List<MedicamentoCatalogoResponse> resultados = repo.buscarSugerencias(normalizado)
                .stream()
                .limit(8)
                .map(m -> MedicamentoCatalogoResponse.builder()
                        .id(m.getId())
                        .nombre(m.getNombre())
                        .dosisSugerida(m.getDosisSugerida())
                        .cantidadSugerida(m.getCantidadSugerida())
                        .indicacionesSugeridas(m.getIndicacionesSugeridas())
                        .vecesUsado(m.getVecesUsado())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Sugerencias encontradas", resultados));
    }

    // ── Registrar uso (guarda o actualiza frecuencia) ─────────────────────────

    /**
     * Se llama automáticamente cada vez que el médico agrega un medicamento
     * a una receta. Si ya existe, incrementa el contador de uso y actualiza
     * los valores sugeridos; si no existe, lo crea.
     */
    @PostMapping("/registrar-uso")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','MEDICO_ESPECIALISTA')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> registrarUso(
            @RequestBody MedicamentoRequest req) {

        if (req.getNombre() == null || req.getNombre().isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Sin nombre, no se registra", null));
        }

        String normalizado = normalizar(req.getNombre());

        repo.findByNombreNormalizado(normalizado).ifPresentOrElse(existente -> {
            existente.setDosisSugerida(req.getDosis());
            existente.setCantidadSugerida(req.getCantidad());
            existente.setIndicacionesSugeridas(req.getIndicaciones());
            existente.setVecesUsado(existente.getVecesUsado() + 1);
            existente.setUltimaVezUsado(LocalDateTime.now());
            repo.save(existente);
        }, () -> {
            Medicamento nuevo = Medicamento.builder()
                    .nombre(req.getNombre())
                    .nombreNormalizado(normalizado)
                    .dosisSugerida(req.getDosis())
                    .cantidadSugerida(req.getCantidad())
                    .indicacionesSugeridas(req.getIndicaciones())
                    .vecesUsado(1)
                    .ultimaVezUsado(LocalDateTime.now())
                    .build();
            repo.save(nuevo);
        });

        return ResponseEntity.ok(ApiResponse.ok("Registrado", null));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Normaliza a mayúsculas sin tildes para evitar duplicados por acentos */
    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        sinTildes = DIACRITICOS.matcher(sinTildes).replaceAll("");
        return sinTildes.toUpperCase();
    }
}