package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsAdminResponse {
    private long totalPacientes;
    private long pacientesActivos;
    private long citasHoy;
    private long citasMes;
    private long citasPendientes;
    private long citasAtendidas;
    private long citasCanceladas;
    private long consultasMes;
    private long totalUsuarios;
}
