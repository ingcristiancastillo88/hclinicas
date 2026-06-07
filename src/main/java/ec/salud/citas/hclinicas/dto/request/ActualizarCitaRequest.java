package ec.salud.citas.hclinicas.dto.request;


import ec.salud.citas.hclinicas.enumerado.TipoCita;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para editar una cita médica (HU-018).
 * Al cambiar fecha u hora se revalida la disponibilidad.
 */
@Data
public class ActualizarCitaRequest {

    @NotNull(message = "La fecha de la cita es obligatoria")
    private LocalDate fechaCita;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    private Integer duracionMinutos = 30;

    private TipoCita tipoCita;

    private String motivoCita;

    private String notasAdicionales;
}
