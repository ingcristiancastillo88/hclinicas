package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Auditoría - registra cada acción realizada en el sistema (HU-006).
 * Registra: usuario, fecha, operación, módulo y detalles.
 * Tabla: auditoria
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_correo", length = 150)
    private String usuarioCorreo;

    @Column(name = "nombre_usuario", length = 200)
    private String nombreUsuario;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;           // CREATE, UPDATE, DELETE, LOGIN, LOGOUT

    @Column(name = "modulo", nullable = false, length = 100)
    private String modulo;           // USUARIOS, PACIENTES, HISTORIAS_CLINICAS, etc.

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @Column(name = "exitoso")
    @Builder.Default
    private Boolean exitoso = true;

    @Column(name = "detalle_error", columnDefinition = "TEXT")
    private String detalleError;
}
