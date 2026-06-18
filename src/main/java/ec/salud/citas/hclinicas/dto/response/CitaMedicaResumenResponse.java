package ec.salud.citas.hclinicas.dto.response;


import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.TipoCita;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO liviano para listados y vista de calendario (HU-019).
 */
@Data
@Builder
public class CitaMedicaResumenResponse {

    private Long      id;
    private LocalDate fechaCita;
    private String horaInicio;
    private String horaFin;
    private Integer   duracionMinutos;
    private TipoCita tipoCita;
    private String    motivoCita;

    // Datos del paciente para mostrar en la vista
    private Long   pacienteId;
    private String pacienteNombreCompleto;
    private String pacienteCedula;
    private String pacienteCelular;

    private EstadoCita estado;
}