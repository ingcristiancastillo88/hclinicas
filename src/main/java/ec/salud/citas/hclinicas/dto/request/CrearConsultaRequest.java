package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para registrar una consulta médica (HU-010).
 */
@Data
public class CrearConsultaRequest {

    @NotNull(message = "El ID de la historia clínica es obligatorio")
    private Long historiaClinicaId;

    @NotNull(message = "La fecha de consulta es obligatoria")
    private LocalDate fechaConsulta;

    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    // Signos vitales
    private Double peso;
    private Double talla;
    private String presionArterial;
    private Integer frecuenciaCardiaca;
    private Double temperatura;
    private Integer saturacionOxigeno;
    private Integer semanasGestacion;

    // Examen y diagnóstico
    private String examenFisico;

    @NotBlank(message = "El diagnóstico principal es obligatorio")
    private String diagnosticoPrincipal;

    private String diagnosticoSecundario;
    private String codigoCie10;

    // Tratamiento
    private String tratamiento;
    private String medicacion;
    private String indicaciones;
    private LocalDate proximaCita;
    private String observaciones;
}