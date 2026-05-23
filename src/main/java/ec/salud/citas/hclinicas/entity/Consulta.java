package ec.salud.citas.hclinicas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Consulta — registro individual de cada atención médica.
 * Pertenece a una HistoriaClinica y puede tener múltiples ArchivoAdjunto.
 * <p>
 * HU-010 Registro de consulta · HU-012 Adjuntar archivos
 * Tabla: consultas
 */
@Entity
@Table(name = "consultas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    // ── Datos de la consulta ──────────────────────────────────────────────────

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDate fechaConsulta;

    @Column(name = "motivo_consulta", nullable = false, columnDefinition = "TEXT")
    private String motivoConsulta;

    // Signos vitales
    @Column(name = "peso")
    private Double peso;                    // kg

    @Column(name = "talla")
    private Double talla;                   // cm

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;         // ej: 120/80

    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;     // lpm

    @Column(name = "temperatura")
    private Double temperatura;             // °C

    @Column(name = "saturacion_oxigeno")
    private Integer saturacionOxigeno;      // %

    @Column(name = "semanas_gestacion")
    private Integer semanasGestacion;       // si aplica

    // Examen físico
    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;

    // Diagnóstico
    @Column(name = "diagnostico_principal", nullable = false, columnDefinition = "TEXT")
    private String diagnosticoPrincipal;

    @Column(name = "diagnostico_secundario", columnDefinition = "TEXT")
    private String diagnosticoSecundario;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    // Tratamiento
    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "medicacion", columnDefinition = "TEXT")
    private String medicacion;

    @Column(name = "indicaciones", columnDefinition = "TEXT")
    private String indicaciones;

    // Próxima cita
    @Column(name = "proxima_cita")
    private LocalDate proximaCita;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    // ── Archivos adjuntos ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "consulta", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ArchivoAdjunto> archivos = new ArrayList<>();

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
