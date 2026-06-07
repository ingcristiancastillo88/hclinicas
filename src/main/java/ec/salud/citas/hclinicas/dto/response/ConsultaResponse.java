package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConsultaResponse {

    private Long      id;
    private Long      historiaClinicaId;

    private LocalDate fechaConsulta;
    private String    motivoConsulta;

    // Signos vitales
    private Double  peso;
    private Double  talla;
    private Double  imc;                    // calculado
    private String  presionArterial;
    private Integer frecuenciaCardiaca;
    private Double  temperatura;
    private Integer saturacionOxigeno;
    private Integer semanasGestacion;

    // Examen y diagnóstico
    private String examenFisico;
    private String diagnosticoPrincipal;
    private String diagnosticoSecundario;
    private String codigoCie10;

    // Tratamiento
    private String    tratamiento;
    private String    medicacion;
    private String    indicaciones;
    private LocalDate proximaCita;
    private String    observaciones;

    // Archivos
    private List<ArchivoAdjuntoResponse> archivos;
    private Integer totalArchivos;

    // Auditoría
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String        creadoPor;
    private String        actualizadoPor;
}