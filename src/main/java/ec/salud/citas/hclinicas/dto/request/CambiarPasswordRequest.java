package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el cambio de contraseña temporal en el primer login.
 */
@Data
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String passwordNueva;

    @NotBlank(message = "La confirmación es obligatoria")
    private String confirmarPassword;
}