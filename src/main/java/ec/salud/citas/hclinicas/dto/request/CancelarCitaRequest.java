package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para cancelar una cita (HU-018).
 * El motivo es obligatorio para registrar la razón en auditoría.
 */
@Data
public class CancelarCitaRequest {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String motivoCancelacion;
}