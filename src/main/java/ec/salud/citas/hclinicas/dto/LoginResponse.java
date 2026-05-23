package ec.salud.citas.hclinicas.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de respuesta al login exitoso (HU-001).
 * Retorna el token JWT y datos básicos del usuario autenticado.
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private String tipo;               // "Bearer"
    private long expiracionMs;

    // Datos del usuario para el frontend
    private Long usuarioId;
    private String nombreCompleto;
    private String correo;
    private String rol;
}
