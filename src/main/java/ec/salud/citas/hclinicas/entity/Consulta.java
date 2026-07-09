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
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta — registro individual de cada atención médica.
 * Pertenece a una HistoriaClinica, puede tener múltiples ArchivoAdjunto.
 *
 * HU-010 · HU-012
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

    // ── Datos generales ───────────────────────────────────────────────────────
    @Column(name = "fecha_consulta", nullable = false)
    private LocalDate fechaConsulta;

    @Column(name = "motivo_consulta", nullable = false, columnDefinition = "TEXT")
    private String motivoConsulta;

    // ── Signos vitales ────────────────────────────────────────────────────────
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
    private Integer semanasGestacion;

    // ── Examen y diagnóstico ──────────────────────────────────────────────────
    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;

    @Column(name = "diagnostico_principal", nullable = false, columnDefinition = "TEXT")
    private String diagnosticoPrincipal;

    @Column(name = "diagnostico_secundario", columnDefinition = "TEXT")
    private String diagnosticoSecundario;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    // ── Tratamiento ───────────────────────────────────────────────────────────
    @Column(name = "tratamiento", columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "medicacion", columnDefinition = "TEXT")
    private String medicacion;

    @Column(name = "indicaciones", columnDefinition = "TEXT")
    private String indicaciones;

    @Column(name = "proxima_cita")
    private LocalDate proximaCita;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "activa")
    @Builder.Default
    private Boolean activa = true;

    // ── Archivos adjuntos ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "consulta",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
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

    @Column(name = "fecha_ultima_menstruacion")
    private LocalDate fechaUltimaMenustracion;

    @Column(name = "codigos_cie10_secundarios", columnDefinition = "TEXT")
    private String codigosCie10SecundariosJson;
}