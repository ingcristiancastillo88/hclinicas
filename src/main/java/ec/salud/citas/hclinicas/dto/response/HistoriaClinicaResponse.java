package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoriaClinicaResponse {

    private Long id;

    // Datos del paciente (resumen)
    private Long pacienteId;
    private String pacienteCedula;
    private String pacienteNombreCompleto;
    private Integer pacienteEdad;

    // Antecedentes gineco-obstétricos
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

    // Stats
    private Long totalConsultas;

    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String creadoPor;
}