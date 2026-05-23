package ec.salud.citas.hclinicas.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para actualización de usuarios (HU-004).
 * La contraseña es opcional en la edición.
 */
@Data
public class ActualizarUsuarioRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    @Size(max = 13)
    private String cedula;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    @Size(max = 150)
    private String correo;

    // Opcional: solo se actualiza si viene con valor
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @Size(max = 15)
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private Long rolId;
}
