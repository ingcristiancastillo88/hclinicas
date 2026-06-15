package ec.salud.citas.hclinicas.dto.response;

import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.enumerado.TipoCita;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class CitaMedicaResponse {

    private Long id;

    // Paciente
    private Long   pacienteId;
    private String pacienteCedula;
    private String pacienteNombreCompleto;
    private String pacienteCelular;

    // Médico
    private Long   usuarioId;
    private String usuarioNombreCompleto;

    // Datos de la cita
    private LocalDate  fechaCita;
    private LocalTime  horaInicio;
    private LocalTime  horaFin;
    private Integer    duracionMinutos;
    private TipoCita tipoCita;
    private String     motivoCita;
    private String     notasAdicionales;

    // Estado
    private EstadoCita estado;
    private String     motivoCancelacion;
    private LocalDateTime fechaCancelacion;

    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String        creadoPor;
    private String        actualizadoPor;
}