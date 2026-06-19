package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Receta médica asociada a una consulta.
 * Tabla: recetas
 */
@Entity
@Table(name = "recetas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id", nullable = false)
    private Consulta consulta;

    /** Indicaciones / prescripcion general */
    @Column(name = "prescripcion", columnDefinition = "TEXT")
    private String prescripcion;

    /** Proxima cita en texto libre */
    @Column(name = "proxima_cita", length = 200)
    private String proximaCita;

    /** Medicamentos en formato JSON */
    @Column(name = "medicamentos_json", columnDefinition = "TEXT")
    private String medicamentosJson;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "creado_por", length = 100)
    private String creadoPor;
}