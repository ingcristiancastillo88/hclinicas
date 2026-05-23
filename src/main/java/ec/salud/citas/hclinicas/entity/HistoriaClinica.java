package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad HistoriaClinica — agrupa todas las consultas de un paciente.
 * Un paciente tiene UNA historia clínica con MUCHAS consultas.
 *
 * HU-010 Registro · HU-011 Visualización · HU-020 Consulta paciente
 * Tabla: historias_clinicas
 */
@Entity
@Table(name = "historias_clinicas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriaClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con Paciente — un paciente, una historia
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private Paciente paciente;

    // Antecedentes gineco-obstétricos generales
    @Column(name = "menarquia", length = 20)
    private String menarquia;               // Edad primera menstruación

    @Column(name = "ciclo_menstrual", length = 50)
    private String cicloMenstrual;          // Regular/Irregular, duración

    @Column(name = "fecha_ultima_menstruacion")
    private String fechaUltimaMenstruacion;

    @Column(name = "gestas")
    private Integer gestas;                 // Número de embarazos

    @Column(name = "partos")
    private Integer partos;

    @Column(name = "cesareas")
    private Integer cesareas;

    @Column(name = "abortos")
    private Integer abortos;

    @Column(name = "hijos_vivos")
    private Integer hijosVivos;

    @Column(name = "metodo_anticonceptivo", length = 100)
    private String metodoAnticonceptivo;

    @Column(name = "ultimo_papanicolau", length = 50)
    private String ultimoPapanicolau;

    @Column(name = "ultima_mamografia", length = 50)
    private String ultimaMamografia;

    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    // Relación con Consultas
    @OneToMany(mappedBy = "historiaClinica", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Consulta> consultas = new ArrayList<>();

    // ── Auditoría ─────────────────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @CreatedBy
    @Column(name = "creado_por", updatable = false, length = 100)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "actualizado_por", length = 100)
    private String actualizadoPor;
}
