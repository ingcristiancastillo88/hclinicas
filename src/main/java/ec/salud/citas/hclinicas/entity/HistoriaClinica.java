package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Historia Clínica — agrupa todas las consultas de un paciente.
 * Un paciente tiene UNA historia clínica con MUCHAS consultas.
 *
 * HU-010 · HU-011 · HU-020
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

    // Un paciente → una historia clínica
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false, unique = true)
    private Paciente paciente;

    // ── Antecedentes gineco-obstétricos ───────────────────────────────────────
    @Column(name = "menarquia", length = 30)
    private String menarquia;

    @Column(name = "ciclo_menstrual", length = 60)
    private String cicloMenstrual;

    @Column(name = "fecha_ultima_menstruacion", length = 30)
    private String fechaUltimaMenstruacion;

    @Column(name = "gestas")
    private Integer gestas;

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

    @Column(name = "ultimo_papanicolau", length = 100)
    private String ultimoPapanicolau;

    @Column(name = "ultima_mamografia", length = 100)
    private String ultimaMamografia;

    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    // Una historia → muchas consultas
    @OneToMany(mappedBy = "historiaClinica",
            cascade = CascadeType.ALL,
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