package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO liviano para listar consultas en el historial (HU-011).
 */
@Data
@Builder
public class ConsultaResumenResponse {
    private Long id;
    private LocalDate fechaConsulta;
    private String motivoConsulta;
    private String diagnosticoPrincipal;
    private String codigoCie10;
    private Double peso;
    private String presionArterial;
    private Integer semanasGestacion;
    private Integer totalArchivos;
    private LocalDateTime fechaCreacion;
    private String creadoPor;
}
