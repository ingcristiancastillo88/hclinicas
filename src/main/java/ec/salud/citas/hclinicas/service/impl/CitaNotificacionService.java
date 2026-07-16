package ec.salud.citas.hclinicas.service.impl;

import ec.salud.citas.hclinicas.entity.CitaMedica;
import ec.salud.citas.hclinicas.enumerado.EstadoCita;
import ec.salud.citas.hclinicas.repository.CitaMedicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de notificaciones automáticas de citas médicas.
 *
 * Envía recordatorios por email:
 *   - 3 días antes de la cita
 *   - 2 días antes de la cita
 *   - 1 día antes de la cita (recordatorio final)
 *
 * Se ejecuta todos los días a las 8:00 AM (hora Ecuador UTC-5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitaNotificacionService {

    private final CitaMedicaRepository citaRepo;
    private final EmailService emailService;

    /** Estados que NO reciben recordatorio */
    private static final List<EstadoCita> ESTADOS_EXCLUIDOS =
            List.of(EstadoCita.CANCELADA, EstadoCita.COMPLETADA);

    /**
     * Cron: todos los días a las 8:00 AM hora Ecuador (America/Guayaquil).
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "America/Guayaquil")
//    @Scheduled(initialDelay = 120000, fixedDelay = Long.MAX_VALUE)
    @Transactional(readOnly = true)
    public void enviarRecordatorios() {
        LocalDate hoy = LocalDate.now();
        log.info("Iniciando recordatorios de citas — {}", hoy);

        enviarParaDia(hoy.plusDays(3), 3);
        enviarParaDia(hoy.plusDays(2), 2);
        enviarParaDia(hoy.plusDays(1), 1);

        log.info("Recordatorios finalizados");
    }

    private void enviarParaDia(LocalDate fechaCita, int diasRestantes) {
        // Reutiliza el método findByFecha que ya tienes en el repositorio
        List<CitaMedica> citas = citaRepo.findByFecha(fechaCita)
                .stream()
                .filter(c -> !ESTADOS_EXCLUIDOS.contains(c.getEstado()))
                .toList();

        log.info("Citas en {} días ({}): {} para notificar",
                diasRestantes, fechaCita, citas.size());

        for (CitaMedica cita : citas) {
            try {
                // Obtener correo del paciente — tu entidad Paciente
                // usa el campo correo (igual que Usuario)
                String correo = obtenerCorreo(cita);
                if (correo == null || correo.isBlank()) {
                    log.warn("Cita {} — paciente sin correo, omitiendo", cita.getId());
                    continue;
                }
                emailService.enviarRecordatorioCitaMedica(
                        correo, cita, diasRestantes);
                log.info("Recordatorio ({} días) → {} | Cita {}",
                        diasRestantes, correo, cita.getId());
            } catch (Exception e) {
                log.error("Error recordatorio cita {}: {}",
                        cita.getId(), e.getMessage());
            }
        }
    }

    private String obtenerCorreo(CitaMedica cita) {
        var pac = cita.getPaciente();
        if (pac == null) return null;
        // Tu entidad Paciente tiene el campo correo (igual que en PacienteServiceImpl)
        try {
            return (String) pac.getClass().getMethod("getCorreo").invoke(pac);
        } catch (Exception ignored) {}
        // Fallback: getEmail()
        try {
            return (String) pac.getClass().getMethod("getEmail").invoke(pac);
        } catch (Exception ignored) {}
        return null;
    }
}