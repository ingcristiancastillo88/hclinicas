package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ActualizarConsultaRequest {

    @NotNull(message = "La fecha de consulta es obligatoria")
    private LocalDate fechaConsulta;

    // ── Parámetros de la visita ───────────────────────────────────────────────
    private String  tipoConsulta;
    private Boolean estaEmbarazada;

    // ── Anamnesis ─────────────────────────────────────────────────────────────
    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    private String enfermedadActual;
    private String reporteExamenesPrevios;

    // ── Signos Vitales ────────────────────────────────────────────────────────
    private Double  peso;
    private Double  talla;
    private String  presionArterial;
    private Integer frecuenciaCardiaca;
    private String  frecuenciaCardiacaTexto;
    private Double  temperatura;
    private String  temperaturaTexto;
    private Integer saturacionOxigeno;
    private String  saturacionTexto;
    private String  frecuenciaRespiratoriaTexto;
    private Integer semanasGestacion;

    // ── Módulo Materno-Fetal ──────────────────────────────────────────────────
    private LocalDate fumConsulta;
    private String    alturaUterina;
    private String    fcFetal;
    private String    presentacionFetal;
    private String    tonoUterino;
    private String    movimientosFetales;
    private String    pesoFetalEstimado;
    private String    scoreMama;

    // ── Examen Físico por Sistemas ────────────────────────────────────────────
    private String examenFisico;
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
    private String       diagnosticoPrincipal;

    private String       diagnosticoSecundario;
    private String       codigoCie10;
    private List<String> codigosCie10Secundarios;

    // ── Tratamiento ───────────────────────────────────────────────────────────
    private String    tratamiento;
    private String    medicacion;
    private String    indicaciones;
    private LocalDate proximaCita;
    private String    observaciones;

    // ── FUM de antecedentes ───────────────────────────────────────────────────
    private LocalDate fechaUltimaMenustracion;
}