package ec.salud.citas.hclinicas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para crear o actualizar la historia clínica de un paciente.
 * Contiene los antecedentes gineco-obstétricos generales.
 */
@Data
public class CrearHistoriaClinicaRequest {

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteId;

    private String menarquia;
    private String cicloMenstrual;
    private String fechaUltimaMenstruacion;
    private Integer gestas;
    private Integer partos;
    private Integer cesareas;
    private Integer abortos;
    private Integer hijosVivos;
    private String metodoAnticonceptivo;
    private String ultimoPapanicolau;
    private String ultimaMamografia;
    private String observacionesGenerales;
}