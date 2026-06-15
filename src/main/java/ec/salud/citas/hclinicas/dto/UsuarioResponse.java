package ec.salud.citas.hclinicas.dto;

import ec.salud.citas.hclinicas.enumerado.EstadoUsuario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para datos de usuario (nunca expone la contraseña).
 */
@Data
@Builder
public class UsuarioResponse {

    private Long id;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private String cedula;
    private String correo;
    private String telefono;
    private String rol;
    private EstadoUsuario estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String creadoPor;
}

