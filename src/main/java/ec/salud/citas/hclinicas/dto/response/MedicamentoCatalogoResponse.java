package ec.salud.citas.hclinicas.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para sugerencias de autocompletado de medicamentos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentoCatalogoResponse {

    private Long id;
    private String nombre;
    private String dosisSugerida;
    private String cantidadSugerida;
    private String indicacionesSugeridas;
    private Integer vecesUsado;
}