package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para actualizar una consulta existente (HU-010).
 */
@Data
public class ActualizarConsultaRequest {

    @NotNull(message = "La fecha de consulta es obligatoria")
    private LocalDate fechaConsulta;

    @NotBlank(message = "El motivo de consulta es obligatorio")
    private String motivoConsulta;

    private Double peso;
    private Double talla;
    private String presionArterial;
    private Integer frecuenciaCardiaca;
    private Double temperatura;
    private Integer saturacionOxigeno;
    private Integer semanasGestacion;
    private String examenFisico;

    @NotBlank(message = "El diagnóstico principal es obligatorio")
    private String diagnosticoPrincipal;

    private String diagnosticoSecundario;
    private String codigoCie10;
    private String tratamiento;
    private String medicacion;
    private String indicaciones;
    private LocalDate proximaCita;
    private String observaciones;
}