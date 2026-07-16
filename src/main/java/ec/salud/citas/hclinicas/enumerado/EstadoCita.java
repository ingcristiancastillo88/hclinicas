package ec.salud.citas.hclinicas.enumerado;

/**
 * Estado de una cita médica.
 * PROGRAMADA → CONFIRMADA → ATENDIDA
 *                         → CANCELADA (desde cualquier estado)
 *                         → NO_ASISTIO
 */
public enum EstadoCita {
    PROGRAMADA,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    COMPLETADA, NO_ASISTIO
}