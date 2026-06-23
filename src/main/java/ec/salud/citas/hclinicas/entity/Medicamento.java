package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Lista maestra simple de medicamentos para autocompletado en la receta.
 * NO es un inventario — solo guarda nombre/dosis/cantidad/indicaciones
 * usados previamente para sugerirlos rápidamente al escribir.
 * Tabla: medicamentos_catalogo
 */
@Entity
@Table(name = "medicamentos_catalogo",
        uniqueConstraints = @UniqueConstraint(columnNames = "nombre_normalizado"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre tal como lo escribió el médico */
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    /** Nombre en mayúsculas sin tildes, para evitar duplicados al buscar */
    @Column(name = "nombre_normalizado", length = 200, nullable = false, unique = true)
    private String nombreNormalizado;

    /** Última dosis usada — se sugiere como valor por defecto */
    @Column(name = "dosis_sugerida", length = 100)
    private String dosisSugerida;

    /** Última cantidad usada */
    @Column(name = "cantidad_sugerida", length = 100)
    private String cantidadSugerida;

    /** Últimas indicaciones de administración usadas */
    @Column(name = "indicaciones_sugeridas", columnDefinition = "TEXT")
    private String indicacionesSugeridas;

    /** Cuántas veces se ha usado — para ordenar sugerencias por frecuencia */
    @Builder.Default
    @Column(name = "veces_usado", nullable = false)
    private Integer vecesUsado = 1;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "ultima_vez_usado")
    private LocalDateTime ultimaVezUsado;
}