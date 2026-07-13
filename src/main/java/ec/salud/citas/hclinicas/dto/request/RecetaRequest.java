package ec.salud.citas.hclinicas.dto.request;

import lombok.Data;
import java.util.List;

/**
 * DTO para generar/guardar una receta médica.
 * Cada medicamento sigue el formato clínico real de la Dra. Alexandra León:
 *   - Nombre genérico (con dosis de presentación)
 *   - Nombre comercial (subrayado en el PDF)
 *   - Presentación y cantidad (Tabletas #10)
 *   - Indicaciones de administración (al reverso / columna INDICACIONES)
 */
@Data
public class RecetaRequest {

    private List<MedicamentoRequest> medicamentos;
    private String prescripcion;
    private String proximaCita;

    @Data
    public static class MedicamentoRequest {
        /** Nombre genérico con dosis de presentación. Ej: "Ibuprofeno 400 mg" */
        private String nombreGenerico;

        /** Nombre comercial. Ej: "BUPREX FLASH". Opcional. */
        private String nombreComercial;

        /** Presentación y cantidad. Ej: "Tabletas #10 (diez)" */
        private String presentacion;

        /** Indicaciones de administración. Ej: "1 tableta cada 8 horas por 3 días" */
        private String indicaciones;
    }
}