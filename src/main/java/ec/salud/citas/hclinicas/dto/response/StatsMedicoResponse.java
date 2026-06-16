package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsMedicoResponse {
    private long totalPacientes;
    private long citasHoy;
    private long citasPendientesHoy;
    private long citasAtendidasHoy;
    private long citasPendientesSemana;
    private long consultasMes;
}
