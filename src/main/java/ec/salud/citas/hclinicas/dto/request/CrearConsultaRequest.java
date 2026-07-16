package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear o actualizar una consulta clínica.
 * Incluye todos los campos del formulario historia-form.component.ts.
 */
@Data
public class CrearConsultaRequest {

    @NotNull(message = "El ID de la historia clínica es obligatorio")
    private Long historiaClinicaId;

    @NotNull(message = "La fecha de consulta es obligatoria")
    private LocalDate fechaConsulta;

    // ── Parámetros de la visita ───────────────────────────────────────────────
    private String  tipoConsulta;       // PRENATAL, GINECO_GENERAL, PROCEDIMIENTO...
    private Boolean estaEmbarazada;     // Activa módulo materno o gineco

    // ── Anamnesis ─────────────────────────────────────────────────────────────
    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    private String enfermedadActual;       // Nota de evolución
    private String reporteExamenesPrevios; // Hallazgos de exámenes traídos

    // ── Signos Vitales ────────────────────────────────────────────────────────
    private Double  peso;
    private Double  talla;
    private String  presionArterial;
    private Integer frecuenciaCardiaca;
    private String  frecuenciaCardiacaTexto;    // "78 lpm"
    private Double  temperatura;
    private String  temperaturaTexto;            // "36.6 °C"
    private Integer saturacionOxigeno;
    private String  saturacionTexto;             // "97%"
    private String  frecuenciaRespiratoriaTexto; // "18 rpm"
    private Integer semanasGestacion;

    // ── Módulo Materno-Fetal ──────────────────────────────────────────────────
    private LocalDate fumConsulta;        // FUM ingresada en la consulta
    private String    alturaUterina;
    private String    fcFetal;
    private String    presentacionFetal;
    private String    tonoUterino;
    private String    movimientosFetales;
    private String    pesoFetalEstimado;
    private String    scoreMama;

    // ── Examen Físico por Sistemas ────────────────────────────────────────────
    private String examenFisico;          // Examen físico general
    private String examenCabeza;
    private String examenTorax;
    private String examenAbdomen;
    private String examenGenital;
    private String examenExtremidades;

    // ── Módulo Ginecológico ───────────────────────────────────────────────────
    private String inspeccionVulva;
    private String especuloscopia;
    private String tactoVaginal;
    private String examenMamas;

    // ── Diagnóstico ───────────────────────────────────────────────────────────
    @NotBlank(message = "El diagnóstico principal es obligatorio")
    private String diagnosticoPrincipal;

    private String       diagnosticoSecundario;
    private String       codigoCie10;
    private List<String> codigosCie10Secundarios; // ["N94.0", "Z34"] etc.

    // ── Tratamiento ───────────────────────────────────────────────────────────
    private String    tratamiento;
    private String    medicacion;
    private String    indicaciones;
    private LocalDate proximaCita;
    private String    observaciones;

    // ── FUM de antecedentes ───────────────────────────────────────────────────
    private LocalDate fechaUltimaMenustracion;
}