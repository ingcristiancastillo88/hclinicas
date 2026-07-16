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
 * Entidad Consulta — registro de cada atención médica.
 * Incluye todos los campos del formulario clínico:
 * parámetros de visita, signos vitales, módulo materno-fetal,
 * módulo ginecológico, examen físico por sistemas y diagnósticos.
 */
@Entity
@Table(name = "consultas")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_clinica_id", nullable = false)
    private HistoriaClinica historiaClinica;

    // ── Parámetros de la visita ───────────────────────────────────────────────
    @Column(name = "fecha_consulta", nullable = false)
    private LocalDate fechaConsulta;

    @Column(name = "tipo_consulta", length = 50)
    private String tipoConsulta;          // PRENATAL, GINECO_GENERAL, PROCEDIMIENTO...

    @Column(name = "esta_embarazada")
    private Boolean estaEmbarazada;       // Activa módulo materno o gineco

    // ── Anamnesis ─────────────────────────────────────────────────────────────
    @Column(name = "motivo_consulta", nullable = false, columnDefinition = "TEXT")
    private String motivoConsulta;

    @Column(name = "enfermedad_actual", columnDefinition = "TEXT")
    private String enfermedadActual;       // Nota de evolución / enfermedad actual

    @Column(name = "reporte_examenes_previos", columnDefinition = "TEXT")
    private String reporteExamenesPrevios; // Hallazgos de exámenes traídos

    // ── Signos Vitales ────────────────────────────────────────────────────────
    @Column(name = "peso")
    private Double peso;

    @Column(name = "talla")
    private Double talla;

    @Column(name = "presion_arterial", length = 20)
    private String presionArterial;

    @Column(name = "frecuencia_cardiaca")
    private Integer frecuenciaCardiaca;

    @Column(name = "frecuencia_cardiaca_texto", length = 50)
    private String frecuenciaCardiacaTexto;  // Texto libre: "78 lpm"

    @Column(name = "temperatura")
    private Double temperatura;

    @Column(name = "temperatura_texto", length = 50)
    private String temperaturaTexto;         // Texto libre: "36.6 °C"

    @Column(name = "saturacion_oxigeno")
    private Integer saturacionOxigeno;

    @Column(name = "saturacion_texto", length = 50)
    private String saturacionTexto;          // Texto libre: "97%"

    @Column(name = "frecuencia_respiratoria_texto", length = 50)
    private String frecuenciaRespiratoriaTexto; // Texto libre: "18 rpm"

    @Column(name = "semanas_gestacion")
    private Integer semanasGestacion;

    // ── Módulo Materno-Fetal (activo cuando estaEmbarazada = true) ─────────────
    @Column(name = "fum_consulta")
    private LocalDate fumConsulta;           // FUM ingresada en la consulta

    @Column(name = "altura_uterina", length = 100)
    private String alturaUterina;

    @Column(name = "fc_fetal", length = 100)
    private String fcFetal;

    @Column(name = "presentacion_fetal", length = 200)
    private String presentacionFetal;

    @Column(name = "tono_uterino", length = 200)
    private String tonoUterino;

    @Column(name = "movimientos_fetales", length = 200)
    private String movimientosFetales;

    @Column(name = "peso_fetal_estimado", length = 100)
    private String pesoFetalEstimado;

    @Column(name = "score_mama", length = 100)
    private String scoreMama;

    // ── Examen Físico por Sistemas ────────────────────────────────────────────
    @Column(name = "examen_fisico", columnDefinition = "TEXT")
    private String examenFisico;            // Examen físico general

    @Column(name = "examen_cabeza", columnDefinition = "TEXT")
    private String examenCabeza;            // Cabeza y cuello

    @Column(name = "examen_torax", columnDefinition = "TEXT")
    private String examenTorax;             // Tórax / cardiopulmonar

    @Column(name = "examen_abdomen", columnDefinition = "TEXT")
    private String examenAbdomen;

    @Column(name = "examen_genital", columnDefinition = "TEXT")
    private String examenGenital;

    @Column(name = "examen_extremidades", columnDefinition = "TEXT")
    private String examenExtremidades;

    // ── Módulo Ginecológico (activo cuando estaEmbarazada = false) ─────────────
    @Column(name = "inspeccion_vulva", columnDefinition = "TEXT")
    private String inspeccionVulva;

    @Column(name = "especuloscopia", columnDefinition = "TEXT")
    private String especuloscopia;

    @Column(name = "tacto_vaginal", columnDefinition = "TEXT")
    private String tactoVaginal;

    @Column(name = "examen_mamas", columnDefinition = "TEXT")
    private String examenMamas;

    // ── Diagnóstico ───────────────────────────────────────────────────────────
    @Column(name = "diagnostico_principal", nullable = false, columnDefinition = "TEXT")
    private String diagnosticoPrincipal;

    @Column(name = "diagnostico_secundario", columnDefinition = "TEXT")
    private String diagnosticoSecundario;

    @Column(name = "codigo_cie10", length = 20)
    private String codigoCie10;

    @Column(name = "codigos_cie10_secundarios", columnDefinition = "TEXT")
    private String codigosCie10SecundariosJson; // JSON array de códigos secundarios

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

    // ── FUM (de antecedentes del paciente) ────────────────────────────────────
    @Column(name = "fecha_ultima_menstruacion")
    private LocalDate fechaUltimaMenustracion;

    // ── Estado ────────────────────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "activa")
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
}