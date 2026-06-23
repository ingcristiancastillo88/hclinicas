package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Catálogo de códigos CIE-10 (Clasificación Internacional de Enfermedades).
 * Se carga una sola vez con el script SQL cie10_data.sql.
 * Tabla: codigos_cie10
 */
@Entity
@Table(name = "codigos_cie10",
        indexes = @Index(name = "idx_cie10_codigo", columnList = "codigo"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoCie10 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código CIE-10. Ej: O80, Z34, A000 */
    @Column(name = "codigo", length = 10, nullable = false, unique = true)
    private String codigo;

    /** Descripción de la enfermedad o condición */
    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion;
}