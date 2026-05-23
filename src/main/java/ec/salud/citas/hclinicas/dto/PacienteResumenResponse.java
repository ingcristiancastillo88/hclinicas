package ec.salud.citas.hclinicas.dto;

import ec.salud.citas.hclinicas.enumerado.EstadoPaciente;
import ec.salud.citas.hclinicas.enumerado.GrupoSanguineo;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de resumen de Paciente para listados y búsquedas (HU-009).
 * Versión liviana que no incluye datos médicos completos,
 * optimizando el peso de la respuesta en las consultas paginadas.
 */
@Data
@Builder
public class PacienteResumenResponse {

    private Long id;
    private String cedula;
    private String historiaNúmero;
    private String nombres;
    private String apellidos;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private String celular;
    private String correo;
    private GrupoSanguineo grupoSanguineo;
    private EstadoPaciente estado;
}
