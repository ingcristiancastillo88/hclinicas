package ec.salud.citas.hclinicas.dto.request;

import ec.salud.citas.hclinicas.enumerado.TipoCita;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para agendar una cita médica (HU-016).
 */
@Data
public class CrearCitaRequest {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    @NotNull(message = "La fecha de la cita es obligatoria")
    //@Future(message = "La fecha de la cita debe ser una fecha futura")
    private LocalDate fechaCita;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    /** Duración en minutos. Por defecto 30. */
    private Integer duracionMinutos = 30;

    private TipoCita tipoCita = TipoCita.CONTROL;

    private String motivoCita;

    private String notasAdicionales;
}
