package ec.salud.citas.hclinicas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa el scheduling y el procesamiento asíncrono.
 *
 * @EnableScheduling → permite @Scheduled en CitaNotificacionService
 * @EnableAsync      → permite @Async en EmailService
 *
 * Si ya tienes @EnableAsync en otra clase (ej: HClinicasApplication),
 * puedes quitar esa anotación de aquí para evitar duplicados.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}