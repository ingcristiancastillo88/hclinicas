package ec.salud.citas.hclinicas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sugerencia de autocompletado de medicamento.
 * Solo nombre genérico y comercial — sin dosis ni cantidades.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoCatalogoResponse {
    private Long    id;
    private String  nombreGenerico;
    private String  nombreComercial;
    private Integer vecesUsado;
}