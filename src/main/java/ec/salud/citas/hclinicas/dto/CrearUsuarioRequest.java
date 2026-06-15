package ec.salud.citas.hclinicas.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para creación y edición de usuarios (HU-004).
 */
@Data
public class CrearUsuarioRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
    private String apellidos;

    @Size(max = 13, message = "La cédula no puede superar 13 caracteres")
    private String cedula;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    @Size(max = 150)
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @Size(max = 15)
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private Long rolId;
}
