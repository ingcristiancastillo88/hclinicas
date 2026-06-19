// ── RecetaRequest.java ────────────────────────────────────────────────────
package ec.salud.citas.hclinicas.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class RecetaRequest {

    private List<MedicamentoRequest> medicamentos;
    private String prescripcion;
    private String proximaCita;

    @Data
    public static class MedicamentoRequest {
        private String nombre;
        private String dosis;
        private String cantidad;
        private String indicaciones;
    }
}