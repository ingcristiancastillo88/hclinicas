package ec.salud.citas.hclinicas.dto.request;

import lombok.Data;

import java.util.List;

/**
 * DTO que recibe el frontend con los datos completos de la receta médica.
 * POST /api/documentos/receta/{consultaId}
 */
@Data
public class RecetaRequest {

    /** Lista de medicamentos a incluir en la receta. */
    private List<MedicamentoRequest> medicamentos;

    /** Prescripción general o instrucciones adicionales. */
    private String prescripcion;

    /** Texto de la próxima cita. Ej: "Regresar en 15 días para control". */
    private String proximaCita;
}
