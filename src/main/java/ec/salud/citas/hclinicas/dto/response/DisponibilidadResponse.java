package ec.salud.citas.hclinicas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Respuesta de disponibilidad de horarios para un día (HU-017).
 * Retorna los slots libres y los ocupados del día consultado.
 */
@Data
@Builder
public class DisponibilidadResponse {

    private LocalDate fecha;
    private boolean   disponible;         // false si el slot exacto está ocupado
    private String    mensaje;

    // Slots ocupados del día (para que el frontend pinte el calendario)
    private List<SlotOcupado> slotsOcupados;

    @Data
    @Builder
    public static class SlotOcupado {
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String    pacienteNombre;
        private String    tipoCita;
        private String    estado;
    }
}