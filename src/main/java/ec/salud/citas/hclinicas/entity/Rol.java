package ec.salud.citas.hclinicas.entity;

import ec.salud.citas.hclinicas.enumerado.RolNombre;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Rol - representa los 4 roles definidos en la tesis (HU-003).
 * Tabla: roles
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private RolNombre nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;
}
