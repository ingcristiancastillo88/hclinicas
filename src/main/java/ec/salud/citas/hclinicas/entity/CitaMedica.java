package ec.salud.citas.hclinicas.entity;

import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.TipoCita;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad CitaMedica — representa el agendamiento de una cita.
 *
 * Reglas de negocio (HU-016 / HU-017 / HU-018):
 *  - No pueden existir dos citas en la misma fecha y hora (validación en servicio).
 *  - El estado es lógico: las citas canceladas no se borran físicamente.
 *  - La cita puede tener un motivo previo (antes de la consulta).
 *
 * Tabla: citas_medicas
 */
@Entity
@Table(
        name = "citas_medicas",
        indexes = {
                // Índice compuesto para la validación de disponibilidad (HU-017 / RNF-005)
                @Index(name = "idx_cita_fecha_hora", columnList = "fecha_cita, hora_inicio")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CitaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con Paciente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    // Relación con Usuario (médico que agenda)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ── Datos de la cita ──────────────────────────────────────────────────────
    @Column(name = "fecha_cita", nullable = false)
    private LocalDate fechaCita;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;                   // calculada: horaInicio + duracion

    @Column(name = "duracion_minutos")
    @Builder.Default
    private Integer duracionMinutos = 30;        // duración por defecto: 30 min

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cita", length = 20)
    @Builder.Default
    private TipoCita tipoCita = TipoCita.CONTROL;

    @Column(name = "motivo_cita", columnDefinition = "TEXT")
    private String motivoCita;                   // motivo previo al llegar

    @Column(name = "notas_adicionales", columnDefinition = "TEXT")
    private String notasAdicionales;

    // ── Estado ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoCita estado = EstadoCita.PROGRAMADA;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

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