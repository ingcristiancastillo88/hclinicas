package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Catálogo de medicamentos para autocompletado en la receta.
 * Solo guarda nombre genérico y nombre comercial — UN registro por medicamento.
 * NO almacena dosis, cantidad ni indicaciones (esas varían por paciente
 * y se escriben directamente en cada receta).
 *
 * Tabla: medicamentos_catalogo
 *
 * Migración SQL (si ya tienes la tabla):
 *   ALTER TABLE medicamentos_catalogo ADD COLUMN IF NOT EXISTS nombre_generico VARCHAR(200);
 *   ALTER TABLE medicamentos_catalogo ADD COLUMN IF NOT EXISTS nombre_comercial VARCHAR(200);
 *   UPDATE medicamentos_catalogo SET nombre_generico = nombre WHERE nombre_generico IS NULL;
 *   ALTER TABLE medicamentos_catalogo ALTER COLUMN nombre_generico SET NOT NULL;
 *   ALTER TABLE medicamentos_catalogo DROP COLUMN IF EXISTS dosis_sugerida;
 *   ALTER TABLE medicamentos_catalogo DROP COLUMN IF EXISTS cantidad_sugerida;
 *   ALTER TABLE medicamentos_catalogo DROP COLUMN IF EXISTS indicaciones_sugeridas;
 */
@Entity
@Table(name = "medicamentos_catalogo",
        uniqueConstraints = @UniqueConstraint(
                columnNames = "nombre_normalizado",
                name = "uk_medicamento_nombre_normalizado"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre genérico del medicamento — Ej: "Ibuprofeno" */
    @Column(name = "nombre_generico", length = 200, nullable = false)
    private String nombreGenerico;

    /** Nombre comercial — Ej: "BUPREX FLASH". Puede ser nulo. */
    @Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;

    /**
     * Clave de deduplicación (mayúsculas, sin tildes) basada en nombre genérico.
     * Evita duplicados cuando el médico escribe "ibuprofeno" o "Ibuprofeno".
     */
    @Column(name = "nombre_normalizado", length = 200, nullable = false, unique = true)
    private String nombreNormalizado;

    /** Cuántas veces se ha prescrito — ordena sugerencias por frecuencia */
    @Builder.Default
    @Column(name = "veces_usado", nullable = false)
    private Integer vecesUsado = 1;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_vez_usado")
    private LocalDateTime ultimaVezUsado;
}